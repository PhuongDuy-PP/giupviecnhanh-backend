package com.gvn.service;

import com.gvn.dto.response.BannerResponse;
import com.gvn.entity.Banner;
import com.gvn.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BannerService {
    
    private final BannerRepository bannerRepository;
    private final FileStorageService fileStorageService;
    
    /**
     * Get all active banners ordered by display_order
     * @return List of active banners sorted by display_order ascending
     */
    @Transactional(readOnly = true)
    public List<BannerResponse> getActiveBanners() {
        try {
            List<Banner> banners = bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
            log.debug("Retrieved {} active banners", banners.size());
            
            return banners.stream()
                    .map(BannerResponse::fromBanner)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error retrieving banners: ", e);
            throw new RuntimeException("Failed to retrieve banners: " + e.getMessage());
        }
    }
    
    /**
     * Get all banners (including inactive) ordered by display_order
     * @return List of all banners sorted by display_order ascending
     */
    @Transactional(readOnly = true)
    public List<BannerResponse> getAllBanners() {
        try {
            List<Banner> banners = bannerRepository.findAllByOrderByDisplayOrderAsc();
            return banners.stream()
                    .map(BannerResponse::fromBanner)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error retrieving all banners: ", e);
            throw new RuntimeException("Failed to retrieve banners: " + e.getMessage());
        }
    }
    
    /**
     * Get banner by ID
     * @param id Banner ID
     * @return BannerResponse
     */
    @Transactional(readOnly = true)
    public BannerResponse getBannerById(UUID id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found with id: " + id));
        return BannerResponse.fromBanner(banner);
    }
    
    /**
     * Create a new banner
     * @param title Banner title
     * @param imageFile Banner image file
     * @param linkUrl Optional link URL
     * @param displayOrder Display order
     * @param isActive Active status
     * @return Created banner
     */
    @Transactional
    public BannerResponse createBanner(String title, MultipartFile imageFile, 
                                      String linkUrl, Integer displayOrder, Boolean isActive) {
        try {
            // Validate required fields
            if (title == null || title.trim().isEmpty()) {
                throw new RuntimeException("Title is required");
            }
            if (imageFile == null || imageFile.isEmpty()) {
                throw new RuntimeException("Banner image is required");
            }
            
            // Store banner image
            String imagePath = fileStorageService.storeFile(imageFile, "banners");
            String imageUrl = fileStorageService.getFileUrl(imagePath);
            
            // Create banner entity
            Banner banner = Banner.builder()
                    .title(title.trim())
                    .imageUrl(imageUrl)
                    .linkUrl(linkUrl != null && !linkUrl.trim().isEmpty() ? linkUrl.trim() : null)
                    .displayOrder(displayOrder != null ? displayOrder : 0)
                    .isActive(isActive != null ? isActive : true)
                    .build();
            
            Banner savedBanner = bannerRepository.save(banner);
            log.info("Banner created successfully: {}", savedBanner.getId());
            
            return BannerResponse.fromBanner(savedBanner);
        } catch (Exception e) {
            log.error("Error creating banner: ", e);
            throw new RuntimeException("Failed to create banner: " + e.getMessage());
        }
    }
    
    /**
     * Update an existing banner
     * @param id Banner ID
     * @param title Banner title (optional)
     * @param imageFile Banner image file (optional)
     * @param linkUrl Link URL (optional)
     * @param displayOrder Display order (optional)
     * @param isActive Active status (optional)
     * @return Updated banner
     */
    @Transactional
    public BannerResponse updateBanner(UUID id, String title, MultipartFile imageFile,
                                      String linkUrl, Integer displayOrder, Boolean isActive) {
        try {
            Banner banner = bannerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Banner not found with id: " + id));
            
            // Update title if provided
            if (title != null && !title.trim().isEmpty()) {
                banner.setTitle(title.trim());
            }
            
            // Update image if provided
            if (imageFile != null && !imageFile.isEmpty()) {
                // Delete old image
                if (banner.getImageUrl() != null) {
                    String oldPath = extractPathFromUrl(banner.getImageUrl());
                    if (oldPath != null) {
                        fileStorageService.deleteFile(oldPath);
                    }
                }
                
                // Store new image
                String imagePath = fileStorageService.storeFile(imageFile, "banners");
                banner.setImageUrl(fileStorageService.getFileUrl(imagePath));
            }
            
            // Update link URL if provided
            if (linkUrl != null) {
                banner.setLinkUrl(linkUrl.trim().isEmpty() ? null : linkUrl.trim());
            }
            
            // Update display order if provided
            if (displayOrder != null) {
                banner.setDisplayOrder(displayOrder);
            }
            
            // Update active status if provided
            if (isActive != null) {
                banner.setIsActive(isActive);
            }
            
            Banner updatedBanner = bannerRepository.save(banner);
            log.info("Banner updated successfully: {}", updatedBanner.getId());
            
            return BannerResponse.fromBanner(updatedBanner);
        } catch (Exception e) {
            log.error("Error updating banner: ", e);
            throw new RuntimeException("Failed to update banner: " + e.getMessage());
        }
    }
    
    /**
     * Delete a banner
     * @param id Banner ID
     */
    @Transactional
    public void deleteBanner(UUID id) {
        try {
            Banner banner = bannerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Banner not found with id: " + id));
            
            // Delete banner image file
            if (banner.getImageUrl() != null) {
                String imagePath = extractPathFromUrl(banner.getImageUrl());
                if (imagePath != null) {
                    fileStorageService.deleteFile(imagePath);
                }
            }
            
            // Delete banner from database
            bannerRepository.delete(banner);
            log.info("Banner deleted successfully: {}", id);
        } catch (Exception e) {
            log.error("Error deleting banner: ", e);
            throw new RuntimeException("Failed to delete banner: " + e.getMessage());
        }
    }
    
    /**
     * Extract relative path from full URL
     * @param url Full URL (e.g., /api/v1/files/banners/image.jpg)
     * @return Relative path (e.g., banners/image.jpg)
     */
    private String extractPathFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        // Remove /api/v1/files/ prefix if present
        if (url.startsWith("/api/v1/files/")) {
            return url.substring("/api/v1/files/".length());
        }
        // If it's already a relative path, return as is
        return url;
    }
}

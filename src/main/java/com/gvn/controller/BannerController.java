package com.gvn.controller;

import com.gvn.dto.response.ApiResponse;
import com.gvn.dto.response.BannerResponse;
import com.gvn.service.BannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
@Slf4j
public class BannerController {
    
    private final BannerService bannerService;
    
    /**
     * GET /api/v1/banners
     * Retrieve all active banners ordered by display_order
     * 
     * @return List of active banners
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getBanners() {
        try {
            List<BannerResponse> banners = bannerService.getActiveBanners();
            return ResponseEntity.ok(
                    ApiResponse.success(banners, "Banners retrieved successfully")
            );
        } catch (RuntimeException e) {
            log.error("Error retrieving banners: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve banners", 500));
        } catch (Exception e) {
            log.error("Unexpected error retrieving banners: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error", 500));
        }
    }
    
    /**
     * GET /api/v1/banners/all
     * Retrieve all banners (including inactive) - for admin use
     * 
     * @return List of all banners
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getAllBanners() {
        try {
            List<BannerResponse> banners = bannerService.getAllBanners();
            return ResponseEntity.ok(
                    ApiResponse.success(banners, "All banners retrieved successfully")
            );
        } catch (RuntimeException e) {
            log.error("Error retrieving all banners: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve banners", 500));
        } catch (Exception e) {
            log.error("Unexpected error retrieving all banners: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error", 500));
        }
    }
    
    /**
     * GET /api/v1/banners/{id}
     * Get banner by ID
     * 
     * @param id Banner ID
     * @return Banner details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> getBannerById(@PathVariable UUID id) {
        try {
            BannerResponse banner = bannerService.getBannerById(id);
            return ResponseEntity.ok(
                    ApiResponse.success(banner, "Banner retrieved successfully")
            );
        } catch (RuntimeException e) {
            log.error("Error retrieving banner: ", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage(), 404));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve banner", 500));
        } catch (Exception e) {
            log.error("Unexpected error retrieving banner: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error", 500));
        }
    }
    
    /**
     * POST /api/v1/banners
     * Create a new banner
     * 
     * @param title Banner title
     * @param image Banner image file
     * @param link_url Optional link URL
     * @param display_order Display order (default: 0)
     * @param is_active Active status (default: true)
     * @return Created banner
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BannerResponse>> createBanner(
            @RequestParam("title") String title,
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "link_url", required = false) String linkUrl,
            @RequestParam(value = "display_order", required = false) Integer displayOrder,
            @RequestParam(value = "is_active", required = false) Boolean isActive
    ) {
        try {
            // Validate required fields
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Title is required", 400));
            }
            if (image == null || image.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Banner image is required", 400));
            }
            
            // Validate file size (10MB max)
            long maxFileSize = 10 * 1024 * 1024; // 10MB
            if (image.getSize() > maxFileSize) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Banner image exceeds maximum size of 10MB", 400));
            }
            
            BannerResponse banner = bannerService.createBanner(title, image, linkUrl, displayOrder, isActive);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(banner, "Banner created successfully"));
        } catch (RuntimeException e) {
            log.error("Error creating banner: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error creating banner: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create banner", 500));
        }
    }
    
    /**
     * PUT /api/v1/banners/{id}
     * Update an existing banner
     * 
     * @param id Banner ID
     * @param title Banner title (optional)
     * @param image Banner image file (optional)
     * @param link_url Link URL (optional)
     * @param display_order Display order (optional)
     * @param is_active Active status (optional)
     * @return Updated banner
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> updateBanner(
            @PathVariable UUID id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "link_url", required = false) String linkUrl,
            @RequestParam(value = "display_order", required = false) Integer displayOrder,
            @RequestParam(value = "is_active", required = false) Boolean isActive
    ) {
        try {
            // Validate file size if image is provided
            if (image != null && !image.isEmpty()) {
                long maxFileSize = 10 * 1024 * 1024; // 10MB
                if (image.getSize() > maxFileSize) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("Banner image exceeds maximum size of 10MB", 400));
                }
            }
            
            BannerResponse banner = bannerService.updateBanner(id, title, image, linkUrl, displayOrder, isActive);
            return ResponseEntity.ok(
                    ApiResponse.success(banner, "Banner updated successfully")
            );
        } catch (RuntimeException e) {
            log.error("Error updating banner: ", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage(), 404));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error updating banner: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update banner", 500));
        }
    }
    
    /**
     * DELETE /api/v1/banners/{id}
     * Delete a banner
     * 
     * @param id Banner ID
     * @return Success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteBanner(@PathVariable UUID id) {
        try {
            bannerService.deleteBanner(id);
            return ResponseEntity.ok(
                    ApiResponse.success(null, "Banner deleted successfully")
            );
        } catch (RuntimeException e) {
            log.error("Error deleting banner: ", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage(), 404));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error deleting banner: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete banner", 500));
        }
    }
}

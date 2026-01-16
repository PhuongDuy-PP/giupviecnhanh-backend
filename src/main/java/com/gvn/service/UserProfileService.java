package com.gvn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gvn.dto.request.ChangePasswordRequest;
import com.gvn.dto.request.UpdateProfileRequest;
import com.gvn.dto.response.UserResponse;
import com.gvn.entity.PartnerProfile;
import com.gvn.entity.User;
import com.gvn.repository.PartnerProfileRepository;
import com.gvn.repository.SessionRepository;
import com.gvn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {
    
    private final UserRepository userRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new RuntimeException("User not authenticated");
    }
    
    public UserResponse getProfile() {
        User user = getCurrentUser();
        return mapToUserResponse(user);
    }
    
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();
        
        if (request.getFull_name() != null) {
            user.setFullName(request.getFull_name());
        }
        if (request.getEmail() != null) {
            // Check if email already exists for another user
            userRepository.findByEmail(request.getEmail())
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(user.getId())) {
                            throw new RuntimeException("Email already exists");
                        }
                    });
            user.setEmail(request.getEmail());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getBirthday() != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                user.setBirthday(LocalDate.parse(request.getBirthday(), formatter));
            } catch (Exception e) {
                throw new RuntimeException("Invalid birthday format. Expected: yyyy/MM/dd");
            }
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        
        User savedUser = userRepository.save(user);
        log.info("Profile updated for user: {}", savedUser.getId());
        return mapToUserResponse(savedUser);
    }
    
    @Transactional
    public UserResponse updateAvatar(MultipartFile image) {
        User user = getCurrentUser();
        
        try {
            // Delete old avatar if exists
            if (user.getAvatarUrl() != null) {
                String oldPath = extractPathFromUrl(user.getAvatarUrl());
                fileStorageService.deleteFile(oldPath);
            }
            
            // Store new avatar
            String avatarPath = fileStorageService.storeFile(image, "avatars");
            String avatarUrl = fileStorageService.getFileUrl(avatarPath);
            user.setAvatarUrl(avatarUrl);
            
            User savedUser = userRepository.save(user);
            log.info("Avatar updated for user: {}", savedUser.getId());
            return mapToUserResponse(savedUser);
        } catch (Exception e) {
            log.error("Error updating avatar: ", e);
            throw new RuntimeException("Failed to update avatar: " + e.getMessage());
        }
    }
    
    @Transactional
    public void deleteAvatar() {
        User user = getCurrentUser();
        
        if (user.getAvatarUrl() != null) {
            String oldPath = extractPathFromUrl(user.getAvatarUrl());
            fileStorageService.deleteFile(oldPath);
            user.setAvatarUrl(null);
            userRepository.save(user);
            log.info("Avatar deleted for user: {}", user.getId());
        }
    }
    
    @Transactional
    public void deleteProfile() {
        User user = getCurrentUser();
        UUID userId = user.getId();
        String phoneNumber = user.getPhoneNumber();
        
        log.info("Starting profile deletion for user: {} with phone: {}", userId, phoneNumber);
        
        // Delete all sessions for this user first (to avoid foreign key constraint violation)
        int deletedSessions = sessionRepository.findByUser(user).size();
        sessionRepository.deleteByUserId(userId);
        log.info("Deleted {} sessions for user: {}", deletedSessions, userId);
        
        // Delete avatar if exists
        if (user.getAvatarUrl() != null) {
            String oldPath = extractPathFromUrl(user.getAvatarUrl());
            fileStorageService.deleteFile(oldPath);
            log.info("Deleted avatar file for user: {}", userId);
        }
        
        // Delete partner profile and related files if exists
        partnerProfileRepository.findByUser(user).ifPresent(partnerProfile -> {
            deletePartnerProfileFiles(partnerProfile);
            partnerProfileRepository.delete(partnerProfile);
            log.info("Deleted partner profile for user: {}", userId);
        });
        
        // Use native query delete to ensure immediate database deletion
        // This bypasses all JPA caching and ensures direct database operation
        userRepository.deleteByPhoneNumber(phoneNumber);
        
        // Flush and clear entity manager to ensure changes are persisted immediately
        entityManager.flush();
        entityManager.clear();
        
        // Verify deletion using native query for accurate check
        Optional<User> deletedUserCheck = userRepository.findByPhoneNumber(phoneNumber);
        if (deletedUserCheck.isPresent()) {
            log.error("User with phone {} still exists after deletion! This should not happen.", phoneNumber);
        } else {
            log.info("Profile deleted successfully for user: {} with phone: {}", userId, phoneNumber);
        }
    }
    
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrent_password(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Update password
        String newPasswordHash = passwordEncoder.encode(request.getNew_password());
        user.setPassword(newPasswordHash);
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getId());
    }
    
    @Transactional
    public UserResponse registerAsPartner(
            String fullName,
            String gender,
            String birthday,
            String address,
            String cccd,
            Integer yearsOfExperience,
            MultipartFile cccdFrontImage,
            MultipartFile cccdBackImage,
            MultipartFile[] healthCertificates
    ) {
        User user = getCurrentUser();
        
        if (user.getHasPartnerProfile()) {
            throw new RuntimeException("User already has a partner profile");
        }
        
        try {
            // Parse birthday
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            LocalDate birthdayDate;
            try {
                birthdayDate = LocalDate.parse(birthday, formatter);
            } catch (Exception e) {
                log.error("Invalid birthday format: {}", birthday, e);
                throw new RuntimeException("Invalid birthday format. Expected: yyyy/MM/dd");
            }
            
            // Store images (reduced logging for performance)
            String cccdFrontPath;
            try {
                cccdFrontPath = fileStorageService.storeFile(cccdFrontImage, "documents/cccd");
            } catch (Exception e) {
                log.error("Failed to store CCCD front image: ", e);
                throw new RuntimeException("Failed to store CCCD front image: " + e.getMessage());
            }
            
            String cccdBackPath;
            try {
                cccdBackPath = fileStorageService.storeFile(cccdBackImage, "documents/cccd");
            } catch (Exception e) {
                log.error("Failed to store CCCD back image: ", e);
                // Clean up front image if back image fails
                try {
                    fileStorageService.deleteFile(cccdFrontPath);
                } catch (Exception cleanupEx) {
                    log.warn("Failed to cleanup front image after back image failure", cleanupEx);
                }
                throw new RuntimeException("Failed to store CCCD back image: " + e.getMessage());
            }
            
            List<String> healthCertUrls = new ArrayList<>();
            if (healthCertificates != null && healthCertificates.length > 0) {
                try {
                    healthCertUrls = fileStorageService.storeMultipleFiles(healthCertificates, "documents/health");
                } catch (Exception e) {
                    log.error("Failed to store health certificates: ", e);
                    // Clean up CCCD images if health certs fail
                    try {
                        fileStorageService.deleteFile(cccdFrontPath);
                        fileStorageService.deleteFile(cccdBackPath);
                    } catch (Exception cleanupEx) {
                        log.warn("Failed to cleanup CCCD images after health cert failure", cleanupEx);
                    }
                    throw new RuntimeException("Failed to store health certificates: " + e.getMessage());
                }
            }
            
            // Convert health certificates URLs to JSON
            String healthCertificatesJson;
            try {
                healthCertificatesJson = objectMapper.writeValueAsString(healthCertUrls);
            } catch (Exception e) {
                log.error("Failed to convert health certificates to JSON: ", e);
                // Clean up all files
                try {
                    fileStorageService.deleteFile(cccdFrontPath);
                    fileStorageService.deleteFile(cccdBackPath);
                    for (String url : healthCertUrls) {
                        String path = extractPathFromUrl(url);
                        if (path != null) {
                            fileStorageService.deleteFile(path);
                        }
                    }
                } catch (Exception cleanupEx) {
                    log.warn("Failed to cleanup files after JSON conversion failure", cleanupEx);
                }
                throw new RuntimeException("Failed to process health certificates: " + e.getMessage());
            }
            
            // Create and save partner profile
            PartnerProfile partnerProfile = PartnerProfile.builder()
                    .user(user)
                    .fullName(fullName)
                    .gender(gender)
                    .birthday(birthdayDate)
                    .address(address)
                    .cccd(cccd)
                    .yearsOfExperience(yearsOfExperience)
                    .cccdFrontImageUrl(fileStorageService.getFileUrl(cccdFrontPath))
                    .cccdBackImageUrl(fileStorageService.getFileUrl(cccdBackPath))
                    .healthCertificatesUrls(healthCertificatesJson)
                    .profileStatus(PartnerProfile.ProfileStatus.PENDING)
                    .build();
            
            try {
                partnerProfileRepository.save(partnerProfile);
            } catch (Exception e) {
                log.error("Failed to save partner profile to database: ", e);
                // Clean up all files
                try {
                    fileStorageService.deleteFile(cccdFrontPath);
                    fileStorageService.deleteFile(cccdBackPath);
                    for (String url : healthCertUrls) {
                        String path = extractPathFromUrl(url);
                        if (path != null) {
                            fileStorageService.deleteFile(path);
                        }
                    }
                } catch (Exception cleanupEx) {
                    log.warn("Failed to cleanup files after database save failure", cleanupEx);
                }
                throw new RuntimeException("Failed to save partner profile: " + e.getMessage());
            }
            
            // Update user
            try {
                user.setHasPartnerProfile(true);
                userRepository.save(user);
            } catch (Exception e) {
                log.error("Failed to update user: ", e);
                // Note: Files and partner profile are already saved, so we don't cleanup here
                throw new RuntimeException("Failed to update user: " + e.getMessage());
            }
            
            log.info("Partner profile registered successfully for user: {}", user.getId());
            return mapToUserResponse(user);
        } catch (RuntimeException e) {
            // Re-throw RuntimeException as-is (already logged)
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error registering partner profile for user {}: ", user.getId(), e);
            log.error("Exception class: {}, message: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                log.error("Caused by: {}", e.getCause().getClass().getName(), e.getCause());
            }
            throw new RuntimeException("Failed to register partner profile: " + e.getMessage() + 
                    " (Type: " + e.getClass().getSimpleName() + ")", e);
        }
    }
    
    @Transactional
    public UserResponse updatePartnerProfile(
            String fullName,
            String gender,
            String birthday,
            String address,
            String cccd,
            Integer yearsOfExperience,
            MultipartFile cccdFrontImage,
            MultipartFile cccdBackImage,
            MultipartFile[] healthCertificates
    ) {
        User user = getCurrentUser();
        
        PartnerProfile partnerProfile = partnerProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Partner profile not found"));
        
        try {
            // Parse birthday
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            LocalDate birthdayDate = LocalDate.parse(birthday, formatter);
            
            // Update fields
            if (fullName != null) partnerProfile.setFullName(fullName);
            if (gender != null) partnerProfile.setGender(gender);
            partnerProfile.setBirthday(birthdayDate);
            if (address != null) partnerProfile.setAddress(address);
            if (cccd != null) partnerProfile.setCccd(cccd);
            if (yearsOfExperience != null) partnerProfile.setYearsOfExperience(yearsOfExperience);
            
            // Update images if provided
            if (cccdFrontImage != null && !cccdFrontImage.isEmpty()) {
                if (partnerProfile.getCccdFrontImageUrl() != null) {
                    String oldPath = extractPathFromUrl(partnerProfile.getCccdFrontImageUrl());
                    if (oldPath != null) {
                        fileStorageService.deleteFile(oldPath);
                    }
                }
                String newPath = fileStorageService.storeFile(cccdFrontImage, "documents/cccd");
                partnerProfile.setCccdFrontImageUrl(fileStorageService.getFileUrl(newPath));
            }
            
            if (cccdBackImage != null && !cccdBackImage.isEmpty()) {
                if (partnerProfile.getCccdBackImageUrl() != null) {
                    String oldPath = extractPathFromUrl(partnerProfile.getCccdBackImageUrl());
                    if (oldPath != null) {
                        fileStorageService.deleteFile(oldPath);
                    }
                }
                String newPath = fileStorageService.storeFile(cccdBackImage, "documents/cccd");
                partnerProfile.setCccdBackImageUrl(fileStorageService.getFileUrl(newPath));
            }
            
            if (healthCertificates != null && healthCertificates.length > 0) {
                // Delete old health certificates
                if (partnerProfile.getHealthCertificatesUrls() != null) {
                    try {
                        List<String> oldUrls = objectMapper.readValue(
                                partnerProfile.getHealthCertificatesUrls(),
                                new TypeReference<List<String>>() {}
                        );
                        for (String oldUrl : oldUrls) {
                            String oldPath = extractPathFromUrl(oldUrl);
                            if (oldPath != null) {
                                fileStorageService.deleteFile(oldPath);
                            }
                        }
                    } catch (JsonProcessingException e) {
                        log.warn("Error parsing old health certificates URLs", e);
                    }
                }
                
                // Store new health certificates (returns URLs)
                List<String> newUrls = fileStorageService.storeMultipleFiles(healthCertificates, "documents/health");
                partnerProfile.setHealthCertificatesUrls(objectMapper.writeValueAsString(newUrls));
            }
            
            // Reset status to PENDING when updating
            partnerProfile.setProfileStatus(PartnerProfile.ProfileStatus.PENDING);
            
            partnerProfileRepository.save(partnerProfile);
            
            log.info("Partner profile updated for user: {}", user.getId());
            return mapToUserResponse(user);
        } catch (Exception e) {
            log.error("Error updating partner profile: ", e);
            throw new RuntimeException("Failed to update partner profile: " + e.getMessage());
        }
    }
    
    private UserResponse mapToUserResponse(User user) {
        PartnerProfile partnerProfile = null;
        if (user.getHasPartnerProfile()) {
            partnerProfile = partnerProfileRepository.findByUser(user).orElse(null);
        }
        
        return UserResponse.builder()
                .id(user.getId())
                .full_name(user.getFullName())
                .gender(user.getGender())
                .email(user.getEmail())
                .birthday(user.getBirthday())
                .address(user.getAddress())
                .phone(user.getPhoneNumber())
                .avatar_url(user.getAvatarUrl())
                .has_partner_profile(user.getHasPartnerProfile())
                .partner_profile(UserResponse.fromPartnerProfile(partnerProfile))
                .build();
    }
    
    private void deletePartnerProfileFiles(PartnerProfile partnerProfile) {
        if (partnerProfile.getCccdFrontImageUrl() != null) {
            String path = extractPathFromUrl(partnerProfile.getCccdFrontImageUrl());
            if (path != null) {
                fileStorageService.deleteFile(path);
            }
        }
        if (partnerProfile.getCccdBackImageUrl() != null) {
            String path = extractPathFromUrl(partnerProfile.getCccdBackImageUrl());
            if (path != null) {
                fileStorageService.deleteFile(path);
            }
        }
        if (partnerProfile.getHealthCertificatesUrls() != null) {
            try {
                List<String> urls = objectMapper.readValue(
                        partnerProfile.getHealthCertificatesUrls(),
                        new TypeReference<List<String>>() {}
                );
                for (String url : urls) {
                    String path = extractPathFromUrl(url);
                    if (path != null) {
                        fileStorageService.deleteFile(path);
                    }
                }
            } catch (JsonProcessingException e) {
                log.warn("Error parsing health certificates URLs for deletion", e);
            }
        }
    }
    
    private String extractPathFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        // Remove /api/v1/files/ prefix if present
        if (url.startsWith("/api/v1/files/")) {
            return url.substring("/api/v1/files/".length());
        }
        // If it's already a relative path, return as is
        return url;
    }
}

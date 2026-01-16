package com.gvn.controller;

import com.gvn.dto.request.ChangePasswordRequest;
import com.gvn.dto.request.UpdateProfileRequest;
import com.gvn.dto.response.ApiResponse;
import com.gvn.dto.response.UserResponse;
import com.gvn.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {
    
    private final UserProfileService userProfileService;
    
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        try {
            UserResponse profile = userProfileService.getProfile();
            return ResponseEntity.ok(ApiResponse.success(profile, "Profile retrieved successfully"));
        } catch (RuntimeException e) {
            log.error("Error getting profile: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), 401));
        } catch (Exception e) {
            log.error("Unexpected error getting profile: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve profile", 500));
        }
    }
    
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        try {
            UserResponse updatedProfile = userProfileService.updateProfile(request);
            return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Profile updated successfully"));
        } catch (RuntimeException e) {
            log.error("Error updating profile: ", e);
            if (e.getMessage().contains("Email already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(e.getMessage(), 409));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error updating profile: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update profile", 500));
        }
    }
    
    @PutMapping("/profile/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> updateAvatar(
            @RequestParam("image") MultipartFile image
    ) {
        try {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Image file is required", 400));
            }
            
            UserResponse updatedProfile = userProfileService.updateAvatar(image);
            return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Avatar updated successfully"));
        } catch (RuntimeException e) {
            log.error("Error updating avatar: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error updating avatar: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update avatar", 500));
        }
    }
    
    @DeleteMapping("/profile/avatar")
    public ResponseEntity<ApiResponse<Object>> deleteAvatar() {
        try {
            userProfileService.deleteAvatar();
            return ResponseEntity.ok(ApiResponse.success(null, "Avatar deleted successfully"));
        } catch (RuntimeException e) {
            log.error("Error deleting avatar: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), 401));
        } catch (Exception e) {
            log.error("Unexpected error deleting avatar: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete avatar", 500));
        }
    }
    
    @DeleteMapping("/profile")
    public ResponseEntity<ApiResponse<Object>> deleteProfile() {
        try {
            userProfileService.deleteProfile();
            return ResponseEntity.ok(ApiResponse.success(null, "Profile deleted successfully"));
        } catch (RuntimeException e) {
            log.error("Error deleting profile: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), 401));
        } catch (Exception e) {
            log.error("Unexpected error deleting profile: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete profile", 500));
        }
    }
    
    @PostMapping("/profile/change-password")
    public ResponseEntity<ApiResponse<Object>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        try {
            userProfileService.changePassword(request);
            return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
        } catch (RuntimeException e) {
            log.error("Error changing password: ", e);
            if (e.getMessage().contains("Current password is incorrect")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(e.getMessage(), 400));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error changing password: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to change password", 500));
        }
    }
    
    @PostMapping("/applicant-register")
    public ResponseEntity<ApiResponse<UserResponse>> registerAsPartner(
            @RequestParam("full_name") String fullName,
            @RequestParam("gender") String gender,
            @RequestParam("birthday") String birthday,
            @RequestParam("address") String address,
            @RequestParam("cccd") String cccd,
            @RequestParam("years_of_experience") Integer yearsOfExperience,
            @RequestParam("cccd_front_image") MultipartFile cccdFrontImage,
            @RequestParam("cccd_back_image") MultipartFile cccdBackImage,
            @RequestParam(value = "health_certificates", required = false) MultipartFile[] healthCertificates
    ) {
        try {
            log.info("Received applicant registration request. Full name: {}, CCCD: {}", fullName, cccd);
            
            // Validate required images
            if (cccdFrontImage == null || cccdFrontImage.isEmpty()) {
                log.warn("CCCD front image is missing");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("CCCD front image is required", 400));
            }
            if (cccdBackImage == null || cccdBackImage.isEmpty()) {
                log.warn("CCCD back image is missing");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("CCCD back image is required", 400));
            }
            
            // Validate file sizes (10MB = 10 * 1024 * 1024 bytes)
            long maxFileSize = 10 * 1024 * 1024; // 10MB
            if (cccdFrontImage.getSize() > maxFileSize) {
                log.warn("CCCD front image too large: {} bytes", cccdFrontImage.getSize());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("CCCD front image exceeds maximum size of 10MB", 400));
            }
            if (cccdBackImage.getSize() > maxFileSize) {
                log.warn("CCCD back image too large: {} bytes", cccdBackImage.getSize());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("CCCD back image exceeds maximum size of 10MB", 400));
            }
            
            // Validate health certificates if provided
            if (healthCertificates != null) {
                for (MultipartFile cert : healthCertificates) {
                    if (cert != null && !cert.isEmpty() && cert.getSize() > maxFileSize) {
                        log.warn("Health certificate too large: {} bytes", cert.getSize());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error("One or more health certificates exceed maximum size of 10MB", 400));
                    }
                }
            }
            
            log.info("File validation passed. Front: {} bytes, Back: {} bytes, Health certs: {}", 
                    cccdFrontImage.getSize(), cccdBackImage.getSize(), 
                    healthCertificates != null ? healthCertificates.length : 0);
            
            UserResponse response = userProfileService.registerAsPartner(
                    fullName, gender, birthday, address, cccd, yearsOfExperience,
                    cccdFrontImage, cccdBackImage, healthCertificates
            );
            log.info("Partner registration completed successfully");
            return ResponseEntity.ok(ApiResponse.success(response, "Partner registration submitted successfully"));
        } catch (RuntimeException e) {
            log.error("Error registering as partner: ", e);
            log.error("Exception stack trace: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error registering as partner: ", e);
            log.error("Exception class: {}, message: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                log.error("Caused by: ", e.getCause());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to register as partner: " + e.getMessage(), 500));
        }
    }
    
    @PutMapping("/partner-profile")
    public ResponseEntity<ApiResponse<UserResponse>> updatePartnerProfile(
            @RequestParam("full_name") String fullName,
            @RequestParam("gender") String gender,
            @RequestParam("birthday") String birthday,
            @RequestParam("address") String address,
            @RequestParam("cccd") String cccd,
            @RequestParam("years_of_experience") Integer yearsOfExperience,
            @RequestParam(value = "cccd_front_image", required = false) MultipartFile cccdFrontImage,
            @RequestParam(value = "cccd_back_image", required = false) MultipartFile cccdBackImage,
            @RequestParam(value = "health_certificates", required = false) MultipartFile[] healthCertificates
    ) {
        try {
            UserResponse response = userProfileService.updatePartnerProfile(
                    fullName, gender, birthday, address, cccd, yearsOfExperience,
                    cccdFrontImage, cccdBackImage, healthCertificates
            );
            return ResponseEntity.ok(ApiResponse.success(response, "Partner profile updated successfully"));
        } catch (RuntimeException e) {
            log.error("Error updating partner profile: ", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage(), 404));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error updating partner profile: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update partner profile", 500));
        }
    }
}

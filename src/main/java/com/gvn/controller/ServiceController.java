package com.gvn.controller;

import com.gvn.dto.response.ApiResponse;
import com.gvn.dto.response.AvailableServiceResponse;
import com.gvn.dto.response.CategoryResponse;
import com.gvn.dto.response.OptionResponse;
import com.gvn.dto.response.ServiceResponse;
import com.gvn.entity.User;
import com.gvn.service.ServiceService;
import com.gvn.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Slf4j
public class ServiceController {
    
    private final ServiceService serviceService;
    private final UserProfileService userProfileService;
    
    /**
     * GET /api/v1/services/get-all-category
     * Get all active categories ordered by display_order
     */
    @GetMapping("/get-all-category")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        try {
            List<CategoryResponse> categories = serviceService.getAllCategories();
            return ResponseEntity.ok(
                    ApiResponse.success(categories, "Success")
            );
        } catch (RuntimeException e) {
            log.error("Error retrieving categories: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve categories", 500));
        } catch (Exception e) {
            log.error("Unexpected error retrieving categories: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error", 500));
        }
    }
    
    /**
     * GET /api/v1/services/get-service-by-category
     * Get services by category ID
     */
    @GetMapping("/get-service-by-category")
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getServicesByCategory(
            @RequestParam("category_id") Integer categoryId
    ) {
        try {
            if (categoryId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("category_id is required", 400));
            }
            
            List<ServiceResponse> services = serviceService.getServicesByCategory(categoryId);
            return ResponseEntity.ok(
                    ApiResponse.success(services, "Success")
            );
        } catch (RuntimeException e) {
            log.error("Error retrieving services by category: ", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage(), 404));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error retrieving services by category: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error", 500));
        }
    }
    
    /**
     * GET /api/v1/services/available
     * Get available services for partner (grouped by category)
     * Only accessible by partners
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<AvailableServiceResponse>>> getAvailableServices() {
        try {
            // Get current user
            User user = userProfileService.getCurrentUser();
            
            // Check if user is a partner
            if (!"partner".equals(user.getUserType()) && !user.getHasPartnerProfile()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("This endpoint is only available for partners", 403));
            }
            
            List<AvailableServiceResponse> availableServices = serviceService.getAvailableServicesForPartner(user);
            return ResponseEntity.ok(
                    ApiResponse.success(availableServices, "Success")
            );
        } catch (RuntimeException e) {
            log.error("Error retrieving available services: ", e);
            if (e.getMessage().contains("not authenticated")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(e.getMessage(), 401));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve available services", 500));
        } catch (Exception e) {
            log.error("Unexpected error retrieving available services: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error", 500));
        }
    }
    
    /**
     * GET /api/v1/services/categories/{id}
     * Get category by ID
     */
    @GetMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Integer id) {
        try {
            CategoryResponse category = serviceService.getCategoryById(id);
            return ResponseEntity.ok(
                    ApiResponse.success(category, "Category retrieved successfully")
            );
        } catch (RuntimeException e) {
            log.error("Error retrieving category: ", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage(), 404));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error retrieving category: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error", 500));
        }
    }
    
    /**
     * POST /api/v1/services/categories
     * Create a new category
     */
    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @RequestParam("name") String name,
            @RequestParam(value = "icon", required = false) MultipartFile iconFile,
            @RequestParam(value = "icon_url", required = false) String iconUrl,
            @RequestParam(value = "display_order", required = false) Integer displayOrder,
            @RequestParam(value = "is_active", required = false) Boolean isActive
    ) {
        try {
            // Validate required fields
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Name is required", 400));
            }
            
            // Validate file size if provided (5MB max)
            if (iconFile != null && !iconFile.isEmpty()) {
                long maxFileSize = 5 * 1024 * 1024; // 5MB
                if (iconFile.getSize() > maxFileSize) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("Icon file exceeds maximum size of 5MB", 400));
                }
            }
            
            CategoryResponse category = serviceService.createCategory(name, iconFile, iconUrl, displayOrder, isActive);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(category, "Category created successfully"));
        } catch (RuntimeException e) {
            log.error("Error creating category: ", e);
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(e.getMessage(), 409));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error creating category: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create category", 500));
        }
    }
    
    /**
     * PUT /api/v1/services/categories/{id}
     * Update an existing category
     */
    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Integer id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "icon", required = false) MultipartFile iconFile,
            @RequestParam(value = "icon_url", required = false) String iconUrl,
            @RequestParam(value = "display_order", required = false) Integer displayOrder,
            @RequestParam(value = "is_active", required = false) Boolean isActive
    ) {
        try {
            // Validate file size if provided (5MB max)
            if (iconFile != null && !iconFile.isEmpty()) {
                long maxFileSize = 5 * 1024 * 1024; // 5MB
                if (iconFile.getSize() > maxFileSize) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("Icon file exceeds maximum size of 5MB", 400));
                }
            }
            
            CategoryResponse category = serviceService.updateCategory(id, name, iconFile, iconUrl, displayOrder, isActive);
            return ResponseEntity.ok(
                    ApiResponse.success(category, "Category updated successfully")
            );
        } catch (RuntimeException e) {
            log.error("Error updating category: ", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage(), 404));
            }
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(e.getMessage(), 409));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error updating category: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update category", 500));
        }
    }
    
    /**
     * DELETE /api/v1/services/categories/{id}
     * Delete a category
     */
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteCategory(@PathVariable Integer id) {
        try {
            serviceService.deleteCategory(id);
            return ResponseEntity.ok(
                    ApiResponse.success(null, "Category deleted successfully")
            );
        } catch (RuntimeException e) {
            log.error("Error deleting category: ", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage(), 404));
            }
            if (e.getMessage().contains("being used")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(e.getMessage(), 400));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error deleting category: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete category", 500));
        }
    }
    
    /**
     * GET /api/v1/services/{id}
     * Get service by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceResponse>> getServiceById(@PathVariable Integer id) {
        try {
            ServiceResponse service = serviceService.getServiceById(id);
            return ResponseEntity.ok(
                    ApiResponse.success(service, "Service retrieved successfully")
            );
        } catch (RuntimeException e) {
            log.error("Error retrieving service: ", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage(), 404));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error retrieving service: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error", 500));
        }
    }
    
    /**
     * POST /api/v1/services
     * Create a new service
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ServiceResponse>> createService(
            @RequestParam("code") String code,
            @RequestParam("name") String name,
            @RequestParam(value = "short_description", required = false) String shortDescription,
            @RequestParam(value = "icon", required = false) MultipartFile iconFile,
            @RequestParam(value = "icon_url", required = false) String iconUrl,
            @RequestParam("category_id") Integer categoryId,
            @RequestParam(value = "is_active", required = false) Boolean isActive
    ) {
        try {
            // Validate required fields
            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Code is required", 400));
            }
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Name is required", 400));
            }
            if (categoryId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Category ID is required", 400));
            }
            
            // Validate file size if provided (5MB max)
            if (iconFile != null && !iconFile.isEmpty()) {
                long maxFileSize = 5 * 1024 * 1024; // 5MB
                if (iconFile.getSize() > maxFileSize) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("Icon file exceeds maximum size of 5MB", 400));
                }
            }
            
            ServiceResponse service = serviceService.createService(code, name, shortDescription, iconFile, iconUrl, categoryId, isActive);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(service, "Service created successfully"));
        } catch (RuntimeException e) {
            log.error("Error creating service: ", e);
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(e.getMessage(), 409));
            }
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage(), 404));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error creating service: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create service", 500));
        }
    }
    
    /**
     * PUT /api/v1/services/{id}
     * Update an existing service
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceResponse>> updateService(
            @PathVariable Integer id,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "short_description", required = false) String shortDescription,
            @RequestParam(value = "icon", required = false) MultipartFile iconFile,
            @RequestParam(value = "icon_url", required = false) String iconUrl,
            @RequestParam(value = "category_id", required = false) Integer categoryId,
            @RequestParam(value = "is_active", required = false) Boolean isActive
    ) {
        try {
            // Validate file size if provided (5MB max)
            if (iconFile != null && !iconFile.isEmpty()) {
                long maxFileSize = 5 * 1024 * 1024; // 5MB
                if (iconFile.getSize() > maxFileSize) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("Icon file exceeds maximum size of 5MB", 400));
                }
            }
            
            ServiceResponse service = serviceService.updateService(id, code, name, shortDescription, iconFile, iconUrl, categoryId, isActive);
            return ResponseEntity.ok(
                    ApiResponse.success(service, "Service updated successfully")
            );
        } catch (RuntimeException e) {
            log.error("Error updating service: ", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage(), 404));
            }
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(e.getMessage(), 409));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error updating service: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update service", 500));
        }
    }
    
    /**
     * DELETE /api/v1/services/{id}
     * Delete a service
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteService(@PathVariable Integer id) {
        try {
            serviceService.deleteService(id);
            return ResponseEntity.ok(
                    ApiResponse.success(null, "Service deleted successfully")
            );
        } catch (RuntimeException e) {
            log.error("Error deleting service: ", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage(), 404));
            }
            if (e.getMessage().contains("being used")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(e.getMessage(), 400));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error deleting service: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete service", 500));
        }
    }
}

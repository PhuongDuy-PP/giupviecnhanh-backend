package com.gvn.service;

import com.gvn.dto.response.AvailableServiceResponse;
import com.gvn.dto.response.CategoryResponse;
import com.gvn.dto.response.OptionResponse;
import com.gvn.dto.response.ServiceResponse;
import com.gvn.entity.Category;
import com.gvn.entity.PartnerService;
import com.gvn.entity.ServiceOption;
import com.gvn.entity.User;
import com.gvn.repository.CategoryRepository;
import com.gvn.repository.PartnerServiceRepository;
import com.gvn.repository.ServiceOptionRepository;
import com.gvn.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceService {
    
    private final CategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceOptionRepository serviceOptionRepository;
    private final PartnerServiceRepository partnerServiceRepository;
    private final FileStorageService fileStorageService;
    
    /**
     * Get all active categories ordered by display_order
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        try {
            List<Category> categories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
            return categories.stream()
                    .map(CategoryResponse::fromCategory)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error retrieving categories: ", e);
            throw new RuntimeException("Failed to retrieve categories: " + e.getMessage());
        }
    }
    
    /**
     * Get services by category ID
     */
    @Transactional(readOnly = true)
    public List<ServiceResponse> getServicesByCategory(Integer categoryId) {
        try {
            // Validate category exists
            categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
            
            List<com.gvn.entity.Service> services = serviceRepository.findByCategoryIdAndIsActiveTrueOrderByNameAsc(categoryId);
            return services.stream()
                    .map(ServiceResponse::fromService)
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving services by category: ", e);
            throw new RuntimeException("Failed to retrieve services: " + e.getMessage());
        }
    }
    
    /**
     * Get available services for partner (grouped by category)
     */
    @Transactional(readOnly = true)
    public List<AvailableServiceResponse> getAvailableServicesForPartner(User user) {
        try {
            // Get all active categories
            List<Category> categories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
            
            // Get partner's active services
            List<PartnerService> partnerServices = partnerServiceRepository.findByUserAndIsActiveTrue(user);
            Map<Integer, Boolean> partnerServiceMap = partnerServices.stream()
                    .collect(Collectors.toMap(
                            ps -> ps.getService().getId(),
                            ps -> true
                    ));
            
            // Group services by category
            return categories.stream()
                    .map(category -> {
                        List<com.gvn.entity.Service> categoryServices = serviceRepository
                                .findByCategoryIdAndIsActiveTrueOrderByNameAsc(category.getId());
                        
                        // Filter only services that partner has
                        List<ServiceResponse> availableServices = categoryServices.stream()
                                .filter(service -> partnerServiceMap.containsKey(service.getId()))
                                .map(ServiceResponse::fromService)
                                .collect(Collectors.toList());
                        
                        return AvailableServiceResponse.builder()
                                .id(category.getId())
                                .name(category.getName())
                                .services(availableServices)
                                .build();
                    })
                    .filter(category -> !category.getServices().isEmpty()) // Only include categories with services
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error retrieving available services for partner: ", e);
            throw new RuntimeException("Failed to retrieve available services: " + e.getMessage());
        }
    }
    
    /**
     * Get options by source and service ID
     */
    @Transactional(readOnly = true)
    public List<OptionResponse> getOptionsBySource(String source, Integer serviceId, 
                                                   Integer serviceDurationTime, String startHour,
                                                   String fromDate, String toDate) {
        try {
            // Validate service exists
            serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service not found with id: " + serviceId));
            
            // Get options by source and service
            List<ServiceOption> options = serviceOptionRepository
                    .findBySourceAndServiceIdAndIsActiveTrueOrderByDisplayOrderAsc(source, serviceId);
            
            // Additional filtering logic can be added here based on optional parameters
            // For now, we return all active options for the source and service
            
            return options.stream()
                    .map(OptionResponse::fromServiceOption)
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving options: ", e);
            throw new RuntimeException("Failed to retrieve options: " + e.getMessage());
        }
    }
    
    /**
     * Get category by ID
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Integer id) {
        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
            return CategoryResponse.fromCategory(category);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving category: ", e);
            throw new RuntimeException("Failed to retrieve category: " + e.getMessage());
        }
    }
    
    /**
     * Create a new category
     */
    @Transactional
    public CategoryResponse createCategory(String name, MultipartFile iconFile, String iconUrl,
                                          Integer displayOrder, Boolean isActive) {
        try {
            // Validate required fields
            if (name == null || name.trim().isEmpty()) {
                throw new RuntimeException("Name is required");
            }
            
            // Check if category with same name already exists
            List<Category> existingCategories = categoryRepository.findAll();
            for (Category cat : existingCategories) {
                if (cat.getName().equalsIgnoreCase(name.trim())) {
                    throw new RuntimeException("Category with name '" + name + "' already exists");
                }
            }
            
            // Handle icon
            String finalIconUrl = null;
            if (iconFile != null && !iconFile.isEmpty()) {
                // Validate file size (5MB max)
                long maxFileSize = 5L * 1024 * 1024; // 5MB
                if (iconFile.getSize() > maxFileSize) {
                    throw new RuntimeException("Icon file exceeds maximum size of 5MB");
                }
                
                // Store icon file
                String iconPath = fileStorageService.storeFile(iconFile, "categories");
                finalIconUrl = fileStorageService.getFileUrl(iconPath);
            } else if (iconUrl != null && !iconUrl.trim().isEmpty()) {
                finalIconUrl = iconUrl.trim();
            }
            
            // Create category entity
            Category category = Category.builder()
                    .name(name.trim())
                    .icon(finalIconUrl)
                    .displayOrder(displayOrder != null ? displayOrder : 0)
                    .isActive(isActive != null ? isActive : true)
                    .build();
            
            Category savedCategory = categoryRepository.save(category);
            log.info("Category created successfully: {}", savedCategory.getId());
            
            return CategoryResponse.fromCategory(savedCategory);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating category: ", e);
            throw new RuntimeException("Failed to create category: " + e.getMessage());
        }
    }
    
    /**
     * Update an existing category
     */
    @Transactional
    public CategoryResponse updateCategory(Integer id, String name, MultipartFile iconFile, String iconUrl,
                                         Integer displayOrder, Boolean isActive) {
        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
            
            // Update name if provided
            if (name != null && !name.trim().isEmpty()) {
                // Check if another category with same name exists
                List<Category> existingCategories = categoryRepository.findAll();
                for (Category cat : existingCategories) {
                    if (!cat.getId().equals(id) && cat.getName().equalsIgnoreCase(name.trim())) {
                        throw new RuntimeException("Category with name '" + name + "' already exists");
                    }
                }
                category.setName(name.trim());
            }
            
            // Update icon if provided
            if (iconFile != null && !iconFile.isEmpty()) {
                // Validate file size (5MB max)
                long maxFileSize = 5L * 1024 * 1024; // 5MB
                if (iconFile.getSize() > maxFileSize) {
                    throw new RuntimeException("Icon file exceeds maximum size of 5MB");
                }
                
                // Delete old icon if exists
                if (category.getIcon() != null) {
                    String oldPath = extractPathFromUrl(category.getIcon());
                    if (oldPath != null) {
                        fileStorageService.deleteFile(oldPath);
                    }
                }
                
                // Store new icon
                String iconPath = fileStorageService.storeFile(iconFile, "categories");
                category.setIcon(fileStorageService.getFileUrl(iconPath));
            } else if (iconUrl != null) {
                // Delete old icon file if switching to URL
                if (category.getIcon() != null && category.getIcon().startsWith("/api/v1/files/")) {
                    String oldPath = extractPathFromUrl(category.getIcon());
                    if (oldPath != null) {
                        fileStorageService.deleteFile(oldPath);
                    }
                }
                category.setIcon(iconUrl.trim().isEmpty() ? null : iconUrl.trim());
            }
            
            // Update display order if provided
            if (displayOrder != null) {
                category.setDisplayOrder(displayOrder);
            }
            
            // Update active status if provided
            if (isActive != null) {
                category.setIsActive(isActive);
            }
            
            Category updatedCategory = categoryRepository.save(category);
            log.info("Category updated successfully: {}", updatedCategory.getId());
            
            return CategoryResponse.fromCategory(updatedCategory);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating category: ", e);
            throw new RuntimeException("Failed to update category: " + e.getMessage());
        }
    }
    
    /**
     * Delete a category
     */
    @Transactional
    public void deleteCategory(Integer id) {
        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
            
            // Check if category is being used by any services
            List<com.gvn.entity.Service> services = serviceRepository.findByCategoryIdAndIsActiveTrueOrderByNameAsc(id);
            if (!services.isEmpty()) {
                throw new RuntimeException("Cannot delete category: Category is being used by " + services.size() + " service(s)");
            }
            
            // Delete icon file if exists
            if (category.getIcon() != null && category.getIcon().startsWith("/api/v1/files/")) {
                String iconPath = extractPathFromUrl(category.getIcon());
                if (iconPath != null) {
                    fileStorageService.deleteFile(iconPath);
                }
            }
            
            // Delete category
            categoryRepository.delete(category);
            log.info("Category deleted successfully: {}", id);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting category: ", e);
            throw new RuntimeException("Failed to delete category: " + e.getMessage());
        }
    }
    
    /**
     * Get service by ID
     */
    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(Integer id) {
        try {
            com.gvn.entity.Service service = serviceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
            return ServiceResponse.fromService(service);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving service: ", e);
            throw new RuntimeException("Failed to retrieve service: " + e.getMessage());
        }
    }
    
    /**
     * Create a new service
     */
    @Transactional
    public ServiceResponse createService(String code, String name, String shortDescription,
                                        MultipartFile iconFile, String iconUrl, Integer categoryId,
                                        Boolean isActive) {
        try {
            // Validate required fields
            if (code == null || code.trim().isEmpty()) {
                throw new RuntimeException("Code is required");
            }
            if (name == null || name.trim().isEmpty()) {
                throw new RuntimeException("Name is required");
            }
            if (categoryId == null) {
                throw new RuntimeException("Category ID is required");
            }
            
            // Check if service code already exists
            if (serviceRepository.findByCode(code.trim()).isPresent()) {
                throw new RuntimeException("Service with code '" + code.trim() + "' already exists");
            }
            
            // Validate category exists
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
            
            // Handle icon
            String finalIconUrl = null;
            if (iconFile != null && !iconFile.isEmpty()) {
                // Validate file size (5MB max)
                long maxFileSize = 5L * 1024 * 1024; // 5MB
                if (iconFile.getSize() > maxFileSize) {
                    throw new RuntimeException("Icon file exceeds maximum size of 5MB");
                }
                
                // Store icon file
                String iconPath = fileStorageService.storeFile(iconFile, "services");
                finalIconUrl = fileStorageService.getFileUrl(iconPath);
            } else if (iconUrl != null && !iconUrl.trim().isEmpty()) {
                finalIconUrl = iconUrl.trim();
            }
            
            // Create service entity
            com.gvn.entity.Service service = com.gvn.entity.Service.builder()
                    .code(code.trim())
                    .name(name.trim())
                    .shortDescription(shortDescription != null ? shortDescription.trim() : null)
                    .icon(finalIconUrl)
                    .category(category)
                    .isActive(isActive != null ? isActive : true)
                    .build();
            
            com.gvn.entity.Service savedService = serviceRepository.save(service);
            log.info("Service created successfully: {}", savedService.getId());
            
            return ServiceResponse.fromService(savedService);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating service: ", e);
            throw new RuntimeException("Failed to create service: " + e.getMessage());
        }
    }
    
    /**
     * Update an existing service
     */
    @Transactional
    public ServiceResponse updateService(Integer id, String code, String name, String shortDescription,
                                        MultipartFile iconFile, String iconUrl, Integer categoryId,
                                        Boolean isActive) {
        try {
            com.gvn.entity.Service service = serviceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
            
            // Update code if provided
            if (code != null && !code.trim().isEmpty()) {
                // Check if another service with same code exists
                serviceRepository.findByCode(code.trim())
                        .ifPresent(existingService -> {
                            if (!existingService.getId().equals(id)) {
                                throw new RuntimeException("Service with code '" + code.trim() + "' already exists");
                            }
                        });
                service.setCode(code.trim());
            }
            
            // Update name if provided
            if (name != null && !name.trim().isEmpty()) {
                service.setName(name.trim());
            }
            
            // Update short description if provided
            if (shortDescription != null) {
                service.setShortDescription(shortDescription.trim().isEmpty() ? null : shortDescription.trim());
            }
            
            // Update icon if provided
            if (iconFile != null && !iconFile.isEmpty()) {
                // Validate file size (5MB max)
                long maxFileSize = 5L * 1024 * 1024; // 5MB
                if (iconFile.getSize() > maxFileSize) {
                    throw new RuntimeException("Icon file exceeds maximum size of 5MB");
                }
                
                // Delete old icon if exists
                if (service.getIcon() != null && service.getIcon().startsWith("/api/v1/files/")) {
                    String oldPath = extractPathFromUrl(service.getIcon());
                    if (oldPath != null) {
                        fileStorageService.deleteFile(oldPath);
                    }
                }
                
                // Store new icon
                String iconPath = fileStorageService.storeFile(iconFile, "services");
                service.setIcon(fileStorageService.getFileUrl(iconPath));
            } else if (iconUrl != null) {
                // Delete old icon file if switching to URL
                if (service.getIcon() != null && service.getIcon().startsWith("/api/v1/files/")) {
                    String oldPath = extractPathFromUrl(service.getIcon());
                    if (oldPath != null) {
                        fileStorageService.deleteFile(oldPath);
                    }
                }
                service.setIcon(iconUrl.trim().isEmpty() ? null : iconUrl.trim());
            }
            
            // Update category if provided
            if (categoryId != null) {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
                service.setCategory(category);
            }
            
            // Update active status if provided
            if (isActive != null) {
                service.setIsActive(isActive);
            }
            
            com.gvn.entity.Service updatedService = serviceRepository.save(service);
            log.info("Service updated successfully: {}", updatedService.getId());
            
            return ServiceResponse.fromService(updatedService);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating service: ", e);
            throw new RuntimeException("Failed to update service: " + e.getMessage());
        }
    }
    
    /**
     * Delete a service
     */
    @Transactional
    public void deleteService(Integer id) {
        try {
            com.gvn.entity.Service service = serviceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
            
            // Check if service is being used by any partners
            List<PartnerService> partnerServices = partnerServiceRepository.findByServiceId(id);
            if (!partnerServices.isEmpty()) {
                throw new RuntimeException("Cannot delete service: Service is being used by " + partnerServices.size() + " partner(s)");
            }
            
            // Delete icon file if exists
            if (service.getIcon() != null && service.getIcon().startsWith("/api/v1/files/")) {
                String iconPath = extractPathFromUrl(service.getIcon());
                if (iconPath != null) {
                    fileStorageService.deleteFile(iconPath);
                }
            }
            
            // Delete service
            serviceRepository.delete(service);
            log.info("Service deleted successfully: {}", id);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting service: ", e);
            throw new RuntimeException("Failed to delete service: " + e.getMessage());
        }
    }
    
    /**
     * Extract relative path from full URL
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

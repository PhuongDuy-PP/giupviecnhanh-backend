package com.gvn.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {
    
    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;
    
    public String storeFile(MultipartFile file, String subdirectory) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }
        
        log.info("Storing file: {} (size: {} bytes, content type: {}) to subdirectory: {}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType(), subdirectory);
        
        try {
            // Resolve absolute path - handle both relative and absolute paths
            Path basePath = Paths.get(uploadDir);
            if (!basePath.isAbsolute()) {
                // If relative, make it absolute from current working directory
                String userDir = System.getProperty("user.dir");
                log.info("Resolving relative path. user.dir: {}, uploadDir: {}", userDir, uploadDir);
                basePath = Paths.get(userDir, uploadDir).toAbsolutePath();
            }
            
            log.info("Base upload path: {}", basePath);
            
            // Create directory if it doesn't exist
            Path uploadPath = basePath.resolve(subdirectory);
            log.info("Target upload directory: {}", uploadPath);
            
            if (!Files.exists(uploadPath)) {
                log.info("Creating upload directory: {}", uploadPath);
                try {
                    Files.createDirectories(uploadPath);
                    log.info("Successfully created upload directory: {}", uploadPath);
                } catch (IOException e) {
                    log.error("Failed to create upload directory: {}", uploadPath, e);
                    throw new IOException("Failed to create upload directory: " + uploadPath + " - " + e.getMessage(), e);
                }
            }
            
            // Check if directory exists and is writable
            if (!Files.exists(uploadPath)) {
                throw new IOException("Upload directory does not exist and could not be created: " + uploadPath);
            }
            
            if (!Files.isDirectory(uploadPath)) {
                throw new IOException("Upload path is not a directory: " + uploadPath);
            }
            
            if (!Files.isWritable(uploadPath)) {
                // Try to get more info about permissions
                try {
                    String permissions = Files.getPosixFilePermissions(uploadPath).toString();
                    log.error("Upload directory is not writable: {} (permissions: {})", uploadPath, permissions);
                } catch (Exception e) {
                    log.error("Upload directory is not writable: {} (could not get permissions)", uploadPath);
                }
                throw new IOException("Upload directory is not writable: " + uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;
            
            // Save file
            Path filePath = uploadPath.resolve(filename);
            log.info("Storing file to: {} (size: {} bytes)", filePath, file.getSize());
            
            try {
                // Use Files.copy with optimized options for better performance
                // This is faster than buffered streams for large files on modern systems
                Files.copy(file.getInputStream(), filePath, 
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
                log.info("File copied successfully to: {}", filePath);
                
                // Verify file was written
                if (!Files.exists(filePath)) {
                    throw new IOException("File was not created after copy operation: " + filePath);
                }
                
                long fileSize = Files.size(filePath);
                log.info("File stored successfully. Path: {}, Size: {} bytes", filePath, fileSize);
                
                if (fileSize == 0) {
                    log.warn("Warning: Stored file has 0 bytes: {}", filePath);
                }
            } catch (IOException e) {
                log.error("Failed to copy file to: {}", filePath, e);
                // Try to delete partial file if it exists
                try {
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                        log.info("Deleted partial file: {}", filePath);
                    }
                } catch (Exception cleanupEx) {
                    log.warn("Failed to cleanup partial file: {}", filePath, cleanupEx);
                }
                throw new IOException("Failed to store file to " + filePath + ": " + e.getMessage(), e);
            }
            
            // Return relative URL path
            String relativePath = subdirectory + "/" + filename;
            log.info("File stored successfully: {} (full path: {})", relativePath, filePath);
            return relativePath;
        } catch (IOException e) {
            log.error("Error storing file to directory: {} (subdirectory: {})", uploadDir, subdirectory, e);
            log.error("Exception details - Class: {}, Message: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                log.error("Caused by: {}", e.getCause().getClass().getName(), e.getCause());
            }
            throw new IOException("Failed to store file: " + e.getMessage(), e);
        }
    }
    
    public List<String> storeMultipleFiles(MultipartFile[] files, String subdirectory) throws IOException {
        List<String> storedUrls = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String path = storeFile(file, subdirectory);
                    storedUrls.add(getFileUrl(path));
                }
            }
        }
        return storedUrls;
    }
    
    public boolean deleteFile(String filePath) {
        try {
            // Resolve absolute path - handle both relative and absolute paths
            Path basePath = Paths.get(uploadDir);
            if (!basePath.isAbsolute()) {
                basePath = Paths.get(System.getProperty("user.dir"), uploadDir).toAbsolutePath();
            }
            
            Path path = basePath.resolve(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("File deleted successfully: {}", filePath);
                return true;
            }
            log.debug("File does not exist: {}", path);
            return false;
        } catch (IOException e) {
            log.error("Error deleting file: {}", filePath, e);
            return false;
        }
    }
    
    public String getFileUrl(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return null;
        }
        // In production, this should return the full URL (e.g., https://cdn.example.com/uploads/...)
        // For now, return relative path that can be served by a static resource handler
        return "/api/v1/files/" + relativePath;
    }
}

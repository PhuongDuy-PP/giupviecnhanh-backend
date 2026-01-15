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
        
        try {
            // Resolve absolute path - handle both relative and absolute paths
            Path basePath = Paths.get(uploadDir);
            if (!basePath.isAbsolute()) {
                // If relative, make it absolute from current working directory
                basePath = Paths.get(System.getProperty("user.dir"), uploadDir).toAbsolutePath();
            }
            
            // Create directory if it doesn't exist
            Path uploadPath = basePath.resolve(subdirectory);
            log.debug("Creating upload directory: {}", uploadPath);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath);
            }
            
            // Check if directory is writable
            if (!Files.isWritable(uploadPath)) {
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
            log.debug("Storing file to: {}", filePath);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Return relative URL path
            String relativePath = subdirectory + "/" + filename;
            log.info("File stored successfully: {} (full path: {})", relativePath, filePath);
            return relativePath;
        } catch (IOException e) {
            log.error("Error storing file to directory: {} (subdirectory: {})", uploadDir, subdirectory, e);
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

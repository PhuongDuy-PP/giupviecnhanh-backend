package com.gvn.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileStorageService {
    
    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;
    
    // Cache resolved base path to avoid repeated resolution
    private volatile Path cachedBasePath;
    private static final int BUFFER_SIZE = 64 * 1024; // 64KB buffer for faster I/O
    private static final ExecutorService fileUploadExecutor = Executors.newFixedThreadPool(4);
    
    private Path getBasePath() {
        if (cachedBasePath == null) {
            synchronized (this) {
                if (cachedBasePath == null) {
                    Path basePath = Paths.get(uploadDir);
                    if (!basePath.isAbsolute()) {
                        basePath = Paths.get(System.getProperty("user.dir"), uploadDir).toAbsolutePath();
                    }
                    cachedBasePath = basePath;
                }
            }
        }
        return cachedBasePath;
    }
    
    public String storeFile(MultipartFile file, String subdirectory) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }
        
        try {
            // Use cached base path for better performance
            Path basePath = getBasePath();
            
            // Create directory if it doesn't exist (createDirectories is idempotent and fast)
            Path uploadPath = basePath.resolve(subdirectory);
            Files.createDirectories(uploadPath);
            
            // Quick check if writable (skip detailed permission checks in hot path)
            if (!Files.isWritable(uploadPath)) {
                log.error("Upload directory is not writable: {}", uploadPath);
                throw new IOException("Upload directory is not writable: " + uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;
            
            // Save file with optimized buffered I/O
            Path filePath = uploadPath.resolve(filename);
            
            try (BufferedInputStream bis = new BufferedInputStream(file.getInputStream(), BUFFER_SIZE);
                 BufferedOutputStream bos = new BufferedOutputStream(
                     Files.newOutputStream(filePath), BUFFER_SIZE)) {
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
                bos.flush();
            }
            
            // Quick verification
            if (!Files.exists(filePath) || Files.size(filePath) == 0) {
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
                throw new IOException("File was not written correctly: " + filePath);
            }
            
            // Return relative URL path
            return subdirectory + "/" + filename;
        } catch (IOException e) {
            log.error("Error storing file (subdirectory: {}): {}", subdirectory, e.getMessage());
            throw new IOException("Failed to store file: " + e.getMessage(), e);
        }
    }
    
    public List<String> storeMultipleFiles(MultipartFile[] files, String subdirectory) throws IOException {
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }
        
        // Filter valid files
        List<MultipartFile> validFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                validFiles.add(file);
            }
        }
        
        if (validFiles.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Parallel upload for multiple files
        if (validFiles.size() > 1) {
            List<CompletableFuture<String>> futures = validFiles.stream()
                .map(file -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return storeFile(file, subdirectory);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, fileUploadExecutor))
                .collect(Collectors.toList());
            
            // Wait for all uploads and collect results
            List<String> storedUrls = new ArrayList<>();
            for (CompletableFuture<String> future : futures) {
                try {
                    String path = future.get();
                    storedUrls.add(getFileUrl(path));
                } catch (Exception e) {
                    log.error("Error in parallel file upload: ", e);
                    throw new IOException("Failed to upload file: " + e.getMessage(), e);
                }
            }
            return storedUrls;
        } else {
            // Single file - no need for parallel processing
            String path = storeFile(validFiles.get(0), subdirectory);
            return List.of(getFileUrl(path));
        }
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

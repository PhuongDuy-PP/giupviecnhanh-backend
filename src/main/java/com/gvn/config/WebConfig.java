package com.gvn.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve absolute path for file serving
        Path basePath = Paths.get(uploadDir);
        if (!basePath.isAbsolute()) {
            basePath = Paths.get(System.getProperty("user.dir"), uploadDir).toAbsolutePath();
        }
        
        String fileLocation = "file:" + basePath.toString() + "/";
        log.info("Configuring file serving from: {}", fileLocation);
        
        // Serve uploaded files
        registry.addResourceHandler("/api/v1/files/**")
                .addResourceLocations(fileLocation);
    }
}

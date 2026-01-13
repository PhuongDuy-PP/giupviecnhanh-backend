package com.gvn.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gvn.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        String message = "Validation error";
        if (!errors.isEmpty()) {
            message = errors.values().iterator().next();
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, 400));
    }
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {
        String message = String.format("Request method '%s' is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(), ex.getSupportedHttpMethods());
        log.warn("Method not supported: {}", message);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(message, 405));
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex) {
        log.error("HTTP message not readable: ", ex);
        String message = "Invalid request body format";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            String causeMessage = ex.getCause().getMessage();
            if (causeMessage.contains("LocalDate") || causeMessage.contains("date")) {
                message = "Invalid date format. Expected format: yyyy/MM/dd";
            } else {
                message = "Invalid request body: " + causeMessage;
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, 400));
    }
    
    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ApiResponse<Object>> handleJsonProcessingException(
            JsonProcessingException ex) {
        log.error("JSON processing error: ", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Invalid JSON format: " + ex.getMessage(), 400));
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
        // This handler should only catch RuntimeExceptions that are NOT handled by controllers
        // Controllers handle their own exceptions, so this is a fallback
        log.error("Unhandled runtime exception (fallback handler): ", ex);
        String message = ex.getMessage();
        if (message == null || message.isEmpty()) {
            message = "An error occurred";
        }
        
        // Determine status code based on message
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (message.contains("not authenticated") || message.contains("Unauthorized") || 
            message.contains("Invalid credentials") || message.contains("Invalid credential")) {
            status = HttpStatus.UNAUTHORIZED;
            // Don't return signup-specific messages for auth errors
            if (message.contains("already exists")) {
                message = "Authentication failed";
            }
        } else if (message.contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        } else if (message.contains("already exists") || message.contains("duplicate")) {
            status = HttpStatus.CONFLICT;
        }
        
        return ResponseEntity.status(status)
                .body(ApiResponse.error(message, status.value()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "An error occurred. Please try again later.",
                        500
                ));
    }
}


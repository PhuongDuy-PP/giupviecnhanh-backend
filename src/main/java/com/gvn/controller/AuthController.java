package com.gvn.controller;

import com.gvn.dto.request.LoginRequest;
import com.gvn.dto.request.RefreshTokenRequest;
import com.gvn.dto.request.RegisterRequest;
import com.gvn.dto.response.ApiResponse;
import com.gvn.dto.response.LoginResponse;
import com.gvn.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Platform", required = false) String platform
    ) {
        try {
            LoginResponse loginResponse = authService.login(request);
            return ResponseEntity.ok(
                    ApiResponse.success(loginResponse, "Login successful")
            );
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            log.warn("Login failed: {}", errorMessage);
            
            // Handle invalid credentials specifically
            if ("Invalid credentials".equals(errorMessage) || 
                (errorMessage != null && errorMessage.contains("Invalid credential"))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(
                                "Phone number or password is incorrect",
                                401
                        ));
            }
            
            // For any other RuntimeException, return 400 with the error message
            // But make sure we don't return signup-specific messages
            if (errorMessage != null && errorMessage.contains("already exists")) {
                log.error("Unexpected 'already exists' error in login endpoint. This should not happen.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(
                                "Login failed. Please check your credentials.",
                                400
                        ));
            }
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(
                            errorMessage != null ? errorMessage : "Login failed",
                            400
                    ));
        } catch (Exception e) {
            log.error("Unexpected error during login: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            "An error occurred. Please try again later.",
                            500
                    ));
        }
    }
    
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        try {
            LoginResponse loginResponse = authService.refreshToken(request);
            return ResponseEntity.ok(
                    ApiResponse.success(loginResponse, "Token refreshed successfully")
            );
        } catch (RuntimeException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(
                            e.getMessage(),
                            401
                    ));
        } catch (Exception e) {
            log.error("Unexpected error during token refresh: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            "An error occurred. Please try again later.",
                            500
                    ));
        }
    }
    
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<LoginResponse>> signup(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = "X-Platform", required = false) String platform
    ) {
        try {
            LoginResponse loginResponse = authService.register(request);
            return ResponseEntity.ok(
                    ApiResponse.success(loginResponse, "Registration successful")
            );
        } catch (RuntimeException e) {
            log.warn("Registration failed: {}", e.getMessage());
            if ("Phone number already exists".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(
                                "This phone number is already registered. Please use a different number or try logging in.",
                                409
                        ));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(
                            e.getMessage(),
                            400
                    ));
        } catch (Exception e) {
            log.error("Unexpected error during registration: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            "Registration failed. Please try again later.",
                            500
                    ));
        }
    }
}


package com.gvn.service;

import com.gvn.dto.request.LoginRequest;
import com.gvn.dto.request.RefreshTokenRequest;
import com.gvn.dto.request.RegisterRequest;
import com.gvn.dto.response.LoginResponse;
import com.gvn.entity.Session;
import com.gvn.entity.User;
import com.gvn.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserService userService;
    private final JwtService jwtService;
    private final SessionRepository sessionRepository;
    
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Authenticate user
        Optional<User> userOpt = userService.authenticate(
                request.getPhone_number(),
                request.getPassword()
        );
        
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid credentials");
        }
        
        User user = userOpt.get();
        
        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getPhoneNumber());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getPhoneNumber());
        String sessionId = UUID.randomUUID().toString();
        
        // Calculate expiration timestamps
        Long accessExpiresAt = jwtService.getExpirationTimestamp(
                jwtService.getAccessTokenExpirationInSeconds()
        );
        Long refreshExpiresAt = jwtService.getExpirationTimestamp(
                jwtService.getRefreshTokenExpirationInSeconds()
        );
        
        // Save session
        Session session = Session.builder()
                .user(user)
                .sessionId(sessionId)
                .deviceInfo(request.getDevice_info())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresAt(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(accessExpiresAt),
                        ZoneId.systemDefault()
                ))
                .refreshTokenExpiresAt(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(refreshExpiresAt),
                        ZoneId.systemDefault()
                ))
                .isActive(true)
                .build();
        
        sessionRepository.save(session);
        
        // Build response
        return LoginResponse.builder()
                .access_token(accessToken)
                .token_type("Bearer")
                .expires_in(jwtService.getAccessTokenExpirationInSeconds())
                .expires_at(accessExpiresAt)
                .refresh_token(refreshToken)
                .refresh_expires_in(jwtService.getRefreshTokenExpirationInSeconds())
                .refresh_expires_at(refreshExpiresAt)
                .session_id(sessionId)
                .user(userService.mapToUserResponse(user))
                .build();
    }
    
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefresh_token();
        
        // Validate refresh token
        if (jwtService.isTokenExpired(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }
        
        // Find session by refresh token
        Optional<Session> sessionOpt = sessionRepository.findByRefreshToken(refreshToken);
        if (sessionOpt.isEmpty()) {
            throw new RuntimeException("Session not found");
        }
        
        Session session = sessionOpt.get();
        
        // Check if session is active
        if (!session.getIsActive()) {
            throw new RuntimeException("Session is not active");
        }
        
        // Check if refresh token is expired
        if (session.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }
        
        User user = session.getUser();
        
        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getPhoneNumber());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId(), user.getPhoneNumber());
        
        // Calculate expiration timestamps
        Long accessExpiresAt = jwtService.getExpirationTimestamp(
                jwtService.getAccessTokenExpirationInSeconds()
        );
        Long refreshExpiresAt = jwtService.getExpirationTimestamp(
                jwtService.getRefreshTokenExpirationInSeconds()
        );
        
        // Update session
        session.setAccessToken(newAccessToken);
        session.setRefreshToken(newRefreshToken);
        session.setAccessTokenExpiresAt(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(accessExpiresAt),
                ZoneId.systemDefault()
        ));
        session.setRefreshTokenExpiresAt(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(refreshExpiresAt),
                ZoneId.systemDefault()
        ));
        
        sessionRepository.save(session);
        
        // Build response
        return LoginResponse.builder()
                .access_token(newAccessToken)
                .token_type("Bearer")
                .expires_in(jwtService.getAccessTokenExpirationInSeconds())
                .expires_at(accessExpiresAt)
                .refresh_token(newRefreshToken)
                .refresh_expires_in(jwtService.getRefreshTokenExpirationInSeconds())
                .refresh_expires_at(refreshExpiresAt)
                .session_id(session.getSessionId())
                .user(userService.mapToUserResponse(user))
                .build();
    }
    
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // Check if phone number already exists
        if (userService.existsByPhoneNumber(request.getPhone_number())) {
            throw new RuntimeException("Phone number already exists");
        }
        
        // Create new user
        User user = userService.createUser(
                request.getPhone_number(),
                request.getPassword(),
                request.getUser_type()
        );
        
        // Generate tokens (automatic login after registration)
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getPhoneNumber());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getPhoneNumber());
        String sessionId = UUID.randomUUID().toString();
        
        // Calculate expiration timestamps
        Long accessExpiresAt = jwtService.getExpirationTimestamp(
                jwtService.getAccessTokenExpirationInSeconds()
        );
        Long refreshExpiresAt = jwtService.getExpirationTimestamp(
                jwtService.getRefreshTokenExpirationInSeconds()
        );
        
        // Save session
        Session session = Session.builder()
                .user(user)
                .sessionId(sessionId)
                .deviceInfo(request.getDevice_info())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresAt(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(accessExpiresAt),
                        ZoneId.systemDefault()
                ))
                .refreshTokenExpiresAt(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(refreshExpiresAt),
                        ZoneId.systemDefault()
                ))
                .isActive(true)
                .build();
        
        sessionRepository.save(session);
        
        // Build response (same format as login)
        return LoginResponse.builder()
                .access_token(accessToken)
                .token_type("Bearer")
                .expires_in(jwtService.getAccessTokenExpirationInSeconds())
                .expires_at(accessExpiresAt)
                .refresh_token(refreshToken)
                .refresh_expires_in(jwtService.getRefreshTokenExpirationInSeconds())
                .refresh_expires_at(refreshExpiresAt)
                .session_id(sessionId)
                .user(userService.mapToUserResponse(user))
                .build();
    }
}


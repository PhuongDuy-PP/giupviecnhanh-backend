package com.gvn.config;

import com.gvn.service.JwtService;
import com.gvn.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserService userService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String token = extractTokenFromRequest(request);
        
        if (token != null) {
            try {
                // Check if token is expired or refresh token
                if (jwtService.isTokenExpired(token)) {
                    log.warn("Token expired for request: {}", request.getRequestURI());
                    // Continue filter chain - SecurityConfig will handle 403 if authentication required
                } else if (jwtService.isRefreshToken(token)) {
                    log.warn("Refresh token used instead of access token for request: {}", request.getRequestURI());
                    // Continue filter chain - SecurityConfig will handle 403 if authentication required
                } else {
                    // Valid access token - extract and set authentication
                    UUID userId = jwtService.extractUserId(token);
                    userService.findById(userId).ifPresent(user -> {
                        UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(user, null, null);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Authentication set for user: {}", userId);
                    });
                }
            } catch (Exception e) {
                log.error("Error processing JWT token for request {}: {}", request.getRequestURI(), e.getMessage());
                // Continue filter chain - SecurityConfig will handle 403 if authentication required
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

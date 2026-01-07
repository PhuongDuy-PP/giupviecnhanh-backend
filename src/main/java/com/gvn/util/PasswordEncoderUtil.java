package com.gvn.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes
 * Use this to generate password hashes for test users
 */
public class PasswordEncoderUtil {
    
    public static void main(String[] args) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Example: encode password "password123"
        String rawPassword = "password123";
        String encodedPassword = encoder.encode(rawPassword);
        
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Encoded password (BCrypt): " + encodedPassword);
        System.out.println("\nUse this hash in your SQL insert statement.");
        
        // Verify
        boolean matches = encoder.matches(rawPassword, encodedPassword);
        System.out.println("Verification: " + matches);
    }
}


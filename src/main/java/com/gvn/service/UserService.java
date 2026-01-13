package com.gvn.service;

import com.gvn.dto.response.UserResponse;
import com.gvn.entity.PartnerProfile;
import com.gvn.entity.User;
import com.gvn.repository.PartnerProfileRepository;
import com.gvn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public Optional<User> authenticate(String phoneNumber, String password) {
        Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String storedPasswordHash = user.getPassword();
            
            if (storedPasswordHash == null || storedPasswordHash.isEmpty()) {
                log.warn("User password is empty for phone: {}", phoneNumber);
                return Optional.empty();
            }
            
            boolean passwordMatches = passwordEncoder.matches(password, storedPasswordHash);
            
            if (passwordMatches) {
                log.info("Authentication successful for phone: {}", phoneNumber);
                return Optional.of(user);
            } else {
                log.warn("Authentication failed: invalid password for phone: {}", phoneNumber);
            }
        } else {
            log.warn("Authentication failed: user not found for phone: {}", phoneNumber);
        }
        
        return Optional.empty();
    }
    
    public UserResponse mapToUserResponse(User user) {
        PartnerProfile partnerProfile = null;
        if (user.getHasPartnerProfile()) {
            partnerProfile = partnerProfileRepository.findByUser(user)
                    .orElse(null);
        }
        
        return UserResponse.builder()
                .id(user.getId())
                .full_name(user.getFullName())
                .gender(user.getGender())
                .email(user.getEmail())
                .birthday(user.getBirthday())
                .address(user.getAddress())
                .phone(user.getPhoneNumber())
                .avatar_url(user.getAvatarUrl())
                .has_partner_profile(user.getHasPartnerProfile())
                .partner_profile(UserResponse.fromPartnerProfile(partnerProfile))
                .build();
    }
    
    public Optional<User> findById(UUID userId) {
        return userRepository.findById(userId);
    }
    
    public boolean existsByPhoneNumber(String phoneNumber) {
        // Use findByPhoneNumber instead of repository's existsByPhoneNumber 
        // to avoid potential cache/index issues with unique constraint
        Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);
        boolean exists = userOpt.isPresent();
        if (exists) {
            log.debug("Phone number {} exists in database", phoneNumber);
        } else {
            log.debug("Phone number {} does not exist in database", phoneNumber);
        }
        return exists;
    }
    
    @Transactional
    public User createUser(String phoneNumber, String password, String userType) {
        // Double check with findByPhoneNumber to ensure accuracy
        Optional<User> existingUser = userRepository.findByPhoneNumber(phoneNumber);
        if (existingUser.isPresent()) {
            log.warn("Attempt to create user with existing phone number: {}", phoneNumber);
            throw new RuntimeException("Phone number already exists");
        }
        
        String hashedPassword = passwordEncoder.encode(password);
        
        User user = User.builder()
                .phoneNumber(phoneNumber)
                .password(hashedPassword)
                .userType(userType != null ? userType : "customer")
                .hasPartnerProfile(false)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("User created successfully with phone: {}", phoneNumber);
        
        return savedUser;
    }
}


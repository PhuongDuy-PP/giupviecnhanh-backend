package com.gvn.dto.response;

import com.gvn.entity.PartnerProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID id;
    private String full_name;
    private String gender;
    private String email;
    private LocalDate birthday;
    private String address;
    private String phone;
    private String avatar_url;
    private Boolean has_partner_profile;
    private PartnerProfileResponse partner_profile;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PartnerProfileResponse {
        private String profile_status;
    }
    
    public static PartnerProfileResponse fromPartnerProfile(PartnerProfile partnerProfile) {
        if (partnerProfile == null) {
            return null;
        }
        return PartnerProfileResponse.builder()
                .profile_status(partnerProfile.getProfileStatus().name().toLowerCase())
                .build();
    }
}


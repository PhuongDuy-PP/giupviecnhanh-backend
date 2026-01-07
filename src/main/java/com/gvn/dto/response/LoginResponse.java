package com.gvn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String access_token;
    @Builder.Default
    private String token_type = "Bearer";
    private Long expires_in;
    private Long expires_at;
    private String refresh_token;
    private Long refresh_expires_in;
    private Long refresh_expires_at;
    private String session_id;
    private UserResponse user;
}


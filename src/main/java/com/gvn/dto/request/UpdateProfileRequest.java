package com.gvn.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {
    
    @NotBlank(message = "Full name is required")
    private String full_name;
    
    @Email(message = "Email should be valid")
    private String email;
    
    private String gender;
    
    private String birthday; // Format: yyyy/MM/dd
    
    private String address;
    
    @NotBlank(message = "Type is required")
    private String type; // Should be "update"
}

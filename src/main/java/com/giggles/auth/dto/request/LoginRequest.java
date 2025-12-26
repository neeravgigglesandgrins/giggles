package com.giggles.auth.dto.request;

import com.giggles.auth.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginRequest {
    
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    
    // Firebase ID token - optional (Firebase verification removed)
    private String firebaseIdToken;
    
    @NotNull(message = "Role is required")
    private UserRole role;
}


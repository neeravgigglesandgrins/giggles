package com.giggles.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SignUpOrLoginRequest {
    
    // Flag to differentiate signup (true) or login (false)
    @NotNull(message = "isSignup flag is required")
    private Boolean isSignup;
    
    // For signup: all fields required
    // For login: phoneNumber/email and password required
    private String name;
    
    private String email;
    
    private String phoneNumber;
    
    private String address;
    
    @NotBlank(message = "Password is required")
    private String password;
}


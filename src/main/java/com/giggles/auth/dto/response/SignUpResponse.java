package com.giggles.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpResponse {
    
    private Long userId;
    private String authToken;
    private String phoneNumber;
    private String email;
    private String role;
    private Boolean isNewAccount;
}


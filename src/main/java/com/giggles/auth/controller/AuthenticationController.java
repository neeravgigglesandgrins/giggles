package com.giggles.auth.controller;

import com.giggles.auth.dto.request.SignUpOrLoginRequest;
import com.giggles.auth.dto.response.AuthResponse;
import com.giggles.auth.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthenticationController {
    
    private final AuthenticationService authenticationService;
    
    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }
    
    @PostMapping("/signup-or-login")
    public ResponseEntity<AuthResponse> signUpOrLogin(
            @Valid @RequestBody SignUpOrLoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("Received signup/login request. isSignup: {}", request.getIsSignup());
        AuthResponse response = authenticationService.signUpOrLogin(request, httpRequest);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}


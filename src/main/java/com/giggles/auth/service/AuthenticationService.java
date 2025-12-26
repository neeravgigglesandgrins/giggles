package com.giggles.auth.service;

import com.giggles.auth.dto.request.SignUpOrLoginRequest;
import com.giggles.auth.dto.response.AuthResponse;
import com.giggles.auth.dto.response.UserDTO;
import com.giggles.auth.entity.UserEntity;
import com.giggles.auth.entity.UserSessionEntity;
import com.giggles.auth.enums.UserRole;
import com.giggles.auth.enums.UserSessionStatus;
import com.giggles.auth.exception.AuthenticationException;
import com.giggles.auth.exception.ErrorCode;
import com.giggles.auth.repository.UserRepository;
import com.giggles.auth.repository.UserSessionRepository;
import com.giggles.auth.util.JwtUtil;
import com.giggles.auth.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;
    
    @Autowired
    public AuthenticationService(UserRepository userRepository, UserSessionRepository userSessionRepository, JwtUtil jwtUtil,
                                 PasswordUtil passwordUtil) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.jwtUtil = jwtUtil;
        this.passwordUtil = passwordUtil;
    }
    
    @Transactional
    public AuthResponse signUpOrLogin(SignUpOrLoginRequest request, HttpServletRequest httpRequest) {
        log.info("Processing signup/login request. isSignup: {}", request.getIsSignup());
        
        if (Boolean.TRUE.equals(request.getIsSignup())) {
            return signUp(request, httpRequest);
        } else {
            return login(request, httpRequest);
        }
    }
    
    private AuthResponse signUp(SignUpOrLoginRequest request, HttpServletRequest httpRequest) {
        log.info("Processing signup for email: {} or phone: {}", request.getEmail(), request.getPhoneNumber());
        
        // Validate required fields for signup
        validateSignupRequest(request);
        
        // Check if user already exists by email or phone number
        boolean userExists = false;
        String existingField = null;
        
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
                userExists = true;
                existingField = "email";
            }
        }
        
        if (!userExists && request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            if (userRepository.existsByPhoneNumberAndDeletedFalse(request.getPhoneNumber())) {
                userExists = true;
                existingField = "phone number";
            }
        }
        
        if (userExists) {
            throw new AuthenticationException(
                    HttpStatus.SC_CONFLICT,
                    ErrorCode.INVALID_CREDENTIALS,
                    "User already exists with this " + existingField + ". Please login."
            );
        }
        
        // Create new user account
        UserEntity user = new UserEntity();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setPassword(passwordUtil.encodePassword(request.getPassword()));
        user.setRole(UserRole.USER); // Default role is USER
        // UserSessionType is MULTI by default in entity
        
        user = userRepository.save(user);
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), 
                user.getPhoneNumber() != null ? user.getPhoneNumber() : user.getEmail(), 
                user.getRole().name());
        LocalDateTime expiry = jwtUtil.getExpiryDateTime();
        
        // Create session
        UserSessionEntity session = new UserSessionEntity();
        session.setUser(user);
        session.setToken(token);
        session.setExpiry(expiry);
        session.setUserSessionStatus(UserSessionStatus.VALID);
        session.setIpAddress(getClientIpAddress(httpRequest));
        session.setUserAgent(httpRequest.getHeader("User-Agent"));
        userSessionRepository.save(session);
        
        log.info("User account created successfully with ID: {}", user.getId());
        
        // Build response
        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .role(user.getRole().name())
                .build();
        
        return AuthResponse.builder()
                .authToken(token)
                .user(userDTO)
                .build();
    }
    
    private AuthResponse login(SignUpOrLoginRequest request, HttpServletRequest httpRequest) {
        log.info("Processing login for email: {} or phone: {}", request.getEmail(), request.getPhoneNumber());
        validateLoginRequest(request);

        UserEntity user = null;
        
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            user = userRepository.findByEmailAndDeletedFalse(request.getEmail()).orElse(null);
        }
        
        if (user == null && request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            user = userRepository.findByPhoneNumberAndDeletedFalse(request.getPhoneNumber()).orElse(null);
        }
        
        if (user == null) {
            throw new AuthenticationException(
                    HttpStatus.SC_NOT_FOUND,
                    ErrorCode.INVALID_CREDENTIALS,
                    "User not found with provided email or phone number"
            );
        }
        if (!passwordUtil.matches(request.getPassword(), user.getPassword())) {
            // Increment login attempts
            user.setLoginAttempts(user.getLoginAttempts() + 1);
            userRepository.save(user);
            
            throw new AuthenticationException(
                    HttpStatus.SC_UNAUTHORIZED,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Invalid password"
            );
        }
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new AuthenticationException(
                    HttpStatus.SC_LOCKED,
                    ErrorCode.ACCOUNT_LOCKED,
                    "Account is locked. Please contact support."
            );
        }
        markExpiredSessionsAsInvalid(user);
        String token = jwtUtil.generateToken(user.getId(), 
                user.getPhoneNumber() != null ? user.getPhoneNumber() : user.getEmail(), 
                user.getRole().name());
        LocalDateTime expiry = jwtUtil.getExpiryDateTime();
        UserSessionEntity session = new UserSessionEntity();
        session.setUser(user);
        session.setToken(token);
        session.setExpiry(expiry);
        session.setUserSessionStatus(UserSessionStatus.VALID);
        session.setIpAddress(getClientIpAddress(httpRequest));
        session.setUserAgent(httpRequest.getHeader("User-Agent"));
        userSessionRepository.save(session);
        user.setLoginAttempts(0);
        user.setIsLocked(false);
        userRepository.save(user);
        
        log.info("User logged in successfully with ID: {}", user.getId());
        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .role(user.getRole().name())
                .build();
        
        return AuthResponse.builder()
                .authToken(token)
                .user(userDTO)
                .build();
    }
    
    private void validateSignupRequest(SignUpOrLoginRequest request) {
        if ((request.getEmail() == null || request.getEmail().trim().isEmpty()) &&
            (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty())) {
            throw new AuthenticationException(
                    HttpStatus.SC_BAD_REQUEST,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Either email or phone number is required"
            );
        }
        
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new AuthenticationException(
                    HttpStatus.SC_BAD_REQUEST,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Password is required"
            );
        }
        
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new AuthenticationException(
                    HttpStatus.SC_BAD_REQUEST,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Name is required"
            );
        }
    }
    
    private void validateLoginRequest(SignUpOrLoginRequest request) {
        if ((request.getEmail() == null || request.getEmail().trim().isEmpty()) &&
            (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty())) {
            throw new AuthenticationException(
                    HttpStatus.SC_BAD_REQUEST,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Either email or phone number is required"
            );
        }
        
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new AuthenticationException(
                    HttpStatus.SC_BAD_REQUEST,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Password is required"
            );
        }
    }
    
    private void markExpiredSessionsAsInvalid(UserEntity user) {
        LocalDateTime now = LocalDateTime.now();
        user.getSessions().stream()
                .filter(session -> session.getUserSessionStatus().equals(UserSessionStatus.VALID))
                .filter(session -> session.getExpiry().isBefore(now))
                .forEach(session -> {
                    session.setUserSessionStatus(UserSessionStatus.INVALID);
                    userSessionRepository.save(session);
                });
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

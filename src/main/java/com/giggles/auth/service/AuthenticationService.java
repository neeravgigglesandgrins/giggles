package com.giggles.auth.service;

import com.giggles.auth.dto.request.LoginRequest;
import com.giggles.auth.dto.request.SignUpOrLoginRequest;
import com.giggles.auth.dto.response.LoginResponse;
import com.giggles.auth.dto.response.SignUpResponse;
import com.giggles.auth.entity.*;
import com.giggles.auth.enums.UserRole;
import com.giggles.auth.enums.UserSessionStatus;
import com.giggles.auth.enums.UserSessionType;
import com.giggles.auth.exception.AuthenticationException;
import com.giggles.auth.exception.CommonException;
import com.giggles.auth.exception.CommonErrorCode;
import com.giggles.auth.exception.ErrorCode;
import com.giggles.auth.repository.*;
import com.giggles.auth.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final UserSessionRepository userSessionRepository;
    private final AdminSessionRepository adminSessionRepository;
    private final JwtUtil jwtUtil;
    private final SqsService sqsService;
    
    @Autowired
    public AuthenticationService(
            UserRepository userRepository,
            AdminRepository adminRepository,
            UserSessionRepository userSessionRepository,
            AdminSessionRepository adminSessionRepository,
            JwtUtil jwtUtil,
            SqsService sqsService) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.userSessionRepository = userSessionRepository;
        this.adminSessionRepository = adminSessionRepository;
        this.jwtUtil = jwtUtil;
        this.sqsService = sqsService;
    }
    
    @Transactional
    public SignUpResponse signUpOrLogin(SignUpOrLoginRequest request, HttpServletRequest httpRequest) {
        log.info("Processing signup/login request for phone: {}", request.getPhoneNumber());
        
        // Step 1: Validate Request (handled by @Valid in controller)
        
        // Step 2: Business Validation
        validateSignup(request);
        
        // Step 3: Process Normal Signup or Login
        if (Boolean.TRUE.equals(request.getIsAnonymous())) {
            return signUpOrLoginAnonymousUser(request, httpRequest);
        } else {
            return processNormalSignUpOrLogin(request, httpRequest);
        }
    }
    
    private void validateSignup(SignUpOrLoginRequest request) {
        log.debug("Validating signup request");
        // Additional business validation can be added here
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new AuthenticationException(
                    HttpStatus.SC_BAD_REQUEST,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Phone number is required"
            );
        }
    }
    
    private SignUpResponse processNormalSignUpOrLogin(SignUpOrLoginRequest request, HttpServletRequest httpRequest) {
        log.info("Processing normal signup/login for phone: {}", request.getPhoneNumber());
        
        // Step 5: Validate Phone Number (Firebase verification removed)
        validateSignUpOrLoginRequest(request);
        
        // Step 6: Decision - Account Exists?
        if (request.getRole() == UserRole.USER) {
            return processUserSignUpOrLogin(request, httpRequest);
        } else {
            return processAdminSignUpOrLogin(request, httpRequest);
        }
    }
    
    private SignUpResponse processUserSignUpOrLogin(SignUpOrLoginRequest request, HttpServletRequest httpRequest) {
        boolean accountExists = userRepository.existsByPhoneNumber(request.getPhoneNumber());
        
        if (!accountExists) {
            // New Account - Sign Up
            return signUpUserAccount(request, httpRequest);
        } else {
            // Existing Account
            UserEntity user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                    .orElseThrow(() -> new AuthenticationException(
                            HttpStatus.SC_NOT_FOUND,
                            ErrorCode.INVALID_CREDENTIALS,
                            "User not found"
                    ));
            
            // Step 7: Decision - AuthToken is NULL?
            // For existing accounts, we need to login to get a new token
            LoginResponse loginResponse = loginUser(user, httpRequest);
            
            // Step 8: Build Final Response
            return createSignUpResponseFromLoginResponse(loginResponse, false);
        }
    }
    
    private SignUpResponse processAdminSignUpOrLogin(SignUpOrLoginRequest request, HttpServletRequest httpRequest) {
        boolean accountExists = adminRepository.existsByPhoneNumber(request.getPhoneNumber());
        
        if (!accountExists) {
            throw new AuthenticationException(
                    HttpStatus.SC_NOT_FOUND,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Admin account not found. Please contact administrator."
            );
        } else {
            AdminEntity admin = adminRepository.findByPhoneNumber(request.getPhoneNumber())
                    .orElseThrow(() -> new AuthenticationException(
                            HttpStatus.SC_NOT_FOUND,
                            ErrorCode.INVALID_CREDENTIALS,
                            "Admin not found"
                    ));
            
            LoginResponse loginResponse = loginAdmin(admin, httpRequest);
            return createSignUpResponseFromLoginResponse(loginResponse, false);
        }
    }
    
    private void validateSignUpOrLoginRequest(SignUpOrLoginRequest request) {
        log.debug("Validating signup/login request");
        
        // Firebase OTP verification removed - basic phone number validation only
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new AuthenticationException(
                    HttpStatus.SC_BAD_REQUEST,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Phone number is required"
            );
        }
        
        // Basic phone number format validation (optional - can be enhanced)
        String phoneNumber = request.getPhoneNumber().trim();
        if (phoneNumber.length() < 10) {
            throw new AuthenticationException(
                    HttpStatus.SC_BAD_REQUEST,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Invalid phone number format"
            );
        }
        
        log.info("Phone number validation passed for phone: {}", request.getPhoneNumber());
    }
    
    private SignUpResponse signUpUserAccount(SignUpOrLoginRequest request, HttpServletRequest httpRequest) {
        log.info("Creating new user account for phone: {}", request.getPhoneNumber());
        
        // Create AccountEntity (UserEntity)
        UserEntity user = new UserEntity();
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEmail(request.getEmail());
        user.setRole(UserRole.USER);
        user.setUserSessionType(UserSessionType.SINGLE);
        user.setIsAnonymous(false);
        user.setLoginAttempts(0);
        user.setIsLocked(false);
        
        user = userRepository.save(user);
        
        // Generate JWT Access Token
        String token = jwtUtil.generateToken(user.getId(), user.getPhoneNumber(), user.getRole().name());
        LocalDateTime expiry = jwtUtil.getExpiryDateTime();
        
        // Create Session
        UserSessionEntity session = new UserSessionEntity();
        session.setUser(user);
        session.setToken(token);
        session.setExpiry(expiry);
        session.setUserSessionStatus(UserSessionStatus.VALID);
        session.setIpAddress(getClientIpAddress(httpRequest));
        session.setUserAgent(httpRequest.getHeader("User-Agent"));
        userSessionRepository.save(session);
        
        // Create Profile
        ProfileEntity profile = new ProfileEntity();
        profile.setUser(user);
        user.setProfile(profile);
        userRepository.save(user);
        
        // Create Vehicle (optional - can be created later)
        VehicleEntity vehicle = new VehicleEntity();
        vehicle.setUser(user);
        vehicle.setVehicleNumber("TEMP-" + user.getId());
        vehicle.setVehicleType("CAR");
        user.getVehicles().add(vehicle);
        userRepository.save(user);
        
        // Upload event to SQS
        sqsService.sendSignupEvent(user.getId(), user.getPhoneNumber(), user.getRole().name());
        
        log.info("User account created successfully with ID: {}", user.getId());
        
        // Return SignUpResponse
        return SignUpResponse.builder()
                .userId(user.getId())
                .authToken(token)
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isNewAccount(true)
                .build();
    }
    
    private LoginResponse loginUser(UserEntity user, HttpServletRequest httpRequest) {
        log.info("Logging in user with ID: {}", user.getId());
        
        // Validate login
        validateLogin(user);
        
        // Check login attempts
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new AuthenticationException(
                    HttpStatus.SC_LOCKED,
                    ErrorCode.ACCOUNT_LOCKED,
                    "Account is locked. Please contact support."
            );
        }
        
        // Check active sessions
        checkActiveUserSession(user);
        
        // Generate JWT Access Token
        String token = jwtUtil.generateToken(user.getId(), user.getPhoneNumber(), user.getRole().name());
        LocalDateTime expiry = jwtUtil.getExpiryDateTime();
        
        // Create new session
        UserSessionEntity session = new UserSessionEntity();
        session.setUser(user);
        session.setToken(token);
        session.setExpiry(expiry);
        session.setUserSessionStatus(UserSessionStatus.VALID);
        session.setIpAddress(getClientIpAddress(httpRequest));
        session.setUserAgent(httpRequest.getHeader("User-Agent"));
        userSessionRepository.save(session);
        
        // Reset login attempts on successful login
        user.setLoginAttempts(0);
        user.setIsLocked(false);
        userRepository.save(user);
        
        log.info("User logged in successfully with ID: {}", user.getId());
        
        return LoginResponse.builder()
                .userId(user.getId())
                .authToken(token)
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
    
    private LoginResponse loginAdmin(AdminEntity admin, HttpServletRequest httpRequest) {
        log.info("Logging in admin with ID: {}", admin.getId());
        
        // Validate login
        if (Boolean.TRUE.equals(admin.getIsLocked())) {
            throw new AuthenticationException(
                    HttpStatus.SC_LOCKED,
                    ErrorCode.ACCOUNT_LOCKED,
                    "Account is locked. Please contact support."
            );
        }
        
        // Check active sessions
        checkActiveAdminSession(admin);
        
        // Generate JWT Access Token
        String token = jwtUtil.generateToken(admin.getId(), admin.getPhoneNumber(), UserRole.ADMIN.name());
        LocalDateTime expiry = jwtUtil.getExpiryDateTime();
        
        // Create new session
        AdminSessionEntity session = new AdminSessionEntity();
        session.setAdmin(admin);
        session.setToken(token);
        session.setExpiry(expiry);
        session.setUserSessionStatus(UserSessionStatus.VALID);
        session.setIpAddress(getClientIpAddress(httpRequest));
        session.setUserAgent(httpRequest.getHeader("User-Agent"));
        adminSessionRepository.save(session);
        
        // Reset login attempts on successful login
        admin.setLoginAttempts(0);
        admin.setIsLocked(false);
        adminRepository.save(admin);
        
        log.info("Admin logged in successfully with ID: {}", admin.getId());
        
        return LoginResponse.builder()
                .userId(admin.getId())
                .authToken(token)
                .phoneNumber(admin.getPhoneNumber())
                .email(admin.getEmail())
                .role(UserRole.ADMIN.name())
                .build();
    }
    
    private void validateLogin(UserEntity user) {
        // Additional login validation can be added here
        if (user == null) {
            throw new AuthenticationException(
                    HttpStatus.SC_UNAUTHORIZED,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Invalid credentials"
            );
        }
    }
    
    private void checkActiveUserSession(UserEntity userEntity) {
        // If MULTI session type, skip the check - allow multiple logins
        if (userEntity.getUserSessionType() == UserSessionType.MULTI) {
            log.debug("MULTI session type - allowing multiple sessions for user: {}", userEntity.getId());
            return;
        }
        
        // SINGLE session type: Check for existing active sessions
        if (CollectionUtils.isEmpty(userEntity.getActiveSessionsEntities())) {
            log.debug("No active sessions found for user: {}", userEntity.getId());
            return;
        }
        
        for (UserSessionEntity session : userEntity.getActiveSessionsEntities()) {
            if (session.getUserSessionStatus().equals(UserSessionStatus.VALID)) {
                if (session.getExpiry().isBefore(LocalDateTime.now())) {
                    // Expired, mark as INVALID
                    updateUserSession(session);
                } else {
                    // Active session exists - BLOCK new login
                    log.warn("Active session exists for user: {}", userEntity.getId());
                    throw new CommonException(
                            HttpStatus.SC_UNAUTHORIZED,
                            ErrorCode.ANOTHER_ACTIVE_SESSION_PRESENT,
                            CommonErrorCode.ANOTHER_ACTIVE_SESSION_PRESENT
                    );
                }
            }
        }
    }
    
    private void checkActiveAdminSession(AdminEntity adminEntity) {
        // If MULTI session type, skip the check - allow multiple logins
        if (adminEntity.getUserSessionType() == UserSessionType.MULTI) {
            log.debug("MULTI session type - allowing multiple sessions for admin: {}", adminEntity.getId());
            return;
        }
        
        // SINGLE session type: Check for existing active sessions
        if (CollectionUtils.isEmpty(adminEntity.getActiveSessionsEntities())) {
            log.debug("No active sessions found for admin: {}", adminEntity.getId());
            return;
        }
        
        for (AdminSessionEntity session : adminEntity.getActiveSessionsEntities()) {
            if (session.getUserSessionStatus().equals(UserSessionStatus.VALID)) {
                if (session.getExpiry().isBefore(LocalDateTime.now())) {
                    // Expired, mark as INVALID
                    updateAdminSession(session);
                } else {
                    // Active session exists - BLOCK new login
                    log.warn("Active session exists for admin: {}", adminEntity.getId());
                    throw new CommonException(
                            HttpStatus.SC_UNAUTHORIZED,
                            ErrorCode.ANOTHER_ACTIVE_SESSION_PRESENT,
                            CommonErrorCode.ANOTHER_ACTIVE_SESSION_PRESENT
                    );
                }
            }
        }
    }
    
    private void updateUserSession(UserSessionEntity session) {
        session.setUserSessionStatus(UserSessionStatus.INVALID);
        userSessionRepository.save(session);
        log.debug("Marked user session as INVALID: {}", session.getId());
    }
    
    private void updateAdminSession(AdminSessionEntity session) {
        session.setUserSessionStatus(UserSessionStatus.INVALID);
        adminSessionRepository.save(session);
        log.debug("Marked admin session as INVALID: {}", session.getId());
    }
    
    private SignUpResponse signUpOrLoginAnonymousUser(SignUpOrLoginRequest request, HttpServletRequest httpRequest) {
        log.info("Processing anonymous user signup/login");
        // Similar logic but with isAnonymous = true
        // For now, treating as normal signup but with anonymous flag
        request.setIsAnonymous(true);
        return processNormalSignUpOrLogin(request, httpRequest);
    }
    
    private SignUpResponse createSignUpResponseFromLoginResponse(LoginResponse loginResponse, boolean isNewAccount) {
        return SignUpResponse.builder()
                .userId(loginResponse.getUserId())
                .authToken(loginResponse.getAuthToken())
                .phoneNumber(loginResponse.getPhoneNumber())
                .email(loginResponse.getEmail())
                .role(loginResponse.getRole())
                .isNewAccount(isNewAccount)
                .build();
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Processing login request for phone: {}", request.getPhoneNumber());
        
        // Firebase OTP verification removed - basic validation only
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new AuthenticationException(
                    HttpStatus.SC_BAD_REQUEST,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Phone number is required"
            );
        }
        
        if (request.getRole() == UserRole.USER) {
            UserEntity user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                    .orElseThrow(() -> new AuthenticationException(
                            HttpStatus.SC_NOT_FOUND,
                            ErrorCode.INVALID_CREDENTIALS,
                            "User not found"
                    ));
            return loginUser(user, httpRequest);
        } else {
            AdminEntity admin = adminRepository.findByPhoneNumber(request.getPhoneNumber())
                    .orElseThrow(() -> new AuthenticationException(
                            HttpStatus.SC_NOT_FOUND,
                            ErrorCode.INVALID_CREDENTIALS,
                            "Admin not found"
                    ));
            return loginAdmin(admin, httpRequest);
        }
    }
}


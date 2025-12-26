package com.giggles.auth.entity;

import com.giggles.auth.enums.UserRole;
import com.giggles.auth.enums.UserSessionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {
    
    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;
    
    @Column(name = "email")
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    private UserSessionType userSessionType = UserSessionType.SINGLE;
    
    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous = false;
    
    @Column(name = "login_attempts")
    private Integer loginAttempts = 0;
    
    @Column(name = "is_locked")
    private Boolean isLocked = false;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSessionEntity> sessions = new ArrayList<>();
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private ProfileEntity profile;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VehicleEntity> vehicles = new ArrayList<>();
    
    public List<UserSessionEntity> getActiveSessionsEntities() {
        return sessions.stream()
                .filter(session -> session.getUserSessionStatus().equals(com.giggles.auth.enums.UserSessionStatus.VALID))
                .toList();
    }
}


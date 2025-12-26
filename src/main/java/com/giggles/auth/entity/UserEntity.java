package com.giggles.auth.entity;

import com.giggles.auth.enums.UserRole;
import com.giggles.auth.enums.UserSessionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "email", unique = true)
    private String email;
    
    @Column(name = "phone_number", unique = true)
    private String phoneNumber;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "password", nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.USER;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    private UserSessionType userSessionType = UserSessionType.MULTI;
    
    @Column(name = "login_attempts")
    private Integer loginAttempts = 0;
    
    @Column(name = "is_locked")
    private Boolean isLocked = false;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSessionEntity> sessions = new ArrayList<>();
    
    public List<UserSessionEntity> getActiveSessionsEntities() {
        return sessions.stream()
                .filter(session -> Objects.equals(session.getUserSessionStatus(), com.giggles.auth.enums.UserSessionStatus.VALID))
                .filter(session -> !session.getExpiry().isBefore(java.time.LocalDateTime.now()))
                .toList();
    }
}


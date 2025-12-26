package com.giggles.auth.entity;

import com.giggles.auth.enums.UserSessionStatus;
import com.giggles.auth.enums.UserSessionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "admins")
public class AdminEntity extends BaseEntity {
    
    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "username", unique = true, nullable = false)
    private String username;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    private UserSessionType userSessionType = UserSessionType.SINGLE;
    
    @Column(name = "login_attempts")
    private Integer loginAttempts = 0;
    
    @Column(name = "is_locked")
    private Boolean isLocked = false;
    
    @OneToMany(mappedBy = "admin", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdminSessionEntity> sessions = new ArrayList<>();
    
    public List<AdminSessionEntity> getActiveSessionsEntities() {
        return sessions.stream()
                .filter(session -> session.getUserSessionStatus().equals(UserSessionStatus.VALID))
                .toList();
    }
}


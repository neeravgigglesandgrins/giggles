package com.giggles.auth.entity;

import com.giggles.auth.enums.UserSessionStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "admin_sessions")
public class AdminSessionEntity extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private AdminEntity admin;
    
    @Column(name = "token", unique = true, nullable = false, length = 2000)
    private String token;
    
    @Column(name = "expiry", nullable = false)
    private LocalDateTime expiry;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserSessionStatus userSessionStatus = UserSessionStatus.VALID;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
}


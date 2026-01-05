package com.giggles.auth.entity;

import com.giggles.auth.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "bookings")
public class BookingEntity extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private SlotEntity slot;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status = BookingStatus.PENDING;
    
    @Column(name = "payment_id")
    private String paymentId;
    
    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}


package com.giggles.auth.repository;

import com.giggles.auth.entity.BookingEntity;
import com.giggles.auth.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
    
    List<BookingEntity> findByUserIdAndDeletedFalse(Long userId);
    
    Optional<BookingEntity> findByIdAndDeletedFalse(Long id);
    
    @Query("SELECT b FROM BookingEntity b WHERE b.status = :status " +
           "AND b.reservedAt < :beforeTime " +
           "AND b.deleted = false")
    List<BookingEntity> findExpiredPendingBookings(
            @Param("status") BookingStatus status,
            @Param("beforeTime") LocalDateTime beforeTime
    );
    
    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.slot.id = :slotId " +
           "AND b.status = :status " +
           "AND b.deleted = false")
    Long countBySlotIdAndStatus(
            @Param("slotId") Long slotId,
            @Param("status") BookingStatus status
    );
    
    Optional<BookingEntity> findByPaymentIdAndDeletedFalse(String paymentId);
}


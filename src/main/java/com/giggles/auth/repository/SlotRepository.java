package com.giggles.auth.repository;

import com.giggles.auth.entity.SlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotRepository extends JpaRepository<SlotEntity, Long> {
    
    @Query("SELECT s FROM SlotEntity s WHERE s.branch.id = :branchId " +
           "AND s.slotDate = :slotDate " +
           "AND s.deleted = false " +
           "ORDER BY s.startTime")
    List<SlotEntity> findByBranchIdAndSlotDate(
            @Param("branchId") Long branchId,
            @Param("slotDate") LocalDate slotDate
    );
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SlotEntity s WHERE s.id = :slotId AND s.deleted = false")
    Optional<SlotEntity> findByIdWithLock(@Param("slotId") Long slotId);
    
    @Query("SELECT s FROM SlotEntity s WHERE s.branch.id = :branchId " +
           "AND s.slotDate = :slotDate " +
           "AND s.startTime = :startTime " +
           "AND s.endTime = :endTime " +
           "AND s.deleted = false")
    Optional<SlotEntity> findByBranchDateAndTime(
            @Param("branchId") Long branchId,
            @Param("slotDate") LocalDate slotDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SlotEntity s WHERE s.branch.id = :branchId " +
           "AND s.slotDate = :slotDate " +
           "AND s.startTime = :startTime " +
           "AND s.endTime = :endTime " +
           "AND s.deleted = false")
    Optional<SlotEntity> findByBranchDateAndTimeWithLock(
            @Param("branchId") Long branchId,
            @Param("slotDate") LocalDate slotDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}


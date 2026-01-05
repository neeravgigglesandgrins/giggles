package com.giggles.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "slots", 
       uniqueConstraints = @UniqueConstraint(
           name = "uk_slot_branch_date_time",
           columnNames = {"branch_id", "slot_date", "start_time", "end_time"}
       ))
public class SlotEntity extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;
    
    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;
    
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    
    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity = 2;
    
    @Column(name = "booked_count", nullable = false)
    private Integer bookedCount = 0;
    
    @OneToMany(mappedBy = "slot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingEntity> bookings = new ArrayList<>();
    
    public boolean isAvailable() {
        return bookedCount < maxCapacity;
    }
    
    public int getAvailableCount() {
        return maxCapacity - bookedCount;
    }
}


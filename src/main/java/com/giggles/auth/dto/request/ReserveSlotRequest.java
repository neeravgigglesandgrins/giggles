package com.giggles.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReserveSlotRequest {
    
    @NotNull(message = "Branch ID is required")
    private Long branchId;
    
    @NotNull(message = "Slot date is required")
    private LocalDate slotDate;
    
    @NotNull(message = "Start time is required")
    private LocalTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalTime endTime;
}


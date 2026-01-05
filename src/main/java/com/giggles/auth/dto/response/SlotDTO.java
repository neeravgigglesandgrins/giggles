package com.giggles.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotDTO {
    
    private Long id;
    private Long branchId;
    private String branchName;
    private String city;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxCapacity;
    private Integer bookedCount;
    private Integer availableCount;
    private Boolean isAvailable;
}


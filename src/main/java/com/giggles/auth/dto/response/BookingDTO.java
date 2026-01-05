package com.giggles.auth.dto.response;

import com.giggles.auth.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {
    
    private Long id;
    private Long userId;
    private Long slotId;
    private BookingStatus status;
    private String paymentId;
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String branchName;
    private String city;
}


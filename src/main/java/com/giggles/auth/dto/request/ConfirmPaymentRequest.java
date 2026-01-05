package com.giggles.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConfirmPaymentRequest {
    
    @NotNull(message = "Booking ID is required")
    private Long bookingId;
    
    @NotBlank(message = "Payment ID is required")
    private String paymentId;
    
    @NotNull(message = "Payment success status is required")
    private Boolean paymentSuccess;
}


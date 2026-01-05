package com.giggles.auth.scheduler;

import com.giggles.auth.service.BookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BookingCleanupScheduler {
    
    @Autowired
    private BookingService bookingService;
    
    // Run every 2 minutes
    @Scheduled(fixedRate = 120000)
    public void expirePendingBookings() {
        try {
            log.debug("Running scheduled cleanup for expired bookings");
            bookingService.expirePendingBookings();
        } catch (Exception e) {
            log.error("Error in scheduled booking cleanup: {}", e.getMessage(), e);
        }
    }
}


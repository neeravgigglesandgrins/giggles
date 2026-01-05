package com.giggles.auth.service;

import com.giggles.auth.dto.request.ConfirmPaymentRequest;
import com.giggles.auth.dto.request.ReserveSlotRequest;
import com.giggles.auth.dto.response.BookingDTO;
import com.giggles.auth.dto.response.ReserveSlotResponse;
import com.giggles.auth.dto.response.SlotDTO;
import com.giggles.auth.entity.BookingEntity;
import com.giggles.auth.entity.BranchEntity;
import com.giggles.auth.entity.SlotEntity;
import com.giggles.auth.entity.UserEntity;
import com.giggles.auth.enums.BookingStatus;
import com.giggles.auth.exception.AuthenticationException;
import com.giggles.auth.exception.ErrorCode;
import com.giggles.auth.repository.BookingRepository;
import com.giggles.auth.repository.BranchRepository;
import com.giggles.auth.repository.SlotRepository;
import com.giggles.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookingService {
    
    private static final int RESERVATION_TIMEOUT_MINUTES = 10;
    
    @Autowired
    private SlotRepository slotRepository;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private BranchRepository branchRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<SlotDTO> getAvailableSlots(Long branchId, LocalDate slotDate) {
        log.info("Fetching available slots for branch: {}, date: {}", branchId, slotDate);
        
        // Validate branch exists
        BranchEntity branch = branchRepository.findByIdAndDeletedFalse(branchId)
                .orElseThrow(() -> new AuthenticationException(
                        HttpStatus.SC_NOT_FOUND,
                        ErrorCode.INVALID_CREDENTIALS,
                        "Branch not found"
                ));
        
        // Get existing slots
        List<SlotEntity> slots = slotRepository.findByBranchIdAndSlotDate(branchId, slotDate);
        
        // If no slots exist for this date, create all slots for the day (9 AM - 7 PM)
        if (slots.isEmpty()) {
            log.info("No slots found for branch: {}, date: {}. Creating slots...", branchId, slotDate);
            createAllSlotsForDate(branch, slotDate);
            // Fetch again after creation
            slots = slotRepository.findByBranchIdAndSlotDate(branchId, slotDate);
        }

        // Filter out fully booked slots - only return slots with available capacity
        return slots.stream()
                .filter(SlotEntity::isAvailable)  // Only slots where bookedCount < maxCapacity
                .map(this::convertToSlotDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReserveSlotResponse reserveSlot(ReserveSlotRequest request, Long userId) {
        log.info("Reserving slot for user: {}, branch: {}, date: {}, time: {} - {}",
                userId, request.getBranchId(), request.getSlotDate(), 
                request.getStartTime(), request.getEndTime());
        BranchEntity branch = branchRepository.findByIdAndDeletedFalse(request.getBranchId())
                .orElseThrow(() -> new AuthenticationException(
                        HttpStatus.SC_NOT_FOUND,
                        ErrorCode.INVALID_CREDENTIALS,
                        "Branch not found"
                ));
        
        if (!branch.getIsActive()) {
            throw new AuthenticationException(
                    HttpStatus.SC_BAD_REQUEST,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Branch is not active"
            );
        }
        
        // Get user
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException(
                        HttpStatus.SC_NOT_FOUND,
                        ErrorCode.INVALID_CREDENTIALS,
                        "User not found"
                ));
        
        // Get or create slot with pessimistic locking (race condition safe)
        SlotEntity slot = getOrCreateSlotWithLock(
                branch,
                request.getSlotDate(),
                request.getStartTime(),
                request.getEndTime()
        );
        
        // Check availability with lock (race condition safe)
        if (!slot.isAvailable()) {
            log.warn("Slot {} is full. bookedCount: {}, maxCapacity: {}", 
                    slot.getId(), slot.getBookedCount(), slot.getMaxCapacity());
            throw new AuthenticationException(
                    HttpStatus.SC_CONFLICT,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Slot is full. Maximum capacity reached."
            );
        }
        
        // Increment booked count
        slot.setBookedCount(slot.getBookedCount() + 1);
        slotRepository.save(slot);
        
        // Create booking with PENDING status
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(RESERVATION_TIMEOUT_MINUTES);
        
        BookingEntity booking = new BookingEntity();
        booking.setUser(user);
        booking.setSlot(slot);
        booking.setStatus(BookingStatus.PENDING);
        booking.setReservedAt(now);
        booking.setExpiresAt(expiresAt);
        
        booking = bookingRepository.save(booking);
        
        log.info("Slot reserved successfully. Booking ID: {}, Expires at: {}", 
                booking.getId(), expiresAt);
        
        // In real implementation, generate payment URL here
        String paymentUrl = "/payment/" + booking.getId();
        
        return ReserveSlotResponse.builder()
                .bookingId(booking.getId())
                .paymentUrl(paymentUrl)
                .expiresAt(expiresAt)
                .message("Slot reserved. Please complete payment within 10 minutes.")
                .build();
    }
    
    @Transactional
    public BookingDTO confirmPayment(ConfirmPaymentRequest request, Long userId) {
        log.info("Confirming payment for booking: {}, paymentId: {}, success: {}", 
                request.getBookingId(), request.getPaymentId(), request.getPaymentSuccess());
        
        BookingEntity booking = bookingRepository.findByIdAndDeletedFalse(request.getBookingId())
                .orElseThrow(() -> new AuthenticationException(
                        HttpStatus.SC_NOT_FOUND,
                        ErrorCode.INVALID_CREDENTIALS,
                        "Booking not found"
                ));
        
        // Verify booking belongs to user
        if (!booking.getUser().getId().equals(userId)) {
            throw new AuthenticationException(
                    HttpStatus.SC_FORBIDDEN,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Booking does not belong to this user"
            );
        }
        
        // Check if booking is still valid
        if (!booking.getStatus().equals(BookingStatus.PENDING)) {
            throw new AuthenticationException(
                    HttpStatus.SC_BAD_REQUEST,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Booking is not in PENDING status. Current status: " + booking.getStatus()
            );
        }
        
        if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Mark as expired and release slot
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            
            // Release slot capacity
            SlotEntity slot = booking.getSlot();
            slot.setBookedCount(Math.max(0, slot.getBookedCount() - 1));
            slotRepository.save(slot);
            
            throw new AuthenticationException(
                    HttpStatus.SC_BAD_REQUEST,
                    ErrorCode.INVALID_CREDENTIALS,
                    "Booking reservation has expired"
            );
        }
        
        if (request.getPaymentSuccess()) {
            // Payment successful - confirm booking
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaymentId(request.getPaymentId());
            bookingRepository.save(booking);
            
            log.info("Payment confirmed. Booking ID: {}", booking.getId());
        } else {
            // Payment failed - expire booking and release slot
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setPaymentId(request.getPaymentId());
            bookingRepository.save(booking);
            
            // Release slot capacity
            SlotEntity slot = booking.getSlot();
            slot.setBookedCount(Math.max(0, slot.getBookedCount() - 1));
            slotRepository.save(slot);
            
            log.info("Payment failed. Booking expired. Booking ID: {}", booking.getId());
        }
        
        return convertToBookingDTO(booking);
    }
    
    @Transactional
    public void expirePendingBookings() {
        log.info("Running scheduled job to expire pending bookings");
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(RESERVATION_TIMEOUT_MINUTES);
        List<BookingEntity> expiredBookings = bookingRepository.findExpiredPendingBookings(
                BookingStatus.PENDING, cutoffTime);
        
        log.info("Found {} expired pending bookings", expiredBookings.size());
        
        for (BookingEntity booking : expiredBookings) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            
            // Release slot capacity
            SlotEntity slot = booking.getSlot();
            slot.setBookedCount(Math.max(0, slot.getBookedCount() - 1));
            slotRepository.save(slot);
            
            log.debug("Expired booking ID: {}, Released slot ID: {}", booking.getId(), slot.getId());
        }
        
        log.info("Completed expiration of {} bookings", expiredBookings.size());
    }
    
    public List<BookingDTO> getUserBookings(Long userId) {
        List<BookingEntity> bookings = bookingRepository.findByUserIdAndDeletedFalse(userId);
        return bookings.stream()
                .map(this::convertToBookingDTO)
                .collect(Collectors.toList());
    }
    
    private SlotDTO convertToSlotDTO(SlotEntity slot) {
        return SlotDTO.builder()
                .id(slot.getId())
                .branchId(slot.getBranch().getId())
                .branchName(slot.getBranch().getName())
                .city(slot.getBranch().getCity())
                .slotDate(slot.getSlotDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .maxCapacity(slot.getMaxCapacity())
                .bookedCount(slot.getBookedCount())
                .availableCount(slot.getAvailableCount())
                .isAvailable(slot.isAvailable())
                .build();
    }
    
    private BookingDTO convertToBookingDTO(BookingEntity booking) {
        SlotEntity slot = booking.getSlot();
        return BookingDTO.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .slotId(slot.getId())
                .status(booking.getStatus())
                .paymentId(booking.getPaymentId())
                .reservedAt(booking.getReservedAt())
                .expiresAt(booking.getExpiresAt())
                .slotDate(slot.getSlotDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .branchName(slot.getBranch().getName())
                .city(slot.getBranch().getCity())
                .build();
    }
    
    /**
     * Get or create slot with pessimistic locking to handle race conditions.
     * If slot doesn't exist, creates it. If another thread creates it first,
     * handles unique constraint violation and fetches the existing slot.
     */
    @Transactional
    private SlotEntity getOrCreateSlotWithLock(
            BranchEntity branch,
            LocalDate slotDate,
            LocalTime startTime,
            LocalTime endTime) {
        
        // First, try to find with lock (this will wait if another thread is creating it)
        Optional<SlotEntity> existingSlot = slotRepository.findByBranchDateAndTimeWithLock(
                branch.getId(),
                slotDate,
                startTime,
                endTime
        );
        
        if (existingSlot.isPresent()) {
            log.debug("Slot found in database: {}", existingSlot.get().getId());
            return existingSlot.get();
        }
        
        // Slot doesn't exist, create it
        log.info("Slot not found, creating new slot for branch: {}, date: {}, time: {} - {}", 
                branch.getId(), slotDate, startTime, endTime);
        
        try {
            SlotEntity newSlot = new SlotEntity();
            newSlot.setBranch(branch);
            newSlot.setSlotDate(slotDate);
            newSlot.setStartTime(startTime);
            newSlot.setEndTime(endTime);
            newSlot.setMaxCapacity(2);
            newSlot.setBookedCount(0);
            
            newSlot = slotRepository.save(newSlot);
            log.info("Created new slot: {}", newSlot.getId());
            return newSlot;
            
        } catch (DataIntegrityViolationException e) {
            // Unique constraint violation - another thread created the slot
            // This is safe because we're in a transaction with lock
            log.info("Slot was created by another thread, fetching it now");
            
            // Fetch the slot that was created by the other thread
            // Use lock again to ensure we get the latest state
            return slotRepository.findByBranchDateAndTimeWithLock(
                    branch.getId(),
                    slotDate,
                    startTime,
                    endTime
            ).orElseThrow(() -> new RuntimeException(
                    "Failed to create or fetch slot after race condition"));
        }
    }
    
    /**
     * Create all slots for a date (9 AM to 7 PM, hourly).
     * Handles race conditions if multiple threads try to create slots simultaneously.
     */
    @Transactional
    private void createAllSlotsForDate(BranchEntity branch, LocalDate slotDate) {
        LocalTime startTime = LocalTime.of(9, 0); // 9 AM
        LocalTime endTime = LocalTime.of(19, 0); // 7 PM
        LocalTime currentTime = startTime;
        
        while (currentTime.isBefore(endTime)) {
            LocalTime slotEndTime = currentTime.plusHours(1);
            
            // Check if already exists (race condition check)
            Optional<SlotEntity> existing = slotRepository.findByBranchDateAndTime(
                    branch.getId(), slotDate, currentTime, slotEndTime);
            
            if (existing.isEmpty()) {
                try {
                    SlotEntity slot = new SlotEntity();
                    slot.setBranch(branch);
                    slot.setSlotDate(slotDate);
                    slot.setStartTime(currentTime);
                    slot.setEndTime(slotEndTime);
                    slot.setMaxCapacity(2);
                    slot.setBookedCount(0);
                    
                    slotRepository.save(slot);
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    // Another thread created it, skip
                    log.debug("Slot already created by another thread: {} - {}", currentTime, slotEndTime);
                }
            }
            
            currentTime = slotEndTime;
        }
    }
}


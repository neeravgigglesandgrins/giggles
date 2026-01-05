package com.giggles.auth.controller;

import com.giggles.auth.dto.request.ConfirmPaymentRequest;
import com.giggles.auth.dto.request.ReserveSlotRequest;
import com.giggles.auth.dto.response.BookingDTO;
import com.giggles.auth.dto.response.ReserveSlotResponse;
import com.giggles.auth.dto.response.SlotDTO;
import com.giggles.auth.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@Slf4j
public class BookingController {
    
    private final BookingService bookingService;
    
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }
    
    @GetMapping("/slots")
    public ResponseEntity<List<SlotDTO>> getAvailableSlots(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate slotDate) {
        log.info("Fetching available slots for branch: {}, date: {}", branchId, slotDate);
        List<SlotDTO> slots = bookingService.getAvailableSlots(branchId, slotDate);
        return ResponseEntity.ok(slots);
    }
    
    @PostMapping("/reserve")
    public ResponseEntity<ReserveSlotResponse> reserveSlot(
            @Valid @RequestBody ReserveSlotRequest request,
            HttpServletRequest httpRequest) {
        log.info("Reserving slot request received");
        Long userId = (Long) httpRequest.getAttribute("userId");
        ReserveSlotResponse response = bookingService.reserveSlot(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/confirm-payment")
    public ResponseEntity<BookingDTO> confirmPayment(
            @Valid @RequestBody ConfirmPaymentRequest request,
            HttpServletRequest httpRequest) {
        log.info("Confirming payment request received");
        Long userId = (Long) httpRequest.getAttribute("userId");
        BookingDTO booking = bookingService.confirmPayment(request, userId);
        return ResponseEntity.ok(booking);
    }
    
    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingDTO>> getMyBookings(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        List<BookingDTO> bookings = bookingService.getUserBookings(userId);
        return ResponseEntity.ok(bookings);
    }
}


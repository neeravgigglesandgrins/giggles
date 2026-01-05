package com.giggles.auth.service;

import com.giggles.auth.entity.BranchEntity;
import com.giggles.auth.entity.SlotEntity;
import com.giggles.auth.repository.BranchRepository;
import com.giggles.auth.repository.SlotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SlotService {
    
    private static final LocalTime START_TIME = LocalTime.of(9, 0); // 9 AM
    private static final LocalTime END_TIME = LocalTime.of(19, 0); // 7 PM
    
    @Autowired
    private SlotRepository slotRepository;
    
    @Autowired
    private BranchRepository branchRepository;
    
    @Transactional
    public void createSlotsForDate(Long branchId, LocalDate date) {
        log.info("Creating slots for branch: {}, date: {}", branchId, date);
        
        BranchEntity branch = branchRepository.findByIdAndDeletedFalse(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found: " + branchId));
        
        List<SlotEntity> slots = new ArrayList<>();
        LocalTime currentTime = START_TIME;
        
        while (currentTime.isBefore(END_TIME)) {
            LocalTime endTime = currentTime.plusHours(1);
            
            // Check if slot already exists
            boolean exists = slotRepository.findByBranchDateAndTime(
                    branchId, date, currentTime, endTime).isPresent();
            
            if (!exists) {
                SlotEntity slot = new SlotEntity();
                slot.setBranch(branch);
                slot.setSlotDate(date);
                slot.setStartTime(currentTime);
                slot.setEndTime(endTime);
                slot.setMaxCapacity(2);
                slot.setBookedCount(0);
                
                slots.add(slot);
            }
            
            currentTime = endTime;
        }
        
        if (!slots.isEmpty()) {
            slotRepository.saveAll(slots);
            log.info("Created {} slots for branch: {}, date: {}", slots.size(), branchId, date);
        } else {
            log.info("All slots already exist for branch: {}, date: {}", branchId, date);
        }
    }
    
    @Transactional
    public void createSlotsForDateRange(Long branchId, LocalDate startDate, LocalDate endDate) {
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            createSlotsForDate(branchId, currentDate);
            currentDate = currentDate.plusDays(1);
        }
    }
}


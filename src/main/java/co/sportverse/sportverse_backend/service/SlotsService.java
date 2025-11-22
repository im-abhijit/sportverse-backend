package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.dto.CreateSlotsRequest;
import co.sportverse.sportverse_backend.entity.TimeSlot;
import co.sportverse.sportverse_backend.entity.VenueSlots;
import co.sportverse.sportverse_backend.repository.SlotsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SlotsService {

    @Autowired
    private SlotsRepository slotsRepository;

    public VenueSlots createSlots(CreateSlotsRequest request) {
        if (request.getVenueId() == null || request.getVenueId().trim().isEmpty()) {
            throw new IllegalArgumentException("venueId is required");
        }
        if (request.getDate() == null || request.getDate().trim().isEmpty()) {
            throw new IllegalArgumentException("date is required (yyyy-MM-dd)");
        }
        if (request.getSlots() == null || request.getSlots().isEmpty()) {
            throw new IllegalArgumentException("slots are required");
        }

        // Convert new slots to TimeSlot objects
        java.util.List<TimeSlot> newSlots = new java.util.ArrayList<>();
        for (CreateSlotsRequest.SlotDto s : request.getSlots()) {
            newSlots.add(new TimeSlot(s.getSlotId(), s.getStartTime(), s.getEndTime(), s.getStartTimeAmPm(), s.getEndTimeAmPm(), s.getPrice(), s.isBooked()));
        }

        // Check for overlaps within new slots
        if (hasOverlaps(newSlots)) {
            throw new IllegalStateException("New slots have overlapping time ranges");
        }

        // Check if slots already exist for this venue/date
        VenueSlots existing = slotsRepository.findByVenueIdAndDate(request.getVenueId(), request.getDate());
        
        if (existing != null && existing.getSlots() != null) {
            // Check for overlaps between new slots and existing slots
            if (hasOverlapsBetween(newSlots, existing.getSlots())) {
                throw new IllegalStateException("New slots overlap with existing slots for this venue and date");
            }
            
            // Merge new slots with existing slots
            java.util.List<TimeSlot> mergedSlots = new java.util.ArrayList<>(existing.getSlots());
            mergedSlots.addAll(newSlots);
            
            // Update existing document with merged slots
            existing.setSlots(mergedSlots);
            return slotsRepository.updateSlots(existing);
        } else {
            // No existing slots, create new
            VenueSlots venueSlots = new VenueSlots(request.getVenueId(), request.getDate(), newSlots);
            return slotsRepository.save(venueSlots);
        }
    }

    private boolean hasOverlaps(java.util.List<TimeSlot> slots) {
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                if (slotsOverlap(slots.get(i), slots.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasOverlapsBetween(java.util.List<TimeSlot> newSlots, java.util.List<TimeSlot> existingSlots) {
        for (TimeSlot newSlot : newSlots) {
            for (TimeSlot existingSlot : existingSlots) {
                if (slotsOverlap(newSlot, existingSlot)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean slotsOverlap(TimeSlot slot1, TimeSlot slot2) {
        String start1 = slot1.getStartTime();
        String end1 = slot1.getEndTime();
        String start1AmPm = slot1.getStartTimeAmPm();
        String end1AmPm = slot1.getEndTimeAmPm();
        
        String start2 = slot2.getStartTime();
        String end2 = slot2.getEndTime();
        String start2AmPm = slot2.getStartTimeAmPm();
        String end2AmPm = slot2.getEndTimeAmPm();
        
        // Convert 12-hour format with AM/PM to minutes for comparison
        int start1Minutes = timeToMinutes12Hour(start1, start1AmPm);
        int end1Minutes = timeToMinutes12Hour(end1, end1AmPm);
        int start2Minutes = timeToMinutes12Hour(start2, start2AmPm);
        int end2Minutes = timeToMinutes12Hour(end2, end2AmPm);
        
        // Two slots overlap if: start1 < end2 AND end1 > start2
        return start1Minutes < end2Minutes && end1Minutes > start2Minutes;
    }

    /**
     * Converts 12-hour format time with AM/PM to minutes since midnight.
     * Examples:
     * - "12:00" + "AM" = 0 minutes (midnight)
     * - "1:00" + "AM" = 60 minutes
     * - "12:00" + "PM" = 720 minutes (noon)
     * - "1:00" + "PM" = 780 minutes (13:00)
     * - "11:59" + "PM" = 1439 minutes (23:59)
     */
    private int timeToMinutes12Hour(String time, String amPm) {
        if (time == null || time.trim().isEmpty()) {
            return 0;
        }
        if (amPm == null || amPm.trim().isEmpty()) {
            return 0;
        }
        
        String[] parts = time.split(":");
        if (parts.length != 2) {
            return 0;
        }
        
        try {
            int hours = Integer.parseInt(parts[0].trim());
            int minutes = Integer.parseInt(parts[1].trim());
            String amPmUpper = amPm.trim().toUpperCase();
            
            // Validate AM/PM
            if (!"AM".equals(amPmUpper) && !"PM".equals(amPmUpper)) {
                return 0;
            }
            
            // Handle 12-hour format conversion
            if ("AM".equals(amPmUpper)) {
                // 12:XX AM becomes 0:XX (midnight hour)
                if (hours == 12) {
                    hours = 0;
                }
                // 1:XX AM to 11:XX AM stay as is
            } else { // PM
                // 12:XX PM stays as 12:XX (noon hour)
                if (hours != 12) {
                    hours += 12; // 1:XX PM becomes 13:XX, 11:XX PM becomes 23:XX
                }
            }
            
            return hours * 60 + minutes;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public VenueSlots getSlotsByVenueAndDate(String venueId, String date) {
        if (venueId == null || venueId.trim().isEmpty()) {
            throw new IllegalArgumentException("venueId is required");
        }
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("date is required (yyyy-MM-dd)");
        }
        return slotsRepository.findByVenueIdAndDate(venueId.trim(), date.trim());
    }

    public boolean deleteSlot(String venueId, String date, String slotId) {
        if (venueId == null || venueId.trim().isEmpty()) {
            throw new IllegalArgumentException("venueId is required");
        }
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("date is required (yyyy-MM-dd)");
        }
        if (slotId == null || slotId.trim().isEmpty()) {
            throw new IllegalArgumentException("slotId is required");
        }
        return slotsRepository.deleteSlot(venueId.trim(), date.trim(), slotId.trim());
    }
}



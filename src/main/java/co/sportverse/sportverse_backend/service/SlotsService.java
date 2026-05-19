package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.dto.CreateSlotsRequest;
import co.sportverse.sportverse_backend.entity.TimeSlot;
import co.sportverse.sportverse_backend.entity.VenueSlots;
import co.sportverse.sportverse_backend.repository.SlotsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SlotsService {

    @Autowired
    private SlotsRepository slotsRepository;

    public SlotReservationResult reserveSlotsForBooking(String venueId, String date, List<String> requestedSlotIds) {
        if (venueId == null || venueId.trim().isEmpty()) {
            throw new IllegalArgumentException("venueId is required");
        }
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("date is required (yyyy-MM-dd)");
        }
        List<String> slotIds = normalizeSlotIds(requestedSlotIds);

        Instant reservedAt = Instant.now();
        Instant reservationExpiresBefore = reservedAt.minus(10, ChronoUnit.MINUTES);
        boolean reserved = slotsRepository.reserveSlotsIfAvailable(
                venueId.trim(),
                date.trim(),
                slotIds,
                reservedAt,
                reservationExpiresBefore
        );
        if (!reserved) {
            throw new IllegalArgumentException("Selected slots are not available");
        }

        int totalAmount = calculateTotalAmount(venueId.trim(), date.trim(), slotIds);
        return new SlotReservationResult(slotIds, reservedAt, totalAmount);
    }

    public void releaseReservedSlotsForBooking(String venueId, String date, List<String> slotIds, Instant reservedAt) {
        if (venueId == null || venueId.trim().isEmpty() || date == null || date.trim().isEmpty()
                || slotIds == null || slotIds.isEmpty() || reservedAt == null) {
            return;
        }
        slotsRepository.releaseReservedSlots(venueId.trim(), date.trim(), slotIds, reservedAt);
    }

    public void ensureSlotsReservedForBooking(String venueId, String date, List<String> requestedSlotIds) {
        if (venueId == null || venueId.trim().isEmpty()) {
            throw new IllegalArgumentException("venueId is required");
        }
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("date is required (yyyy-MM-dd)");
        }
        List<String> slotIds = normalizeSlotIds(requestedSlotIds);
        boolean reserved = slotsRepository.areSlotsReserved(venueId.trim(), date.trim(), slotIds);
        if (!reserved) {
            throw new IllegalArgumentException("Selected slots are not reserved");
        }
    }

    public void markReservedSlotsBookedForBooking(String venueId, String date, List<String> requestedSlotIds) {
        if (venueId == null || venueId.trim().isEmpty()) {
            throw new IllegalArgumentException("venueId is required");
        }
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("date is required (yyyy-MM-dd)");
        }
        List<String> slotIds = normalizeSlotIds(requestedSlotIds);
        boolean updated = slotsRepository.markReservedSlotsBooked(venueId.trim(), date.trim(), slotIds);
        if (!updated) {
            throw new IllegalArgumentException("Selected slots are not reserved");
        }
    }

    private List<String> normalizeSlotIds(List<String> requestedSlotIds) {
        if (requestedSlotIds == null || requestedSlotIds.isEmpty()) {
            throw new IllegalArgumentException("slotIds are required");
        }
        LinkedHashSet<String> slotIds = new LinkedHashSet<>();
        for (String slotId : requestedSlotIds) {
            if (slotId == null || slotId.trim().isEmpty()) {
                throw new IllegalArgumentException("slotId is required");
            }
            slotIds.add(slotId.trim());
        }
        return new ArrayList<>(slotIds);
    }

    private int calculateTotalAmount(String venueId, String date, List<String> slotIds) {
        VenueSlots venueSlots = slotsRepository.findByVenueIdAndDate(venueId, date);
        if (venueSlots == null || venueSlots.getSlots() == null) {
            throw new IllegalArgumentException("Slots not found for venue/date");
        }

        Set<String> requestedSlotIds = new HashSet<>(slotIds);
        Set<String> pricedSlotIds = new HashSet<>();
        int totalAmount = 0;
        for (TimeSlot slot : venueSlots.getSlots()) {
            if (slot.getSlotId() != null && requestedSlotIds.contains(slot.getSlotId())) {
                pricedSlotIds.add(slot.getSlotId());
                if (slot.getPrice() > 0) {
                    totalAmount += slot.getPrice();
                }
            }
        }
        if (pricedSlotIds.size() != requestedSlotIds.size()) {
            throw new IllegalArgumentException("One or more selected slots were not found");
        }
        if (totalAmount <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }
        return totalAmount;
    }

    public static class SlotReservationResult {
        private final List<String> slotIds;
        private final Instant reservedAt;
        private final int totalAmount;

        public SlotReservationResult(List<String> slotIds, Instant reservedAt, int totalAmount) {
            this.slotIds = slotIds;
            this.reservedAt = reservedAt;
            this.totalAmount = totalAmount;
        }

        public List<String> getSlotIds() {
            return slotIds;
        }

        public Instant getReservedAt() {
            return reservedAt;
        }

        public int getTotalAmount() {
            return totalAmount;
        }
    }

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
            TimeSlot ts = new TimeSlot(s.getSlotId(), s.getStartTime(), s.getEndTime(),
                    s.getStartTimeAmPm(), s.getEndTimeAmPm(), s.getPrice(), s.isBooked());
            ts.setStatus(s.isBooked() ? "BOOKED" : "AVAILABLE");
            newSlots.add(ts);
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
            
            // Sort merged slots by start time
            mergedSlots.sort((slot1, slot2) -> {
                int start1Minutes = timeToMinutes12Hour(slot1.getStartTime(), slot1.getStartTimeAmPm());
                int start2Minutes = timeToMinutes12Hour(slot2.getStartTime(), slot2.getStartTimeAmPm());
                return Integer.compare(start1Minutes, start2Minutes);
            });
            
            // Update existing document with merged slots
            existing.setSlots(mergedSlots);
            return slotsRepository.updateSlots(existing);
        } else {
            // Sort new slots by start time before saving
            newSlots.sort((slot1, slot2) -> {
                int start1Minutes = timeToMinutes12Hour(slot1.getStartTime(), slot1.getStartTimeAmPm());
                int start2Minutes = timeToMinutes12Hour(slot2.getStartTime(), slot2.getStartTimeAmPm());
                return Integer.compare(start1Minutes, start2Minutes);
            });
            
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
        VenueSlots venueSlots = slotsRepository.findByVenueIdAndDate(venueId.trim(), date.trim());
        
        // Sort slots by start time if slots exist
        if (venueSlots != null && venueSlots.getSlots() != null && !venueSlots.getSlots().isEmpty()) {
            venueSlots.getSlots().sort((slot1, slot2) -> {
                int start1Minutes = timeToMinutes12Hour(slot1.getStartTime(), slot1.getStartTimeAmPm());
                int start2Minutes = timeToMinutes12Hour(slot2.getStartTime(), slot2.getStartTimeAmPm());
                return Integer.compare(start1Minutes, start2Minutes);
            });
        }
        
        return venueSlots;
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

    /**
     * Creates slots for {@code request} on the date {@code LocalDate.now() + daysAhead} (ISO format).
     */
    public VenueSlots createSlotsForDatePlusDays(CreateSlotsRequest request, long daysAhead) {
        if (request.getVenueId() == null || request.getVenueId().trim().isEmpty()) {
            throw new IllegalArgumentException("venueId is required");
        }
        if (request.getSlots() == null || request.getSlots().isEmpty()) {
            throw new IllegalArgumentException("slots are required");
        }
        String dateString = LocalDate.now().plusDays(daysAhead).format(DateTimeFormatter.ISO_LOCAL_DATE);
        request.setDate(dateString);
        return createSlots(request);
    }

    public VenueSlots createSlotsForNextDays(CreateSlotsRequest request) {
        return createSlotsForDatePlusDays(request, 15);
    }
}



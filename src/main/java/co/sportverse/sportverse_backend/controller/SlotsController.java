package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.CreateSlotsRequest;
import co.sportverse.sportverse_backend.dto.SlotsResponse;
import co.sportverse.sportverse_backend.entity.VenueSlots;
import co.sportverse.sportverse_backend.service.SlotsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/slots")
public class SlotsController {

    private static final Logger logger = LoggerFactory.getLogger(SlotsController.class);

    @Autowired
    private SlotsService slotsService;

    @PostMapping
    public ResponseEntity<ApiResponse> createSlots(@RequestBody CreateSlotsRequest request) {
        logger.info("POST /api/slots - Creating slots for venueId: {}, date: {}, slots count: {}",
                request.getVenueId(), request.getDate(),
                request.getSlots() != null ? request.getSlots().size() : 0);
        VenueSlots saved = slotsService.createSlots(request);
        logger.info("POST /api/slots - Successfully created slots. Document ID: {}, Total slots: {}",
                saved.getId(), saved.getSlots() != null ? saved.getSlots().size() : 0);
        return ResponseEntity.ok(new ApiResponse(true, "Slots created successfully", new SlotsResponse(saved)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getSlotsByVenueAndDate(
            @RequestParam(value = "venueId", required = false) String venueId,
            @RequestParam(value = "date", required = false) String date) {
        logger.info("GET /api/slots - Fetching slots for venueId: {}, date: {}", venueId, date);
        VenueSlots vs = slotsService.getSlotsByVenueAndDate(venueId, date);
        if (vs == null) {
            logger.info("GET /api/slots - No slots found for venueId: {}, date: {}", venueId, date);
            return ResponseEntity.ok(new ApiResponse(true, "No slots found for given venue and date", null));
        }
        logger.info("GET /api/slots - Successfully retrieved slots. Total slots: {}",
                vs.getSlots() != null ? vs.getSlots().size() : 0);
        return ResponseEntity.ok(new ApiResponse(true, "Slots retrieved successfully", new SlotsResponse(vs)));
    }

    @PostMapping("/next-days")
    public ResponseEntity<ApiResponse> createSlotsForNextDays(@RequestBody CreateSlotsRequest request) {
        logger.info("POST /api/slots/next-days - Creating slots for venueId: {}, slots count: {}",
                request.getVenueId(),
                request.getSlots() != null ? request.getSlots().size() : 0);
        VenueSlots saved = slotsService.createSlotsForNextDays(request);
        logger.info("POST /api/slots/next-days - Successfully created slots. Document ID: {}, Total slots: {}, Date: {}",
                saved.getId(), saved.getSlots() != null ? saved.getSlots().size() : 0, saved.getDate());
        return ResponseEntity.ok(new ApiResponse(true, "Slots created successfully", new SlotsResponse(saved)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse> deleteSlot(
            @RequestParam(value = "venueId", required = false) String venueId,
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "slotId", required = false) String slotId) {
        logger.info("DELETE /api/slots - Deleting slot. venueId: {}, date: {}, slotId: {}", venueId, date, slotId);
        boolean deleted = slotsService.deleteSlot(venueId, date, slotId);
        if (deleted) {
            logger.info("DELETE /api/slots - Successfully deleted slot. venueId: {}, date: {}, slotId: {}",
                    venueId, date, slotId);
            return ResponseEntity.ok(new ApiResponse(true, "Slot deleted successfully", null));
        } else {
            logger.warn("DELETE /api/slots - Slot not found. venueId: {}, date: {}, slotId: {}",
                    venueId, date, slotId);
            return ResponseEntity.ok(new ApiResponse(false, "Slot not found for given venue, date, and slotId", null));
        }
    }
}

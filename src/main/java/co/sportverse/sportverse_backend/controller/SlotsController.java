package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.CreateSlotsRequest;
import co.sportverse.sportverse_backend.dto.SlotsResponse;
import co.sportverse.sportverse_backend.entity.TimeSlot;
import co.sportverse.sportverse_backend.entity.VenueSlots;
import co.sportverse.sportverse_backend.service.SlotsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/slots")
@CrossOrigin(origins = {
        "https://sportverse.co.in",
        "http://localhost:8083"
})
public class SlotsController {

    private static final Logger logger = LoggerFactory.getLogger(SlotsController.class);

    @Autowired
    private SlotsService slotsService;

    @PostMapping
    public ResponseEntity<ApiResponse> createSlots(@RequestBody CreateSlotsRequest request) {
        logger.info("POST /api/slots - Creating slots for venueId: {}, date: {}, slots count: {}", 
                request.getVenueId(), request.getDate(), 
                request.getSlots() != null ? request.getSlots().size() : 0);
        try {
            if (request.getVenueId() == null || request.getVenueId().trim().isEmpty()) {
                logger.warn("POST /api/slots - Validation failed: venueId is required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "venueId is required"));
            }
            if (request.getDate() == null || request.getDate().trim().isEmpty()) {
                logger.warn("POST /api/slots - Validation failed: date is required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "date is required (yyyy-MM-dd)"));
            }
            if (request.getSlots() == null || request.getSlots().isEmpty()) {
                logger.warn("POST /api/slots - Validation failed: slots are required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "slots are required"));
            }

            VenueSlots saved = slotsService.createSlots(request);
            logger.info("POST /api/slots - Successfully created slots. Document ID: {}, Total slots: {}", 
                    saved.getId(), saved.getSlots() != null ? saved.getSlots().size() : 0);
            return ResponseEntity.ok(new ApiResponse(true, "Slots created successfully", new SlotsResponse(saved)));
        } catch (Exception e) {
            logger.error("POST /api/slots - Error creating slots for venueId: {}, date: {}", 
                    request.getVenueId(), request.getDate(), e);
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error creating slots: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getSlotsByVenueAndDate(
            @RequestParam(value = "venueId", required = false) String venueId,
            @RequestParam(value = "date", required = false) String date) {
        logger.info("GET /api/slots - Fetching slots for venueId: {}, date: {}", venueId, date);
        try {
            if (venueId == null || venueId.trim().isEmpty()) {
                logger.warn("GET /api/slots - Validation failed: venueId is required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "venueId is required"));
            }
            if (date == null || date.trim().isEmpty()) {
                logger.warn("GET /api/slots - Validation failed: date is required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "date is required (yyyy-MM-dd)"));
            }
            VenueSlots vs = slotsService.getSlotsByVenueAndDate(venueId, date);
            if (vs == null) {
                logger.info("GET /api/slots - No slots found for venueId: {}, date: {}", venueId, date);
                return ResponseEntity.ok(new ApiResponse(true, "No slots found for given venue and date", null));
            }
            logger.info("GET /api/slots - Successfully retrieved slots. Total slots: {}", 
                    vs.getSlots() != null ? vs.getSlots().size() : 0);
            return ResponseEntity.ok(new ApiResponse(true, "Slots retrieved successfully", new SlotsResponse(vs)));
        } catch (Exception e) {
            logger.error("GET /api/slots - Error fetching slots for venueId: {}, date: {}", venueId, date, e);
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error fetching slots: " + e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse> deleteSlot(
            @RequestParam(value = "venueId", required = false) String venueId,
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "slotId", required = false) String slotId) {
        logger.info("DELETE /api/slots - Deleting slot. venueId: {}, date: {}, slotId: {}", venueId, date, slotId);
        try {
            if (venueId == null || venueId.trim().isEmpty()) {
                logger.warn("DELETE /api/slots - Validation failed: venueId is required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "venueId is required"));
            }
            if (date == null || date.trim().isEmpty()) {
                logger.warn("DELETE /api/slots - Validation failed: date is required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "date is required (yyyy-MM-dd)"));
            }
            if (slotId == null || slotId.trim().isEmpty()) {
                logger.warn("DELETE /api/slots - Validation failed: slotId is required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "slotId is required"));
            }
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
        } catch (Exception e) {
            logger.error("DELETE /api/slots - Error deleting slot. venueId: {}, date: {}, slotId: {}", 
                    venueId, date, slotId, e);
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error deleting slot: " + e.getMessage()));
        }
    }
}



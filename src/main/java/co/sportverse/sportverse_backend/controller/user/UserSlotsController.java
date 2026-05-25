package co.sportverse.sportverse_backend.controller.user;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.AuthenticatedUser;
import co.sportverse.sportverse_backend.dto.SlotsResponse;
import co.sportverse.sportverse_backend.entity.VenueSlots;
import co.sportverse.sportverse_backend.service.SlotsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Authenticated user APIs for browsing venue availability (JWT required via Spring Security).
 */
@RestController
@RequestMapping("/api/user/slots")
public class UserSlotsController {

    private static final Logger logger = LoggerFactory.getLogger(UserSlotsController.class);

    @Autowired
    private SlotsService slotsService;

    /**
     * List time slots for a venue on a given date.
     *
     * @param venue venue id ({@link org.bson.types.ObjectId} string as stored)
     * @param date date in ISO form {@code yyyy-MM-dd}
     */
    @GetMapping
    public ResponseEntity<ApiResponse> listSlotsForVenueAndDate(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam String venue,
            @RequestParam String date) {
        String userLabel =
                authenticatedUser != null && authenticatedUser.getSubject() != null ? authenticatedUser.getSubject() : "anonymous";
        logger.info("GET /api/user/slots - user subject: {}, venue: {}, date: {}", userLabel, venue, date);

        VenueSlots vs = slotsService.getSlotsByVenueAndDate(venue, date);
        if (vs == null) {
            SlotsResponse empty = new SlotsResponse();
            empty.setVenueId(venue.trim());
            empty.setDate(date.trim());
            empty.setSlots(List.of());
            logger.info("GET /api/user/slots - no document for venue {}, date {}", venue, date);
            return ResponseEntity.ok(new ApiResponse(true, "No slots found for given venue and date", empty));
        }

        SlotsResponse data = new SlotsResponse(vs);
        int n = data.getSlots() != null ? data.getSlots().size() : 0;
        logger.info("GET /api/user/slots - {} slots for subject {}", n, userLabel);
        return ResponseEntity.ok(new ApiResponse(true, "Slots retrieved successfully", data));
    }
}

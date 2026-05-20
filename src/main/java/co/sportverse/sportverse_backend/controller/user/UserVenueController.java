package co.sportverse.sportverse_backend.controller.user;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.AuthenticatedUser;
import co.sportverse.sportverse_backend.dto.VenueResponse;
import co.sportverse.sportverse_backend.entity.Venue;
import co.sportverse.sportverse_backend.service.VenueService;
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
 * Public user APIs for venue discovery. If a valid JWT is supplied, it is used only for logging.
 */
@RestController
@RequestMapping("/api/user/venues")
public class UserVenueController {

    private static final Logger logger = LoggerFactory.getLogger(UserVenueController.class);

    @Autowired
    private VenueService venueService;

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse> getTrendingVenues(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        String userLabel =
                authenticatedUser != null && authenticatedUser.getSubject() != null ? authenticatedUser.getSubject() : "anonymous";
        logger.info("GET /api/user/venues/trending - user subject: {}", userLabel);

        List<Venue> venues = venueService.getTrendingVenues();
        List<VenueResponse> responses = venues.stream().map(VenueResponse::new).toList();

        logger.info("GET /api/user/venues/trending - {} venues for subject {}", responses.size(), userLabel);
        return ResponseEntity.ok(new ApiResponse(true, "Trending venues retrieved successfully", responses));
    }

    /**
     * Query venues by optional {@code city} and/or {@code sport}.
     * At least one of {@code city} or {@code sport} must be provided.
     */
    @GetMapping
    public ResponseEntity<ApiResponse> searchVenues(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String sport) {
        String userLabel =
                authenticatedUser != null && authenticatedUser.getSubject() != null ? authenticatedUser.getSubject() : "anonymous";
        logger.info("GET /api/user/venues - user subject: {}, city: {}, sport: {}", userLabel, city, sport);

        List<Venue> venues = venueService.searchVenuesByCityAndSport(city, sport);
        List<VenueResponse> responses = venues.stream().map(VenueResponse::new).toList();

        logger.info("GET /api/user/venues - {} venues for subject {}", responses.size(), userLabel);
        return ResponseEntity.ok(new ApiResponse(true, "Venues retrieved successfully", responses));
    }
}

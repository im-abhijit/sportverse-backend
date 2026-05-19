package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.CreateVenueRequest;
import co.sportverse.sportverse_backend.dto.VenueResponse;
import co.sportverse.sportverse_backend.entity.Venue;
import co.sportverse.sportverse_backend.service.VenueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private static final Logger logger = LoggerFactory.getLogger(VenueController.class);

    @Autowired
    private VenueService venueService;

    @PostMapping
    public ResponseEntity<ApiResponse> createVenue(@RequestBody CreateVenueRequest request) {
        logger.info("POST /api/venues - Processing venue. id: {}, name: {}, partnerId: {}, city: {}",
                request.getId(), request.getName(), request.getPartnerId(), request.getCity());
        var result = venueService.createOrUpdateVenue(request);
        VenueResponse response = new VenueResponse(result.getVenue());
        String message = result.isUpdate() ? "Venue updated successfully" : "Venue created successfully";
        return ResponseEntity.ok(new ApiResponse(true, message, response));
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<ApiResponse> getVenuesByCity(@PathVariable String city) {
        logger.info("GET /api/venues/city/{} - Fetching venues by city", city);
        java.util.List<Venue> venues = venueService.getVenuesByCity(city);
        java.util.List<VenueResponse> responses = venues.stream()
                .map(VenueResponse::new)
                .toList();
        logger.info("GET /api/venues/city/{} - Successfully retrieved {} venues", city, responses.size());
        return ResponseEntity.ok(new ApiResponse(true, "Venues retrieved successfully", responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getVenueById(@PathVariable String id) {
        logger.info("GET /api/venues/{} - Fetching venue by ID", id);
        Venue venue = venueService.getVenueById(id);
        if (venue != null) {
            logger.info("GET /api/venues/{} - Successfully retrieved venue", id);
            VenueResponse response = new VenueResponse(venue);
            return ResponseEntity.ok(new ApiResponse(true, "Venue found", response));
        } else {
            logger.warn("GET /api/venues/{} - Venue not found", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getVenues(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String sport,
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) String id) {
        logger.info("GET /api/venues - Fetching venues. city: {}, sport: {}, partnerId: {}, id: {}", city, sport, partnerId, id);
        java.util.List<Venue> venues = venueService.getVenues(city, sport, partnerId, id);
        java.util.List<VenueResponse> responses = venues.stream()
                .map(VenueResponse::new)
                .toList();
        logger.info("GET /api/venues - Successfully retrieved {} venues", responses.size());
        return ResponseEntity.ok(new ApiResponse(true, "Venues retrieved successfully", responses));
    }

    @GetMapping("/partner/{partnerId}")
    public ResponseEntity<ApiResponse> getVenuesByOwner(@PathVariable String partnerId) {
        logger.info("GET /api/venues/partner/{} - Fetching venues by partner", partnerId);
        java.util.List<Venue> venues = venueService.getVenues(null, null, partnerId, null);
        java.util.List<VenueResponse> responses = venues.stream()
                .map(VenueResponse::new)
                .toList();
        logger.info("GET /api/venues/partner/{} - Successfully retrieved {} venues", partnerId, responses.size());
        return ResponseEntity.ok(new ApiResponse(true, "Venues retrieved successfully", responses));
    }
}

package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.CreateVenueRequest;
import co.sportverse.sportverse_backend.dto.VenueResponse;
import co.sportverse.sportverse_backend.entity.Venue;
import co.sportverse.sportverse_backend.repository.VenueRepository;
import co.sportverse.sportverse_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/venues")
@CrossOrigin(origins = "*")
public class VenueController {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse> createVenue(
            @RequestBody CreateVenueRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            // Validate required fields
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Venue name is required"));
            }

            if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Venue location is required"));
            }

            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "User ID is required. Please provide X-User-Id header"));
            }

            // Validate photos (max 3)
            if (request.getPhotos() != null && request.getPhotos().size() > 3) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Maximum 3 photos allowed"));
            }

            // Create venue entity
            Venue venue = new Venue(
                request.getName(),
                request.getDescription(),
                request.getGames(),
                request.getLocation(),
                request.getPhotos(),
                userId,
                request.getCity()
            );

            // Save venue using repository
            Venue savedVenue = venueRepository.save(venue);

            // Mark the user as a venue owner
            userRepository.updateIsVenueOwner(userId, true);
            VenueResponse response = new VenueResponse(savedVenue);

            return ResponseEntity.ok(new ApiResponse(true, "Venue created successfully", response));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, "Error creating venue: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getVenueById(@PathVariable String id) {
        try {
            Venue venue = venueRepository.findById(id);
            if (venue != null) {
                VenueResponse response = new VenueResponse(venue);
                return ResponseEntity.ok(new ApiResponse(true, "Venue found", response));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, "Error fetching venue: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAllVenues() {
        try {
            java.util.List<Venue> venues = venueRepository.findAll();
            java.util.List<VenueResponse> responses = venues.stream()
                    .map(VenueResponse::new)
                    .toList();
            return ResponseEntity.ok(new ApiResponse(true, "Venues retrieved successfully", responses));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, "Error fetching venues: " + e.getMessage()));
        }
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<ApiResponse> getVenuesByCity(@PathVariable String city) {
        try {
            if (city == null || city.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "City is required"));
            }
            java.util.List<Venue> venues = venueRepository.findByCity(city.trim());
            java.util.List<VenueResponse> responses = venues.stream().map(VenueResponse::new).toList();
            return ResponseEntity.ok(new ApiResponse(true, "Venues retrieved successfully", responses));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error fetching venues: " + e.getMessage()));
        }
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<ApiResponse> getVenuesByOwner(@PathVariable String ownerId) {
        try {
            java.util.List<Venue> venues = venueRepository.findByOwnerId(ownerId);
            java.util.List<VenueResponse> responses = venues.stream()
                    .map(VenueResponse::new)
                    .toList();
            return ResponseEntity.ok(new ApiResponse(true, "Venues retrieved successfully", responses));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, "Error fetching venues: " + e.getMessage()));
        }
    }
}


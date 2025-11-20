package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.CreateVenueRequest;
import co.sportverse.sportverse_backend.dto.VenueResponse;
import co.sportverse.sportverse_backend.entity.Venue;
import co.sportverse.sportverse_backend.repository.VenueRepository;
import co.sportverse.sportverse_backend.repository.UserRepository;
import co.sportverse.sportverse_backend.repository.PartnerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/venues")
@CrossOrigin(origins = {
        "https://sportverse.co.in",
        "http://localhost:8083"
})
public class VenueController {

    private static final Logger logger = LoggerFactory.getLogger(VenueController.class);

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @PostMapping
    public ResponseEntity<ApiResponse> createVenue(@RequestBody CreateVenueRequest request) {
        logger.info("POST /api/venues - Processing venue. id: {}, name: {}, partnerId: {}, city: {}", 
                request.getId(), request.getName(), request.getPartnerId(), request.getCity());
        try {
            // Validate required fields
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                logger.warn("POST /api/venues - Validation failed: Venue name is required");
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Venue name is required"));
            }

            if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
                logger.warn("POST /api/venues - Validation failed: Venue location is required");
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Venue location is required"));
            }

            if (request.getPartnerId() == null || request.getPartnerId().trim().isEmpty()) {
                logger.warn("POST /api/venues - Validation failed: Partner ID is required");
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Partner ID is required. Please provide "));
            }

            if (request.getPartnerMobileNo() == null || request.getPartnerMobileNo().trim().isEmpty()) {
                logger.warn("POST /api/venues - Validation failed: Partner mobile no is required");
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Partner mobile no is required. Please provide"));
            }

            // Validate photos (max 3)
            if (request.getPhotos() != null && request.getPhotos().size() > 8) {
                logger.warn("POST /api/venues - Validation failed: Maximum 3 photos allowed. Received: {}", 
                        request.getPhotos().size());
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Maximum 3 photos allowed"));
            }

            Venue savedVenue;
            boolean isUpdate = false;

            // Check if venue exists by ID
            if (request.getId() != null && !request.getId().trim().isEmpty()) {
                Venue existingVenue = venueRepository.findById(request.getId().trim());
                if (existingVenue != null) {
                    // Update existing venue
                    logger.info("POST /api/venues - Updating existing venue. venueId: {}", request.getId());
                    isUpdate = true;
                    
                    // Create venue entity with updated values
                    Venue venue = new Venue(
                        request.getName(),
                        request.getDescription(),
                        request.getGames(),
                        request.getLocation(),
                        request.getPhotos(),
                        request.getPartnerId(),
                        request.getCity(),
                        request.getPartnerMobileNo(),
                        request.getQrCodeImage(),
                        request.getUpiId(),
                        request.getAmenities()
                    );
                    venue.setId(request.getId().trim());
                    
                    // Update venue using repository
                    savedVenue = venueRepository.update(venue);
                    logger.info("POST /api/venues - Successfully updated venue. venueId: {}", savedVenue.getId());
                } else {
                    // ID provided but venue doesn't exist, create new venue
                    logger.info("POST /api/venues - Venue ID provided but not found, creating new venue. id: {}", request.getId());
                    Venue venue = new Venue(
                        request.getName(),
                        request.getDescription(),
                        request.getGames(),
                        request.getLocation(),
                        request.getPhotos(),
                        request.getPartnerId(),
                        request.getCity(),
                        request.getPartnerMobileNo(),
                        request.getQrCodeImage(),
                        request.getUpiId(),
                        request.getAmenities()
                    );
                    savedVenue = venueRepository.save(venue);
                    logger.info("POST /api/venues - Successfully created venue. venueId: {}", savedVenue.getId());
                }
            } else {
                // No ID provided, create new venue
                logger.info("POST /api/venues - Creating new venue");
                Venue venue = new Venue(
                    request.getName(),
                    request.getDescription(),
                    request.getGames(),
                    request.getLocation(),
                    request.getPhotos(),
                    request.getPartnerId(),
                    request.getCity(),
                    request.getPartnerMobileNo(),
                    request.getQrCodeImage(),
                    request.getUpiId(),
                    request.getAmenities()
                );
                savedVenue = venueRepository.save(venue);
                logger.info("POST /api/venues - Successfully created venue. venueId: {}", savedVenue.getId());
            }

            // Add venue ID to partner's venues list (only for new venues)
            if (!isUpdate && request.getPartnerId() != null && !request.getPartnerId().trim().isEmpty()) {
                try {
                    partnerRepository.addVenueToPartner(request.getPartnerId().trim(), savedVenue.getId());
                    logger.info("POST /api/venues - Successfully added venue to partner. partnerId: {}, venueId: {}", 
                            request.getPartnerId(), savedVenue.getId());
                } catch (Exception e) {
                    // Log error but don't fail the venue creation
                    logger.error("POST /api/venues - Failed to update partner venues. partnerId: {}, venueId: {}", 
                            request.getPartnerId(), savedVenue.getId(), e);
                }
            }

            VenueResponse response = new VenueResponse(savedVenue);
            String message = isUpdate ? "Venue updated successfully" : "Venue created successfully";

            return ResponseEntity.ok(new ApiResponse(true, message, response));

        } catch (Exception e) {
            logger.error("POST /api/venues - Error processing venue. id: {}, name: {}, partnerId: {}", 
                    request.getId(), request.getName(), request.getPartnerId(), e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, "Error processing venue: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getVenueById(@PathVariable String id) {
        logger.info("GET /api/venues/{} - Fetching venue by ID", id);
        try {
            Venue venue = venueRepository.findById(id);
            if (venue != null) {
                logger.info("GET /api/venues/{} - Successfully retrieved venue", id);
                VenueResponse response = new VenueResponse(venue);
                return ResponseEntity.ok(new ApiResponse(true, "Venue found", response));
            } else {
                logger.warn("GET /api/venues/{} - Venue not found", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("GET /api/venues/{} - Error fetching venue", id, e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, "Error fetching venue: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAllVenues() {
        logger.info("GET /api/venues - Fetching all venues");
        try {
            java.util.List<Venue> venues = venueRepository.findAll();
            java.util.List<VenueResponse> responses = venues.stream()
                    .map(VenueResponse::new)
                    .toList();
            logger.info("GET /api/venues - Successfully retrieved {} venues", responses.size());
            return ResponseEntity.ok(new ApiResponse(true, "Venues retrieved successfully", responses));
        } catch (Exception e) {
            logger.error("GET /api/venues - Error fetching all venues", e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, "Error fetching venues: " + e.getMessage()));
        }
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<ApiResponse> getVenuesByCity(@PathVariable String city) {
        logger.info("GET /api/venues/city/{} - Fetching venues by city", city);
        try {
            if (city == null || city.trim().isEmpty()) {
                logger.warn("GET /api/venues/city/{} - Validation failed: City is required", city);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "City is required"));
            }
            java.util.List<Venue> venues = venueRepository.findByCity(city.trim());
            java.util.List<VenueResponse> responses = venues.stream().map(VenueResponse::new).toList();
            logger.info("GET /api/venues/city/{} - Successfully retrieved {} venues", city, responses.size());
            return ResponseEntity.ok(new ApiResponse(true, "Venues retrieved successfully", responses));
        } catch (Exception e) {
            logger.error("GET /api/venues/city/{} - Error fetching venues by city", city, e);
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error fetching venues: " + e.getMessage()));
        }
    }

    @GetMapping("/partner/{partnerId}")
    public ResponseEntity<ApiResponse> getVenuesByOwner(@PathVariable String partnerId) {
        logger.info("GET /api/venues/partner/{} - Fetching venues by partner", partnerId);
        try {
            java.util.List<Venue> venues = venueRepository.findByPartnerId(partnerId);
            java.util.List<VenueResponse> responses = venues.stream()
                    .map(VenueResponse::new)
                    .toList();
            logger.info("GET /api/venues/partner/{} - Successfully retrieved {} venues", partnerId, responses.size());
            return ResponseEntity.ok(new ApiResponse(true, "Venues retrieved successfully", responses));
        } catch (Exception e) {
            logger.error("GET /api/venues/partner/{} - Error fetching venues by partner", partnerId, e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, "Error fetching venues: " + e.getMessage()));
        }
    }
}


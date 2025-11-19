package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.BookingItemResponse;
import co.sportverse.sportverse_backend.dto.CreateBookingRequest;
import co.sportverse.sportverse_backend.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = {
        "https://sportverse.co.in",
        "http://localhost:8083"
})
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    @Autowired
    private BookingService bookingService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> getBookingsByUser(@PathVariable String userId) {
        logger.info("GET /api/bookings/user/{} - Fetching bookings for user", userId);
        try {
            if (userId == null || userId.trim().isEmpty()) {
                logger.warn("GET /api/bookings/user/{} - Validation failed: userId is required", userId);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "userId is required"));
            }
            List<BookingItemResponse> bookings = bookingService.getUserBookings(userId.trim());
            logger.info("GET /api/bookings/user/{} - Successfully retrieved {} bookings", userId, bookings.size());
            return ResponseEntity.ok(new ApiResponse(true, "Bookings retrieved successfully", bookings));
        } catch (Exception e) {
            logger.error("GET /api/bookings/user/{} - Error fetching bookings", userId, e);
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error fetching bookings: " + e.getMessage()));
        }
    }

    @GetMapping("/partner/{partnerId}")
    public ResponseEntity<ApiResponse> getBookingsByPartner(@PathVariable String partnerId) {
        logger.info("GET /api/bookings/partner/{} - Fetching bookings for partner", partnerId);
        try {
            if (partnerId == null || partnerId.trim().isEmpty()) {
                logger.warn("GET /api/bookings/partner/{} - Validation failed: partnerId is required", partnerId);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "partnerId is required"));
            }
            List<BookingItemResponse> bookings = bookingService.getPartnerBookings(partnerId.trim());
            logger.info("GET /api/bookings/partner/{} - Successfully retrieved {} bookings", partnerId, bookings.size());
            return ResponseEntity.ok(new ApiResponse(true, "Partner bookings retrieved successfully", bookings));
        } catch (Exception e) {
            logger.error("GET /api/bookings/partner/{} - Error fetching partner bookings", partnerId, e);
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error fetching partner bookings: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createBooking(@RequestBody CreateBookingRequest request) {
        logger.info("POST /api/bookings - Creating booking. partnerId: {}, venueId: {}, userId: {}, date: {}, slots count: {}", 
                request.getPartnerId(), request.getVenueId(), request.getUserId(), request.getDate(),
                request.getSlots() != null ? request.getSlots().size() : 0);
        try {
            // Validate required fields
            if (request.getPartnerId() == null || request.getPartnerId().trim().isEmpty()) {
                logger.warn("POST /api/bookings - Validation failed: partnerId is required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "partnerId is required"));
            }
            if (request.getVenueId() == null || request.getVenueId().trim().isEmpty()) {
                logger.warn("POST /api/bookings - Validation failed: venueId is required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "venueId is required"));
            }
            if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
                logger.warn("POST /api/bookings - Validation failed: userId is required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "userId is required"));
            }
            if (request.getDate() == null || request.getDate().trim().isEmpty()) {
                logger.warn("POST /api/bookings - Validation failed: date is required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "date is required (yyyy-MM-dd)"));
            }
            if (request.getSlots() == null || request.getSlots().isEmpty()) {
                logger.warn("POST /api/bookings - Validation failed: slots are required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "slots are required"));
            }

            // Create booking with CONFIRMED status (hardcoded)
            String bookingId = bookingService.createBooking(
                    request.getPartnerId().trim(),
                    request.getUserId().trim(),
                    request.getVenueId().trim(),
                    request.getSlots(),
                    request.getDate().trim()
            );

            logger.info("POST /api/bookings - Successfully created booking. bookingId: {}", bookingId);
            return ResponseEntity.ok(new ApiResponse(true, "Booking created successfully", bookingId));
        } catch (Exception e) {
            logger.error("POST /api/bookings - Error creating booking. partnerId: {}, venueId: {}, userId: {}", 
                    request.getPartnerId(), request.getVenueId(), request.getUserId(), e);
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error creating booking: " + e.getMessage()));
        }
    }

    @GetMapping("/user/mobile/{mobileNumber}")
    public ResponseEntity<ApiResponse> getBookingsByMobileNumber(@PathVariable String mobileNumber) {
        logger.info("GET /api/bookings/user/mobile/{} - Fetching bookings by mobile number", mobileNumber);
        try {
            if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
                logger.warn("GET /api/bookings/user/mobile/{} - Validation failed: Mobile number is required", mobileNumber);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Mobile number is required"));
            }

            // Format phone number (ensure it starts with +)
            String phoneNumber = mobileNumber.trim();
            List<BookingItemResponse> bookings = bookingService.getUserBookingsByMobileNumber(phoneNumber);
            logger.info("GET /api/bookings/user/mobile/{} - Successfully retrieved {} bookings", mobileNumber, bookings.size());
            return ResponseEntity.ok(new ApiResponse(true, "Bookings retrieved successfully", bookings));
        } catch (Exception e) {
            logger.error("GET /api/bookings/user/mobile/{} - Error fetching bookings", mobileNumber, e);
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error fetching bookings: " + e.getMessage()));
        }
    }
}



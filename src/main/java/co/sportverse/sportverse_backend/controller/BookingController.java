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

    @GetMapping
    public ResponseEntity<ApiResponse> getBookings(
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) String bookingId) {
        logger.info("GET /api/bookings - mobile: {}, partnerId: {}, bookingId: {}", mobile, partnerId, bookingId);
        List<BookingItemResponse> bookings = bookingService.getBookings(mobile, partnerId, bookingId);
        logger.info("GET /api/bookings - Successfully retrieved {} bookings", bookings.size());
        return ResponseEntity.ok(new ApiResponse(true, "Bookings retrieved successfully", bookings));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> getBookingsByUser(@PathVariable String userId) {
        logger.info("GET /api/bookings/user/{} - Fetching bookings (userId/phone)", userId);
        List<BookingItemResponse> bookings = bookingService.listBookingsForUserPath(userId);
        logger.info("GET /api/bookings/user/{} - Successfully retrieved {} bookings", userId, bookings.size());
        return ResponseEntity.ok(new ApiResponse(true, "Bookings retrieved successfully", bookings));
    }

    @GetMapping("/partner/{partnerId}")
    public ResponseEntity<ApiResponse> getBookingsByPartner(@PathVariable String partnerId) {
        logger.info("GET /api/bookings/partner/{} - Fetching bookings for partner", partnerId);
        List<BookingItemResponse> bookings = bookingService.listPartnerBookingsForPath(partnerId);
        logger.info("GET /api/bookings/partner/{} - Successfully retrieved {} bookings", partnerId, bookings.size());
        return ResponseEntity.ok(new ApiResponse(true, "Partner bookings retrieved successfully", bookings));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createBooking(@RequestBody CreateBookingRequest request) {
        logger.info("POST /api/bookings - Creating booking. partnerId: {}, venueId: {}, userId: {}, date: {}, slots count: {}",
                request.getPartnerId(), request.getVenueId(), request.getUserId(), request.getDate(),
                request.getSlots() != null ? request.getSlots().size() : 0);
        String bookingId = bookingService.createBookingFromRequest(request);
        logger.info("POST /api/bookings - Successfully created booking. bookingId: {}, status: {}, paymentStatus: {}",
                bookingId, request.getStatus(), request.getPaymentStatus());
        return ResponseEntity.ok(new ApiResponse(true, "Booking created successfully", bookingId));
    }

    @GetMapping("/user/mobile/{mobileNumber}")
    public ResponseEntity<ApiResponse> getBookingsByMobileNumber(@PathVariable String mobileNumber) {
        logger.info("GET /api/bookings/user/mobile/{} - Fetching bookings by mobile number", mobileNumber);
        List<BookingItemResponse> bookings = bookingService.listBookingsForMobilePath(mobileNumber);
        logger.info("GET /api/bookings/user/mobile/{} - Successfully retrieved {} bookings", mobileNumber, bookings.size());
        return ResponseEntity.ok(new ApiResponse(true, "Bookings retrieved successfully", bookings));
    }

    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<ApiResponse> confirmBooking(@PathVariable String bookingId) {
        logger.info("POST /api/bookings/{}/confirm - Confirming booking", bookingId);
        bookingService.confirmBooking(bookingId);
        logger.info("POST /api/bookings/{}/confirm - Successfully confirmed booking", bookingId);
        return ResponseEntity.ok(new ApiResponse(true, "Booking confirmed successfully"));
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<ApiResponse> cancelBooking(@PathVariable String bookingId) {
        logger.info("DELETE /api/bookings/{} - Cancelling booking", bookingId);
        bookingService.cancelBooking(bookingId);
        logger.info("DELETE /api/bookings/{} - Successfully cancelled booking", bookingId);
        return ResponseEntity.ok(new ApiResponse(true, "Booking cancelled successfully"));
    }
}

package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.BookingItemResponse;
import co.sportverse.sportverse_backend.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> getBookingsByUser(@PathVariable String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "userId is required"));
            }
            List<BookingItemResponse> bookings = bookingService.getUserBookings(userId.trim());
            return ResponseEntity.ok(new ApiResponse(true, "Bookings retrieved successfully", bookings));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error fetching bookings: " + e.getMessage()));
        }
    }
}



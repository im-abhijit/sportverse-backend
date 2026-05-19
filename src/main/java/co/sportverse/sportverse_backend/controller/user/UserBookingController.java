package co.sportverse.sportverse_backend.controller.user;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.AuthenticatedUser;
import co.sportverse.sportverse_backend.dto.CreateBookingOrderRequest;
import co.sportverse.sportverse_backend.dto.CreateBookingOrderResponse;
import co.sportverse.sportverse_backend.dto.UserBookingsPageResponse;
import co.sportverse.sportverse_backend.security.AuthenticatedUserSupport;
import co.sportverse.sportverse_backend.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated user APIs for own bookings (JWT subject = normalized mobile digits from token).
 */
@RestController
@RequestMapping("/api/user/bookings")
public class UserBookingController {

    private static final Logger logger = LoggerFactory.getLogger(UserBookingController.class);

    @Autowired
    private BookingService bookingService;

    @GetMapping
    public ResponseEntity<ApiResponse> listMyBookings(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) String bookingStatus,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        String userSubject = AuthenticatedUserSupport.requireUserSubject(authenticatedUser);
        int p = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 10;
        logger.info("GET /api/user/bookings - subject: {}, bookingStatus filter: {}, page: {}, pageSize: {}",
                userSubject, bookingStatus, p, size);

        UserBookingsPageResponse data =
                bookingService.listMyBookingsPaged(userSubject, bookingStatus, p, size);
        logger.info("GET /api/user/bookings - {} items returned for subject {}", data.getItems().size(), userSubject);
        return ResponseEntity.ok(new ApiResponse(true, "Bookings retrieved successfully", data));
    }

    /**
     * Same as {@code POST /api/bookings/create-order} (slot reservation + pending booking) without creating a Razorpay order.
     */
    @PostMapping("/create-order-manual")
    public ResponseEntity<ApiResponse> createOrderManual(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody CreateBookingOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        String userSubject = AuthenticatedUserSupport.requireUserSubject(authenticatedUser);
        logger.info("POST /api/user/bookings/create-order-manual - partnerId: {}, venueId: {}, principal: {}, date: {}, slots count: {}",
                request.getPartnerId(), request.getVenueId(), userSubject, request.getDate(),
                request.getSlotIds() != null ? request.getSlotIds().size() : 0);
        CreateBookingOrderResponse response =
                bookingService.reserveSlotsCreateBookingManual(request, userSubject);
        logger.info("POST /api/user/bookings/create-order-manual - bookingId: {}", response.getBookingId());
        return ResponseEntity.ok(new ApiResponse(true, "Pending booking created (manual, no Razorpay order)", response));
    }
}

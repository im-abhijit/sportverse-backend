package co.sportverse.sportverse_backend.controller.partner;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.AuthenticatedUser;
import co.sportverse.sportverse_backend.dto.BookingItemResponse;
import co.sportverse.sportverse_backend.dto.ConfirmPartnerOrderManualRequest;
import co.sportverse.sportverse_backend.dto.CreateBookingOrderRequest;
import co.sportverse.sportverse_backend.dto.CreateBookingOrderResponse;
import co.sportverse.sportverse_backend.security.AuthenticatedUserSupport;
import co.sportverse.sportverse_backend.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated partner booking APIs; {@code partnerId} is taken from the JWT.
 */
@RestController
@RequestMapping("/api/partner/bookings")
public class PartnerBookingController {

    private static final Logger logger = LoggerFactory.getLogger(PartnerBookingController.class);

    @Autowired
    private BookingService bookingService;

    /**
     * Same as {@code POST /api/user/bookings/create-order-manual}: reserve slots and create a pending booking without Razorpay.
     * Body must include {@code userId} (customer), {@code venueId}, {@code date}, {@code slotIds}; {@code partnerId} is ignored in favor of the token.
     */
    @PostMapping("/create-order-manual")
    public ResponseEntity<ApiResponse> createOrderManual(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody CreateBookingOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        String partnerId = AuthenticatedUserSupport.requirePartnerId(authenticatedUser);
        logger.info("POST /api/partner/bookings/create-order-manual - partnerId: {}, venueId: {}, customer userId: {}, date: {}, slots count: {}",
                partnerId, request.getVenueId(), request.getUserId(), request.getDate(),
                request.getSlotIds() != null ? request.getSlotIds().size() : 0);
        CreateBookingOrderResponse response =
                bookingService.reserveSlotsCreateBookingManualForPartner(request, partnerId);
        logger.info("POST /api/partner/bookings/create-order-manual - bookingId: {}", response.getBookingId());
        return ResponseEntity.ok(new ApiResponse(true, "Pending booking created (manual, no Razorpay order)", response));
    }

    /**
     * Same operational steps as payment verify (pending → confirm, reserve checks, slots booked), without order-id
     * matching or signature verification; stored order / payment ids are random UUIDs.
     */
    @PostMapping("/confirm-order-manual")
    public ResponseEntity<ApiResponse> confirmOrderManual(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody ConfirmPartnerOrderManualRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        String partnerId = AuthenticatedUserSupport.requirePartnerId(authenticatedUser);
        if (request.getBookingId() == null || request.getBookingId().isBlank()) {
            throw new IllegalArgumentException("bookingId is required");
        }
        logger.info("POST /api/partner/bookings/confirm-order-manual - partnerId: {}, bookingId: {}",
                partnerId, request.getBookingId());
        BookingItemResponse booking =
                bookingService.confirmBookingPaymentManualForPartner(request.getBookingId(), partnerId);
        logger.info("POST /api/partner/bookings/confirm-order-manual - confirmed bookingId: {}", booking.getId());
        return ResponseEntity.ok(new ApiResponse(true, "Payment confirmed (manual)", booking));
    }
}

package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.CreatePaymentOrderRequest;
import co.sportverse.sportverse_backend.dto.VerifyPaymentRequest;
import co.sportverse.sportverse_backend.repository.BookingRepository;
import co.sportverse.sportverse_backend.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = {
        "https://sportverse.co.in",
        "http://localhost:8083"
})
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingRepository bookingRepository;

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse> createOrder(@RequestBody CreatePaymentOrderRequest request) {
        logger.info("POST /api/payments/create-order - Creating payment order. amount: {}, userId: {}, venueId: {}, date: {}, slotIds count: {}", 
                request.getAmount(), request.getUserId(), request.getVenueId(), request.getDate(),
                request.getSlotIds() != null ? request.getSlotIds().size() : 0);
        try {
            if (request.getAmount() <= 0) {
                logger.warn("POST /api/payments/create-order - Validation failed: Amount must be > 0. Received: {}", request.getAmount());
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Amount must be > 0"));
            }
            if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
                logger.warn("POST /api/payments/create-order - Validation failed: userId is required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "userId is required"));
            }
            if (request.getVenueId() == null || request.getVenueId().trim().isEmpty()) {
                logger.warn("POST /api/payments/create-order - Validation failed: venueId is required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "venueId is required"));
            }
            if (request.getSlotIds() == null || request.getSlotIds().isEmpty()) {
                logger.warn("POST /api/payments/create-order - Validation failed: slotIds are required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "slotIds are required"));
            }
            if (request.getDate() == null || request.getDate().trim().isEmpty()) {
                logger.warn("POST /api/payments/create-order - Validation failed: date is required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "date is required (yyyy-MM-dd)"));
            }

            Map<String, Object> orderData = paymentService.createOrder(
                    request.getAmount(),
                    request.getUserId(),
                    request.getVenueId(),
                    request.getSlotIds(),
                    request.getDate()
            );
            logger.info("POST /api/payments/create-order - Successfully created order. orderId: {}", 
                    orderData.get("orderId"));
            return ResponseEntity.ok(new ApiResponse(true, "Order created", orderData));
        } catch (Exception e) {
            logger.error("POST /api/payments/create-order - Error creating order. amount: {}, userId: {}, venueId: {}", 
                    request.getAmount(), request.getUserId(), request.getVenueId(), e);
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error creating order: " + e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verifyPayment(@RequestBody VerifyPaymentRequest request) {
        logger.info("POST /api/payments/verify - Verifying payment. orderId: {}, paymentId: {}", 
                request.getRazorpay_order_id(), request.getRazorpay_payment_id());
        try {
            if (request.getRazorpay_order_id() == null || request.getRazorpay_order_id().trim().isEmpty() ||
                request.getRazorpay_payment_id() == null || request.getRazorpay_payment_id().trim().isEmpty() ||
                request.getRazorpay_signature() == null || request.getRazorpay_signature().trim().isEmpty()){
                logger.warn("POST /api/payments/verify - Validation failed: Missing required fields");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Missing required fields"));
            }

            boolean valid = paymentService.verifyAndUpdate(
                request.getRazorpay_order_id(),
                request.getRazorpay_payment_id(),
                request.getRazorpay_signature()
            );

            if (valid) {
                logger.info("POST /api/payments/verify - Payment verified successfully. orderId: {}", 
                        request.getRazorpay_order_id());
                return ResponseEntity.ok(new ApiResponse(true, "Payment verified", null));
            }
            logger.warn("POST /api/payments/verify - Invalid signature. orderId: {}", request.getRazorpay_order_id());
            return ResponseEntity.ok(new ApiResponse(false, "Invalid signature", null));
        } catch (Exception e) {
            logger.error("POST /api/payments/verify - Error verifying payment. orderId: {}", 
                    request.getRazorpay_order_id(), e);
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error verifying payment: " + e.getMessage()));
        }
    }

}
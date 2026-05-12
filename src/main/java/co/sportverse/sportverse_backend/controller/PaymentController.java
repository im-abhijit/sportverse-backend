package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.CreatePaymentOrderRequest;
import co.sportverse.sportverse_backend.dto.VerifyPaymentRequest;
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

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse> createOrder(@RequestBody CreatePaymentOrderRequest request) {
        logger.info("POST /api/payments/create-order - Creating payment order. amount: {}, userId: {}, venueId: {}, date: {}, slotIds count: {}",
                request.getAmount(), request.getUserId(), request.getVenueId(), request.getDate(),
                request.getSlotIds() != null ? request.getSlotIds().size() : 0);
        Map<String, Object> orderData = paymentService.createOrderFromRequest(request);
        logger.info("POST /api/payments/create-order - Successfully created order. orderId: {}",
                orderData.get("orderId"));
        return ResponseEntity.ok(new ApiResponse(true, "Order created", orderData));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verifyPayment(@RequestBody VerifyPaymentRequest request) {
        logger.info("POST /api/payments/verify - Verifying payment. orderId: {}, paymentId: {}",
                request.getRazorpay_order_id(), request.getRazorpay_payment_id());
        boolean valid = paymentService.verifyPaymentFromRequest(request);
        if (valid) {
            logger.info("POST /api/payments/verify - Payment verified successfully. orderId: {}",
                    request.getRazorpay_order_id());
            return ResponseEntity.ok(new ApiResponse(true, "Payment verified", null));
        }
        logger.warn("POST /api/payments/verify - Invalid signature. orderId: {}", request.getRazorpay_order_id());
        return ResponseEntity.ok(new ApiResponse(false, "Invalid signature", null));
    }
}

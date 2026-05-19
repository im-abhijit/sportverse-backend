package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.config.RazorpayConfig;
import co.sportverse.sportverse_backend.dto.BookingItemResponse;
import co.sportverse.sportverse_backend.dto.CreatePaymentOrderRequest;
import co.sportverse.sportverse_backend.dto.VerifyPaymentRequest;
import co.sportverse.sportverse_backend.entity.User;
import co.sportverse.sportverse_backend.repository.BookingRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private RazorpayConfig razorpayConfig;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SlotsService slotsService;

    @Autowired
    private UserService userService;

    private RazorpayClient client() throws RazorpayException {
        return new RazorpayClient(razorpayConfig.getKey_id(), razorpayConfig.getKey_secret());
    }

    public Map<String, Object> createOrderFromRequest(CreatePaymentOrderRequest request) {
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getVenueId() == null || request.getVenueId().trim().isEmpty()) {
            throw new IllegalArgumentException("venueId is required");
        }
        if (request.getSlotIds() == null || request.getSlotIds().isEmpty()) {
            throw new IllegalArgumentException("slotIds are required");
        }
        if (request.getDate() == null || request.getDate().trim().isEmpty()) {
            throw new IllegalArgumentException("date is required (yyyy-MM-dd)");
        }
        return createOrder(
                request.getAmount(),
                request.getUserId().trim(),
                request.getVenueId().trim(),
                request.getSlotIds(),
                request.getDate().trim()
        );
    }

    public BookingItemResponse verifyPaymentFromRequest(VerifyPaymentRequest request, String jwtSubject) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getBookingId() == null || request.getBookingId().trim().isEmpty()
                || request.getRazorpay_order_id() == null || request.getRazorpay_order_id().trim().isEmpty()
                || request.getRazorpay_payment_id() == null || request.getRazorpay_payment_id().trim().isEmpty()
                || request.getRazorpay_signature() == null || request.getRazorpay_signature().trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required fields");
        }

        User user = userService.requireUserForJwtSubject(jwtSubject);

        BookingService.PaymentVerificationContext context = bookingService.getPendingPaymentVerificationContext(
                request.getBookingId().trim(),
                user
        );

        String requestOrderId = request.getRazorpay_order_id().trim();
        if (!context.getRazorpayOrderId().equals(requestOrderId)) {
            throw new IllegalArgumentException("Razorpay order id does not match booking");
        }

        slotsService.ensureSlotsReservedForBooking(context.getVenueId(), context.getDate(), context.getSlotIds());

        String paymentId = request.getRazorpay_payment_id().trim();
        String signature = request.getRazorpay_signature().trim();
        boolean isValid = verifySignature(requestOrderId, paymentId, signature);
        if (!isValid) {
            return null;
        }

        bookingService.confirmPayment(
                context.getBookingId(),
                requestOrderId,
                paymentId,
                signature
        );
        slotsService.markReservedSlotsBookedForBooking(context.getVenueId(), context.getDate(), context.getSlotIds());
        return bookingService.getBookingDetailsById(context.getBookingId());
    }

    public Map<String, Object> createOrder(int amountInRupees, String userId, String venueId, java.util.List<String> slotIds, String date) {
        String bookingId = null;
        try {
            // 1️⃣ Create booking first (with null orderId)
            try {
                bookingId = bookingRepository.createBooking(userId, venueId, slotIds, date, amountInRupees, null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to persist booking: " + e.getMessage());
            }

            // 2️⃣ Create Razorpay order (receipt max 40 chars; use bookingId)
            int amountInPaise = amountInRupees * 100;
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            String receipt = bookingId.length() <= 40 ? bookingId : bookingId.substring(0, 40);
            orderRequest.put("receipt", receipt);

            Order order = client().orders.create(orderRequest);

            String orderId = order.get("id");
            int amount = order.get("amount");
            String currency = order.get("currency");

            // 3️⃣ Update booking with Razorpay orderId
            bookingRepository.updateRazorpayOrderId(bookingId, orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("key", razorpayConfig.getKey_id());
            response.put("orderId", orderId);
            response.put("amount", amount);
            response.put("currency", currency);
            return response;
        } catch (RazorpayException e) {
            // Razorpay failed: remove the orphaned booking
            if (bookingId != null) {
                try {
                    bookingRepository.deleteById(bookingId);
                    logger.info("Deleted orphaned booking {} after Razorpay order creation failed", bookingId);
                } catch (Exception deleteEx) {
                    logger.error("Failed to delete orphaned booking {} after Razorpay failure", bookingId, deleteEx);
                }
            }
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage());
        }
    }

    public boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + '|' + paymentId;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(razorpayConfig.getKey_secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hashBytes = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = new String(Base64.getEncoder().encode(hashBytes));
            // Razorpay expects hex string, not base64. Convert to hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String expected = hexString.toString();
            return expected.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

}



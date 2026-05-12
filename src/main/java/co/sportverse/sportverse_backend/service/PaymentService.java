package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.config.RazorpayConfig;
import co.sportverse.sportverse_backend.dto.CreatePaymentOrderRequest;
import co.sportverse.sportverse_backend.dto.VerifyPaymentRequest;
import co.sportverse.sportverse_backend.repository.BookingRepository;
import co.sportverse.sportverse_backend.entity.BookingStatus;
import co.sportverse.sportverse_backend.entity.PaymentStatus;
import co.sportverse.sportverse_backend.repository.SlotsRepository;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.bson.Document;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private RazorpayConfig razorpayConfig;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SlotsRepository slotsRepository;

    @Autowired
    private MongoClient mongoClient;

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

    public boolean verifyPaymentFromRequest(VerifyPaymentRequest request) {
        if (request.getRazorpay_order_id() == null || request.getRazorpay_order_id().trim().isEmpty()
                || request.getRazorpay_payment_id() == null || request.getRazorpay_payment_id().trim().isEmpty()
                || request.getRazorpay_signature() == null || request.getRazorpay_signature().trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required fields");
        }
        return verifyAndUpdate(
                request.getRazorpay_order_id().trim(),
                request.getRazorpay_payment_id().trim(),
                request.getRazorpay_signature().trim()
        );
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

    public boolean verifyAndUpdate(String orderId, String paymentId, String signature) {
        try {
            // 1️⃣ Fetch booking by Razorpay orderId
            Document booking = bookingRepository.findByRazorpayOrderId(orderId);
            if (booking == null) {
                return false;
            }

            // 2️⃣ Extract payment info
            Document paymentInfo = (Document) booking.get("payment");
            if (paymentInfo == null) {
                return false;
            }

            String storedOrderId = paymentInfo.getString("razorpayOrderId");
            String storedPaymentId = paymentInfo.getString("razorpayPaymentId");
            String bookingStatusStr = booking.getString("bookingStatus");

            // 3️⃣ Idempotent retry: if already PAID with this paymentId, ensure slots are marked and return true
            if (BookingStatus.PAID.name().equals(bookingStatusStr) && Objects.equals(storedPaymentId, paymentId)) {
                logger.info("verifyAndUpdate: idempotent retry for orderId={}, ensuring slots marked", orderId);
                ensureSlotsMarked(booking);
                return true;
            }

            // 4️⃣ Verify Razorpay signature
            boolean isValid = verifySignature(storedOrderId, paymentId, signature);

            // 5️⃣ Update payment and booking status, 6️⃣ mark slots - use transaction when supported
            PaymentStatus paymentStatus = isValid ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
            BookingStatus bookingStatus = isValid ? BookingStatus.PAID : BookingStatus.FAILED;

            try (ClientSession session = mongoClient.startSession()) {
                session.startTransaction();
                try {
                    bookingRepository.updatePaymentByOrderId(session, orderId, paymentStatus, paymentId, signature, bookingStatus);
                    if (isValid) {
                        ensureSlotsMarked(session, booking);
                    }
                    session.commitTransaction();
                } catch (Exception e) {
                    session.abortTransaction();
                    throw e;
                }
            } catch (Exception txEx) {
                // Fallback: MongoDB transactions require replica set; run without transaction
                logger.warn("Transaction not supported (replica set required), falling back to non-transactional: {}", txEx.getMessage());
                bookingRepository.updatePaymentByOrderId(orderId, paymentStatus, paymentId, signature, bookingStatus);
                if (isValid) {
                    ensureSlotsMarkedWithRetry(booking);
                }
            }

            return isValid;

        } catch (Exception e) {
            logger.error("verifyAndUpdate failed", e);
            return false;
        }
    }

    private void ensureSlotsMarked(ClientSession session, Document booking) {
        String venueId = booking.getObjectId("venueId").toString();
        String date = booking.getString("date");
        @SuppressWarnings("unchecked")
        List<String> slotIds = (List<String>) booking.get("slotIds");
        if (venueId != null && date != null && slotIds != null && !slotIds.isEmpty()) {
            slotsRepository.markSlotsBooked(session, venueId, date, slotIds);
        }
    }

    private void ensureSlotsMarked(Document booking) {
        String venueId = booking.getObjectId("venueId").toString();
        String date = booking.getString("date");
        @SuppressWarnings("unchecked")
        List<String> slotIds = (List<String>) booking.get("slotIds");
        if (venueId != null && date != null && slotIds != null && !slotIds.isEmpty()) {
            slotsRepository.markSlotsBooked(venueId, date, slotIds);
        }
    }

    private static final int SLOT_MARK_RETRY_ATTEMPTS = 3;
    private static final long SLOT_MARK_RETRY_DELAY_MS = 100;

    private void ensureSlotsMarkedWithRetry(Document booking) {
        String venueId = booking.getObjectId("venueId").toString();
        String date = booking.getString("date");
        @SuppressWarnings("unchecked")
        List<String> slotIds = (List<String>) booking.get("slotIds");
        if (venueId == null || date == null || slotIds == null || slotIds.isEmpty()) {
            return;
        }
        for (int attempt = 1; attempt <= SLOT_MARK_RETRY_ATTEMPTS; attempt++) {
            try {
                slotsRepository.markSlotsBooked(venueId, date, slotIds);
                return;
            } catch (Exception e) {
                logger.warn("markSlotsBooked attempt {}/{} failed for venueId={}, date={}", attempt, SLOT_MARK_RETRY_ATTEMPTS, venueId, date, e);
                if (attempt < SLOT_MARK_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(SLOT_MARK_RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during slot mark retry", ie);
                    }
                } else {
                    throw new RuntimeException("Failed to mark slots after " + SLOT_MARK_RETRY_ATTEMPTS + " attempts: " + e.getMessage(), e);
                }
            }
        }
    }

}



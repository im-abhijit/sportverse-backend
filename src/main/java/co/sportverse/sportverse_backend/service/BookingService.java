package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.dto.CancelBookingRequest;
import co.sportverse.sportverse_backend.dto.UserBookingsPageResponse;
import co.sportverse.sportverse_backend.dto.CreateBookingOrderRequest;
import co.sportverse.sportverse_backend.dto.CreateBookingOrderResponse;
import co.sportverse.sportverse_backend.dto.BookingItemResponse;
import co.sportverse.sportverse_backend.dto.CreateBookingRequest;
import co.sportverse.sportverse_backend.dto.RefundDtoResponse;
import co.sportverse.sportverse_backend.dto.VenueResponse;
import co.sportverse.sportverse_backend.entity.BookingStatus;
import co.sportverse.sportverse_backend.entity.PaymentStatus;
import co.sportverse.sportverse_backend.entity.TimeSlot;
import co.sportverse.sportverse_backend.entity.User;
import co.sportverse.sportverse_backend.entity.Venue;
import co.sportverse.sportverse_backend.entity.VenueSlots;
import co.sportverse.sportverse_backend.repository.BookingRepository;
import co.sportverse.sportverse_backend.repository.PartnerRepository;
import co.sportverse.sportverse_backend.repository.UserRepository;
import co.sportverse.sportverse_backend.repository.SlotsRepository;
import co.sportverse.sportverse_backend.repository.VenueRepository;
import co.sportverse.sportverse_backend.service.UserService;
import co.sportverse.sportverse_backend.util.BookingEarliestSlotStart;
import com.mongodb.ReadConcern;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private SlotsRepository slotsRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private ExpoPushNotificationService expoPushNotificationService;

    @Autowired
    private co.sportverse.sportverse_backend.service.NotificationService notificationService;

    @Autowired
    private RazorpayOrderService razorpayOrderService;

    @Autowired
    private RazorpayRefundService razorpayRefundService;

    @Autowired
    private SlotsService slotsService;

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    public List<BookingItemResponse> getUserBookings(String userId) {
        Query query = new Query();
        addUserIdMatchCriteria(query, userId);
        List<Document> bookingDocs = bookingRepository.findByQuery(query);
        List<BookingItemResponse> responses = new ArrayList<>();
        for (Document doc : bookingDocs) {
            responses.add(mapDocumentToBookingItem(doc));
        }
        return responses;
    }

    public List<BookingItemResponse> getPartnerBookings(String partnerId) {
        List<String> venueIds = partnerRepository.getVenueIdsByPartnerId(partnerId);
        if (venueIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<ObjectId> venueObjectIds = new ArrayList<>();
        for (String vid : venueIds) {
            venueObjectIds.add(new ObjectId(vid));
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("venueId").in(venueObjectIds));
        query.addCriteria(Criteria.where("bookingStatus").ne("CANCELLED"));

        List<Document> bookingDocs = bookingRepository.findByQuery(query);
        List<BookingItemResponse> responses = new ArrayList<>();
        for (Document doc : bookingDocs) {
            responses.add(mapDocumentToBookingItem(doc));
        }
        return responses;
    }

    /**
     * Unified list fetch: exactly one of {@code mobile}, {@code partnerId}, or {@code bookingId} must be provided.
     */
    public List<BookingItemResponse> getBookings(String mobile, String partnerId, String bookingId) {
        boolean hasMobile = mobile != null && !mobile.trim().isEmpty();
        boolean hasPartner = partnerId != null && !partnerId.trim().isEmpty();
        boolean hasBookingId = bookingId != null && !bookingId.trim().isEmpty();
        int count = (hasMobile ? 1 : 0) + (hasPartner ? 1 : 0) + (hasBookingId ? 1 : 0);
        if (count == 0) {
            throw new IllegalArgumentException("Provide one query parameter: mobile, partnerId, or bookingId");
        }
        if (count > 1) {
            throw new IllegalArgumentException("Provide only one of: mobile, partnerId, bookingId");
        }
        if (hasBookingId) {
            try {
                Query query = new Query(Criteria.where("_id").is(new ObjectId(bookingId.trim())));
                List<Document> docs = bookingRepository.findByQuery(query);
                if (docs.isEmpty()) {
                    return new ArrayList<>();
                }
                return List.of(mapDocumentToBookingItem(docs.get(0)));
            } catch (IllegalArgumentException e) {
                return new ArrayList<>();
            }
        }
        if (hasPartner) {
            return getPartnerBookings(partnerId.trim());
        }
        return getUserBookingsByMobileNumber(mobile.trim());
    }

    private BookingItemResponse mapDocumentToBookingItem(Document doc) {
        BookingItemResponse item = new BookingItemResponse();
        item.setId(doc.getObjectId("_id").toString());
        String venueId = doc.getObjectId("venueId").toString();
        String date = doc.getString("date");
        item.setDate(date);
        item.setAmount(doc.getInteger("amount", 0));
        item.setBookingStatus(doc.getString("bookingStatus"));
        item.setPaymentScreenshotUrl(doc.getString("paymentScreenshotUrl"));
        String uid = doc.getString("userId");
        if (uid != null) {
            item.setUserId(uid);
        }
        Document paymentDoc = (Document) doc.get("payment");
        if (paymentDoc != null) {
            item.setPaymentStatus(paymentDoc.getString("status"));
        }

        Venue venue = venueRepository.findById(venueId);
        if (venue != null) {
            item.setVenue(new VenueResponse(venue));
        }

        @SuppressWarnings("unchecked")
        List<Document> slotsDocs = (List<Document>) doc.get("slots");
        if (slotsDocs != null && !slotsDocs.isEmpty()) {
            List<TimeSlot> selected = new ArrayList<>();
            for (Document slotDoc : slotsDocs) {
                selected.add(TimeSlot.fromDocument(slotDoc));
            }
            item.setSlots(selected);
        } else {
            @SuppressWarnings("unchecked")
            List<String> slotIds = (List<String>) doc.get("slotIds");
            if (slotIds != null && !slotIds.isEmpty()) {
                VenueSlots vs = slotsService.getSlotsByVenueAndDate(venueId, date);
                if (vs != null && vs.getSlots() != null) {
                    Set<String> target = new HashSet<>(slotIds);
                    List<TimeSlot> selected = new ArrayList<>();
                    for (TimeSlot s : vs.getSlots()) {
                        if (s.getSlotId() != null && target.contains(s.getSlotId())) {
                            selected.add(s);
                        }
                    }
                    item.setSlots(selected);
                } else {
                    item.setSlots(new ArrayList<>());
                }
            } else {
                item.setSlots(new ArrayList<>());
            }
        }

        @SuppressWarnings("unchecked")
        List<String> persistedSlotIds = (List<String>) doc.get("slotIds");
        if (persistedSlotIds != null && !persistedSlotIds.isEmpty()) {
            item.setSlotIds(new ArrayList<>(persistedSlotIds));
        } else {
            item.setSlotIds(extractEmbeddedSlotIds(doc));
        }

        Object refundRaw = doc.get("refundDto");
        if (refundRaw instanceof Document refundDoc) {
            item.setRefundDto(mapRefundDocument(refundDoc));
        }

        return item;
    }

    private RefundDtoResponse mapRefundDocument(Document rd) {
        if (rd == null) {
            return null;
        }
        RefundDtoResponse o = new RefundDtoResponse();
        o.setStatus(rd.getString("status"));
        o.setRefundId(rd.getString("refundId"));
        Object amt = rd.get("amount");
        if (amt instanceof Number n) {
            o.setAmount(n.longValue());
        }
        o.setPaymentId(rd.getString("paymentId"));
        Object created = rd.get("createdAt");
        if (created instanceof Number n) {
            o.setCreatedAt(n.longValue());
        }
        Object acqRaw = rd.get("acquirer_data");
        if (acqRaw instanceof List<?> list) {
            List<String> acq = new ArrayList<>();
            for (Object x : list) {
                if (x != null) {
                    acq.add(x.toString());
                }
            }
            o.setAcquirerData(acq);
        }
        o.setSpeed(rd.getString("speed"));
        o.setError(rd.getString("error"));
        return o;
    }

    private List<String> extractEmbeddedSlotIds(Document doc) {
        @SuppressWarnings("unchecked")
        List<Document> slotsDocs = (List<Document>) doc.get("slots");
        if (slotsDocs == null || slotsDocs.isEmpty()) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (Document s : slotsDocs) {
            if (s == null) {
                continue;
            }
            String sid = s.getString("slotId");
            if (sid != null && !sid.isBlank()) {
                out.add(sid.trim());
            }
        }
        return out.isEmpty() ? null : out;
    }

    private List<String> resolveSlotIdsForMutation(Document booking) {
        @SuppressWarnings("unchecked")
        List<String> slotIds = (List<String>) booking.get("slotIds");
        if (slotIds != null && !slotIds.isEmpty()) {
            List<String> normalized = new ArrayList<>();
            for (String s : slotIds) {
                if (s != null && !s.trim().isEmpty()) {
                    normalized.add(s.trim());
                }
            }
            if (!normalized.isEmpty()) {
                return normalized;
            }
        }
        List<String> fromSlots = extractEmbeddedSlotIds(booking);
        if (fromSlots == null) {
            throw new IllegalArgumentException("Booking has no slot ids");
        }
        return fromSlots;
    }

    private static String bookingUserIdString(Document booking) {
        Object raw = booking.get("userId");
        if (raw == null) {
            return null;
        }
        if (raw instanceof ObjectId oid) {
            return oid.toHexString();
        }
        return raw.toString().trim();
    }

    private static String normalizeIndianMobileDigits(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String cleaned = raw.trim().replaceAll("\\s+", "");
        if (cleaned.startsWith("91") && cleaned.length() == 12) {
            return cleaned.substring(2);
        }
        return cleaned;
    }

    private boolean matchesBookingUser(Document booking, String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        String expected = userId.trim();
        String actual = bookingUserIdString(booking);
        if (actual == null || actual.isEmpty()) {
            return false;
        }
        if (actual.equals(expected)) {
            return true;
        }
        try {
            if (actual.equals(new ObjectId(expected).toHexString())) {
                return true;
            }
        } catch (IllegalArgumentException ignored) {
            // expected is not a Mongo id
        }
        String n1 = normalizeIndianMobileDigits(expected);
        String n2 = normalizeIndianMobileDigits(actual);
        return !n1.isEmpty() && n1.equals(n2);
    }

    private void assertBookingOwnedByUser(Document booking, String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (bookingUserIdString(booking) == null || bookingUserIdString(booking).isEmpty()) {
            throw new IllegalArgumentException("Booking has no userId");
        }
        if (!matchesBookingUser(booking, userId)) {
            throw new IllegalArgumentException("You are not allowed to cancel this booking");
        }
    }

    /**
     * Ensures the booking's stored user reference matches the authenticated user (Mongo id and/or phone shapes).
     */
    private void assertBookingOwnedByAuthenticatedUser(Document booking, User user, String forbiddenMessage) {
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        if (bookingUserIdString(booking) == null || bookingUserIdString(booking).isEmpty()) {
            throw new IllegalArgumentException("Booking has no userId");
        }
        String phone = user.getPhone() != null ? user.getPhone().trim() : "";
        if (!phone.isEmpty() && matchesBookingUser(booking, phone)) {
            return;
        }
        throw new IllegalArgumentException(forbiddenMessage);
    }

    /**
     * Cancels the booking (refund PENDING) and releases venue slots atomically on a replica set / sharded cluster.
     */
    private void cancelBookingAndReleaseSlotsInTransaction(String bookingId, String venueId, String date, List<String> slotIds) {
        TransactionOptions opts = TransactionOptions.builder()
                .readConcern(ReadConcern.MAJORITY)
                .writeConcern(WriteConcern.MAJORITY)
                .build();
        try (ClientSession session = mongoClient.startSession()) {
            session.withTransaction(() -> {
                bookingRepository.cancelBookingWithRefundPending(session, bookingId);
                slotsRepository.releaseBookedSlotsForCancellation(session, venueId, date, slotIds);
                return null;
            }, opts);
        }
    }

    public BookingItemResponse cancelConfirmedBookingWithRefund(CancelBookingRequest request, String jwtSubject) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getBookingId() == null || request.getBookingId().isBlank()) {
            throw new IllegalArgumentException("bookingId is required");
        }
        String bookingId = request.getBookingId().trim();
        Document booking = bookingRepository.findById(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }

        User user = userService.requireUserForJwtSubject(jwtSubject);
        assertBookingOwnedByAuthenticatedUser(
                booking, user, "You are not allowed to cancel this booking");

        String bookingStatus = booking.getString("bookingStatus");
        if (BookingStatus.CANCELLED.name().equals(bookingStatus)) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }
        if (!BookingStatus.CONFIRMED.name().equals(bookingStatus)) {
            throw new IllegalArgumentException("Only CONFIRMED bookings can be cancelled via this API");
        }

        Document payment = (Document) booking.get("payment");
        String paymentStatus = payment != null ? payment.getString("status") : null;
        if (!PaymentStatus.SUCCESS.name().equals(paymentStatus)) {
            throw new IllegalArgumentException("Payment must be SUCCESS to request a refund");
        }
        String razorpayPaymentId = payment != null ? payment.getString("razorpayPaymentId") : null;
        if (razorpayPaymentId == null || razorpayPaymentId.isBlank()) {
            throw new IllegalArgumentException("Booking has no Razorpay payment id");
        }

        ZonedDateTime earliest = BookingEarliestSlotStart.earliestStart(booking);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        if (!now.isBefore(earliest)) {
            throw new IllegalArgumentException("Cancellation is not allowed after the earliest slot start time");
        }

        String venueId = booking.getObjectId("venueId").toString();
        String date = booking.getString("date");
        List<String> slotIds = resolveSlotIdsForMutation(booking);

        String bookingUserId = booking.getString("userId");
        if (bookingUserId == null || bookingUserId.isBlank()) {
            throw new IllegalStateException("Booking has no userId for Razorpay refund resolution");
        }

        cancelBookingAndReleaseSlotsInTransaction(bookingId, venueId, date, slotIds);

        try {
            Document refundFields = razorpayRefundService.createFullRefundDocument(razorpayPaymentId, bookingId, bookingUserId);
            bookingRepository.replaceRefundDto(bookingId, refundFields);
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Refund failed";
            bookingRepository.patchRefundDto(bookingId,
                    new Document("status", "FAILED").append("error", msg));
            throw e;
        }

        Document updated = bookingRepository.findById(bookingId);
        if (updated == null) {
            throw new IllegalStateException("Booking disappeared after cancellation");
        }
        return mapDocumentToBookingItem(updated);
    }

    private static void addUserIdMatchCriteria(Query query, String userId) {
        try {
            ObjectId oid = new ObjectId(userId);
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("userId").is(userId),
                    Criteria.where("userId").is(oid)
            ));
        } catch (IllegalArgumentException e) {
            query.addCriteria(Criteria.where("userId").is(userId));
        }
    }

    public List<BookingItemResponse> listBookingsForUserPath(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId/phone is required");
        }
        return getBookings(userId.trim(), null, null);
    }

    public List<BookingItemResponse> listBookingsForMobilePath(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number is required");
        }
        return getBookings(mobileNumber.trim(), null, null);
    }

    public List<BookingItemResponse> listPartnerBookingsForPath(String partnerId) {
        if (partnerId == null || partnerId.trim().isEmpty()) {
            throw new IllegalArgumentException("partnerId is required");
        }
        return getBookings(null, partnerId.trim(), null);
    }

    public String createBookingFromRequest(CreateBookingRequest request) {
        if (request.getPartnerId() == null || request.getPartnerId().trim().isEmpty()) {
            throw new IllegalArgumentException("partnerId is required");
        }
        if (request.getVenueId() == null || request.getVenueId().trim().isEmpty()) {
            throw new IllegalArgumentException("venueId is required");
        }
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getDate() == null || request.getDate().trim().isEmpty()) {
            throw new IllegalArgumentException("date is required (yyyy-MM-dd)");
        }
        if (request.getSlots() == null || request.getSlots().isEmpty()) {
            throw new IllegalArgumentException("slots are required");
        }
        return createBooking(
                request.getPartnerId().trim(),
                request.getUserId().trim(),
                request.getVenueId().trim(),
                request.getSlots(),
                request.getDate().trim(),
                request.getStatus(),
                request.getPaymentStatus(),
                request.getPaymentScreenshotUrl()
        );
    }

    public CreateBookingOrderResponse reserveSlotsCreateBookingAndOrder(
            CreateBookingOrderRequest request, String authenticatedUserSubject) {
        validateCreateBookingOrderRequest(request);

        User user = userService.requireUserForJwtSubject(authenticatedUserSubject);
        String userId = user.getPhone();

        String partnerId = request.getPartnerId().trim();
        String venueId = request.getVenueId().trim();
        String date = request.getDate().trim();
        SlotsService.SlotReservationResult reservation = slotsService.reserveSlotsForBooking(venueId, date, request.getSlotIds());
        List<String> slotIds = reservation.getSlotIds();
        int totalAmount = reservation.getTotalAmount();

        String bookingId = null;
        try {
            bookingId = bookingRepository.createBookingWithSlotIds(
                    partnerId,
                    userId,
                    venueId,
                    slotIds,
                    date,
                    totalAmount,
                    BookingStatus.PENDING.name(),
                    PaymentStatus.PENDING.name(),
                    request.getPaymentScreenshotUrl()
            );

            RazorpayOrderService.RazorpayOrderResult order =
                    razorpayOrderService.createOrder(totalAmount, bookingId, userId);
            bookingRepository.updateRazorpayOrderId(bookingId, order.getOrderId());

            return new CreateBookingOrderResponse(
                    bookingId,
                    order.getKey(),
                    order.getOrderId(),
                    order.getAmount(),
                    order.getCurrency()
            );
        } catch (RuntimeException e) {
            if (bookingId != null) {
                try {
                    bookingRepository.deleteById(bookingId);
                } catch (Exception deleteEx) {
                    System.err.println("Failed to delete booking after order creation failure: " + deleteEx.getMessage());
                }
            }
            try {
                slotsService.releaseReservedSlotsForBooking(venueId, date, slotIds, reservation.getReservedAt());
            } catch (Exception releaseEx) {
                System.err.println("Failed to release reserved slots after order creation failure: " + releaseEx.getMessage());
            }
            throw e;
        }
    }

    /**
     * Same as {@link #reserveSlotsCreateBookingAndOrder} except no Razorpay order is created; booking stays
     * pending with {@code razorpayOrderId} unset.
     */
    public CreateBookingOrderResponse reserveSlotsCreateBookingManual(
            CreateBookingOrderRequest request, String authenticatedUserSubject) {
        validateCreateBookingOrderRequest(request);

        User user = userService.requireUserForJwtSubject(authenticatedUserSubject);
        String userId = user.getPhone();

        String partnerId = request.getPartnerId().trim();
        String venueId = request.getVenueId().trim();
        String date = request.getDate().trim();
        SlotsService.SlotReservationResult reservation = slotsService.reserveSlotsForBooking(venueId, date, request.getSlotIds());
        List<String> slotIds = reservation.getSlotIds();
        int totalAmount = reservation.getTotalAmount();

        String bookingId = null;
        try {
            bookingId = bookingRepository.createBookingWithSlotIds(
                    partnerId,
                    userId,
                    venueId,
                    slotIds,
                    date,
                    totalAmount,
                    BookingStatus.PENDING.name(),
                    PaymentStatus.PENDING.name(),
                    request.getPaymentScreenshotUrl()
            );

            return new CreateBookingOrderResponse(
                    bookingId,
                    null,
                    null,
                    totalAmount * 100,
                    "INR"
            );
        } catch (RuntimeException e) {
            if (bookingId != null) {
                try {
                    bookingRepository.deleteById(bookingId);
                } catch (Exception deleteEx) {
                    System.err.println("Failed to delete booking after manual order failure: " + deleteEx.getMessage());
                }
            }
            try {
                slotsService.releaseReservedSlotsForBooking(venueId, date, slotIds, reservation.getReservedAt());
            } catch (Exception releaseEx) {
                System.err.println("Failed to release reserved slots after manual order failure: " + releaseEx.getMessage());
            }
            throw e;
        }
    }

    /**
     * Partner-initiated manual pending booking (no Razorpay). {@code partnerId} comes from JWT; booking {@code userId}
     * is the customer identifier from {@link CreateBookingOrderRequest#getUserId()} (e.g. phone as stored on users/bookings).
     */
    public CreateBookingOrderResponse reserveSlotsCreateBookingManualForPartner(
            CreateBookingOrderRequest request,
            String partnerIdFromToken) {
        validatePartnerManualBookingRequest(request);
        String partnerId = partnerIdFromToken.trim();
        String userId = request.getUserId().trim();
        String venueId = request.getVenueId().trim();
        String date = request.getDate().trim();
        SlotsService.SlotReservationResult reservation = slotsService.reserveSlotsForBooking(venueId, date, request.getSlotIds());
        List<String> slotIds = reservation.getSlotIds();
        int totalAmount = reservation.getTotalAmount();

        String bookingId = null;
        try {
            bookingId = bookingRepository.createBookingWithSlotIds(
                    partnerId,
                    userId,
                    venueId,
                    slotIds,
                    date,
                    totalAmount,
                    BookingStatus.PENDING.name(),
                    PaymentStatus.PENDING.name(),
                    request.getPaymentScreenshotUrl()
            );

            return new CreateBookingOrderResponse(
                    bookingId,
                    null,
                    null,
                    totalAmount * 100,
                    "INR"
            );
        } catch (RuntimeException e) {
            if (bookingId != null) {
                try {
                    bookingRepository.deleteById(bookingId);
                } catch (Exception deleteEx) {
                    System.err.println("Failed to delete partner manual booking after failure: " + deleteEx.getMessage());
                }
            }
            try {
                slotsService.releaseReservedSlotsForBooking(venueId, date, slotIds, reservation.getReservedAt());
            } catch (Exception releaseEx) {
                System.err.println("Failed to release reserved slots after partner manual order failure: " + releaseEx.getMessage());
            }
            throw e;
        }
    }

    private static void validatePartnerManualBookingRequest(CreateBookingOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("userId is required (customer phone or id on the booking)");
        }
        if (request.getVenueId() == null || request.getVenueId().trim().isEmpty()) {
            throw new IllegalArgumentException("venueId is required");
        }
        if (request.getDate() == null || request.getDate().trim().isEmpty()) {
            throw new IllegalArgumentException("date is required (yyyy-MM-dd)");
        }
        if (request.getSlotIds() == null || request.getSlotIds().isEmpty()) {
            throw new IllegalArgumentException("slotIds are required");
        }
    }

    private static void validateCreateBookingOrderRequest(CreateBookingOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getPartnerId() == null || request.getPartnerId().trim().isEmpty()) {
            throw new IllegalArgumentException("partnerId is required");
        }
        if (request.getVenueId() == null || request.getVenueId().trim().isEmpty()) {
            throw new IllegalArgumentException("venueId is required");
        }
        if (request.getDate() == null || request.getDate().trim().isEmpty()) {
            throw new IllegalArgumentException("date is required (yyyy-MM-dd)");
        }
        if (request.getSlotIds() == null || request.getSlotIds().isEmpty()) {
            throw new IllegalArgumentException("slotIds are required");
        }
    }

    public PaymentVerificationContext getPendingPaymentVerificationContext(String bookingId, User authenticatedUser) {
        if (bookingId == null || bookingId.trim().isEmpty()) {
            throw new IllegalArgumentException("bookingId is required");
        }
        if (authenticatedUser == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }

        Document booking = bookingRepository.findById(bookingId.trim());
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }

        assertBookingOwnedByAuthenticatedUser(
                booking, authenticatedUser, "You are not allowed to verify payment for this booking");

        String bookingStatus = booking.getString("bookingStatus");
        Document payment = (Document) booking.get("payment");
        String paymentStatus = payment != null ? payment.getString("status") : null;
        if (!BookingStatus.PENDING.name().equals(bookingStatus) || !PaymentStatus.PENDING.name().equals(paymentStatus)) {
            throw new IllegalArgumentException("Booking and payment must be PENDING");
        }

        String venueId = booking.getObjectId("venueId").toString();
        String date = booking.getString("date");
        @SuppressWarnings("unchecked")
        List<String> slotIds = (List<String>) booking.get("slotIds");
        if (slotIds == null || slotIds.isEmpty()) {
            throw new IllegalArgumentException("Booking has no slotIds");
        }
        String razorpayOrderId = payment.getString("razorpayOrderId");
        if (razorpayOrderId == null || razorpayOrderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Booking has no Razorpay order id");
        }

        return new PaymentVerificationContext(bookingId.trim(), venueId, date, slotIds, razorpayOrderId);
    }

    public void confirmPayment(String bookingId, String orderId, String paymentId, String signature) {
        bookingRepository.updatePaymentByBookingId(
                bookingId,
                PaymentStatus.SUCCESS,
                orderId,
                paymentId,
                signature,
                BookingStatus.CONFIRMED
        );
    }

    /**
     * Partner-only confirmation analogous to Razorpay {@code /api/payments/verify} minus order-id matching (no RZP flow)
     * and no signature verification. Persists synthetic order and payment ids (random UUIDs) and confirms the booking.
     */
    public BookingItemResponse confirmBookingPaymentManualForPartner(String bookingId, String partnerIdFromToken) {
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("bookingId is required");
        }
        if (partnerIdFromToken == null || partnerIdFromToken.isBlank()) {
            throw new IllegalArgumentException("partnerId is required");
        }

        Document booking = bookingRepository.findById(bookingId.trim());
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }
        String storedPartnerId = booking.getString("partnerId");
        if (storedPartnerId == null || !storedPartnerId.trim().equals(partnerIdFromToken.trim())) {
            throw new IllegalArgumentException("You are not allowed to confirm payment for this booking");
        }

        String bookingStatus = booking.getString("bookingStatus");
        Document payment = (Document) booking.get("payment");
        String paymentStatus = payment != null ? payment.getString("status") : null;
        if (!BookingStatus.PENDING.name().equals(bookingStatus) || !PaymentStatus.PENDING.name().equals(paymentStatus)) {
            throw new IllegalArgumentException("Booking and payment must be PENDING");
        }

        String venueId = booking.getObjectId("venueId").toString();
        String date = booking.getString("date");
        List<String> slotIds = resolveSlotIdsForMutation(booking);

        slotsService.ensureSlotsReservedForBooking(venueId, date, slotIds);

        String syntheticOrderId = UUID.randomUUID().toString();
        String syntheticPaymentId = UUID.randomUUID().toString();
        confirmPayment(bookingId.trim(), syntheticOrderId, syntheticPaymentId, "");
        slotsService.markReservedSlotsBookedForBooking(venueId, date, slotIds);

        return getBookingDetailsById(bookingId.trim());
    }

    public BookingItemResponse getBookingDetailsById(String bookingId) {
        Document booking = bookingRepository.findById(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }
        return mapDocumentToBookingItem(booking);
    }

    public static class PaymentVerificationContext {
        private final String bookingId;
        private final String venueId;
        private final String date;
        private final List<String> slotIds;
        private final String razorpayOrderId;

        public PaymentVerificationContext(String bookingId, String venueId, String date, List<String> slotIds, String razorpayOrderId) {
            this.bookingId = bookingId;
            this.venueId = venueId;
            this.date = date;
            this.slotIds = slotIds;
            this.razorpayOrderId = razorpayOrderId;
        }

        public String getBookingId() {
            return bookingId;
        }

        public String getVenueId() {
            return venueId;
        }

        public String getDate() {
            return date;
        }

        public List<String> getSlotIds() {
            return slotIds;
        }

        public String getRazorpayOrderId() {
            return razorpayOrderId;
        }
    }

    public String createBooking(String partnerId, String userId, String venueId, List<CreateBookingRequest.SlotDto> slotDtos, String date, String status, String paymentStatus, String paymentScreenshotUrl) {
        // Calculate total amount from slot DTOs and extract slot IDs
        int totalAmount = 0;
        List<String> slotIds = new ArrayList<>();
        
        if (slotDtos != null && !slotDtos.isEmpty()) {
            for (CreateBookingRequest.SlotDto slotDto : slotDtos) {
                if (slotDto.getPrice() > 0) {
                    totalAmount += slotDto.getPrice();
                }
                if (slotDto.getSlotId() != null) {
                    slotIds.add(slotDto.getSlotId());
                }
            }
        }
        
        // Create booking with provided status and paymentStatus, storing complete slot details
        String bookingId = bookingRepository.createBookingDirect(partnerId, userId, venueId, slotDtos, date, totalAmount, status, paymentStatus, paymentScreenshotUrl);
        
        // Only mark slots as booked if both paymentStatus and status are SUCCESS
        // For booking status, SUCCESS or CONFIRMED are considered success states
        boolean shouldMarkBooked = paymentStatus != null && "SUCCESS".equalsIgnoreCase(paymentStatus.trim()) && 
                                   status != null && ("SUCCESS".equalsIgnoreCase(status.trim()));
        
        if (shouldMarkBooked && !slotIds.isEmpty()) {
            try {
                slotsRepository.markSlotsBooked(venueId, date, slotIds);
            } catch (Exception e) {
                // Log error but don't fail booking creation
                System.err.println("Failed to mark slots as booked: " + e.getMessage());
            }
        }

        // Send Expo notification if status and paymentStatus are PENDING
        boolean shouldSendNotification = paymentStatus != null && "PENDING".equalsIgnoreCase(paymentStatus.trim()) && 
                                        status != null && "PENDING".equalsIgnoreCase(status.trim());
        
        if (shouldSendNotification) {
            try {
                // Get Expo tokens for the partner
                List<String> expoTokens = partnerRepository.getExpoTokens(partnerId);
                if (expoTokens != null && !expoTokens.isEmpty()) {
                    // Get venue details for notification
                    Venue venue = venueRepository.findById(venueId);
                    String venueName = venue != null ? venue.getName() : "Unknown Venue";
                    
                    // Send notification to all tokens using Expo Server SDK
                    expoPushNotificationService.sendBookingNotification(
                            expoTokens,
                            bookingId,
                            venueName,
                            date,
                            String.valueOf(totalAmount)
                    );
                } else {
                    System.err.println("Expo tokens not found for partner: " + partnerId);
                }
            } catch (Exception e) {
                // Log error but don't fail booking creation
                System.err.println("Failed to send Expo notification: " + e.getMessage());
            }
        }
        
        return bookingId;
    }
    
    public void confirmBooking(String bookingId) {
        if (bookingId == null || bookingId.trim().isEmpty()) {
            throw new IllegalArgumentException("bookingId is required");
        }

        org.bson.Document booking = bookingRepository.findById(bookingId.trim());
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }

        bookingRepository.confirmBooking(bookingId.trim());
        
        // Mark slots as booked in the slots collection
        String venueId = booking.getObjectId("venueId").toString();
        String date = booking.getString("date");
        
        @SuppressWarnings("unchecked")
        List<Document> slotsDocs = (List<Document>) booking.get("slots");
        if (slotsDocs != null && !slotsDocs.isEmpty()) {
            List<String> slotIds = new ArrayList<>();
            for (Document slotDoc : slotsDocs) {
                String slotId = slotDoc.getString("slotId");
                if (slotId != null) {
                    slotIds.add(slotId);
                }
            }
            if (!slotIds.isEmpty()) {
                try {
                    slotsRepository.markSlotsBooked(venueId, date, slotIds);
                } catch (Exception e) {
                    System.err.println("Failed to mark slots as booked: " + e.getMessage());
                }
            }
        }
    }

    public List<BookingItemResponse> getUserBookingsByMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String cleaned = mobileNumber.trim().replaceAll("\\s+", "");

        if (cleaned.startsWith("91") && cleaned.length() == 12) {
            cleaned = cleaned.substring(2);
        }
        return getUserBookings(cleaned);
    }

    /**
     * Paged bookings for the authenticated JWT subject (normalized mobile digits — same semantics as listing by phone).
     */
    public UserBookingsPageResponse listMyBookingsPaged(
            String jwtSubject,
            String bookingStatusFilter,
            int page,
            int pageSize) {
        if (jwtSubject == null || jwtSubject.trim().isEmpty()) {
            throw new IllegalArgumentException("Authenticated user identifier is required");
        }
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(pageSize <= 0 ? 10 : pageSize, 1), 100);

        String cleaned = jwtSubject.trim().replaceAll("\\s+", "");
        if (cleaned.startsWith("91") && cleaned.length() == 12) {
            cleaned = cleaned.substring(2);
        }

        Query query = new Query();
        addUserIdMatchCriteria(query, cleaned);
        if (bookingStatusFilter != null && !bookingStatusFilter.isBlank()) {
            try {
                BookingStatus bs = BookingStatus.valueOf(bookingStatusFilter.trim().toUpperCase());
                query.addCriteria(Criteria.where("bookingStatus").is(bs.name()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid bookingStatus; use one of: " + java.util.Arrays.toString(BookingStatus.values()));
            }
        }

        Document filter = query.getQueryObject();
        List<Document> bookingDocs =
                bookingRepository.findByQueryPaged(filter, safePage * safeSize, safeSize);
        List<BookingItemResponse> items = new ArrayList<>();
        for (Document doc : bookingDocs) {
            items.add(mapDocumentToBookingItem(doc));
        }
        return new UserBookingsPageResponse(items, safePage, safeSize);
    }
    
    public void cancelBooking(String bookingId) {
        if (bookingId == null || bookingId.trim().isEmpty()) {
            throw new IllegalArgumentException("bookingId is required");
        }

        org.bson.Document booking = bookingRepository.findById(bookingId.trim());
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }

        bookingRepository.cancelBooking(bookingId.trim());
        
        // Mark slots as free in the slots collection
        String venueId = booking.getObjectId("venueId").toString();
        String date = booking.getString("date");
        
        @SuppressWarnings("unchecked")
        List<Document> slotsDocs = (List<Document>) booking.get("slots");
        if (slotsDocs != null && !slotsDocs.isEmpty()) {
            List<String> slotIds = new ArrayList<>();
            for (Document slotDoc : slotsDocs) {
                String slotId = slotDoc.getString("slotId");
                if (slotId != null) {
                    slotIds.add(slotId);
                }
            }
            if (!slotIds.isEmpty()) {
                try {
                    slotsRepository.markSlotsFree(venueId, date, slotIds);
                } catch (Exception e) {
                    System.err.println("Failed to mark slots as free: " + e.getMessage());
                }
            }
        }
    }

    public record AccountDeletionResult(long bookingsRemoved, boolean userRemoved) {}

    /**
     * Deletes all {@code bookings} whose {@code userId} equals the JWT {@code subject}, frees slots when possible,
     * then deletes the {@code users} document.
     */
    public AccountDeletionResult deleteUserAccountForJwtSubject(String jwtSubject) {
        User user = userService.requireUserForJwtSubject(jwtSubject);
        List<Document> bookings = bookingRepository.findAllByUserId(jwtSubject);

        long bookingsRemoved = 0;
        for (Document booking : bookings) {
            try {
                releaseSlotsForDeletedAccountBooking(booking);
            } catch (Exception e) {
                logger.warn("Account deletion: could not release slots for booking {}",
                        booking != null && booking.getObjectId("_id") != null
                                ? booking.getObjectId("_id").toHexString()
                                : "?",
                        e);
            }
            String bid = extractBookingHexId(booking);
            if (bid != null) {
                bookingRepository.deleteById(bid);
                bookingsRemoved++;
            }
        }

        boolean userRemoved = userRepository.deleteById(user.getId()) > 0;
        if (!userRemoved) {
            logger.warn("Account deletion: user document was not deleted for id {}", user.getId());
        }
        return new AccountDeletionResult(bookingsRemoved, userRemoved);
    }

    private static String extractBookingHexId(Document booking) {
        if (booking == null || booking.getObjectId("_id") == null) {
            return null;
        }
        return booking.getObjectId("_id").toHexString();
    }

    private void releaseSlotsForDeletedAccountBooking(Document booking) {
        if (booking == null || booking.get("venueId") == null || booking.getString("date") == null) {
            return;
        }
        String venueId = booking.getObjectId("venueId").toString();
        String date = booking.getString("date");
        List<String> slotIds = resolveSlotIdsForMutation(booking);
        if (slotIds == null || slotIds.isEmpty()) {
            return;
        }
        slotsRepository.markSlotsFree(venueId, date, slotIds);
    }
}



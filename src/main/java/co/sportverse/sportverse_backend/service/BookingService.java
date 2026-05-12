package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.dto.BookingItemResponse;
import co.sportverse.sportverse_backend.dto.CreateBookingRequest;
import co.sportverse.sportverse_backend.dto.VenueResponse;
import co.sportverse.sportverse_backend.entity.TimeSlot;
import co.sportverse.sportverse_backend.entity.Venue;
import co.sportverse.sportverse_backend.entity.VenueSlots;
import co.sportverse.sportverse_backend.entity.User;
import co.sportverse.sportverse_backend.repository.BookingRepository;
import co.sportverse.sportverse_backend.repository.PartnerRepository;
import co.sportverse.sportverse_backend.repository.SlotsRepository;
import co.sportverse.sportverse_backend.repository.VenueRepository;
import co.sportverse.sportverse_backend.service.UserService;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BookingService {

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
                TimeSlot slot = new TimeSlot();
                slot.setSlotId(slotDoc.getString("slotId"));
                slot.setStartTime(slotDoc.getString("startTime"));
                slot.setEndTime(slotDoc.getString("endTime"));
                slot.setStartTimeAmPm(slotDoc.getString("startTimeAmPm"));
                slot.setEndTimeAmPm(slotDoc.getString("endTimeAmPm"));
                Object priceValue = slotDoc.get("price");
                if (priceValue instanceof Number) {
                    slot.setPrice(((Number) priceValue).intValue());
                }
                slot.setBooked(slotDoc.getBoolean("isBooked", false));
                selected.add(slot);
            }
            item.setSlots(selected);
        } else {
            @SuppressWarnings("unchecked")
            List<String> slotIds = (List<String>) doc.get("slotIds");
            if (slotIds != null && !slotIds.isEmpty()) {
                VenueSlots vs = slotsRepository.findByVenueIdAndDate(venueId, date);
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

        return item;
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
}



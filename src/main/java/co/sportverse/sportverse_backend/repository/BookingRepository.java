package co.sportverse.sportverse_backend.repository;

import co.sportverse.sportverse_backend.entity.BookingStatus;
import co.sportverse.sportverse_backend.entity.PaymentStatus;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

@Component
public class BookingRepository {

    private final MongoCollection<Document> bookingsCollection;

    @Autowired
    public BookingRepository(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase("sportverse");
        this.bookingsCollection = database.getCollection("bookings");
    }

    public void setOrderCreated(String bookingId, String orderId, int amount, String currency) {
        Bson filter = eq("bookingId", bookingId);
        Document paymentDoc = new Document("orderId", orderId)
                .append("paymentId", null)
                .append("signature", null);
        bookingsCollection.updateOne(filter, combine(
                set("status", PaymentStatus.PENDING.name()),
                set("amount", amount),
                set("payment", paymentDoc)
        ));
    }

    public void updatePaymentStatus(String bookingId, PaymentStatus status, String orderId, String paymentId, String signature) {
        Bson filter = eq("bookingId", bookingId);
        Document paymentDoc = new Document("orderId", orderId)
                .append("paymentId", paymentId)
                .append("signature", signature);
        bookingsCollection.updateOne(filter, combine(
                set("status", status.name()),
                set("payment", paymentDoc)
        ));
    }

    public String createBooking(String userId, String venueId, List<String> slotIds, String date, int amount, String razorpayOrderId) {
        Instant now = java.time.Instant.now();
        List<String> slotIdList = new ArrayList<>();
        if (slotIds != null) {
            slotIdList.addAll(slotIds);
        }
        Document booking = new Document()
                .append("userId", new org.bson.types.ObjectId(userId))
                .append("venueId", new org.bson.types.ObjectId(venueId))
                .append("slotIds", slotIdList)
                .append("date", date)
                .append("amount", amount)
                .append("payment", new Document()
                        .append("razorpayOrderId", razorpayOrderId)
                        .append("razorpayPaymentId", null)
                        .append("razorpaySignature", null)
                        .append("status", PaymentStatus.PENDING.name())
                )
                .append("bookingStatus", BookingStatus.INITIATED.name())
                .append("createdAt", now)
                .append("updatedAt", now);
        bookingsCollection.insertOne(booking);
        return booking.getObjectId("_id").toString();
    }

    public Document findByRazorpayOrderId(String razorpayOrderId) {
        Bson filter = eq("payment.razorpayOrderId", razorpayOrderId);
        return bookingsCollection.find(filter).first();
    }

    public void updatePaymentByOrderId(String razorpayOrderId, PaymentStatus paymentStatus, String paymentId, String signature, BookingStatus bookingStatus) {
        Bson filter = eq("payment.razorpayOrderId", razorpayOrderId);
        java.time.Instant now = java.time.Instant.now();
        bookingsCollection.updateOne(filter, combine(
                set("payment.razorpayPaymentId", paymentId),
                set("payment.razorpaySignature", signature),
                set("payment.status", paymentStatus.name()),
                set("bookingStatus", bookingStatus.name()),
                set("updatedAt", now)
        ));
    }

    public java.util.List<Document> findByUserId(String userId) {
        Bson filter = eq("userId", userId);
        return bookingsCollection.find(filter)
                .sort(descending("createdAt"))
                .into(new java.util.ArrayList<>());
    }

    public java.util.List<Document> findByVenueIds(List<String> venueIds) {
        if (venueIds == null || venueIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<org.bson.types.ObjectId> venueObjectIds = new ArrayList<>();
        for (String vid : venueIds) {
            venueObjectIds.add(new org.bson.types.ObjectId(vid));
        }
        Bson filter = in("venueId", venueObjectIds);
        return bookingsCollection.find(filter)
                .sort(descending("createdAt"))
                .into(new ArrayList<>());
    }

    public String createBookingDirect(String partnerId, String userId, String venueId, List<co.sportverse.sportverse_backend.dto.CreateBookingRequest.SlotDto> slotDtos, String date, int amount, String status, String paymentStatus) {
        Instant now = Instant.now();
        List<Document> slotsList = new ArrayList<>();
        if (slotDtos != null) {
            for (co.sportverse.sportverse_backend.dto.CreateBookingRequest.SlotDto slotDto : slotDtos) {
                Document slotDoc = new Document()
                        .append("slotId", slotDto.getSlotId())
                        .append("startTime", slotDto.getStartTime())
                        .append("endTime", slotDto.getEndTime())
                        .append("startTimeAmPm", slotDto.getStartTimeAmPm())
                        .append("endTimeAmPm", slotDto.getEndTimeAmPm())
                        .append("price", slotDto.getPrice())
                        .append("isBooked", slotDto.isBooked());
                slotsList.add(slotDoc);
            }
        }
        
        // Use provided status and paymentStatus, or default values
        String bookingStatusValue = (status != null && !status.trim().isEmpty()) ? status.trim() : BookingStatus.PENDING.name();
        String paymentStatusValue = (paymentStatus != null && !paymentStatus.trim().isEmpty()) ? paymentStatus.trim() : PaymentStatus.PENDING.name();
        
        Document booking = new Document()
                .append("partnerId", partnerId)
                .append("userId", userId)
                .append("venueId", new org.bson.types.ObjectId(venueId))
                .append("slots", slotsList)
                .append("date", date)
                .append("amount", amount)
                .append("payment", new Document()
                        .append("razorpayOrderId", null)
                        .append("razorpayPaymentId", null)
                        .append("razorpaySignature", null)
                        .append("status", paymentStatusValue)
                )
                .append("bookingStatus", bookingStatusValue)
                .append("createdAt", now)
                .append("updatedAt", now);
        bookingsCollection.insertOne(booking);
        return booking.getObjectId("_id").toString();
    }
    
    public Document findById(String bookingId) {
        if (bookingId == null || bookingId.trim().isEmpty()) {
            return null;
        }
        try {
            Bson filter = eq("_id", new org.bson.types.ObjectId(bookingId.trim()));
            return bookingsCollection.find(filter).first();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public void confirmBooking(String bookingId) {
        if (bookingId == null || bookingId.trim().isEmpty()) {
            throw new IllegalArgumentException("Booking ID is required");
        }
        try {
            Bson filter = eq("_id", new org.bson.types.ObjectId(bookingId.trim()));
            Instant now = Instant.now();
            bookingsCollection.updateOne(filter, combine(
                    set("bookingStatus", BookingStatus.SUCCESS.name()),
                    set("payment.status", PaymentStatus.SUCCESS.name()),
                    set("updatedAt", now)
            ));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid booking ID format: " + bookingId);
        }
    }
}



package co.sportverse.sportverse_backend.repository;

import co.sportverse.sportverse_backend.entity.VenueSlots;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;

@Component
public class SlotsRepository {

    private final MongoCollection<Document> slotsCollection;

    @Autowired
    public SlotsRepository(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase("sportverse");
        this.slotsCollection = database.getCollection("slots");
    }

    public VenueSlots save(VenueSlots venueSlots) {
        if (venueSlots.getCreatedAt() == null) {
            venueSlots.setCreatedAt(LocalDateTime.now());
        }
        Document doc = venueSlots.toDocument();
        slotsCollection.insertOne(doc);
        venueSlots.setId(doc.getObjectId("_id").toString());
        return venueSlots;
    }

    public VenueSlots findByVenueIdAndDate(String venueId, String date) {
        Bson filter = and(eq("venueId", new org.bson.types.ObjectId(venueId)), eq("date", date));
        return VenueSlots.fromDocument(slotsCollection.find(filter).first());
    }

    public void markSlotsBooked(String venueId, String date, java.util.List<String> slotIds) {
        if (slotIds == null || slotIds.isEmpty()) return;
        Bson filter = and(eq("venueId", new org.bson.types.ObjectId(venueId)), eq("date", date));
        Document doc = slotsCollection.find(filter).first();
        if (doc == null) return;
        java.util.List<Document> slotDocs = (java.util.List<Document>) doc.get("slots");
        if (slotDocs != null) {
            java.util.Set<String> target = new java.util.HashSet<>(slotIds);
            for (Document s : slotDocs) {
                String sid = s.getString("slotId");
                if (sid != null && target.contains(sid)) {
                    s.put("isBooked", true);
                    s.put("status", "BOOKED");
                    s.remove("reservedAt");
                }
            }
            doc.put("slots", slotDocs);
            slotsCollection.replaceOne(filter, doc);
        }
    }

    public void markSlotsBooked(ClientSession session, String venueId, String date, java.util.List<String> slotIds) {
        if (slotIds == null || slotIds.isEmpty()) return;
        Bson filter = and(eq("venueId", new org.bson.types.ObjectId(venueId)), eq("date", date));
        Document doc = slotsCollection.find(session, filter).first();
        if (doc == null) return;
        java.util.List<Document> slotDocs = (java.util.List<Document>) doc.get("slots");
        if (slotDocs != null) {
            java.util.Set<String> target = new java.util.HashSet<>(slotIds);
            for (Document s : slotDocs) {
                String sid = s.getString("slotId");
                if (sid != null && target.contains(sid)) {
                    s.put("isBooked", true);
                    s.put("status", "BOOKED");
                    s.remove("reservedAt");
                }
            }
            doc.put("slots", slotDocs);
            slotsCollection.replaceOne(session, filter, doc);
        }
    }

    public void markSlotsFree(String venueId, String date, java.util.List<String> slotIds) {
        if (slotIds == null || slotIds.isEmpty()) return;
        Bson filter = and(eq("venueId", new org.bson.types.ObjectId(venueId)), eq("date", date));
        Document doc = slotsCollection.find(filter).first();
        if (doc == null) return;
        java.util.List<Document> slotDocs = (java.util.List<Document>) doc.get("slots");
        if (slotDocs != null) {
            java.util.Set<String> target = new java.util.HashSet<>(slotIds);
            for (Document s : slotDocs) {
                String sid = s.getString("slotId");
                if (sid != null && target.contains(sid.trim())) {
                    s.put("isBooked", false);
                    s.put("status", "AVAILABLE");
                    s.remove("reservedAt");
                }
            }
            doc.put("slots", slotDocs);
            slotsCollection.replaceOne(filter, doc);
        }
    }

    /**
     * After cancellation: BOOKED slots (or legacy {@code isBooked} true) matching ids become AVAILABLE for re-booking.
     */
    public void releaseBookedSlotsForCancellation(String venueId, String date, java.util.List<String> slotIds) {
        if (slotIds == null || slotIds.isEmpty()) {
            return;
        }
        Bson filter = and(eq("venueId", new org.bson.types.ObjectId(venueId)), eq("date", date));
        Document doc = slotsCollection.find(filter).first();
        if (doc == null) {
            return;
        }
        java.util.List<Document> slotDocs = (java.util.List<Document>) doc.get("slots");
        if (slotDocs != null) {
            java.util.Set<String> normalized = slotIds.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.toSet());
            for (Document s : slotDocs) {
                String sid = s.getString("slotId");
                if (sid == null || !normalized.contains(sid.trim())) {
                    continue;
                }
                Object statusObj = s.get("status");
                boolean bookedLike = Boolean.TRUE.equals(s.getBoolean("isBooked", false))
                        || "BOOKED".equals(statusObj)
                        || (statusObj instanceof String ss && "BOOKED".equalsIgnoreCase(ss));
                if (bookedLike) {
                    s.put("status", "AVAILABLE");
                    s.put("isBooked", false);
                    s.remove("reservedAt");
                }
            }
            doc.put("slots", slotDocs);
            slotsCollection.replaceOne(filter, doc);
        }
    }

    public void releaseBookedSlotsForCancellation(ClientSession session, String venueId, String date, java.util.List<String> slotIds) {
        if (slotIds == null || slotIds.isEmpty()) {
            return;
        }
        Bson filter = and(eq("venueId", new org.bson.types.ObjectId(venueId)), eq("date", date));
        Document doc = slotsCollection.find(session, filter).first();
        if (doc == null) {
            return;
        }
        java.util.List<Document> slotDocs = (java.util.List<Document>) doc.get("slots");
        if (slotDocs != null) {
            java.util.Set<String> normalized = slotIds.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.toSet());
            for (Document s : slotDocs) {
                String sid = s.getString("slotId");
                if (sid == null || !normalized.contains(sid.trim())) {
                    continue;
                }
                Object statusObj = s.get("status");
                boolean bookedLike = Boolean.TRUE.equals(s.getBoolean("isBooked", false))
                        || "BOOKED".equals(statusObj)
                        || (statusObj instanceof String ss && "BOOKED".equalsIgnoreCase(ss));
                if (bookedLike) {
                    s.put("status", "AVAILABLE");
                    s.put("isBooked", false);
                    s.remove("reservedAt");
                }
            }
            doc.put("slots", slotDocs);
            slotsCollection.replaceOne(session, filter, doc);
        }
    }

    public boolean reserveSlotsIfAvailable(String venueId, String date, List<String> slotIds, Instant reservedAt, Instant reservationExpiresBefore) {
        if (slotIds == null || slotIds.isEmpty()) {
            throw new IllegalArgumentException("slotIds are required");
        }

        List<Document> requiredSlots = new ArrayList<>();
        for (String slotId : slotIds) {
            if (slotId == null || slotId.trim().isEmpty()) {
                throw new IllegalArgumentException("slotId is required");
            }
            requiredSlots.add(new Document("$elemMatch", new Document("slotId", slotId.trim()).append("$or", List.of(
                    new Document("status", "AVAILABLE"),
                    new Document("status", "RESERVED").append("reservedAt", new Document("$lt", Date.from(reservationExpiresBefore)))
            ))));
        }

        Document filter = new Document("venueId", new org.bson.types.ObjectId(venueId))
                .append("date", date)
                .append("slots", new Document("$all", requiredSlots));

        Document update = new Document("$set", new Document("slots.$[slot].status", "RESERVED")
                .append("slots.$[slot].reservedAt", Date.from(reservedAt))
                .append("slots.$[slot].isBooked", false));

        UpdateOptions options = new UpdateOptions().arrayFilters(List.of(
                new Document("slot.slotId", new Document("$in", slotIds.stream().map(String::trim).toList()))
        ));

        UpdateResult result = slotsCollection.updateOne(filter, update, options);
        return result.getModifiedCount() == 1;
    }

    public boolean areSlotsReserved(String venueId, String date, List<String> slotIds) {
        if (slotIds == null || slotIds.isEmpty()) {
            return false;
        }

        List<Document> requiredSlots = new ArrayList<>();
        for (String slotId : slotIds) {
            requiredSlots.add(new Document("$elemMatch", new Document("slotId", slotId.trim())
                    .append("status", "RESERVED")));
        }

        Document filter = new Document("venueId", new org.bson.types.ObjectId(venueId))
                .append("date", date)
                .append("slots", new Document("$all", requiredSlots));
        return slotsCollection.find(filter).first() != null;
    }

    public boolean markReservedSlotsBooked(String venueId, String date, List<String> slotIds) {
        if (slotIds == null || slotIds.isEmpty()) {
            return false;
        }

        List<Document> requiredSlots = new ArrayList<>();
        for (String slotId : slotIds) {
            requiredSlots.add(new Document("$elemMatch", new Document("slotId", slotId.trim())
                    .append("status", "RESERVED")));
        }

        Document filter = new Document("venueId", new org.bson.types.ObjectId(venueId))
                .append("date", date)
                .append("slots", new Document("$all", requiredSlots));
        Document update = new Document("$set", new Document("slots.$[slot].status", "BOOKED")
                .append("slots.$[slot].isBooked", true))
                .append("$unset", new Document("slots.$[slot].reservedAt", ""));
        UpdateOptions options = new UpdateOptions().arrayFilters(List.of(
                new Document("slot.slotId", new Document("$in", slotIds.stream().map(String::trim).toList()))
                        .append("slot.status", "RESERVED")
        ));

        UpdateResult result = slotsCollection.updateOne(filter, update, options);
        return result.getModifiedCount() == 1;
    }

    public void releaseReservedSlots(String venueId, String date, List<String> slotIds, Instant reservedAt) {
        if (slotIds == null || slotIds.isEmpty()) {
            return;
        }

        Document filter = new Document("venueId", new org.bson.types.ObjectId(venueId))
                .append("date", date);
        Date reservedAtDate = Date.from(reservedAt);
        Document update = new Document("$set", new Document("slots.$[slot].status", "AVAILABLE"))
                .append("$unset", new Document("slots.$[slot].reservedAt", ""));
        UpdateOptions options = new UpdateOptions().arrayFilters(List.of(
                new Document("slot.slotId", new Document("$in", slotIds.stream().map(String::trim).toList()))
                        .append("slot.status", "RESERVED")
                        .append("slot.reservedAt", reservedAtDate)
        ));

        slotsCollection.updateOne(filter, update, options);
    }

    public VenueSlots updateSlots(VenueSlots venueSlots) {
        Bson filter = and(eq("venueId", new org.bson.types.ObjectId(venueSlots.getVenueId())), eq("date", venueSlots.getDate()));
        Document doc = venueSlots.toDocument();
        slotsCollection.replaceOne(filter, doc);
        return venueSlots;
    }

    public boolean deleteSlot(String venueId, String date, String slotId) {
        Bson filter = and(eq("venueId", new org.bson.types.ObjectId(venueId)), eq("date", date));
        Document doc = slotsCollection.find(filter).first();
        if (doc == null) {
            return false;
        }
        java.util.List<Document> slotDocs = (java.util.List<Document>) doc.get("slots");
        if (slotDocs == null || slotDocs.isEmpty()) {
            return false;
        }
        boolean found = false;
        java.util.List<Document> updatedSlots = new java.util.ArrayList<>();
        for (Document slotDoc : slotDocs) {
            String sid = slotDoc.getString("slotId");
            if (sid != null && sid.equals(slotId)) {
                found = true;
                // Skip this slot (don't add it to updatedSlots)
            } else {
                updatedSlots.add(slotDoc);
            }
        }
        if (found) {
            doc.put("slots", updatedSlots);
            slotsCollection.replaceOne(filter, doc);
            return true;
        }
        return false;
    }
}



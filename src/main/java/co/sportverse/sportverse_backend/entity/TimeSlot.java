package co.sportverse.sportverse_backend.entity;

import org.bson.Document;

import java.time.Instant;
import java.util.Date;

public class TimeSlot {

    String slotId;
    String startTime;
    String endTime;
    String startTimeAmPm;
    String endTimeAmPm;
    int price;
    boolean isBooked;
    /** e.g. {@code AVAILABLE}, {@code RESERVED}, {@code BOOKED} (matches {@code SlotsRepository}). */
    String status;
    /** When the slot was marked {@code RESERVED} (Mongo {@code Date} round-trip). */
    Instant reservedAt;

    public TimeSlot() {}

    public TimeSlot(String slotId, String startTime, String endTime, int price, boolean isBooked) {
        this.slotId = slotId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.isBooked = isBooked;
    }

    public TimeSlot(String slotId, String startTime, String endTime, String startTimeAmPm, String endTimeAmPm, int price, boolean isBooked) {
        this.slotId = slotId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startTimeAmPm = startTimeAmPm;
        this.endTimeAmPm = endTimeAmPm;
        this.price = price;
        this.isBooked = isBooked;
    }

    public String getSlotId() {
        return slotId;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public boolean isBooked() {
        return isBooked;
    }

    public void setBooked(boolean booked) {
        isBooked = booked;
    }

    public String getStartTimeAmPm() {
        return startTimeAmPm;
    }

    public void setStartTimeAmPm(String startTimeAmPm) {
        this.startTimeAmPm = startTimeAmPm;
    }

    public String getEndTimeAmPm() {
        return endTimeAmPm;
    }

    public void setEndTimeAmPm(String endTimeAmPm) {
        this.endTimeAmPm = endTimeAmPm;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(Instant reservedAt) {
        this.reservedAt = reservedAt;
    }

    public static TimeSlot fromDocument(Document doc) {
        if (doc == null) return null;
        TimeSlot slot = new TimeSlot();
        slot.setSlotId(doc.getString("slotId"));
        slot.setStartTime(doc.getString("startTime"));
        slot.setEndTime(doc.getString("endTime"));
        slot.setStartTimeAmPm(doc.getString("startTimeAmPm"));
        slot.setEndTimeAmPm(doc.getString("endTimeAmPm"));
        Object priceValue = doc.get("price");
        if (priceValue instanceof Number) {
            slot.setPrice(((Number) priceValue).intValue());
        }
        slot.setBooked(doc.getBoolean("isBooked", false));
        String st = doc.getString("status");
        if (st == null || st.isBlank()) {
            slot.setStatus(slot.isBooked() ? "BOOKED" : "AVAILABLE");
        } else {
            slot.setStatus(st);
        }

        Object ra = doc.get("reservedAt");
        if (ra != null) {
            if (ra instanceof Date) {
                slot.setReservedAt(((Date) ra).toInstant());
            } else if (ra instanceof Instant) {
                slot.setReservedAt((Instant) ra);
            } else if (ra instanceof Number) {
                slot.setReservedAt(Instant.ofEpochMilli(((Number) ra).longValue()));
            }
        }

        return slot;
    }

    public Document toDocument() {
        Document doc = new Document();
        doc.append("slotId", this.slotId);
        doc.append("startTime", this.startTime);
        doc.append("endTime", this.endTime);
        doc.append("startTimeAmPm", this.startTimeAmPm);
        doc.append("endTimeAmPm", this.endTimeAmPm);
        doc.append("price", this.price);
        doc.append("isBooked", this.isBooked);
        String st = status;
        if (st == null || st.isBlank()) {
            st = isBooked ? "BOOKED" : "AVAILABLE";
        }
        doc.append("status", st);
        if (reservedAt != null) {
            doc.append("reservedAt", Date.from(reservedAt));
        }
        return doc;
    }
}



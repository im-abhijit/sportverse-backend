package co.sportverse.sportverse_backend.dto.home;

/**
 * Next booking teaser; when {@code exists} is false, other fields may be null.
 */
public class UpcomingBookingDto {
    private boolean exists;
    private String bookingId;
    private String venueName;
    private String sport;
    private String date;
    private String slotStartsAt;
    private String slotEndsAt;
    private String status;
    private Integer amount;

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSlotStartsAt() {
        return slotStartsAt;
    }

    public void setSlotStartsAt(String slotStartsAt) {
        this.slotStartsAt = slotStartsAt;
    }

    public String getSlotEndsAt() {
        return slotEndsAt;
    }

    public void setSlotEndsAt(String slotEndsAt) {
        this.slotEndsAt = slotEndsAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}

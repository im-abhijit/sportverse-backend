package co.sportverse.sportverse_backend.dto;

import java.util.List;

public class CreateBookingOrderRequest {
    private String partnerId;
    private String venueId;
    private String date;
    private List<String> slotIds;
    private String paymentScreenshotUrl;
    private String userId;

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getVenueId() {
        return venueId;
    }

    public void setVenueId(String venueId) {
        this.venueId = venueId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<String> getSlotIds() {
        return slotIds;
    }

    public void setSlotIds(List<String> slotIds) {
        this.slotIds = slotIds;
    }

    public String getPaymentScreenshotUrl() {
        return paymentScreenshotUrl;
    }

    public void setPaymentScreenshotUrl(String paymentScreenshotUrl) {
        this.paymentScreenshotUrl = paymentScreenshotUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}

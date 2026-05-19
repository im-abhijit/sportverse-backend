package co.sportverse.sportverse_backend.dto;

public class CreateBookingOrderResponse {
    private String bookingId;
    private String key;
    private String orderId;
    private int amount;
    private String currency;

    public CreateBookingOrderResponse() {}

    public CreateBookingOrderResponse(String bookingId, String key, String orderId, int amount, String currency) {
        this.bookingId = bookingId;
        this.key = key;
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}

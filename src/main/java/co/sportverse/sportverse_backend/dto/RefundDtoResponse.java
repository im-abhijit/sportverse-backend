package co.sportverse.sportverse_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class RefundDtoResponse {
    private String status;
    private String refundId;
    private Long amount;
    private String paymentId;
    private Long createdAt;
    @JsonProperty("acquirer_data")
    private List<String> acquirerData = new ArrayList<>();
    private String speed;
    private String error;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRefundId() {
        return refundId;
    }

    public void setRefundId(String refundId) {
        this.refundId = refundId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getAcquirerData() {
        return acquirerData;
    }

    public void setAcquirerData(List<String> acquirerData) {
        this.acquirerData = acquirerData != null ? acquirerData : new ArrayList<>();
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

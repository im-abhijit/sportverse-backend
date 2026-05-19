package co.sportverse.sportverse_backend.dto;

public class SendOtpRequest {
    private String phoneNumber;
    private String channel;

    public SendOtpRequest() {}

    public SendOtpRequest(String phoneNumber, String channel) {
        this.phoneNumber = phoneNumber;
        this.channel = channel;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}

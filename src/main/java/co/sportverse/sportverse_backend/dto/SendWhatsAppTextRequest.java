package co.sportverse.sportverse_backend.dto;

public class SendWhatsAppTextRequest {
    private String toPhone;
    private String body;

    public SendWhatsAppTextRequest() {}

    public String getToPhone() {
        return toPhone;
    }

    public void setToPhone(String toPhone) {
        this.toPhone = toPhone;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}

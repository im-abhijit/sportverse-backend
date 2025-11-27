package co.sportverse.sportverse_backend.dto;

public class ExpoTokenRequest {
    private String expoToken;

    public ExpoTokenRequest() {}

    public ExpoTokenRequest(String expoToken) {
        this.expoToken = expoToken;
    }

    public String getExpoToken() {
        return expoToken;
    }

    public void setExpoToken(String expoToken) {
        this.expoToken = expoToken;
    }
}


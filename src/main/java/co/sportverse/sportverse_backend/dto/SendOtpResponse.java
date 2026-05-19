package co.sportverse.sportverse_backend.dto;

import co.sportverse.sportverse_backend.enums.OtpChannel;
import co.sportverse.sportverse_backend.enums.SendOtpResultCode;

public class SendOtpResponse {

    private boolean success;

    private SendOtpResultCode code;

    private String message;

    private OtpChannel channel;

    private boolean fallbackAvailable;

    public SendOtpResponse(boolean success, SendOtpResultCode code, String message, OtpChannel channel, boolean fallbackAvailable) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.channel = channel;
        this.fallbackAvailable = fallbackAvailable;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public SendOtpResultCode getCode() {
        return code;
    }

    public void setCode(SendOtpResultCode code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OtpChannel getChannel() {
        return channel;
    }

    public void setChannel(OtpChannel channel) {
        this.channel = channel;
    }

    public boolean isFallbackAvailable() {
        return fallbackAvailable;
    }

    public void setFallbackAvailable(boolean fallbackAvailable) {
        this.fallbackAvailable = fallbackAvailable;
    }
}

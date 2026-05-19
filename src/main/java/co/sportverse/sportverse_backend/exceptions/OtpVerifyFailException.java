package co.sportverse.sportverse_backend.exceptions;

public class OtpVerifyFailException extends RuntimeException {
    public OtpVerifyFailException(String message) {
        super(message);
    }
}

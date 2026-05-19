package co.sportverse.sportverse_backend.exceptions;


public class OtpSendFailException extends RuntimeException {

    public OtpSendFailException(String message) {
        super(message);
    }
}
package co.sportverse.sportverse_backend.constants;

public class Constant {

    /** 10-digit Indian mobile without country code; {@code sendOtp} skips provider send for this number. */
    public static final String OTP_SEND_BYPASS_PHONE_DIGITS = "8218851537";

    /** With {@link #OTP_SEND_BYPASS_PHONE_DIGITS}, {@code verifyOtpCode} skips provider verify when code matches. */
    public static final String OTP_VERIFY_BYPASS_CODE = "000000";
}

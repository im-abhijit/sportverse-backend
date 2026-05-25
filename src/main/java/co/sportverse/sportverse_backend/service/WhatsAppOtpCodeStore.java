package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.exceptions.InvalidOtpException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds WhatsApp OTP codes until verified or TTL expires (single-node; replace with Redis for multi-instance).
 */
@Service
public class WhatsAppOtpCodeStore {

    private static final long TTL_MS = 10 * 60 * 1000L;

    private final ConcurrentHashMap<String, StoredOtp> byPhoneDigits = new ConcurrentHashMap<>();

    public void save(String whatsappIntlDigits, String code) {
        byPhoneDigits.put(whatsappIntlDigits, new StoredOtp(code, System.currentTimeMillis() + TTL_MS));
    }

    /** Validates code and clears entry; {@link InvalidOtpException} on mismatch or expiry. */
    public void validateAndConsume(String whatsappIntlDigits, String submittedCode) {
        String attempt = submittedCode != null ? submittedCode.trim() : "";
        StoredOtp entry = byPhoneDigits.get(whatsappIntlDigits);
        if (entry == null || System.currentTimeMillis() > entry.expiresAtMs()) {
            byPhoneDigits.remove(whatsappIntlDigits);
            throw new InvalidOtpException("Invalid Otp. Please enter correct Otp");
        }
        if (!entry.code().equals(attempt)) {
            throw new InvalidOtpException("Invalid Otp. Please enter correct Otp");
        }
        byPhoneDigits.remove(whatsappIntlDigits);
    }

    private record StoredOtp(String code, long expiresAtMs) {}
}

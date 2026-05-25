package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.exceptions.OtpSendFailException;
import co.sportverse.sportverse_backend.exceptions.OtpVerifyFailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class WhatsappOtpProvider implements OtpProvider {

    private static final Logger logger = LoggerFactory.getLogger(WhatsappOtpProvider.class);

    private final WhatsAppMessagingService whatsAppMessagingService;
    private final WhatsAppOtpCodeStore whatsAppOtpCodeStore;
    private final SecureRandom secureRandom = new SecureRandom();

    WhatsappOtpProvider(WhatsAppMessagingService whatsAppMessagingService, WhatsAppOtpCodeStore whatsAppOtpCodeStore) {
        this.whatsAppMessagingService = whatsAppMessagingService;
        this.whatsAppOtpCodeStore = whatsAppOtpCodeStore;
    }

    @Override
    public void sentOtp(String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) {
            throw new OtpSendFailException("Phone number is required");
        }
        String code = generateSixDigitOtp();
        try {
            whatsAppMessagingService.sendLoginOtpTemplate(mobile, code);
            String digits = whatsAppMessagingService.normalizeRecipient(mobile);
            whatsAppOtpCodeStore.save(digits, code);
            logger.debug("WhatsApp OTP sent for {}", maskDigits(digits));
        } catch (IllegalStateException e) {
            throw new OtpSendFailException(e.getMessage() != null ? e.getMessage() : "WhatsApp Cloud API is not configured");
        } catch (IllegalArgumentException e) {
            throw new OtpSendFailException(e.getMessage() != null ? e.getMessage() : "Failed to send WhatsApp OTP");
        } catch (RuntimeException e) {
            logger.warn("WhatsApp OTP send failed", e);
            throw new OtpSendFailException("Failed to send WhatsApp OTP");
        }
    }

    @Override
    public boolean verifyOtp(String mobile, String otp) {
        try {
            if (mobile == null || mobile.trim().isEmpty()) {
                throw new OtpVerifyFailException("Phone number is required");
            }
            if (otp == null || otp.trim().isEmpty()) {
                throw new OtpVerifyFailException("OTP code is required");
            }
            String digits = whatsAppMessagingService.normalizeRecipient(mobile);
            whatsAppOtpCodeStore.validateAndConsume(digits, otp);
            return true;
        } catch (IllegalArgumentException e) {
            throw new OtpVerifyFailException(e.getMessage() != null ? e.getMessage() : "Failed to verify WhatsApp OTP");
        }
    }

    private String generateSixDigitOtp() {
        int value = 100000 + secureRandom.nextInt(900000);
        return Integer.toString(value);
    }

    private static String maskDigits(String digits) {
        if (digits == null || digits.length() <= 6) {
            return "***";
        }
        return digits.substring(0, 2) + "..." + digits.substring(digits.length() - 2);
    }
}

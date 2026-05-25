package co.sportverse.sportverse_backend.config;

import co.sportverse.sportverse_backend.constants.Constant;
import co.sportverse.sportverse_backend.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RazorpayConfig {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayConfig.class);

    @Value("${razorpay.key_id}")
    private String key_id;

    @Value("${razorpay.key_secret}")
    private String key_secret;

    /** Test credentials for payer matching {@link Constant#OTP_SEND_BYPASS_PHONE_DIGITS}. */
    @Value("${razorpay.test.key_id:}")
    private String testKeyId;

    @Value("${razorpay.test.key_secret:}")
    private String testKeySecret;

    /** Razorpay key pair for APIs where you know payer phone / subject. */
    public record ResolvedKeys(String keyId, String keySecret) {}

    public ResolvedKeys resolveKeysForPayerPhone(String payerPhoneRaw) {
        String norm = JwtService.normalizeIndianPhoneDigits(payerPhoneRaw);
        if (!Constant.OTP_SEND_BYPASS_PHONE_DIGITS.equals(norm)) {
            logger.info("Razorpay: using test keys for dummy payer : test keys : {} {} ", key_id, key_secret);
            return new ResolvedKeys(key_id.trim(), key_secret.trim());
        }
        if (isBlank(testKeyId) || isBlank(testKeySecret)) {
            throw new IllegalStateException(
                    "Dummy user Razorpay is not configured: set razorpay.test.key_id and razorpay.test.key_secret");
        }
        logger.info("Razorpay: using test keys for dummy payer : test keys : {} {} ", testKeyId, testKeySecret);
        return new ResolvedKeys(testKeyId.trim(), testKeySecret.trim());
    }

    public String getKey_id() {
        return key_id;
    }

    public void setKey_id(String key_id) {
        this.key_id = key_id;
    }

    public String getKey_secret() {
        return key_secret;
    }

    public void setKey_secret(String key_secret) {
        this.key_secret = key_secret;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

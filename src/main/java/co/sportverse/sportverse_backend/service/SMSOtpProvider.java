package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.config.TwilioProperties;
import co.sportverse.sportverse_backend.dto.VerifyOtpResponse;
import co.sportverse.sportverse_backend.exceptions.InvalidOtpException;
import co.sportverse.sportverse_backend.exceptions.OtpSendFailException;
import co.sportverse.sportverse_backend.exceptions.OtpVerifyFailException;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import org.springframework.stereotype.Service;

@Service
public class SMSOtpProvider implements OtpProvider {

    private final TwilioProperties properties;

    SMSOtpProvider(TwilioProperties properties) {
        this.properties = properties;
    }

    @Override
    public void sentOtp(String mobile) {
        try {
            mobile = toTwilioE164India(mobile);
            Verification verification = Verification.creator(
                    properties.getVerificationServiceSid(),
                    mobile,
                    "sms"
            ).create();
        }
        catch (Exception e) {
            throw new OtpSendFailException("Failed to send SMS OTP");
        }
    }

    @Override
    public boolean verifyOtp(String mobile, String otp) {
        VerificationCheck verificationCheck = null;
        try {
            mobile = toTwilioE164India(mobile);
            verificationCheck = VerificationCheck.creator(properties.getVerificationServiceSid())
                    .setTo(mobile)
                    .setCode(otp)
                    .create();
        }
        catch (Exception e) {
            throw new OtpVerifyFailException("Failed to verify SMS OTP");
        }
        if (!verificationCheck.getValid() || !"approved".equals(verificationCheck.getStatus())) {
            throw new InvalidOtpException("Invalid Otp. Please enter correct Otp");
        }
        return true;
    }

    private String toTwilioE164India(String raw) {
        String n = raw;
        if (n.startsWith("+")) {
            n = n.substring(1);
        }
        if (n.startsWith("0")) {
            n = n.substring(1);
        }
        return "+91" + n;
    }
}

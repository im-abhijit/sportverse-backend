package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.dto.GenerateOtpRequest;
import co.sportverse.sportverse_backend.dto.GenerateOtpResponse;
import co.sportverse.sportverse_backend.dto.PartnerLoginRequest;
import co.sportverse.sportverse_backend.dto.PartnerLoginResponse;
import co.sportverse.sportverse_backend.dto.VerifyOtpRequest;
import co.sportverse.sportverse_backend.dto.VerifyOtpResponse;
import co.sportverse.sportverse_backend.entity.User;
import co.sportverse.sportverse_backend.repository.PartnerRepository;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserService userService;

    @Autowired
    private PartnerRepository partnerRepository;

    public GenerateOtpResponse generateOtp(GenerateOtpRequest request) {
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        if (request.getPhoneNumber().equals("8937828771")) {
            logger.info("Auth generate-otp: test phone, returning sample response");
            return new GenerateOtpResponse(true, "OTP sent successfully via SMS", "sample_sid", "pending");
        }

        String channel = request.getChannel();
        if (channel == null || channel.trim().isEmpty()) {
            channel = "sms";
        }
        if (!"sms".equals(channel) && !"whatsapp".equals(channel)) {
            throw new IllegalArgumentException("Invalid channel. Supported channels: sms, whatsapp");
        }

        String twilioPhoneNumber = toTwilioE164India(request.getPhoneNumber().trim());

        Verification verification = otpService.sendOtp(twilioPhoneNumber, channel);
        logger.info("OTP sent. phoneNumber: {}, channel: {}, sid: {}", twilioPhoneNumber, channel, verification.getSid());

        return new GenerateOtpResponse(
                true,
                "OTP sent successfully via " + channel.toUpperCase(),
                verification.getSid(),
                verification.getStatus()
        );
    }

    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        if (request.getPhoneNumber().equals("8937828771")) {
            logger.info("Auth verify-otp: test phone, returning sample response");
            return new VerifyOtpResponse(
                    true,
                    "OTP verified successfully - User logged in",
                    "success",
                    true,
                    "+918937828771",
                    "Abhijit"
            );
        }

        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("OTP code is required");
        }

        String originalPhoneNumber = request.getPhoneNumber().trim();
        String twilioPhoneNumber = toTwilioE164India(originalPhoneNumber);

        VerificationCheck verificationCheck = otpService.verifyOtp(twilioPhoneNumber, request.getCode().trim());

        boolean isValid = verificationCheck.getValid();
        String status = verificationCheck.getStatus();

        if (!isValid) {
            logger.warn("Invalid OTP. phoneNumber: {}, status: {}", twilioPhoneNumber, status);
            return new VerifyOtpResponse(false, "Invalid OTP code", status, false);
        }

        User existingUser = userService.getUserByMobileNumber(originalPhoneNumber);
        User user;
        if (existingUser != null) {
            user = existingUser;
            logger.info("OTP verified for existing user. userId: {}, phone: {}", user.getId(), originalPhoneNumber);
        } else {
            String defaultName = "User_" + originalPhoneNumber.substring(Math.max(0, originalPhoneNumber.length() - 4));
            user = userService.createUser(defaultName, originalPhoneNumber, false);
            logger.info("OTP verified, new user. userId: {}, phone: {}", user.getId(), originalPhoneNumber);
        }

        String message = existingUser != null
                ? "OTP verified successfully - User logged in"
                : "OTP verified successfully - New user created";

        return new VerifyOtpResponse(
                true,
                message,
                status,
                true,
                user.getPhone(),
                user.getName()
        );
    }

    public PartnerLoginResponse partnerLogin(PartnerLoginRequest request) {
        if (request.getPartnerId() == null || request.getPartnerId().trim().isEmpty()) {
            throw new IllegalArgumentException("Partner ID is required");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        Document partner = partnerRepository.findByPartnerId(request.getPartnerId().trim());

        if (partner == null) {
            logger.warn("Partner login: partner not found. partnerId: {}", request.getPartnerId());
            return new PartnerLoginResponse(false, "Invalid partner ID or password");
        }

        String storedPassword = partner.getString("password");
        if (storedPassword == null || !storedPassword.equals(request.getPassword().trim())) {
            logger.warn("Partner login: invalid password. partnerId: {}", request.getPartnerId());
            return new PartnerLoginResponse(false, "Invalid partner ID or password");
        }

        logger.info("Partner login successful. partnerId: {}", request.getPartnerId());
        return new PartnerLoginResponse(true, "Partner login successful", request.getPartnerId().trim());
    }

    private static String toTwilioE164India(String raw) {
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

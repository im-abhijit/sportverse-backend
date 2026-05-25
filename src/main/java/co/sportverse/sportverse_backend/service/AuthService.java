package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.constants.Constant;
import co.sportverse.sportverse_backend.dto.*;
import co.sportverse.sportverse_backend.entity.User;
import co.sportverse.sportverse_backend.enums.OtpChannel;
import co.sportverse.sportverse_backend.enums.SendOtpResultCode;
import co.sportverse.sportverse_backend.repository.PartnerRepository;
import co.sportverse.sportverse_backend.service.factory.OtpProviderFactory;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserService userService;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private OtpProviderFactory otpProviderFactory;

    @Autowired
    private JwtService jwtService;

    public GenerateOtpResponse generateOtp(SendOtpRequest request) {
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
            VerifyOtpResponse resp = new VerifyOtpResponse(
                    true,
                    "OTP verified successfully - User logged in",
                    "success",
                    true,
                    "+918937828771",
                    "Abhijit"
            );
            resp.setJwtToken(jwtService.createUserAccessToken(JwtService.normalizeIndianPhoneDigits("8937828771")));
            return resp;
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
            user = userService.createUser(defaultName, originalPhoneNumber, false,null,null,null);
            logger.info("OTP verified, new user. userId: {}, phone: {}", user.getId(), originalPhoneNumber);
        }

        String message = existingUser != null
                ? "OTP verified successfully - User logged in"
                : "OTP verified successfully - New user created";

        VerifyOtpResponse resp = new VerifyOtpResponse(
                true,
                message,
                status,
                true,
                user.getPhone(),
                user.getName()
        );
        resp.setJwtToken(jwtService.createUserAccessToken(JwtService.normalizeIndianPhoneDigits(user.getPhone())));
        return resp;
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

    public SendOtpResponse sendOtp(SendOtpRequest request) {
        SendOtpResponse bypass = buildSendOtpBypassResponseIfApplicable(request);
        if (bypass != null) {
            return bypass;
        }
        OtpProvider otpProvider = otpProviderFactory.getOtpProvider(OtpChannel.getChannel(request.getChannel()));
        otpProvider.sentOtp(request.getPhoneNumber());
        SendOtpResponse response = new SendOtpResponse(true, SendOtpResultCode.OTP_SENT_SUCCESS,"",OtpChannel.getChannel(request.getChannel()),false);
        return response;
    }

    public VerifyOtpResponse verifyOtpCode(VerifyOtpRequest request) {
        if (!shouldSkipOtpProviderVerify(request)) {
            OtpProvider otpProvider = otpProviderFactory.getOtpProvider(OtpChannel.getChannel(request.getChannel()));
            otpProvider.verifyOtp(request.getPhoneNumber(), request.getCode());
        } else {
            logger.info("Auth verify-otp: test phone with bypass code, skipping provider verify");
        }
        User user = userService.getUserByMobileNumber(request.getPhoneNumber());
        boolean isNewUser = false;
        String message;
        if(Objects.nonNull(user)) {
            message = "Welcome back!";
        }
        else{
            isNewUser = true;
            user = userService.createUser(null, request.getPhoneNumber(), false, request.getFirstName(), request.getLastName(), request.getEmail());
            message = "Account created successfully";
        }
        String norm = JwtService.normalizeIndianPhoneDigits(user.getPhone());
        String jwtToken = jwtService.createUserAccessToken(norm);
        String username = StringUtils.isNotBlank(user.getFirstName()) ? user.getFirstName() : user.getName();
        if(StringUtils.isBlank(username)) {
            username=user.getPhone();
        }
        return new VerifyOtpResponse(true,isNewUser,message,jwtToken,user.getPhone(),username);
    }

    private static boolean shouldSkipOtpProviderVerify(VerifyOtpRequest request) {
        return Constant.OTP_SEND_BYPASS_PHONE_DIGITS.equals(JwtService.normalizeIndianPhoneDigits(request.getPhoneNumber()))
                && Constant.OTP_VERIFY_BYPASS_CODE.equals(
                        request.getCode() != null ? request.getCode().trim() : "");
    }

    private SendOtpResponse buildSendOtpBypassResponseIfApplicable(SendOtpRequest request) {
        if (!Constant.OTP_SEND_BYPASS_PHONE_DIGITS.equals(JwtService.normalizeIndianPhoneDigits(request.getPhoneNumber()))) {
            return null;
        }
        logger.info("Auth send-otp: test phone, skipping OTP send");
        return new SendOtpResponse(true, SendOtpResultCode.OTP_SENT_SUCCESS, "", OtpChannel.getChannel(request.getChannel()), false);
    }
}

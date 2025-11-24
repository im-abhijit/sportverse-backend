package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.GenerateOtpRequest;
import co.sportverse.sportverse_backend.dto.GenerateOtpResponse;
import co.sportverse.sportverse_backend.dto.PartnerLoginRequest;
import co.sportverse.sportverse_backend.dto.PartnerLoginResponse;
import co.sportverse.sportverse_backend.dto.VerifyOtpRequest;
import co.sportverse.sportverse_backend.dto.VerifyOtpResponse;
import co.sportverse.sportverse_backend.entity.User;
import co.sportverse.sportverse_backend.repository.PartnerRepository;
import co.sportverse.sportverse_backend.service.OtpService;
import co.sportverse.sportverse_backend.service.UserService;
import org.bson.Document;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
        "https://sportverse.co.in",
        "http://localhost:8083"
})
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserService userService;

    @Autowired
    private PartnerRepository partnerRepository;

    @PostMapping("/generate-otp")
    public ResponseEntity<GenerateOtpResponse> generateOtp(@RequestBody GenerateOtpRequest request) {
        logger.info("POST /api/auth/generate-otp - Generating OTP. phoneNumber: {}, channel: {}", 
                request.getPhoneNumber(), request.getChannel());
        try {
            // Validate phone number
            if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
                logger.warn("POST /api/auth/generate-otp - Validation failed: Phone number is required");
                return ResponseEntity.badRequest()
                    .body(new GenerateOtpResponse(false, "Phone number is required"));
            }

            if(request.getPhoneNumber().equals("8937828771")){
                logger.info("POST /api/auth/generate-otp - Using test phone number, returning sample response");
                return ResponseEntity.ok(new GenerateOtpResponse(
                        true,
                        "OTP sent successfully via SMS",
                        "sample_sid",
                        "pending"
                ));
            }

            // Validate channel
            String channel = request.getChannel();
            if (channel == null || channel.trim().isEmpty()) {
                channel = "sms"; // Default to SMS
            }

            if (!channel.equals("sms") && !channel.equals("whatsapp")) {
                logger.warn("POST /api/auth/generate-otp - Validation failed: Invalid channel. Received: {}", channel);
                return ResponseEntity.badRequest()
                    .body(new GenerateOtpResponse(false, "Invalid channel. Supported channels: sms, whatsapp"));
            }

            // Format phone number for Twilio: always append +91
            String twilioPhoneNumber = request.getPhoneNumber().trim();
            // Remove any existing + prefix
            if (twilioPhoneNumber.startsWith("+")) {
                twilioPhoneNumber = twilioPhoneNumber.substring(1);
            }
            // Remove leading 0 if present
            if (twilioPhoneNumber.startsWith("0")) {
                twilioPhoneNumber = twilioPhoneNumber.substring(1);
            }
            // Always append +91 for Twilio
            twilioPhoneNumber = "+91" + twilioPhoneNumber;

            // Send OTP using Twilio Verification API
            Verification verification = otpService.sendOtp(twilioPhoneNumber, channel);
            logger.info("POST /api/auth/generate-otp - Successfully sent OTP. phoneNumber: {}, channel: {}, sid: {}", 
                    twilioPhoneNumber, channel, verification.getSid());
            
            return ResponseEntity.ok(new GenerateOtpResponse(
                true, 
                "OTP sent successfully via " + channel.toUpperCase(), 
                verification.getSid(),
                verification.getStatus()
            ));
            
        } catch (Exception e) {
            logger.error("POST /api/auth/generate-otp - Error generating OTP. phoneNumber: {}", 
                    request.getPhoneNumber(), e);
            return ResponseEntity.internalServerError()
                .body(new GenerateOtpResponse(false, "An error occurred while sending OTP: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        logger.info("POST /api/auth/verify-otp - Verifying OTP. phoneNumber: {}", request.getPhoneNumber());
        try {
            // Validate phone number
            if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
                logger.warn("POST /api/auth/verify-otp - Validation failed: Phone number is required");
                return ResponseEntity.badRequest()
                    .body(new VerifyOtpResponse(false, "Phone number is required"));
            }

            if(request.getPhoneNumber().equals("8937828771")){
                logger.info("POST /api/auth/verify-otp - Using test phone number, returning sample response");
                return ResponseEntity.ok(new VerifyOtpResponse(
                        true,
                        "OTP verified successfully - User logged in",
                        "success",
                        true,
                        "+918937828771",
                        "Abhijit"
                ));
            }

            // Validate OTP code
            if (request.getCode() == null || request.getCode().trim().isEmpty()) {
                logger.warn("POST /api/auth/verify-otp - Validation failed: OTP code is required");
                return ResponseEntity.badRequest()
                    .body(new VerifyOtpResponse(false, "OTP code is required"));
            }

            // Store original phone number from request (for saving user details)
            String originalPhoneNumber = request.getPhoneNumber().trim();
            
            // Format phone number for Twilio: always append +91
            String twilioPhoneNumber = originalPhoneNumber;
            // Remove any existing + prefix
            if (twilioPhoneNumber.startsWith("+")) {
                twilioPhoneNumber = twilioPhoneNumber.substring(1);
            }
            // Remove leading 0 if present
            if (twilioPhoneNumber.startsWith("0")) {
                twilioPhoneNumber = twilioPhoneNumber.substring(1);
            }
            // Always append +91 for Twilio
            twilioPhoneNumber = "+91" + twilioPhoneNumber;

            // Verify OTP using Twilio Verification API (use +91 version)
            VerificationCheck verificationCheck = otpService.verifyOtp(twilioPhoneNumber, request.getCode().trim());
            
            boolean isValid = verificationCheck.getValid();
            String status = verificationCheck.getStatus();
            
            if (isValid) {
                // Check if user exists, if not create a new user (use original phone number without +91)
                User existingUser = userService.getUserByMobileNumber(originalPhoneNumber);
                User user;
                
                if (existingUser != null) {
                    user = existingUser;
                    logger.info("POST /api/auth/verify-otp - OTP verified for existing user. userId: {}, phoneNumber: {}", 
                            user.getId(), originalPhoneNumber);
                } else {
                    // Create new user with default name (can be updated later)
                    String defaultName = "User_" + originalPhoneNumber.substring(originalPhoneNumber.length() - 4);
                    user = userService.createUser(defaultName, originalPhoneNumber, false);
                    logger.info("POST /api/auth/verify-otp - OTP verified, new user created. userId: {}, phoneNumber: {}", 
                            user.getId(), originalPhoneNumber);
                }
                
                String message = existingUser != null ? 
                    "OTP verified successfully - User logged in" : 
                    "OTP verified successfully - New user created";
                
                return ResponseEntity.ok(new VerifyOtpResponse(
                    true,
                    message,
                    status,
                    true,
                    user.getPhone(),
                    user.getName()
                ));
            } else {
                logger.warn("POST /api/auth/verify-otp - Invalid OTP code. phoneNumber: {}, status: {}", 
                        twilioPhoneNumber, status);
                return ResponseEntity.ok(new VerifyOtpResponse(
                    false, 
                    "Invalid OTP code", 
                    status,
                    false
                ));
            }
            
        } catch (Exception e) {
            logger.error("POST /api/auth/verify-otp - Error verifying OTP. phoneNumber: {}", 
                    request.getPhoneNumber(), e);
            return ResponseEntity.internalServerError()
                .body(new VerifyOtpResponse(false, "An error occurred while verifying OTP: " + e.getMessage()));
        }
    }

    @PostMapping("/partner/login")
    public ResponseEntity<PartnerLoginResponse> partnerLogin(@RequestBody PartnerLoginRequest request) {
        logger.info("POST /api/auth/partner/login - Partner login attempt. partnerId: {}", request.getPartnerId());
        try {
            // Validate partnerId
            if (request.getPartnerId() == null || request.getPartnerId().trim().isEmpty()) {
                logger.warn("POST /api/auth/partner/login - Validation failed: Partner ID is required");
                return ResponseEntity.badRequest()
                    .body(new PartnerLoginResponse(false, "Partner ID is required"));
            }

            // Validate password
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                logger.warn("POST /api/auth/partner/login - Validation failed: Password is required");
                return ResponseEntity.badRequest()
                    .body(new PartnerLoginResponse(false, "Password is required"));
            }

            // Fetch partner from database
            Document partner = partnerRepository.findByPartnerId(request.getPartnerId().trim());
            
            if (partner == null) {
                logger.warn("POST /api/auth/partner/login - Partner not found. partnerId: {}", request.getPartnerId());
                return ResponseEntity.ok(new PartnerLoginResponse(false, "Invalid partner ID or password"));
            }

            // Verify password
            String storedPassword = partner.getString("password");
            if (storedPassword == null || !storedPassword.equals(request.getPassword().trim())) {
                logger.warn("POST /api/auth/partner/login - Invalid password. partnerId: {}", request.getPartnerId());
                return ResponseEntity.ok(new PartnerLoginResponse(false, "Invalid partner ID or password"));
            }

            // Login successful
            logger.info("POST /api/auth/partner/login - Partner login successful. partnerId: {}", request.getPartnerId());
            return ResponseEntity.ok(new PartnerLoginResponse(
                true,
                "Partner login successful",
                request.getPartnerId().trim()
            ));

        } catch (Exception e) {
            logger.error("POST /api/auth/partner/login - Error during partner login. partnerId: {}", 
                    request.getPartnerId(), e);
            return ResponseEntity.internalServerError()
                .body(new PartnerLoginResponse(false, "An error occurred while logging in: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.debug("GET /api/auth/health - Health check");
        return ResponseEntity.ok("Auth Service is running");
    }
}

package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.GenerateOtpRequest;
import co.sportverse.sportverse_backend.dto.GenerateOtpResponse;
import co.sportverse.sportverse_backend.dto.PartnerLoginRequest;
import co.sportverse.sportverse_backend.dto.PartnerLoginResponse;
import co.sportverse.sportverse_backend.dto.VerifyOtpRequest;
import co.sportverse.sportverse_backend.dto.VerifyOtpResponse;
import co.sportverse.sportverse_backend.service.AuthService;
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
    private AuthService authService;

    @PostMapping("/generate-otp")
    public ResponseEntity<GenerateOtpResponse> generateOtp(@RequestBody GenerateOtpRequest request) {
        logger.info("POST /api/auth/generate-otp - Generating OTP. phoneNumber: {}, channel: {}",
                request.getPhoneNumber(), request.getChannel());
        GenerateOtpResponse response = authService.generateOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        logger.info("POST /api/auth/verify-otp - Verifying OTP. phoneNumber: {}", request.getPhoneNumber());
        VerifyOtpResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/partner/login")
    public ResponseEntity<PartnerLoginResponse> partnerLogin(@RequestBody PartnerLoginRequest request) {
        logger.info("POST /api/auth/partner/login - Partner login attempt. partnerId: {}", request.getPartnerId());
        PartnerLoginResponse response = authService.partnerLogin(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.debug("GET /api/auth/health - Health check");
        return ResponseEntity.ok("Auth Service is running");
    }
}

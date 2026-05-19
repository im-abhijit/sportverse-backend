package co.sportverse.sportverse_backend.controller.user;

import co.sportverse.sportverse_backend.dto.SendOtpRequest;
import co.sportverse.sportverse_backend.dto.SendOtpResponse;
import co.sportverse.sportverse_backend.dto.VerifyOtpRequest;
import co.sportverse.sportverse_backend.dto.VerifyOtpResponse;
import co.sportverse.sportverse_backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/user/auth")
public class UserAuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/send-otp")
    public ResponseEntity<SendOtpResponse> sendOtp(@RequestBody SendOtpRequest request) {
        return ResponseEntity.ok(authService.sendOtp(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> sendOtp(@RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtpCode(request));
    }

}

package co.sportverse.sportverse_backend.advice;

import co.sportverse.sportverse_backend.controller.AuthController;
import co.sportverse.sportverse_backend.dto.GenerateOtpResponse;
import co.sportverse.sportverse_backend.dto.PartnerLoginResponse;
import co.sportverse.sportverse_backend.dto.VerifyOtpResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(AuthControllerAdvice.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        logger.warn("Auth API - invalid request: {}", e.getMessage());
        return ResponseEntity.badRequest().body(buildClientError(request.getRequestURI(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception e, HttpServletRequest request) {
        logger.error("Auth API - unexpected error", e);
        String msg = e.getMessage() != null ? e.getMessage() : "Unexpected error";
        String bodyMsg = buildServerErrorMessage(request.getRequestURI(), msg);
        return ResponseEntity.internalServerError().body(buildServerErrorBody(request.getRequestURI(), bodyMsg));
    }

    private static Object buildClientError(String uri, String message) {
        if (uri != null && uri.contains("/generate-otp")) {
            return new GenerateOtpResponse(false, message);
        }
        if (uri != null && uri.contains("/verify-otp")) {
            return new VerifyOtpResponse(false, message);
        }
        if (uri != null && uri.contains("/partner/login")) {
            return new PartnerLoginResponse(false, message);
        }
        return new GenerateOtpResponse(false, message);
    }

    private static Object buildServerErrorBody(String uri, String message) {
        if (uri != null && uri.contains("/generate-otp")) {
            return new GenerateOtpResponse(false, message);
        }
        if (uri != null && uri.contains("/verify-otp")) {
            return new VerifyOtpResponse(false, message);
        }
        if (uri != null && uri.contains("/partner/login")) {
            return new PartnerLoginResponse(false, message);
        }
        if (uri != null && uri.contains("/health")) {
            return "Auth Service error";
        }
        return new GenerateOtpResponse(false, message);
    }

    private static String buildServerErrorMessage(String uri, String raw) {
        if (uri != null && uri.contains("/generate-otp")) {
            return "An error occurred while sending OTP: " + raw;
        }
        if (uri != null && uri.contains("/verify-otp")) {
            return "An error occurred while verifying OTP: " + raw;
        }
        if (uri != null && uri.contains("/partner/login")) {
            return "An error occurred while logging in: " + raw;
        }
        return raw;
    }
}

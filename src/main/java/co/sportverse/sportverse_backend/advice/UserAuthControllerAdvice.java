package co.sportverse.sportverse_backend.advice;


import co.sportverse.sportverse_backend.controller.user.UserAuthController;
import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.exceptions.InvalidOtpException;
import co.sportverse.sportverse_backend.exceptions.OtpSendFailException;
import co.sportverse.sportverse_backend.exceptions.OtpVerifyFailException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = UserAuthController.class)
public class UserAuthControllerAdvice {

    @ExceptionHandler(OtpSendFailException.class)
    public ResponseEntity<ApiResponse> handleOtpSendFailed(OtpSendFailException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Failed to send otp. Please try again.";
        return ResponseEntity.badRequest().body(new ApiResponse(false, message));
    }

    @ExceptionHandler(OtpVerifyFailException.class)
    public ResponseEntity<ApiResponse> handleOtpVerifyFailed(OtpVerifyFailException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Failed to verify otp. Please try again.";
        return ResponseEntity.badRequest().body(new ApiResponse(false, message));
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ApiResponse> handleInvalidOtp(InvalidOtpException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Incorrect Otp. Please enter a valid OTP code";
        return ResponseEntity.badRequest().body(new ApiResponse(false, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "System error";
        return ResponseEntity.badRequest().body(new ApiResponse(false, message));
    }
}

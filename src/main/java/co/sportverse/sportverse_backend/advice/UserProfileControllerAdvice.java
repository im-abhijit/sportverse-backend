package co.sportverse.sportverse_backend.advice;

import co.sportverse.sportverse_backend.controller.user.UserProfileController;
import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.exceptions.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = UserProfileController.class)
public class UserProfileControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileControllerAdvice.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgument(IllegalArgumentException e) {
        logger.warn("User profile API - invalid request: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse> handleUserNotFound(UserNotFoundException e) {
        logger.warn("User profile API - user not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleUnexpected(Exception e) {
        logger.error("User profile API - unexpected error", e);
        String message = e.getMessage() != null ? e.getMessage() : "Unexpected error";
        return ResponseEntity.internalServerError().body(new ApiResponse(false, message));
    }
}

package co.sportverse.sportverse_backend.advice;

import co.sportverse.sportverse_backend.controller.user.UserVenueController;
import co.sportverse.sportverse_backend.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = UserVenueController.class)
public class UserVenueControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(UserVenueControllerAdvice.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgument(IllegalArgumentException e) {
        logger.warn("User venue API - invalid request: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleUnexpected(Exception e) {
        logger.error("User venue API - unexpected error", e);
        String message = e.getMessage() != null ? e.getMessage() : "Unexpected error";
        return ResponseEntity.internalServerError().body(new ApiResponse(false, message));
    }
}

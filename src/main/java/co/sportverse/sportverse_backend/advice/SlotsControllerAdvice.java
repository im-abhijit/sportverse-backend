package co.sportverse.sportverse_backend.advice;

import co.sportverse.sportverse_backend.controller.SlotsController;
import co.sportverse.sportverse_backend.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = SlotsController.class)
public class SlotsControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(SlotsControllerAdvice.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgument(IllegalArgumentException e) {
        logger.warn("Slots API - invalid request: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse> handleIllegalState(IllegalStateException e) {
        logger.warn("Slots API - invalid state: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleUnexpected(Exception e) {
        logger.error("Slots API - unexpected error", e);
        String message = e.getMessage() != null ? e.getMessage() : "Unexpected error";
        return ResponseEntity.internalServerError().body(new ApiResponse(false, message));
    }
}

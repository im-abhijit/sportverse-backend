package co.sportverse.sportverse_backend.advice;

import co.sportverse.sportverse_backend.controller.AuthImageKitController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = AuthImageKitController.class)
public class AuthImageKitControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(AuthImageKitControllerAdvice.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        logger.warn("Authenticated ImageKit API - bad request: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getMessage() != null ? e.getMessage() : "Bad request");
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        logger.error("Authenticated ImageKit API - unexpected error", e);
        Map<String, String> error = new HashMap<>();
        error.put("error", "Error generating upload token: " + (e.getMessage() != null ? e.getMessage() : "Unexpected error"));
        return ResponseEntity.internalServerError().body(error);
    }
}

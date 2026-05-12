package co.sportverse.sportverse_backend.advice;

import co.sportverse.sportverse_backend.controller.ImageKitController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = ImageKitController.class)
public class ImageKitControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ImageKitControllerAdvice.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        logger.error("ImageKit API - unexpected error", e);
        Map<String, String> error = new HashMap<>();
        error.put("error", "Error generating upload token: " + (e.getMessage() != null ? e.getMessage() : "Unexpected error"));
        return ResponseEntity.internalServerError().body(error);
    }
}

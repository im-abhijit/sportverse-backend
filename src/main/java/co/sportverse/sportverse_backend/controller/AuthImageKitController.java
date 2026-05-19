package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.AuthenticatedUser;
import co.sportverse.sportverse_backend.service.ImageKitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Authenticated variant of {@link ImageKitController}: same upload-token payload, requires a valid JWT
 * ({@link AuthenticatedUser}). Base path is {@code /api/secure/imagekit} so this does not fall under
 * {@code /api/auth/**} (which is permit-all in security config).
 */
@RestController
@RequestMapping("/api/secure/imagekit")
public class AuthImageKitController {

    private static final Logger logger = LoggerFactory.getLogger(AuthImageKitController.class);

    @Autowired
    private ImageKitService imageKitService;

    @PostMapping("/upload-token")
    public ResponseEntity<Map<String, String>> getImageKitAuth(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new IllegalArgumentException("Authentication is required");
        }
        logger.info("POST /api/secure/imagekit/upload-token - Generating ImageKit upload token (authenticated)");
        Map<String, String> response = imageKitService.createUploadAuthenticationParameters();
        logger.info("POST /api/secure/imagekit/upload-token - Successfully generated upload token");
        return ResponseEntity.ok(response);
    }
}

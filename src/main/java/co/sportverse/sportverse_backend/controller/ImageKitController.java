package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.config.ImageKitConfig;
import io.imagekit.sdk.ImageKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/imagekit")
@CrossOrigin(origins = {
        "https://sportverse.co.in",
        "http://localhost:8083"
})
public class ImageKitController {

    private static final Logger logger = LoggerFactory.getLogger(ImageKitController.class);

    @Autowired
    private ImageKitConfig imageKitConfig;

    @PostMapping("/upload-token")
    public ResponseEntity<Map<String, String>> getImageKitAuth() {
        logger.info("POST /api/imagekit/upload-token - Generating ImageKit upload token");
        try {
            Map<String, String> response = ImageKit.getInstance().getAuthenticationParameters(null, (System.currentTimeMillis()/1000)+300l);
            response.put("publicKey", imageKitConfig.getPublicKey());
            response.put("urlEndpoint", imageKitConfig.getUrlEndpoint());
            logger.info("POST /api/imagekit/upload-token - Successfully generated upload token");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("POST /api/imagekit/upload-token - Error generating upload token", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error generating upload token: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

}


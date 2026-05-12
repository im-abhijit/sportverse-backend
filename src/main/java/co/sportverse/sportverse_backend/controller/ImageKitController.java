package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.service.ImageKitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private ImageKitService imageKitService;

    @PostMapping("/upload-token")
    public ResponseEntity<Map<String, String>> getImageKitAuth() {
        logger.info("POST /api/imagekit/upload-token - Generating ImageKit upload token");
        Map<String, String> response = imageKitService.createUploadAuthenticationParameters();
        logger.info("POST /api/imagekit/upload-token - Successfully generated upload token");
        return ResponseEntity.ok(response);
    }
}

package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.ExpoTokenRequest;
import co.sportverse.sportverse_backend.repository.PartnerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/partners")
@CrossOrigin(origins = {
        "https://sportverse.co.in",
        "http://localhost:8083"
})
public class PartnerController {

    private static final Logger logger = LoggerFactory.getLogger(PartnerController.class);

    @Autowired
    private PartnerRepository partnerRepository;

    @PostMapping("/{partnerId}/expo-token")
    public ResponseEntity<ApiResponse> updateExpoToken(
            @PathVariable String partnerId,
            @RequestBody ExpoTokenRequest request) {
        logger.info("POST /api/partners/{}/expo-token - Updating Expo token for partner", partnerId);
        try {
            if (partnerId == null || partnerId.trim().isEmpty()) {
                logger.warn("POST /api/partners/{}/expo-token - Validation failed: partnerId is required", partnerId);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "partnerId is required"));
            }
            if (request == null || request.getExpoToken() == null || request.getExpoToken().trim().isEmpty()) {
                logger.warn("POST /api/partners/{}/expo-token - Validation failed: expoToken is required", partnerId);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "expoToken is required"));
            }

            boolean updated = partnerRepository.addExpoToken(partnerId.trim(), request.getExpoToken().trim());
            if (!updated) {
                logger.warn("POST /api/partners/{}/expo-token - Partner not found", partnerId);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Partner not found"));
            }

            logger.info("POST /api/partners/{}/expo-token - Successfully added Expo token", partnerId);
            return ResponseEntity.ok(new ApiResponse(true, "Expo token added successfully"));
        } catch (Exception e) {
            logger.error("POST /api/partners/{}/expo-token - Error updating Expo token", partnerId, e);
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error updating Expo token: " + e.getMessage()));
        }
    }
}


package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.ExpoTokenRequest;
import co.sportverse.sportverse_backend.dto.PushSubscriptionRequest;
import co.sportverse.sportverse_backend.entity.PushSubscription;
import co.sportverse.sportverse_backend.repository.PartnerRepository;
import co.sportverse.sportverse_backend.repository.PushSubscriptionRepository;
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

    @Autowired
    private PushSubscriptionRepository pushSubscriptionRepository;

    @PostMapping("/{partnerId}/push-subscription")
    public ResponseEntity<ApiResponse> savePushSubscription(
            @PathVariable String partnerId,
            @RequestBody PushSubscriptionRequest request) {
        logger.info("POST /api/partners/{}/push-subscription - Saving push subscription for partner", partnerId);
        try {
            if (partnerId == null || partnerId.trim().isEmpty()) {
                logger.warn("POST /api/partners/{}/push-subscription - Validation failed: partnerId is required", partnerId);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "partnerId is required"));
            }
            if (request == null) {
                logger.warn("POST /api/partners/{}/push-subscription - Validation failed: request body is required", partnerId);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Request body is required"));
            }
            if (request.getEndpoint() == null || request.getEndpoint().trim().isEmpty()) {
                logger.warn("POST /api/partners/{}/push-subscription - Validation failed: endpoint is required", partnerId);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "endpoint is required"));
            }
            if (request.getKeys() == null) {
                logger.warn("POST /api/partners/{}/push-subscription - Validation failed: keys are required", partnerId);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "keys are required"));
            }
            if (request.getKeys().getP256dh() == null || request.getKeys().getP256dh().trim().isEmpty()) {
                logger.warn("POST /api/partners/{}/push-subscription - Validation failed: keys.p256dh is required", partnerId);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "keys.p256dh is required"));
            }
            if (request.getKeys().getAuth() == null || request.getKeys().getAuth().trim().isEmpty()) {
                logger.warn("POST /api/partners/{}/push-subscription - Validation failed: keys.auth is required", partnerId);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "keys.auth is required"));
            }

            // Verify partner exists
            org.bson.Document partner = partnerRepository.findByPartnerId(partnerId.trim());
            if (partner == null) {
                logger.warn("POST /api/partners/{}/push-subscription - Partner not found", partnerId);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Partner not found"));
            }

            // Create or update push subscription
            PushSubscription subscription = new PushSubscription(
                    partnerId.trim(),
                    request.getEndpoint().trim(),
                    request.getKeys().getP256dh().trim(),
                    request.getKeys().getAuth().trim()
            );

            PushSubscription saved = pushSubscriptionRepository.updateByPartnerId(partnerId.trim(), subscription);
            logger.info("POST /api/partners/{}/push-subscription - Successfully saved push subscription. subscriptionId: {}", 
                    partnerId, saved.getId());
            return ResponseEntity.ok(new ApiResponse(true, "Push subscription saved successfully", saved.getId()));
        } catch (Exception e) {
            logger.error("POST /api/partners/{}/push-subscription - Error saving push subscription", partnerId, e);
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error saving push subscription: " + e.getMessage()));
        }
    }

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

            boolean updated = partnerRepository.updateExpoToken(partnerId.trim(), request.getExpoToken().trim());
            if (!updated) {
                logger.warn("POST /api/partners/{}/expo-token - Partner not found", partnerId);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Partner not found"));
            }

            logger.info("POST /api/partners/{}/expo-token - Successfully updated Expo token", partnerId);
            return ResponseEntity.ok(new ApiResponse(true, "Expo token updated successfully"));
        } catch (Exception e) {
            logger.error("POST /api/partners/{}/expo-token - Error updating Expo token", partnerId, e);
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error updating Expo token: " + e.getMessage()));
        }
    }
}


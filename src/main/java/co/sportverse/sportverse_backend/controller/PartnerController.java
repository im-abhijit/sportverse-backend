package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.ExpoTokenRequest;
import co.sportverse.sportverse_backend.service.PartnerService;
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
    private PartnerService partnerService;

    @PostMapping("/{partnerId}/expo-token")
    public ResponseEntity<ApiResponse> updateExpoToken(
            @PathVariable String partnerId,
            @RequestBody ExpoTokenRequest request) {
        logger.info("POST /api/partners/{}/expo-token - Updating Expo token for partner", partnerId);
        partnerService.updateExpoToken(partnerId, request);
        logger.info("POST /api/partners/{}/expo-token - Successfully added Expo token", partnerId);
        return ResponseEntity.ok(new ApiResponse(true, "Expo token added successfully"));
    }
}

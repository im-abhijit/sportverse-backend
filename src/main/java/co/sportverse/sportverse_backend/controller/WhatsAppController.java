package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.SendWhatsAppTextRequest;
import co.sportverse.sportverse_backend.service.WhatsAppMessagingService;
import co.sportverse.sportverse_backend.service.WhatsAppWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
@CrossOrigin(origins = {
        "https://sportverse.co.in",
        "http://localhost:8083"
})
public class WhatsAppController {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppController.class);

    @Autowired
    private WhatsAppMessagingService whatsAppMessagingService;

    @Autowired
    private WhatsAppWebhookService whatsAppWebhookService;

    @PostMapping("/messages/text")
    public ResponseEntity<ApiResponse> sendTextMessage(@RequestBody SendWhatsAppTextRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        logger.info("POST /api/whatsapp/messages/text - Sending WhatsApp text message");
        Map<String, Object> data = whatsAppMessagingService.sendTextMessage(request.getToPhone(), request.getBody());
        logger.info("POST /api/whatsapp/messages/text - Successfully sent WhatsApp text message. messageId: {}",
                data.get("messageId"));
        return ResponseEntity.ok(new ApiResponse(true, "WhatsApp message sent successfully", data));
    }

    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if (challenge == null || challenge.trim().isEmpty()) {
            throw new IllegalArgumentException("hub.challenge is required");
        }

        boolean valid = whatsAppWebhookService.isWebhookVerificationValid(mode, verifyToken);
        if (!valid) {
            logger.warn("GET /api/whatsapp/webhook - Webhook verification failed");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
        }

        logger.info("GET /api/whatsapp/webhook - Webhook verified successfully");
        return ResponseEntity.ok(challenge);
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse> receiveWebhook(@RequestBody Map<String, Object> payload) {
        logger.info("POST /api/whatsapp/webhook - Received webhook callback");
        Map<String, Object> data = whatsAppWebhookService.handleWebhookPayload(payload);
        return ResponseEntity.ok(new ApiResponse(true, "WhatsApp webhook received", data));
    }
}

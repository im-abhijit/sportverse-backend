package co.sportverse.sportverse_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WhatsAppWebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookService.class);

    @Value("${whatsapp.cloud.webhook-verify-token:}")
    private String webhookVerifyToken;

    public boolean isWebhookVerificationValid(String mode, String verifyToken) {
        if (isBlank(webhookVerifyToken)) {
            throw new IllegalStateException("WhatsApp webhook verify token is not configured");
        }
        return "subscribe".equals(mode) && webhookVerifyToken.trim().equals(verifyToken);
    }

    public Map<String, Object> handleWebhookPayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("Webhook payload is required");
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("object", payload.get("object"));

        int entryCount = 0;
        int messageCount = 0;
        int statusCount = 0;

        for (Object entryObject : asList(payload.get("entry"))) {
            Map<?, ?> entry = asMap(entryObject);
            if (entry == null) {
                continue;
            }
            entryCount++;

            for (Object changeObject : asList(entry.get("changes"))) {
                Map<?, ?> change = asMap(changeObject);
                if (change == null) {
                    continue;
                }

                Map<?, ?> value = asMap(change.get("value"));
                if (value == null) {
                    continue;
                }

                messageCount += logIncomingMessages(value);
                statusCount += logStatusUpdates(value);
            }
        }

        summary.put("entryCount", entryCount);
        summary.put("messageCount", messageCount);
        summary.put("statusCount", statusCount);

        logger.info("WhatsAppWebhookService - Received webhook callback. entries: {}, messages: {}, statuses: {}",
                entryCount, messageCount, statusCount);
        return summary;
    }

    private int logIncomingMessages(Map<?, ?> value) {
        int count = 0;
        for (Object messageObject : asList(value.get("messages"))) {
            Map<?, ?> message = asMap(messageObject);
            if (message == null) {
                continue;
            }

            count++;
            Object from = message.get("from");
            Object messageId = message.get("id");
            Object type = message.get("type");
            logger.info("WhatsAppWebhookService - Incoming WhatsApp message. from: {}, messageId: {}, type: {}",
                    maskPhone(asString(from)), messageId, type);
        }
        return count;
    }

    private int logStatusUpdates(Map<?, ?> value) {
        int count = 0;
        for (Object statusObject : asList(value.get("statuses"))) {
            Map<?, ?> status = asMap(statusObject);
            if (status == null) {
                continue;
            }

            count++;
            Object recipientId = status.get("recipient_id");
            Object messageId = status.get("id");
            Object statusValue = status.get("status");
            logger.info("WhatsAppWebhookService - WhatsApp message status update. recipient: {}, messageId: {}, status: {}",
                    maskPhone(asString(recipientId)), messageId, statusValue);
        }
        return count;
    }

    private List<?> asList(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> map ? map : null;
    }

    private String asString(Object value) {
        return value instanceof String ? (String) value : null;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 6) {
            return phone;
        }
        return phone.substring(0, 2) + "..." + phone.substring(phone.length() - 4);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

package co.sportverse.sportverse_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WhatsAppMessagingService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppMessagingService.class);
    private static final String GRAPH_API_BASE_URL = "https://graph.facebook.com";

    @Value("${whatsapp.cloud.graph-api-version:v21.0}")
    private String graphApiVersion;

    @Value("${whatsapp.cloud.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.cloud.access-token:}")
    private String accessToken;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WhatsAppMessagingService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> sendTextMessage(String toPhone, String body) {
        validateRequest(toPhone, body);
        validateConfiguration();

        String recipient = normalizeRecipient(toPhone);
        String messageBody = body.trim();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken.trim());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messaging_product", "whatsapp");
        requestBody.put("to", recipient);
        requestBody.put("type", "text");
        requestBody.put("text", Map.of(
                "preview_url", false,
                "body", messageBody
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        String url = buildMessagesUrl();

        try {
            logger.info("WhatsAppMessagingService - Sending WhatsApp text message. recipient: {}", maskPhone(recipient));
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> responseBody = response.getBody();
            Map<String, Object> data = new HashMap<>();
            data.put("toPhone", recipient);
            data.put("messageId", extractMessageId(responseBody));

            logger.info("WhatsAppMessagingService - WhatsApp text message sent. recipient: {}, messageId: {}",
                    maskPhone(recipient), data.get("messageId"));
            return data;
        } catch (RestClientResponseException e) {
            String errorMessage = extractGraphErrorMessage(e);
            logger.warn("WhatsAppMessagingService - Graph API rejected WhatsApp message. recipient: {}, status: {}, error: {}",
                    maskPhone(recipient), e.getStatusCode(), errorMessage);
            throw new RuntimeException("Failed to send WhatsApp message: " + errorMessage, e);
        }
    }

    private void validateRequest(String toPhone, String body) {
        if (toPhone == null || toPhone.trim().isEmpty()) {
            throw new IllegalArgumentException("toPhone is required");
        }
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("body is required");
        }
    }

    private void validateConfiguration() {
        if (isBlank(graphApiVersion) || isBlank(phoneNumberId) || isBlank(accessToken)) {
            throw new IllegalStateException("WhatsApp Cloud API is not configured");
        }
    }

    private String buildMessagesUrl() {
        String version = graphApiVersion.trim();
        if (!version.startsWith("v")) {
            version = "v" + version;
        }
        return GRAPH_API_BASE_URL + "/" + version + "/" + phoneNumberId.trim() + "/messages";
    }

    private String normalizeRecipient(String rawPhone) {
        String digits = rawPhone.trim().replaceAll("[^0-9]", "");
        while (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (digits.length() == 10) {
            digits = "91" + digits;
        }
        if (digits.length() < 11 || digits.length() > 15) {
            throw new IllegalArgumentException("toPhone must be a valid international WhatsApp phone number");
        }
        return digits;
    }

    private String extractMessageId(Map<String, Object> responseBody) {
        if (responseBody == null) {
            return null;
        }
        Object messages = responseBody.get("messages");
        if (!(messages instanceof List<?> messageList) || messageList.isEmpty()) {
            return null;
        }
        Object firstMessage = messageList.get(0);
        if (!(firstMessage instanceof Map<?, ?> message)) {
            return null;
        }
        Object id = message.get("id");
        return id instanceof String ? (String) id : null;
    }

    private String extractGraphErrorMessage(RestClientResponseException e) {
        String responseBody = e.getResponseBodyAsString();
        if (isBlank(responseBody)) {
            return "Graph API returned " + e.getStatusCode();
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(responseBody, new TypeReference<>() {});
            Object error = payload.get("error");
            if (error instanceof Map<?, ?> errorMap) {
                Object message = errorMap.get("message");
                Object code = errorMap.get("code");
                if (message instanceof String && code != null) {
                    return message + " (code " + code + ")";
                }
                if (message instanceof String) {
                    return (String) message;
                }
            }
        } catch (Exception ignored) {
            logger.debug("WhatsAppMessagingService - Could not parse Graph error response");
        }

        return responseBody;
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

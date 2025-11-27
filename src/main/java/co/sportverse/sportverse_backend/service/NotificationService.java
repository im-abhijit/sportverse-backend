package co.sportverse.sportverse_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final String EXPO_PUSH_API_URL = "https://exp.host/--/api/v2/push/send";
    
    private final RestTemplate restTemplate;

    public NotificationService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Send Expo push notification using HTTP/2 API
     * @param expoToken The Expo push token (format: ExponentPushToken[...])
     * @param title Notification title
     * @param body Notification body
     * @param data Additional data payload (optional)
     * @return true if notification was sent successfully
     */
    public boolean sendPushNotification(String expoToken, String title, String body, Map<String, Object> data) {
        if (expoToken == null || expoToken.trim().isEmpty()) {
            logger.warn("NotificationService - Cannot send notification: expoToken is null or empty");
            return false;
        }

        try {
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("to", expoToken.trim());
            requestBody.put("title", title != null ? title : "");
            requestBody.put("body", body != null ? body : "");
            
            // Add data payload if provided
            if (data != null && !data.isEmpty()) {
                requestBody.put("data", data);
            }

            // Set HTTP headers as per Expo API requirements
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("host", "exp.host");
            headers.set("accept", "application/json");
            headers.set("accept-encoding", "gzip, deflate");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            logger.info("NotificationService - Sending Expo push notification to token: {}", maskToken(expoToken));
            
            ResponseEntity<String> response = restTemplate.exchange(
                    EXPO_PUSH_API_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("NotificationService - Successfully sent Expo push notification. Response: {}", response.getBody());
                return true;
            } else {
                logger.warn("NotificationService - Failed to send Expo push notification. Status: {}, Response: {}", 
                        response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            logger.error("NotificationService - Error sending Expo push notification", e);
            return false;
        }
    }

    /**
     * Send booking notification to partner
     * @param expoToken The Expo push token
     * @param bookingId Booking ID
     * @param venueName Venue name
     * @param date Booking date
     * @param amount Booking amount
     * @return true if notification was sent successfully
     */
    public boolean sendBookingNotification(String expoToken, String bookingId, String venueName, String date, String amount) {
        String title = "New Booking Received";
        String body = String.format("New booking for %s on %s - ₹%s", venueName, date, amount);

        Map<String, Object> data = new HashMap<>();
        data.put("bookingId", bookingId);
        data.put("venueName", venueName);
        data.put("date", date);
        data.put("amount", amount);
        data.put("type", "new_booking");

        return sendPushNotification(expoToken, title, body, data);
    }

    /**
     * Mask token for logging (show only first and last few characters)
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 20) {
            return token;
        }
        return token.substring(0, 15) + "..." + token.substring(token.length() - 10);
    }
}

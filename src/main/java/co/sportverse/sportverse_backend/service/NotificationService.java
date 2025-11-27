package co.sportverse.sportverse_backend.service;

import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Value("${vapid.private.key}")
    private String vapidPrivateKey;

    @Value("${vapid.public.key}")
    private String vapidPublicKey;

    @Value("${vapid.subject:mailto:admin@sportverse.co.in}")
    private String vapidSubject;

    @Autowired
    private RestTemplate restTemplate;


    @PostConstruct
    public void init() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }


    public boolean sendPushNotification(
            String endpoint,
            String p256dh,
            String auth,
            String title,
            String body,
            Map<String, Object> data
    ) {
        try {
            // Build payload JSON
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", title);
            payload.put("body", body);

            if (data != null) {
                payload.putAll(data);
            }

            String payloadJson = convertToJson(payload);

            // Build Notification object
            Notification notification = new Notification(
                    endpoint,
                    p256dh,
                    auth,
                    payloadJson.getBytes(StandardCharsets.UTF_8)
            );

            // Setup PushService
            PushService pushService = new PushService();
            pushService.setPublicKey(vapidPublicKey);
            pushService.setPrivateKey(vapidPrivateKey);
            pushService.setSubject(vapidSubject);

            // Send encrypted push
            HttpResponse response = pushService.send(notification);

            int status = response.getStatusLine().getStatusCode();
            logger.info("PUSH RESULT CODE = {}", status);

            return status == 201 || status == 204;

        } catch (Exception e) {
            logger.error("Error sending push notification", e);
            return false;
        }
    }


//    /**
//     * Create VAPID JWT token for Web Push Protocol
//     *
//     * JWT Structure: <JWTHeader>.<Payload>.<Signature>
//     *
//     * JWT Header: {"typ": "JWT", "alg": "ES256"} - base64url encoded
//     * Payload: {"aud": "<push-service-origin>", "exp": <expiration-seconds>, "sub": "<subject>"} - base64url encoded
//     * Signature: ES256 signature of header.payload using VAPID private key
//     *
//     * Final Authorization header: "WebPush " + JWT
//     */
//    private String createVapidJWT(String subscriptionEndpoint) throws Exception {
//        try {
//            // Extract origin from endpoint URL (audience)
//            // Example: https://fcm.googleapis.com/fcm/send/... -> https://fcm.googleapis.com
//            URL url = new URL(subscriptionEndpoint);
//            String audience = url.getProtocol() + "://" + url.getHost();
//            logger.debug("NotificationService - VAPID audience (push service origin): {}", audience);
//
//            // Calculate expiration time (must be within 24 hours, in seconds since epoch)
//            Instant now = Instant.now();
//            Instant expiration = now.plusSeconds(12 * 60 * 60); // 12 hours from now
//            long expSeconds = expiration.getEpochSecond();
//
//            // Create JWT claims set
//            // The library will automatically map these to "aud", "exp", "sub" in the JWT payload
//            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
//                    .audience(audience)  // Maps to "aud" in JWT
//                    .expirationTime(java.util.Date.from(expiration))  // Maps to "exp" in JWT (as seconds since epoch)
//                    .subject(vapidSubject)  // Maps to "sub" in JWT
//                    .build();
//
//            // Create JWT header with explicit type
//            // JWSHeader.Builder will include "typ": "JWT" and "alg": "ES256"
//            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
//                    .type(com.nimbusds.jose.JOSEObjectType.JWT)  // Explicitly set type to JWT
//                    .build();
//
//            // Create signed JWT
//            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
//
//            // Load VAPID private key (base64url encoded, use as-is)
//            // VAPID private key is the raw private key bytes (32 bytes for P-256) base64url encoded
//            ECKey ecKey = loadVapidPrivateKey(vapidPrivateKey, vapidPublicKey);
//
//            // Sign JWT using ES256 algorithm with VAPID private key
//            // This creates the signature by signing: <base64url(header)>.<base64url(payload)>
//            ECDSASigner signer = new ECDSASigner(ecKey);
//            signedJWT.sign(signer);
//
//            // Serialize JWT: combines signingInputString (<header>.<payload>) with signature
//            // Final format: <header>.<payload>.<signature>
//            String jwt = signedJWT.serialize();
//            logger.debug("NotificationService - VAPID JWT created successfully. Length: {}", jwt.length());
//
//            return jwt;
//        } catch (Exception e) {
//            logger.error("NotificationService - Error creating VAPID JWT", e);
//            throw e;
//        }
//    }
//
//    /**
//     * Load VAPID private key from base64url encoded string
//     * VAPID keys are raw key material (32 bytes for P-256) base64url encoded
//     * We construct a JWK (JSON Web Key) format from the base64url encoded keys
//     */
//    private ECKey loadVapidPrivateKey(String privateKeyBase64, String publicKeyBase64) throws Exception {
//        try {
//            // VAPID keys are base64url encoded raw key material
//            // Private key: 32 bytes (raw private key value for P-256 curve)
//            // Public key: 65 bytes (uncompressed EC point: 0x04 + 32 bytes X + 32 bytes Y)
//
//            // Decode the keys
//            byte[] privateKeyBytes = Base64.getUrlDecoder().decode(privateKeyBase64);
//            byte[] publicKeyBytes = Base64.getUrlDecoder().decode(publicKeyBase64);
//
//            // Extract X and Y coordinates from public key (skip first byte which is 0x04)
//            if (publicKeyBytes.length != 65 || publicKeyBytes[0] != 0x04) {
//                throw new Exception("Invalid VAPID public key format. Expected 65 bytes with 0x04 prefix.");
//            }
//
//            byte[] x = new byte[32];
//            byte[] y = new byte[32];
//            System.arraycopy(publicKeyBytes, 1, x, 0, 32);
//            System.arraycopy(publicKeyBytes, 33, y, 0, 32);
//
//            // Construct ECKey from raw key material
//            // Using P-256 curve (secp256r1)
//            ECKey ecKey = new ECKey.Builder(
//                    com.nimbusds.jose.jwk.Curve.P_256,
//                    com.nimbusds.jose.util.Base64URL.encode(x),
//                    com.nimbusds.jose.util.Base64URL.encode(y)
//            )
//            .d(com.nimbusds.jose.util.Base64URL.encode(privateKeyBytes))
//            .build();
//
//            return ecKey;
//        } catch (Exception e) {
//            logger.error("NotificationService - Error loading VAPID private key", e);
//            throw new Exception("Failed to load VAPID private key: " + e.getMessage(), e);
//        }
//    }
//
    /**
     * Convert map to JSON string (simple implementation)
     */
    private String convertToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            } else {
                json.append(value);
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    /**
     * Escape JSON string
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

//    /**
//     * Mask endpoint for logging (show only first and last few characters)
//     */
//    private String maskEndpoint(String endpoint) {
//        if (endpoint.length() <= 50) {
//            return endpoint;
//        }
//        return endpoint.substring(0, 30) + "..." + endpoint.substring(endpoint.length() - 20);
//    }

    /**
     * Send booking notification
     * @param subscriptionEndpoint The full subscription endpoint URL
     * @param bookingId Booking ID
     * @param venueName Venue name
     * @param date Booking date
     * @param amount Booking amount
     * @return true if notification was sent successfully
     */
    public boolean sendBookingNotification(String subscriptionEndpoint, String p256dh, String auth,  String bookingId, String venueName, String date, String amount) {
        Map<String, Object> data = new HashMap<>();
        data.put("bookingId", bookingId);
        data.put("venueName", venueName);
        data.put("date", date);
        data.put("amount", amount);
        data.put("type", "new_booking");

        String title = "New Booking Received";
        String body = String.format("New booking for %s on %s - ₹%s", venueName, date, amount);

        return sendPushNotification(subscriptionEndpoint, p256dh,auth, title, body, data);
    }
}

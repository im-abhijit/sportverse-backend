package co.sportverse.sportverse_backend.service;

import com.niamedtech.expo.exposerversdk.ExpoPushNotificationClient;
import com.niamedtech.expo.exposerversdk.request.PushNotification;
import com.niamedtech.expo.exposerversdk.response.TicketResponse;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class ExpoPushNotificationService {

    private final ExpoPushNotificationClient client;

    public ExpoPushNotificationService() {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        this.client = ExpoPushNotificationClient
                .builder()
                .setHttpClient(httpClient)
                // Optional: set access token if needed
                // .setAccessToken("EXPO_ACCESS_TOKEN")
                .build();
    }

    /**
     * Sends a push notification to an Expo push token
     *
     * @param expoPushToken "ExponentPushToken[xxxxxxx]"
     * @param title Title of notification
     * @param message Body text
     * @return Status string ("ok", "error", etc.)
     */
    public String sendNotification(String expoPushToken, String title, String message)  {

        try {
            List<String> toList = new ArrayList<>();
            toList.add(expoPushToken);

            PushNotification notification = new PushNotification();
            notification.setTo(toList);
            notification.setTitle(title);
            notification.setBody(message);
            notification.setChannelId("booking-alert");
            List<PushNotification> notifications = new ArrayList<>();
            notifications.add(notification);

            List<TicketResponse.Ticket> response = client.sendPushNotifications(notifications);

            for (TicketResponse.Ticket ticket : response) {
                System.out.println("Expo Ticket ID: " + ticket.getId());
                System.out.println("Status: " + ticket.getStatus());

                return ticket.getStatus().name(); // OK or ERROR
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR";
        }

        return "ERROR";
    }

    public String sendBookingNotification(String expoToken, String bookingId, String venueName, String date, String amount) {
        String title = "New Booking Received";
        String body = String.format("New booking for %s on %s - ₹%s", venueName, date, amount);

        Map<String, Object> data = new HashMap<>();
        data.put("bookingId", bookingId);
        data.put("venueName", venueName);
        data.put("date", date);
        data.put("amount", amount);
        data.put("type", "new_booking");

        return sendNotification(expoToken, title, body);
    }

}

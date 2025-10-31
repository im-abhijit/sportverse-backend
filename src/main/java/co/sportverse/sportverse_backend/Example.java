package co.sportverse.sportverse_backend;
import com.twilio.type.PhoneNumber;
import java.util.HashMap;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import org.json.JSONObject;

public class Example {
    public static final String ACCOUNT_SID = "ACdabe2fcb6f64fb36ea1b528dc9a02d2f";
    public static final String AUTH_TOKEN = "7152710e18194cf4abc26a7d75ee1298";


    public static void main(String[] args) {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        Message message =
                Message.creator(
                                new PhoneNumber("whatsapp:+91"),
                                new PhoneNumber("whatsapp:+14155238886"),
                                (String) null
                        )
                        .setContentSid("HXb5b62575e6e4ff6129ad7c8efe1f983e")
                        .setContentVariables(new JSONObject(new HashMap<String, Object>() {{
                            put("1", "22 July 2026");
                            put("2", "3:15pm");
                        }}).toString())
                        .create();

        System.out.println(message.getSid());
    }
}
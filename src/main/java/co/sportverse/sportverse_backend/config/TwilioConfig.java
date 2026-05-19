package co.sportverse.sportverse_backend.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwilioConfig {

    private final TwilioProperties properties;

    public TwilioConfig(TwilioProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {

        Twilio.init(
                properties.getAccountSid(),
                properties.getAuthToken()
        );
    }
}

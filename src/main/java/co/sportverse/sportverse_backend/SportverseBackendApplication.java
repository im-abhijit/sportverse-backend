package co.sportverse.sportverse_backend;

import co.sportverse.sportverse_backend.config.JwtProperties;
import co.sportverse.sportverse_backend.config.TwilioProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties({JwtProperties.class, TwilioProperties.class})
public class SportverseBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SportverseBackendApplication.class, args);
	}

}

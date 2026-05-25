package co.sportverse.sportverse_backend.config;

import co.sportverse.sportverse_backend.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * When {@code sportverse.cors.allow-all=true} (default), allows any browser origin ({@code *} pattern).
     * Set {@code sportverse.cors.allow-all=false} and tune {@code allowed-origins} / {@code allowed-origin-patterns} for stricter production CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${sportverse.cors.allow-all:true}") boolean allowAllOrigins,
            @Value("${sportverse.cors.allowed-origins:https://sportverse.co.in,http://localhost:8083,https://sportverseapp.netlify.app}") String allowedOrigins,
            @Value("${sportverse.cors.allowed-origin-patterns:https://*.netlify.app,http://localhost:*}") String allowedOriginPatterns) {
        CorsConfiguration config = new CorsConfiguration();
        if (allowAllOrigins) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            List<String> origins = Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            if (!origins.isEmpty()) {
                config.setAllowedOrigins(origins);
            }
            List<String> originPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            if (!originPatterns.isEmpty()) {
                config.setAllowedOriginPatterns(originPatterns);
            }
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http.cors(c -> c.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                /**
                 * Default is public ({@code permitAll}). Only handlers that enforce JWT via
                 * {@link org.springframework.security.core.annotation.AuthenticationPrincipal} + {@link co.sportverse.sportverse_backend.security.AuthenticatedUserSupport}
                 * (or equivalent null checks) are authenticated — e.g. {@code GET /api/user/slots} and {@code GET /api/user/venues} stay public because they only log the principal optionally.
                 */
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/user/home").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/user/bookings").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/user/bookings/create-order-manual").authenticated()
                        .requestMatchers("/api/user/profile/**").authenticated()
                        .requestMatchers("/api/partner/bookings/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/payments/verify").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/bookings/create-order").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/bookings/cancel").authenticated()
                        .requestMatchers("/api/secure/imagekit/**").authenticated()
                        .anyRequest()
                        .permitAll())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

package co.sportverse.sportverse_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Matches Spendwise-style JWT binding: {@code jwt.secret}, {@code jwt.expiration-ms}.
 */
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HS256 signing secret; use at least 32 bytes in production (e.g. {@code JWT_SECRET} env). */
    private String secret = "";

    private long expirationMs = 86_400_000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}

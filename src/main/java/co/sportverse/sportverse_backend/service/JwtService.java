package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.config.JwtProperties;
import co.sportverse.sportverse_backend.dto.AuthenticatedUser;
import co.sportverse.sportverse_backend.security.AuthenticatedUserSupport;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Normalized Indian mobile in digits-only form (same pattern as OTP user matching).
     */
    public static String normalizeIndianPhoneDigits(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String digits = raw.replaceAll("\\D+", "");
        if (digits.startsWith("91") && digits.length() == 12) {
            return digits.substring(2);
        }
        return digits;
    }

    public String createPartnerAccessToken(String username, String partnerId) {
        SecretKey key = Keys.hmacShaKeyFor(secretBytes());
        Instant now = Instant.now();
        Instant exp = now.plusMillis(jwtProperties.getExpirationMs());
        return Jwts.builder()
                .subject(username)
                .claim("role", AuthenticatedUserSupport.ROLE_VENUE_OWNER)
                .claim("partnerId", partnerId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }


    /** JWT {@code sub} holds normalized mobile digits only. */
    public String createUserAccessToken(String normalizedPhoneDigits) {
        if (normalizedPhoneDigits == null || normalizedPhoneDigits.isBlank()) {
            throw new IllegalArgumentException("Phone subject is required for JWT");
        }
        SecretKey key = Keys.hmacShaKeyFor(secretBytes());
        Instant now = Instant.now();
        Instant exp = now.plusMillis(jwtProperties.getExpirationMs());
        return Jwts.builder()
                .subject(normalizedPhoneDigits.trim())
                .claim("role", AuthenticatedUserSupport.ROLE_USER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    /**
     * True if signature, structure and expiry are valid; false otherwise.
     * <p>Note: Does not inspect {@code sub}. For resolving the user identifier in one pass (e.g. in a servlet
     * filter), prefer {@link #validateTokenAndResolveUserSubject(String)} instead of chaining this with
     * {@link #extractUserIdFromToken(String)} — calling both parses and verifies the JWT twice.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            verifiedClaims(token.trim());
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Single JWT parse + verify: signature, expiry and non-empty {@code sub}. Use this instead of calling
     * {@link #validateToken(String)} and then {@link #extractUserIdFromToken(String)}, which repeats the crypto work.
     *
     * @return normalized subject (currently Indian mobile digits) or empty when invalid/absent/bad Bearer value
     */
    public Optional<String> validateTokenAndResolveUserSubject(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = verifiedClaims(token.trim());
            String sub = claims.getSubject();
            if (sub == null || sub.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(sub.trim());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Validates the token and returns the {@code sub} claim. In this app that is the user identifier:
     * normalized Indian mobile digits (same value used when issuing the token).
     *
     * @throws IllegalArgumentException if token is blank
     * @throws JwtException if signature, expiry, or subject is invalid
     * <p>Prefer {@link #validateTokenAndResolveUserSubject(String)} when avoiding a redundant prior
     * {@link #validateToken(String)} call (see {@link #validateToken} javadoc).
     */
    public String extractUserIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT is required");
        }
        return validateTokenAndResolveUserSubject(token)
                .orElseThrow(() -> new JwtException("Invalid JWT or missing subject"));
    }

    private Claims verifiedClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretBytes());
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private byte[] secretBytes() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 bytes (configure JWT_SECRET or jwt.secret)");
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }


    public Optional<AuthenticatedUser> validateAndResolvePrincipal(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = verifiedClaims(token.trim());
            String subject = claims.getSubject();
            String role = claims.get("role", String.class);
            String partnerId = claims.get("partnerId", String.class);
            if (subject == null || subject.isBlank()) {
                return Optional.empty();
            }
            if (role == null || role.isBlank()) {
                role = AuthenticatedUserSupport.ROLE_USER;
            }
            return Optional.of(
                    new AuthenticatedUser(
                            subject.trim(),
                            role.trim(),
                            partnerId
                    )
            );

        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

}

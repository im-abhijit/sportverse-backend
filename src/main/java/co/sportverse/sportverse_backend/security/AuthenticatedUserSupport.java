package co.sportverse.sportverse_backend.security;

import co.sportverse.sportverse_backend.dto.AuthenticatedUser;

/**
 * Helpers for JWT principals issued as {@link AuthenticatedUser}.
 */
public final class AuthenticatedUserSupport {

    /** App-customer JWTs (OTP / user auth). Matches claim on {@link co.sportverse.sportverse_backend.service.JwtService#createUserAccessToken}. */
    public static final String ROLE_USER = "ROLE_USER";

    /** Partner / venue-owner JWTs. Matches claim on {@link co.sportverse.sportverse_backend.service.JwtService#createPartnerAccessToken}. */
    public static final String ROLE_VENUE_OWNER = "ROLE_VENUE_OWNER";

    private AuthenticatedUserSupport() {
    }

    /**
     * Returns the JWT {@code partnerId} claim for a partner principal ({@link #ROLE_VENUE_OWNER}).
     *
     * @throws IllegalArgumentException if principal, role, or partner id is invalid
     */
    public static String requirePartnerId(AuthenticatedUser principal) {
        if (principal == null || principal.getPartnerId() == null || principal.getPartnerId().isBlank()) {
            throw new IllegalArgumentException("Authentication is required");
        }
        if (!ROLE_VENUE_OWNER.equals(principal.getRole())) {
            throw new IllegalArgumentException("Partner authentication is required");
        }
        return principal.getPartnerId().trim();
    }

    /**
     * Returns the JWT {@code subject} for an app-customer principal (normalized phone digits, etc.).
     *
     * @throws IllegalArgumentException if principal/subject absent or principal is not a user token ({@link #ROLE_USER})
     */
    public static String requireUserSubject(AuthenticatedUser principal) {
        if (principal == null || principal.getSubject() == null || principal.getSubject().isBlank()) {
            throw new IllegalArgumentException("Authentication is required");
        }
        if (!ROLE_USER.equals(principal.getRole())) {
            throw new IllegalArgumentException("User authentication is required");
        }
        return principal.getSubject().trim();
    }
}

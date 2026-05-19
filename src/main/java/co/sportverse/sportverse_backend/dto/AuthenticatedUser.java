package co.sportverse.sportverse_backend.dto;

public class AuthenticatedUser {
    private final String subject;

    private final String role;

    private final String partnerId;

    public AuthenticatedUser(
            String subject,
            String role,
            String partnerId
    ) {
        this.subject = subject;
        this.role = role;
        this.partnerId = partnerId;
    }

    public String getSubject() {
        return subject;
    }

    public String getRole() {
        return role;
    }

    public String getPartnerId() {
        return partnerId;
    }
}

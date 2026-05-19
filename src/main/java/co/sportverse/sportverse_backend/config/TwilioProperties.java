package co.sportverse.sportverse_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "twilio")
public class TwilioProperties {

    private Account account;

    /** Binds {@code twilio.auth.token} */
    private Auth auth;

    private Verification verification;

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Verification getVerification() {
        return verification;
    }

    public void setVerification(Verification verification) {
        this.verification = verification;
    }

    public String getAccountSid() {
        return account != null ? account.getSid() : null;
    }

    public String getAuthToken() {
        return auth != null ? auth.getToken() : null;
    }

    public String getVerificationServiceSid() {
        if (verification == null || verification.getService() == null) {
            return null;
        }
        return verification.getService().getSid();
    }

    public static class Account {
        private String sid;

        public String getSid() {
            return sid;
        }

        public void setSid(String sid) {
            this.sid = sid;
        }
    }

    public static class Auth {
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class Verification {
        private Service service;

        public Service getService() {
            return service;
        }

        public void setService(Service service) {
            this.service = service;
        }

        public static class Service {
            private String sid;

            public String getSid() {
                return sid;
            }

            public void setSid(String sid) {
                this.sid = sid;
            }
        }
    }
}

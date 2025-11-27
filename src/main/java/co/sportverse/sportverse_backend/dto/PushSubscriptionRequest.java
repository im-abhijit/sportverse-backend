package co.sportverse.sportverse_backend.dto;

public class PushSubscriptionRequest {
    private String endpoint;
    private Keys keys;

    public static class Keys {
        private String p256dh;
        private String auth;

        public Keys() {}

        public Keys(String p256dh, String auth) {
            this.p256dh = p256dh;
            this.auth = auth;
        }

        public String getP256dh() {
            return p256dh;
        }

        public void setP256dh(String p256dh) {
            this.p256dh = p256dh;
        }

        public String getAuth() {
            return auth;
        }

        public void setAuth(String auth) {
            this.auth = auth;
        }
    }

    public PushSubscriptionRequest() {}

    public PushSubscriptionRequest(String endpoint, Keys keys) {
        this.endpoint = endpoint;
        this.keys = keys;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Keys getKeys() {
        return keys;
    }

    public void setKeys(Keys keys) {
        this.keys = keys;
    }
}


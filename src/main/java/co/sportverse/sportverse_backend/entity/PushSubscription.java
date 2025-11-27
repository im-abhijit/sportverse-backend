package co.sportverse.sportverse_backend.entity;

import org.bson.Document;

import java.util.Date;

public class PushSubscription {
    private String id;
    private String partnerId;
    private String endpoint;
    private String p256dh;
    private String auth;
    private Date createdAt;
    private Date updatedAt;

    public PushSubscription() {}

    public PushSubscription(String partnerId, String endpoint, String p256dh, String auth) {
        this.partnerId = partnerId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static PushSubscription fromDocument(Document doc) {
        if (doc == null) return null;
        PushSubscription subscription = new PushSubscription();
        subscription.setId(doc.getObjectId("_id").toString());
        subscription.setPartnerId(doc.getString("partnerId"));
        subscription.setEndpoint(doc.getString("endpoint"));
        subscription.setP256dh(doc.getString("p256dh"));
        subscription.setAuth(doc.getString("auth"));
        subscription.setCreatedAt(doc.getDate("createdAt"));
        subscription.setUpdatedAt(doc.getDate("updatedAt"));
        return subscription;
    }

    public Document toDocument() {
        Document doc = new Document();
        if (this.id != null) {
            doc.append("_id", new org.bson.types.ObjectId(this.id));
        }
        doc.append("partnerId", this.partnerId);
        doc.append("endpoint", this.endpoint);
        doc.append("p256dh", this.p256dh);
        doc.append("auth", this.auth);
        doc.append("createdAt", this.createdAt != null ? this.createdAt : new Date());
        doc.append("updatedAt", this.updatedAt != null ? this.updatedAt : new Date());
        return doc;
    }
}


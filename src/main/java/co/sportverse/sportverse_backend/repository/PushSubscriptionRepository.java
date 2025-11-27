package co.sportverse.sportverse_backend.repository;

import co.sportverse.sportverse_backend.entity.PushSubscription;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

import static com.mongodb.client.model.Filters.eq;

@Component
public class PushSubscriptionRepository {

    private final MongoCollection<Document> pushSubscriptionsCollection;

    @Autowired
    public PushSubscriptionRepository(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase("sportverse");
        this.pushSubscriptionsCollection = database.getCollection("push_subscriptions");
    }

    public PushSubscription save(PushSubscription subscription) {
        Document doc = subscription.toDocument();
        pushSubscriptionsCollection.insertOne(doc);
        subscription.setId(doc.getObjectId("_id").toString());
        return subscription;
    }

    public PushSubscription findByPartnerId(String partnerId) {
        Bson filter = eq("partnerId", partnerId);
        Document doc = pushSubscriptionsCollection.find(filter).first();
        return PushSubscription.fromDocument(doc);
    }

    public PushSubscription updateByPartnerId(String partnerId, PushSubscription subscription) {
        Bson filter = eq("partnerId", partnerId);
        Document existing = pushSubscriptionsCollection.find(filter).first();
        
        if (existing == null) {
            // Create new subscription
            return save(subscription);
        } else {
            // Update existing subscription
            subscription.setId(existing.getObjectId("_id").toString());
            subscription.setCreatedAt(existing.getDate("createdAt"));
            subscription.setUpdatedAt(new Date());
            
            Document doc = subscription.toDocument();
            doc.remove("_id"); // Don't update _id
            pushSubscriptionsCollection.updateOne(filter, new Document("$set", doc));
            return subscription;
        }
    }

    public boolean deleteByPartnerId(String partnerId) {
        Bson filter = eq("partnerId", partnerId);
        long deletedCount = pushSubscriptionsCollection.deleteOne(filter).getDeletedCount();
        return deletedCount > 0;
    }
}


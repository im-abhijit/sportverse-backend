package co.sportverse.sportverse_backend.repository;

import co.sportverse.sportverse_backend.entity.User;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

@Component
public class UserRepository {

    private final MongoCollection<Document> usersCollection;

    @Autowired
    public UserRepository(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase("sportverse");
        this.usersCollection = database.getCollection("users");
    }

    public User findByMobileNo(String mobileNo) {
        Bson filter = eq("phone", mobileNo);
        User result = User.fromDocument(usersCollection.find(filter).first());
        return result;
    }

    public User save(User user) {
        // Set createdAt if it's a new user
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }

        Document doc = user.toDocument();

        // If user has an ID, update existing document; otherwise insert new
        if (user.getId() != null) {
            Bson filter = eq("_id", new org.bson.types.ObjectId(user.getId()));
            usersCollection.replaceOne(filter, doc);
            return user;
        } else {
            // Insert new document and get the generated ID
            usersCollection.insertOne(doc);
            user.setId(doc.getObjectId("_id").toString());
            return user;
        }
    }

    public void updateIsVenueOwner(String userId, boolean isVenueOwner) {
        Bson filter = eq("_id", new org.bson.types.ObjectId(userId));
        Bson update = set("isVenueOwner", isVenueOwner);
        usersCollection.updateOne(filter, update);
    }

    /**
     * Updates profile fields by user id; only non-null arguments are applied.
     */
    public User updateProfileById(String userId, String firstName, String lastName, String email) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        ObjectId oid;
        try {
            oid = new ObjectId(userId.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
        Bson filter = eq("_id", oid);
        Document existing = usersCollection.find(filter).first();
        if (existing == null) {
            return null;
        }
        List<Bson> patches = new ArrayList<>();
        if (firstName != null) {
            patches.add(set("firstName", firstName.trim()));
        }
        if (lastName != null) {
            patches.add(set("lastName", lastName.trim()));
        }
        if (email != null) {
            patches.add(set("email", email.trim()));
        }
        if (!patches.isEmpty()) {
            usersCollection.updateOne(filter, combine(patches));
        }
        return User.fromDocument(usersCollection.find(filter).first());
    }

    public User updateByMobileNo(String mobileNo, String name, String city) {
        Bson filter = eq("phone", mobileNo);
        Document existing = usersCollection.find(filter).first();
        if (existing == null) {
            return null;
        }
        if (name != null && !name.trim().isEmpty()) {
            usersCollection.updateOne(filter, set("name", name.trim()));
        }
        if (city != null && !city.trim().isEmpty()) {
            usersCollection.updateOne(filter, set("city", city.trim()));
        }
        Document updated = usersCollection.find(filter).first();
        return User.fromDocument(updated);
    }

    public long deleteById(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return 0;
        }
        try {
            Bson filter = eq("_id", new ObjectId(userId.trim()));
            return usersCollection.deleteOne(filter).getDeletedCount();
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }
}

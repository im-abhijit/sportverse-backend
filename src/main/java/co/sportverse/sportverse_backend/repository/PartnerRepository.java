package co.sportverse.sportverse_backend.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.addToSet;
import static com.mongodb.client.model.Updates.set;

@Component
public class PartnerRepository {

    private final MongoCollection<Document> partnersCollection;

    @Autowired
    public PartnerRepository(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase("sportverse");
        this.partnersCollection = database.getCollection("partners");
    }

    public List<String> getVenueIdsByPartnerId(String partnerId) {
        Bson filter = eq("partnerId", partnerId);
        Document doc = partnersCollection.find(filter).first();
        if (doc == null) {
            return new ArrayList<>();
        }
        @SuppressWarnings("unchecked")
        List<org.bson.types.ObjectId> venueObjectIds = (List<org.bson.types.ObjectId>) doc.get("venues");
        if (venueObjectIds == null || venueObjectIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> venueIds = new ArrayList<>();
        for (org.bson.types.ObjectId oid : venueObjectIds) {
            venueIds.add(oid.toString());
        }
        return venueIds;
    }

    public Document findByPartnerId(String partnerId) {
        Bson filter = eq("partnerId", partnerId);
        return partnersCollection.find(filter).first();
    }

    public void addVenueToPartner(String partnerId, String venueId) {
        Bson filter = eq("partnerId", partnerId);
        Document partner = partnersCollection.find(filter).first();
        if (partner == null) {
            return; // Partner not found, skip update
        }
        // Add venue ID to the venues array (using addToSet to avoid duplicates)
        partnersCollection.updateOne(filter, addToSet("venues", new org.bson.types.ObjectId(venueId)));
    }

    public boolean addExpoToken(String partnerId, String expoToken) {
        Bson filter = eq("partnerId", partnerId);
        Document partner = partnersCollection.find(filter).first();
        if (partner == null) {
            return false; // Partner not found
        }
        
        // Check if expoTokens field exists
        Object expoTokensObj = partner.get("expoTokens");
        if (expoTokensObj == null) {
            // Create new list with the token
            List<String> tokens = new ArrayList<>();
            tokens.add(expoToken.trim());
            partnersCollection.updateOne(filter, set("expoTokens", tokens));
        } else {
            // Add to existing list (using addToSet to avoid duplicates)
            partnersCollection.updateOne(filter, addToSet("expoTokens", expoToken.trim()));
        }
        
        return true;
    }

    public List<String> getExpoTokens(String partnerId) {
        Bson filter = eq("partnerId", partnerId);
        Document partner = partnersCollection.find(filter).first();
        if (partner == null) {
            return new ArrayList<>();
        }
        
        @SuppressWarnings("unchecked")
        List<String> tokens = (List<String>) partner.get("expoTokens");
        if (tokens == null) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(tokens);
    }
}


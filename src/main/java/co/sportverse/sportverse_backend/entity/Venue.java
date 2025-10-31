package co.sportverse.sportverse_backend.entity;

import org.bson.Document;

import java.util.List;

public class Venue {

    String id;
    String name;
    String description;
    List<String> games;
    String location;
    List<String> photos; // max 3 photos
    String ownerId;
    String city;   // Reference to User

    // Constructors
    public Venue() {}

    public Venue(String name, String description, List<String> games, String location, List<String> photos, String ownerId, String city) {
        this.name = name;
        this.description = description;
        this.games = games;
        this.location = location;
        this.photos = photos;
        this.ownerId = ownerId;
        this.city = city;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getGames() {
        return games;
    }

    public void setGames(List<String> games) {
        this.games = games;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String toString() {
        return "Venue{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", games=" + games +
                ", location='" + location + '\'' +
                ", photos=" + photos +
                ", ownerId='" + ownerId + '\'' +
                '}';
    }

    // MongoDB conversion methods
    public static Venue fromDocument(Document doc) {
        if (doc == null) return null;
        Venue venue = new Venue();
        venue.setId(doc.getObjectId("_id").toString());
        venue.setName(doc.getString("name"));
        venue.setDescription(doc.getString("description"));
        venue.setGames(doc.getList("games", String.class));
        venue.setLocation(doc.getString("location"));
        venue.setPhotos(doc.getList("photos", String.class));
        venue.setOwnerId(doc.getString("ownerId"));
        venue.setCity(doc.getString("city"));
        return venue;
    }

    public Document toDocument() {
        Document doc = new Document();
        if (this.id != null) {
            doc.append("_id", new org.bson.types.ObjectId(this.id));
        }
        doc.append("name", this.name);
        doc.append("description", this.description);
        doc.append("games", this.games);
        doc.append("location", this.location);
        doc.append("photos", this.photos);
        doc.append("ownerId", this.ownerId);
        doc.append("city", this.city);
        return doc;
    }
}

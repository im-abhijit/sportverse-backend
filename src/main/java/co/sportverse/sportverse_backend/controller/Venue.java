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
    String partnerId;
    String partnerMobileNo;
    String city;   // Reference to User
    String qrCodeImage;
    String upiId;
    List<String> amenities;
    String thumbnailUrl;
    String venueMode; // "manual" or "automatic"
    private String operatingHoursDisplay;
    private boolean isOpenNow;
    private Integer minPrice;
    private Integer maxPrice;

    // Constructors
    public Venue() {}

    public Venue(String name, String description, List<String> games, String location, List<String> photos, String partnerId, String city, String partnerMobileNo, String qrCodeImage, String upiId, List<String> amenities, String thumbnailUrl) {
        this.name = name;
        this.description = description;
        this.games = games;
        this.location = location;
        this.photos = photos;
        this.partnerId = partnerId;
        this.city = city;
        this.partnerMobileNo = partnerMobileNo;
        this.qrCodeImage = qrCodeImage;
        this.upiId = upiId;
        this.amenities = amenities;
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getVenueMode() {
        return venueMode;
    }

    public void setVenueMode(String venueMode) {
        this.venueMode = venueMode;
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

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPartnerMobileNo() {
        return partnerMobileNo;
    }

    public void setPartnerMobileNo(String partnerMobileNo) {
        this.partnerMobileNo = partnerMobileNo;
    }

    public String getQrCodeImage() {
        return qrCodeImage;
    }

    public void setQrCodeImage(String qrCodeImage) {
        this.qrCodeImage = qrCodeImage;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getOperatingHoursDisplay() {
        return operatingHoursDisplay;
    }

    public void setOperatingHoursDisplay(String operatingHoursDisplay) {
        this.operatingHoursDisplay = operatingHoursDisplay;
    }

    public boolean isOpenNow() {
        return isOpenNow;
    }

    public void setOpenNow(boolean openNow) {
        isOpenNow = openNow;
    }

    public Integer getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Integer minPrice) {
        this.minPrice = minPrice;
    }

    public Integer getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Integer maxPrice) {
        this.maxPrice = maxPrice;
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
                ", ownerId='" + partnerId + '\'' +
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
        venue.setPartnerId(doc.getString("partnerId"));
        venue.setCity(doc.getString("city"));
        venue.setPartnerMobileNo(doc.getString("partnerMobileNo"));
        venue.setQrCodeImage(doc.getString("qrCodeImage"));
        venue.setUpiId(doc.getString("upiId"));
        venue.setAmenities(doc.getList("amenities", String.class));
        venue.setThumbnailUrl(doc.getString("thumbnailUrl"));
        String mode = doc.getString("venueMode");
        venue.setVenueMode(mode != null && !mode.isEmpty() ? mode : "manual");
        venue.setOperatingHoursDisplay(doc.getString("operatingHoursDisplay"));
        venue.setMinPrice(doc.getInteger("minPrice"));
        venue.setMaxPrice(doc.getInteger("maxPrice"));
        venue.setOpenNow(doc.getBoolean("openNow"));
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
        doc.append("partnerId", this.partnerId);
        doc.append("city", this.city);
        doc.append("partnerMobileNo", this.partnerMobileNo);
        doc.append("qrCodeImage", this.qrCodeImage);
        doc.append("upiId", this.upiId);
        doc.append("amenities", this.amenities);
        doc.append("thumbnailUrl", this.thumbnailUrl);
        doc.append("venueMode", this.venueMode != null ? this.venueMode : "manual");
        doc.append("minPrice", this.minPrice);
        doc.append("maxPrice", this.maxPrice);
        doc.append("operatingHoursDisplay", this.operatingHoursDisplay);
        doc.append("openNow", this.isOpenNow);
        return doc;
    }

}

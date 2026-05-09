package co.sportverse.sportverse_backend.dto;

import java.util.List;

public class CreateVenueRequest {
    private String id; // Optional: if provided, will update existing venue
    private String name;
    private String description;
    private List<String> games;
    private String location;
    private List<String> photos;
    private String city;
    private String partnerId;
    private String partnerMobileNo;
    private String qrCodeImage;
    private String upiId;
    private List<String> amenities;
    private String thumbnailUrl;
    private String venueMode;
    private String operatingHoursDisplay;
    private boolean isOpenNow;
    private Integer minPrice;
    private Integer maxPrice;
    public CreateVenueRequest() {}

    public CreateVenueRequest(String name, String description, List<String> games, String location, List<String> photos) {
        this.name = name;
        this.description = description;
        this.games = games;
        this.location = location;
        this.photos = photos;
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
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getVenueMode() {
        return venueMode;
    }

    public void setVenueMode(String venueMode) {
        this.venueMode = venueMode;
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
}


package co.sportverse.sportverse_backend.dto;

import co.sportverse.sportverse_backend.entity.Venue;

public class VenueResponse {
    private String id;
    private String name;
    private String description;
    private java.util.List<String> games;
    private String addtress;
    private java.util.List<String> photos;
    private String ownerId;
    private String city;

    public VenueResponse() {}

    public VenueResponse(Venue venue) {
        this.id = venue.getId();
        this.name = venue.getName();
        this.description = venue.getDescription();
        this.games = venue.getGames();
        this.addtress = venue.getLocation();
        this.photos = venue.getPhotos();
        this.ownerId = venue.getOwnerId();
        this.city = venue.getCity();
    }

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

    public java.util.List<String> getGames() {
        return games;
    }

    public void setGames(java.util.List<String> games) {
        this.games = games;
    }

    public String getAddtress() {
        return addtress;
    }

    public void setAddtress(String addtress) {
        this.addtress = addtress;
    }

    public java.util.List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(java.util.List<String> photos) {
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
}


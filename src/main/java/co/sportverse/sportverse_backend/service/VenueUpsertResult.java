package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.entity.Venue;

public final class VenueUpsertResult {

    private final Venue venue;
    private final boolean update;

    public VenueUpsertResult(Venue venue, boolean update) {
        this.venue = venue;
        this.update = update;
    }

    public Venue getVenue() {
        return venue;
    }

    public boolean isUpdate() {
        return update;
    }
}

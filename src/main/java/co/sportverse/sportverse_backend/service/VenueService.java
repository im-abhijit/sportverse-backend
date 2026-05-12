package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.dto.CreateVenueRequest;
import co.sportverse.sportverse_backend.entity.Venue;
import co.sportverse.sportverse_backend.repository.PartnerRepository;
import co.sportverse.sportverse_backend.repository.VenueRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VenueService {

    private static final Logger logger = LoggerFactory.getLogger(VenueService.class);

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    public VenueUpsertResult createOrUpdateVenue(CreateVenueRequest request) {
        validateCreateRequest(request);

        boolean isUpdate = false;
        Venue savedVenue;
        String requestId = request.getId() != null ? request.getId().trim() : null;

        if (requestId != null && !requestId.isEmpty()) {
            Venue existing = venueRepository.findById(requestId);
            if (existing != null) {
                isUpdate = true;
                copyRequestOntoVenue(existing, request);
                existing.setId(requestId);
                savedVenue = venueRepository.update(existing);
                logger.info("Updated venue. venueId: {}", savedVenue.getId());
            } else {
                logger.info("Venue ID provided but not found, creating new. id: {}", requestId);
                savedVenue = venueRepository.save(buildVenueFromRequest(request));
                logger.info("Created venue. venueId: {}", savedVenue.getId());
            }
        } else {
            logger.info("Creating new venue (no id)");
            savedVenue = venueRepository.save(buildVenueFromRequest(request));
            logger.info("Created venue. venueId: {}", savedVenue.getId());
        }

        if (!isUpdate && request.getPartnerId() != null && !request.getPartnerId().trim().isEmpty()) {
            try {
                partnerRepository.addVenueToPartner(request.getPartnerId().trim(), savedVenue.getId());
                logger.info("Added venue to partner. partnerId: {}, venueId: {}",
                        request.getPartnerId(), savedVenue.getId());
            } catch (Exception e) {
                logger.error("Failed to update partner venues. partnerId: {}, venueId: {}",
                        request.getPartnerId(), savedVenue.getId(), e);
            }
        }

        return new VenueUpsertResult(savedVenue, isUpdate);
    }

    private static void validateCreateRequest(CreateVenueRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Venue name is required");
        }
        if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Venue location is required");
        }
        if (request.getPartnerId() == null || request.getPartnerId().trim().isEmpty()) {
            throw new IllegalArgumentException("Partner ID is required. Please provide ");
        }
        if (request.getPartnerMobileNo() == null || request.getPartnerMobileNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Partner mobile no is required. Please provide");
        }
        if (request.getPhotos() != null && request.getPhotos().size() > 8) {
            throw new IllegalArgumentException("Maximum 3 photos allowed");
        }
    }

    private static Venue buildVenueFromRequest(CreateVenueRequest request) {
        Venue venue = new Venue(
                request.getName(),
                request.getDescription(),
                request.getGames(),
                request.getLocation(),
                request.getPhotos(),
                request.getPartnerId(),
                request.getCity(),
                request.getPartnerMobileNo(),
                request.getQrCodeImage(),
                request.getUpiId(),
                request.getAmenities(),
                request.getThumbnailUrl()
        );
        applyExtendedVenueFields(venue, request);
        return venue;
    }

    private static void copyRequestOntoVenue(Venue venue, CreateVenueRequest request) {
        venue.setName(request.getName());
        venue.setDescription(request.getDescription());
        venue.setGames(request.getGames());
        venue.setLocation(request.getLocation());
        venue.setPhotos(request.getPhotos());
        venue.setPartnerId(request.getPartnerId());
        venue.setCity(request.getCity());
        venue.setPartnerMobileNo(request.getPartnerMobileNo());
        venue.setQrCodeImage(request.getQrCodeImage());
        venue.setUpiId(request.getUpiId());
        venue.setAmenities(request.getAmenities());
        venue.setVenueMode(request.getVenueMode() != null ? request.getVenueMode() : "manual");
        venue.setId(request.getId());
        venue.setOperatingHoursDisplay(request.getOperatingHoursDisplay());
        venue.setMinPrice(request.getMinPrice());
        venue.setMaxPrice(request.getMaxPrice());
        venue.setOpenNow(request.isOpenNow());
    }

    private static void applyExtendedVenueFields(Venue venue, CreateVenueRequest request) {
        venue.setVenueMode(request.getVenueMode() != null ? request.getVenueMode() : "manual");
        venue.setOperatingHoursDisplay(request.getOperatingHoursDisplay());
        venue.setMinPrice(request.getMinPrice());
        venue.setMaxPrice(request.getMaxPrice());
        venue.setOpenNow(request.isOpenNow());
    }

    public List<Venue> getVenues(String city, String sport, String partnerId, String id) {
        Query query = new Query();

        if (city != null && !city.trim().isEmpty()) {
            query.addCriteria(Criteria.where("city").is(city.trim()));
        }

        if (sport != null && !sport.trim().isEmpty()) {
            query.addCriteria(Criteria.where("games").in(sport.trim()));
        }

        if (partnerId != null && !partnerId.trim().isEmpty()) {
            query.addCriteria(Criteria.where("partnerId").is(partnerId.trim()));
        }

        if (id != null && !id.trim().isEmpty()) {
            String venueId = id.trim();
            try {
                new ObjectId(venueId);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid venue ID format: " + venueId);
            }
            query.addCriteria(Criteria.where("_id").is(new ObjectId(venueId)));
        }

        return venueRepository.findByQuery(query);
    }

    public List<Venue> getVenuesByCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City is required");
        }
        return getVenues(city, null, null, null);
    }

    public Venue getVenueById(String id) {
        return venueRepository.findById(id);
    }
}

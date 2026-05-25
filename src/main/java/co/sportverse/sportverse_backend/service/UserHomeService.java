package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.dto.home.HeroSectionDto;
import co.sportverse.sportverse_backend.dto.home.HomeVenueLiteDto;
import co.sportverse.sportverse_backend.dto.home.OffersBannerDto;
import co.sportverse.sportverse_backend.dto.home.PartnerBannerDto;
import co.sportverse.sportverse_backend.dto.home.SportsCategoryDto;
import co.sportverse.sportverse_backend.dto.home.UpcomingBookingDto;
import co.sportverse.sportverse_backend.dto.home.UserHomeScreenDto;
import co.sportverse.sportverse_backend.entity.Venue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the user home screen payload. Business rules (venues, booking, personalization) go here later.
 */
@Service
public class UserHomeService {

    private static final Logger logger = LoggerFactory.getLogger(UserHomeService.class);

    @Autowired
    private VenueService venueService;

    /**
     * @param jwtSubject stable user identifier from JWT (phone / normalized id), for future personalization
     */
    public UserHomeScreenDto buildHomeScreen(String jwtSubject) {
        logger.debug("Home screen skeleton for subject {}", jwtSubject);
        UserHomeScreenDto dto = new UserHomeScreenDto();

        dto.setHeroSection(new HeroSectionDto("Ready to book your next game?", "", "", "Delhi"));

        List<SportsCategoryDto> sportsCategoryDtos = new ArrayList<>();

        SportsCategoryDto cricket = new SportsCategoryDto();
        cricket.setName("Cricket");
        sportsCategoryDtos.add(cricket);

        SportsCategoryDto football = new SportsCategoryDto();
        football.setName("Football");
        sportsCategoryDtos.add(football);

        SportsCategoryDto badminton = new SportsCategoryDto();
        badminton.setName("Badminton");
        sportsCategoryDtos.add(badminton);

        SportsCategoryDto swimming = new SportsCategoryDto();
        swimming.setName("Swimming");
        sportsCategoryDtos.add(swimming);

        dto.setSportsCategories(sportsCategoryDtos);

        List<Venue> venues = venueService.listTrendingVenues();
        List<HomeVenueLiteDto> trendingLite = toLiteVenues(venues);
        dto.setTrendingVenues(trendingLite);


        UpcomingBookingDto upcoming = new UpcomingBookingDto();
        upcoming.setExists(false);
        dto.setUpcomingBooking(upcoming);

        OffersBannerDto offers = new OffersBannerDto();
        offers.setEnabled(false);
        dto.setOffersBanner(offers);

        dto.getWhyChooseSportVerse().clear();

        PartnerBannerDto partner = new PartnerBannerDto();
        partner.setEnabled(true);
        partner.setTitle("Have a venue?");
        partner.setSubtitle("Partner with SportVerse and grow your business with us.");
        partner.setCtaText("Know more ->");
        dto.setPartnerBanner(partner);

        return dto;
    }

    private static List<HomeVenueLiteDto> toLiteVenues(List<Venue> venues) {
        if (venues == null || venues.isEmpty()) {
            return Collections.emptyList();
        }
        return venues.stream()
                .filter(v -> v != null && v.getId() != null)
                .map(UserHomeService::toLiteVenue)
                .collect(Collectors.toList());
    }

    /** Maps a persisted {@link Venue} to the home API lite shape (ratings/tags filled by business rules later). */
    private static HomeVenueLiteDto toLiteVenue(Venue venue) {
        HomeVenueLiteDto lite = new HomeVenueLiteDto();
        lite.setVenueId(venue.getId());
        lite.setName(venue.getName());
        lite.setCity(venue.getCity());
        lite.setLocation(venue.getLocation());
        lite.setSport(firstGameLabel(venue));
        lite.setThumbnailUrl(venue.getThumbnailUrl());
        lite.setRating(null);
        lite.setOpenNow(venue.isOpenNow());
        lite.setStartingPrice(venue.getMinPrice());
        List<String> tags = new ArrayList<>();
        tags.add("Trending");
        lite.setTags(tags);
        return lite;
    }

    private static String firstGameLabel(Venue venue) {
        List<String> games = venue.getGames();
        if (games == null || games.isEmpty()) {
            return null;
        }
        String first = games.get(0);
        return first != null && !first.isBlank() ? first.trim() : null;
    }
}

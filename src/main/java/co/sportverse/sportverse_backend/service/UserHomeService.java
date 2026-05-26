package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.dto.VenueResponse;
import co.sportverse.sportverse_backend.dto.home.HeroSectionDto;
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
        List<VenueResponse> trendingVenues = toVenueResponses(venues);
        dto.setTrendingVenues(trendingVenues);


        UpcomingBookingDto upcoming = new UpcomingBookingDto();
        upcoming.setExists(true);
        upcoming.setAmount(1000);
        upcoming.setDate("27-05-2026");
        upcoming.setSlotStartsAt("7 AM");
        upcoming.setSlotEndsAt("7 PM");
        upcoming.setVenueName("The Sports Arena");
        upcoming.setStatus("CONFIRMED");
        dto.setUpcomingBooking(upcoming);

        OffersBannerDto offers = new OffersBannerDto();
        offers.setEnabled(false);
        dto.setOffersBanner(offers);

        dto.getWhyChooseSportVerse().clear();

        PartnerBannerDto partner = new PartnerBannerDto();
        partner.setEnabled(true);
        partner.setTitle("Have a venue?");
        partner.setSubtitle("Partner with SportVerse and grow your business with us.");
        partner.setCtaText("Know more");
        dto.setPartnerBanner(partner);

        return dto;
    }

    private static List<VenueResponse> toVenueResponses(List<Venue> venues) {
        if (venues == null || venues.isEmpty()) {
            return Collections.emptyList();
        }
        return venues.stream()
                .filter(v -> v != null && v.getId() != null)
                .map(VenueResponse::new)
                .collect(Collectors.toList());
    }
}

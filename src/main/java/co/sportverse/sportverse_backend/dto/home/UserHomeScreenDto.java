package co.sportverse.sportverse_backend.dto.home;

import co.sportverse.sportverse_backend.dto.VenueResponse;

import java.util.ArrayList;
import java.util.List;

/** Payload under {@link co.sportverse.sportverse_backend.dto.ApiResponse#getData()} for home screen. */
public class UserHomeScreenDto {

    private HeroSectionDto heroSection;
    private List<SportsCategoryDto> sportsCategories = new ArrayList<>();
    private List<VenueResponse> recommendedVenues = new ArrayList<>();
    private List<VenueResponse> trendingVenues = new ArrayList<>();
    private List<NearbyVenueCardDto> nearbyVenues = new ArrayList<>();
    private UpcomingBookingDto upcomingBooking;
    private OffersBannerDto offersBanner;
    private List<WhyChooseItemDto> whyChooseSportVerse = new ArrayList<>();
    private PartnerBannerDto partnerBanner;

    public HeroSectionDto getHeroSection() {
        return heroSection;
    }

    public void setHeroSection(HeroSectionDto heroSection) {
        this.heroSection = heroSection;
    }

    public List<SportsCategoryDto> getSportsCategories() {
        return sportsCategories;
    }

    public void setSportsCategories(List<SportsCategoryDto> sportsCategories) {
        this.sportsCategories = sportsCategories != null ? sportsCategories : new ArrayList<>();
    }

    public List<VenueResponse> getRecommendedVenues() {
        return recommendedVenues;
    }

    public void setRecommendedVenues(List<VenueResponse> recommendedVenues) {
        this.recommendedVenues = recommendedVenues != null ? recommendedVenues : new ArrayList<>();
    }

    public List<VenueResponse> getTrendingVenues() {
        return trendingVenues;
    }

    public void setTrendingVenues(List<VenueResponse> trendingVenues) {
        this.trendingVenues = trendingVenues != null ? trendingVenues : new ArrayList<>();
    }

    public List<NearbyVenueCardDto> getNearbyVenues() {
        return nearbyVenues;
    }

    public void setNearbyVenues(List<NearbyVenueCardDto> nearbyVenues) {
        this.nearbyVenues = nearbyVenues != null ? nearbyVenues : new ArrayList<>();
    }

    public UpcomingBookingDto getUpcomingBooking() {
        return upcomingBooking;
    }

    public void setUpcomingBooking(UpcomingBookingDto upcomingBooking) {
        this.upcomingBooking = upcomingBooking;
    }

    public OffersBannerDto getOffersBanner() {
        return offersBanner;
    }

    public void setOffersBanner(OffersBannerDto offersBanner) {
        this.offersBanner = offersBanner;
    }

    public List<WhyChooseItemDto> getWhyChooseSportVerse() {
        return whyChooseSportVerse;
    }

    public void setWhyChooseSportVerse(List<WhyChooseItemDto> whyChooseSportVerse) {
        this.whyChooseSportVerse = whyChooseSportVerse != null ? whyChooseSportVerse : new ArrayList<>();
    }

    public PartnerBannerDto getPartnerBanner() {
        return partnerBanner;
    }

    public void setPartnerBanner(PartnerBannerDto partnerBanner) {
        this.partnerBanner = partnerBanner;
    }
}

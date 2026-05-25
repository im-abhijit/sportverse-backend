package co.sportverse.sportverse_backend.dto.home;

/**
 * Home hero block; copy and assets to be filled by business logic later.
 */
public class HeroSectionDto {
    private String title;
    private String subtitle;
    private String backgroundImage;
    private String city;

    public HeroSectionDto() {}

    public HeroSectionDto(String title, String subtitle, String backgroundImage, String city) {
        this.title = title;
        this.subtitle = subtitle;
        this.backgroundImage = backgroundImage;
        this.city = city;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}

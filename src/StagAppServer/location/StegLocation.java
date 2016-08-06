package StagAppServer.location;

public class StegLocation {
    public static final String NO_TITLE = "clear_title";
    private final Integer id;
    private final Integer stegId;

    private String profileId;
    private final Double latitude;
    private final Double longitude;
    private String city = "clear";
    private String state = "clear";
    private Integer areaMask;
    private String title;
    private Integer type = 0;

    public StegLocation(Integer id, Integer stegId, Double latitude, Double longitude) {
        this.id = id;
        this.stegId = stegId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.title = NO_TITLE;
    }

    public Integer getId() {
        return id;
    }

    public Integer getStegId() {
        return stegId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getAreaMask() {
        return areaMask;
    }

    public void setAreaMask(Integer areaMask) {
        this.areaMask = areaMask;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }
}

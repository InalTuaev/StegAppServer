package StagAppServer.location;


public class PrizeLocation {
    public static final String NO_TITLE = "clear_title";
    private final Integer id;
    private final Integer stegId;

    private String winnerId;
    private Boolean won = false;
    private final Double latitude;
    private final Double longitude;
    private final String title;
    private final Integer value;


    public PrizeLocation(Integer id, Integer stegId, Double latitude, Double longitude, String title, Integer value) {
        this.id = id;
        this.stegId = stegId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.title = title;
        this.value = value;
    }

    public PrizeLocation(Integer id, Integer stegId, String winnerId, Double latitude, Double longitude, String title, Integer value) {
        this.id = id;
        this.stegId = stegId;
        this.winnerId = winnerId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.title = title;
        this.value = value;
        won = true;
    }

    public Integer getId() {
        return id;
    }

    public Integer getStegId() {
        return stegId;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
        this.won = true;
    }

    public Boolean isWon() {
        return won;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getTitle() {
        return title;
    }

    public Integer getValue() {
        return value;
    }
}

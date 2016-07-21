package StagAppServer.location;


public class UserLocation {
    private final String profileId;
    private final Double longitude;
    private final Double latitude;

    public UserLocation(String profileId, Double longitude, Double latitude){
        this.profileId = profileId;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getProfileId(){
        return profileId;
    }

    public Double getLongitude(){
        return longitude;
    }

    public Double getLatitude(){
        return latitude;
    }
}

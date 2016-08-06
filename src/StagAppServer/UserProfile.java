package StagAppServer;

public class UserProfile {

	public static final String NO_VALUE = "clear";
	public static final String MALE = "man";
	public static final String FEMALE = "woman";
	public static final String ANONYM = "anonym";

	private String userId;
	private String userName;
	private String userSex;
	private String userState;
	private String userCity;
	private Integer userAge;
	private String userPhoto;
	private Boolean isFriend;
	private Double latitude;
	private Double longitude;

	public UserProfile(String userId){
		this.userId = userId;
		userName = "clear";
		userSex = "clear";
		userState = "clear";
		userCity = "clear";
		userAge = -1;
		userPhoto = "clear";
		isFriend = false;
		latitude = 0.0;
		longitude = 0.0;
	}

	public UserProfile(){
		userId = "clear";
		userName = "clear";
		userSex = "clear";
		userState = "clear";
		userCity = "clear";
		userAge = -1;
		userPhoto = "clear";
		isFriend = false;
		latitude = 0.0;
		longitude = 0.0;
	}
	
	public void setId(String userId){
		this.userId = userId;
	}
	public void setName(String userName){
		this.userName = userName;
	}
	public void setSex(String userSex){
		this.userSex = userSex;
	}
	public void setState(String userState){
		this.userState = userState;
	}
	public void setCity(String userCity){
		this.userCity = userCity;
	}
	public void setAge(Integer userAge){
		this.userAge = userAge;
	}
	public void setPhoto(String userPhoto){
		this.userPhoto = userPhoto;
	}
	public void setIsFriend(Boolean isFriend){
		this.isFriend = isFriend;
	}

	public String getId(){
		return userId;
	}
	public String getName(){
		return userName;
	}
	public String getSex(){
		return userSex;
	}
	public String getState(){
		return userState;
	}
	public String getCity(){
		return userCity;
	}
	public Integer getAge(){
		return userAge;
	}
	public String getPhoto(){
		return userPhoto;
	}
	public Boolean getIsFriend(){
		return isFriend;
	}

	public void setCoordinates(Double longitude, Double latitude){
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public Double getLatitude(){
		return latitude;
	}

	public Double getLongitude(){
		return longitude;
	}
}

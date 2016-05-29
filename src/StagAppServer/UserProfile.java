package StagAppServer;

class UserProfile {
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
	
	UserProfile(String userId){
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

	UserProfile(){
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
	
	void setId(String userId){
		this.userId = userId;
	}
	void setName(String userName){
		this.userName = userName;
	}
	void setSex(String userSex){
		this.userSex = userSex;
	}
	void setState(String userState){
		this.userState = userState;
	}
	void setCity(String userCity){
		this.userCity = userCity;
	}
	void setAge(Integer userAge){
		this.userAge = userAge;
	}
	void setPhoto(String userPhoto){
		this.userPhoto = userPhoto;
	}
	void setIsFriend(Boolean isFriend){
		this.isFriend = isFriend;
	}
	
	String getId(){
		return userId;
	}
	String getName(){
		return userName;
	}
	String getSex(){
		return userSex;
	}
	String getState(){
		return userState;
	}
	String getCity(){
		return userCity;
	}
	Integer getAge(){
		return userAge;
	}
	String getPhoto(){
		return userPhoto;
	}
	Boolean getIsFriend(){
		return isFriend;
	}
	
	void setCoordinates(Double longitude, Double latitude){
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	Double getLatitude(){
		return latitude;
	}
	
	Double getLongitude(){
		return longitude;
	}
}

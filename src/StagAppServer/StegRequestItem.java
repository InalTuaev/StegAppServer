package StagAppServer;

class StegRequestItem {
	private Integer stegId;
	private String city;
	private String senderName;

	
	StegRequestItem(Integer stegId, String city, String senderName){
		this.stegId = stegId;
		this.city = city;
		this.senderName = senderName;
	}

	Integer getStegId(){
		return stegId;
	}

	String getCity(){
		return city;
	}

	String getSenderName(){
		return senderName;
	}

}

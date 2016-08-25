package StagAppServer.dataClasses;

public class StegRequestItem {
	private Integer stegId;
	private String city;
	private String senderName;
	private Integer stegMode;

	
	public StegRequestItem(Integer stegId, String city, String senderName){
		this.stegId = stegId;
		this.city = city;
		this.senderName = senderName;
		stegMode = StagData.STEG_MODE;
	}

	public Integer getStegId(){
		return stegId;
	}

	public String getCity(){
		return city;
	}

	public String getSenderName(){
		return senderName;
	}

	public Integer getStegMode() {
		return stegMode;
	}

	public void setStegMode(Integer stegMode) {
		this.stegMode = stegMode;
	}
}

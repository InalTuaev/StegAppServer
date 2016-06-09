package StagAppServer;

import java.sql.Date;
import java.sql.Time;

public class StagData {
	public static final int STEG_AREA_MASK_EVERYWHERE = 0;
	public static final int STEG_AREA_MASK_NEAR = 1;
	public static final int STEG_AREA_MASK_CITY = 2;
	public static final int STEG_AREA_MASK_STATE = 4;
	public static final int STEG_AREA_MASK = 7;
	
	public static final int STEG_SEX_MASK_ALL = 0;
	public static final int STEG_SEX_MASK_MAN = 64;
	public static final int STEG_SEX_MASK_WOMAN = 128;
	public static final int STEG_SEX_MASK = 192;
	
		public Integer stegId;
	  	public String mesType;
	    public String mesSender;
	    public String senderName;
	    public String mesReciever;
	    public Integer stagType;
	    public Integer lifeTime;
	    public Boolean anonym;
	    public Integer filter;
	    public String mesText;
	    public String voiceDataFile;
	    public String cameraDataFile;
	    public Integer comments;
	    public Integer likes;
	    public Integer gets;
	    public Integer saves;
	    public Date date;
	    public Time time;
	    public Boolean sended;
	    public Boolean liked;
	private Boolean isActive;

	    public  StagData(){
	    	stegId = -1;
	        mesType = "stag";
	        mesSender = "clear";
	        mesReciever = "clear";
	        stagType = -1;
	        lifeTime = -1;
	        anonym = false;
	        filter =-1;
	        mesText = "clear";
	        voiceDataFile = "clear";
	        cameraDataFile ="clear";
	        comments = 0;
	        likes = 0;
	        gets = 0;
	        saves = 0;
	        sended = false;
	        liked = false;

	    }
	    public void setStegId(Integer stegId){
	    	this.stegId = stegId;
	    }
	    public void setMesType(String mesType) {
	        this.mesType = mesType;
	    }
	    public void setMesSender(String mesSender) {
	        this.mesSender = mesSender;
	    }
	    public void setMesReciever(String mesReciever) {
	        this.mesReciever = mesReciever;
	    }
	    public void setMesText(String mesText) {
	        this.mesText = mesText;
	    }
	    public void setStagType(Integer stagType) {
	        this.stagType = stagType;
	    }
	    public void setVoiceDataPath(String voiceDataFile) {
	        this.voiceDataFile = voiceDataFile;
	    }
	    public void setCameraDataPath (String cameraDataFile){
	    	this.cameraDataFile = cameraDataFile;
	    }
	    public void setLifeTime(Integer lifeTime){
	        this.lifeTime = lifeTime;
	    }
	    public void setAnonym(Boolean anonym) {
	        this.anonym = anonym;
	    }
	    public void setFilter(Integer filter){
	        this.filter = filter;
	    }

	void setIsActive(Boolean value){
		isActive = value;
	}

	Boolean isActive(){
		return isActive;
	}
}
       
package StagAppServer;

import java.sql.Date;
import java.sql.Time;

public class NewsData {
	Integer id;
	String type;
	String ownerId;
	String profileId;
	String profileName;
	String profileImg;
	Integer stegId;
	String stegImg;
	Date date;
	Time time;
	Boolean sended;
	
	public NewsData(){
		stegImg = "clear";
		profileImg = "clear";
		sended = false;
	}
}

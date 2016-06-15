package StagAppServer;

import java.sql.Date;
import java.sql.Time;

class NewsData {

	public static final String NOTIFICATION_TYPE_COMMENT = "comment";
	public static final String NOTIFICATION_TYPE_LIKE = "like";
	public static final String NOTIFICATION_TYPE_GET = "get";
	public static final String NOTIFICATION_TYPE_SAVE = "save";
	public static final String NOTIFICATION_TYPE_FRIEND = "friend";
	public static final String NOTIFICATION_TYPE_PRIVATE_STEG = "privateSteg";

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

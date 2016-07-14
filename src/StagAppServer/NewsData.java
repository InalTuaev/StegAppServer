package StagAppServer;

import java.sql.Date;
import java.sql.Time;

class NewsData {
	public static final int NOTIFICATION_FRIEND = 0;
	public static final int NOTIFICATION_COMMENT = 1;
	public static final int NOTIFICATION_LIKE = 2;
	public static final int NOTIFICATION_GET = 3;
	public static final int NOTIFICATION_SAVE = 4;
	public static final int NOTIFICATION_PRIVATE_STEG = 5;
	public static final int NOTIFICATION_COMMENT_LIKE = 6;
	public static final int NOTIFICATION_FAVORITE_STEG_COMMENT = 7;


	public static final String NOTIFICATION_TYPE_COMMENT = "comment";
	public static final String NOTIFICATION_TYPE_LIKE = "like";
	public static final String NOTIFICATION_TYPE_GET = "get";
	public static final String NOTIFICATION_TYPE_SAVE = "save";
	public static final String NOTIFICATION_TYPE_FRIEND = "friend";
	public static final String NOTIFICATION_TYPE_PRIVATE_STEG = "privateSteg";
	public static final String NOTIFICATION_TYPE_COM_LIKE = "commentLike";
	public static final String NOTIFICATION_TYPE_FAV_COMMENT = "favComment";

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

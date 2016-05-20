package StagAppServer;

import java.sql.Date;
import java.sql.Time;

class LikeData {
	public Integer id;
	public Integer stegId;
	public String profileId;
	public String profileName;
	public String profileImg;
	public Date date;
	public Time time;
	
	public LikeData(){
		id = -1;
		stegId = -1;
		profileId = "clear";
		profileName = "clear";
		profileImg = "clear";
	}
}

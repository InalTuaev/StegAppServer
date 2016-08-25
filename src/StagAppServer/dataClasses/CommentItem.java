package StagAppServer.dataClasses;

import java.sql.Date;
import java.sql.Time;

public class CommentItem {
	private Integer id;
	private Integer stegId;
	private Integer type;
	private String profileId;
	private String text;
	private Date date;
	private Time time;
	private Boolean loaded;
	private CommentData comment;
	
	
	public CommentItem(){
		this.loaded = false;
	}
	
	public void setId(Integer id){
		this.id = id;
	}
	
	public void setStegId(Integer stegId){
		this.stegId = stegId;
	}
	
	public void setType(Integer type){
		this.type = type;
	}
	
	public void setProfileId(String profileId){
		this.profileId = profileId;
	}
	
	public void setText(String text){
		this.text = text;
	}
	
	public void setDate(Date date){
		this.date = date;
	}
	
	public void setTime(Time time){
		this.time = time;
	}
	
	public void setComment(CommentData comment){
		this.comment = comment;
		this.loaded = true;
	}
	
	public Integer getId(){
		return this.id;
	}
	
	public Integer getStegId(){
		return stegId;
	}
	
	public Integer getType(){
		return type;
	}
	
	public String getProfileId(){
		return profileId;
	}
	
	public String getText(){
		return text;
	}
	
	public Date getDate(){
		return date;
	}
	
	public Time getTime(){
		return time;
	}
	
	public Boolean isLoaded(){
		return loaded;
	}
	
	public CommentData getComment(){
		if(loaded)
			return comment;
		else 
			return null;
	}
	
}

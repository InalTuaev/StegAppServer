package StagAppServer;

import java.util.HashMap;

public class StegItem {
	private Integer stegId;
	private String mesSender;
	private StagData steg;
	private Boolean loaded;
	private Boolean anonym;
	private Integer likes;
	private Integer comments;
	private Boolean liked;
	public HashMap <String, UserProfile> recieverIds;
	private Integer recieverCount;
	
	public StegItem(Integer stegId, String mesSender, Boolean anonym){
		this.stegId = stegId;
		this.mesSender = mesSender;
		loaded = false;
		this.anonym = anonym;
		this.likes = 0;
		this.comments = 0;
		this.liked = false;
		recieverIds = new HashMap<>();
		recieverCount = 0;
	}
	
	public Integer getStegId(){
		return stegId;
	}

	public String getMesSender(){
		return mesSender;
	}
	
	public Boolean isLoaded(){
		return loaded;
	}
	
	public StagData getSteg(){
		if(loaded)
			return steg;
		return null;
	}
	
	public Integer getRecieverCount(){
		return recieverCount;
	}
	
	public void setRecieverCount(Integer count){
		this.recieverCount = count;
	}
	
	public Boolean isAnonym(){
		return anonym;
	}
	
	public void setSteg(StagData steg){
		this.steg = steg;
		this.loaded = true;
		this.anonym = steg.anonym;
		this.likes = steg.likes;
		this.liked = steg.liked;
		this.comments = steg.comments;
	}
	
	public void refreshValues(StegItem stegItem){
		setLikes(stegItem.likes);
		setComments(stegItem.comments);
		setLiked(stegItem.liked);
	}
	
	public void setLikes(Integer likes){
		this.likes = likes;
		if(steg !=  null){
			steg.likes = likes;
		}
	}
	
	public void setLiked(Boolean liked){
		this.liked = liked;
		if(steg !=  null){
			steg.liked = liked;
		}
	}
	
	public void setComments(Integer comments){
		this.comments = comments;
		if(steg !=  null){
			steg.comments = comments;
		}
	}
	
	public Integer getLikes(){
		return this.likes;
	}
	
	public Integer getComments(){
		return this.comments;
	}
	
	public Boolean isLiked(){
		return this.liked;
	}
}

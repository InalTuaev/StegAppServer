package StagAppServer;

import java.sql.Date;
import java.sql.Time;

public class CommentData {
	public static final int COMMENT_TEXT_MASK = 1;
	public static final int COMMENT_IMAGE_MASK = 2;
	public static final int COMMENT_VIDEO_MASK = 4;
	public static final int COMMENT_VOICE_MASK = 8;
	public static final int COMMENT_MEDIA_CONTENT_MASK = 14;
	
	public Integer id;
	public Integer stegId;
	public String profileId;
	private String text;
	public Date date;
	public Time time;
	private Integer commentType;
	private String cameraData;
	private String voiceData;
	private Integer likesCount;
	private Boolean isLiked;

	public CommentData(){
		id = -1;
		stegId = -1;
		profileId = "clear";
		text = "clear";
		cameraData = "clear";
		voiceData = "clear";
		commentType = 0;
		likesCount = 0;
		isLiked = false;
	}

	public String getText() {
		return text;
	}

	public void setText(String text){
		if (text != null){
			this.text = text;
		}
	}

	public String getImgData(){
		if ((commentType & COMMENT_IMAGE_MASK) != 0)
			return cameraData;
		return null;
	}

	public void setImgData(String imagePath){
		if(!imagePath.equals("clear")){
			this.cameraData = imagePath;
		}
	}

	public String getVideoData(){
		if ((commentType & COMMENT_VIDEO_MASK) != 0)
			return cameraData;
		return null;
	}

	public void setVideoData(String videoPath){
		if(!videoPath.equals("clear")){
			cameraData = videoPath;
		}
	}

	public String getVoiceData(){
		if((commentType & COMMENT_VOICE_MASK) != 0)
			return voiceData;
		return null;
	}

	public void setVoiceData(String voicePath){
		if(!voicePath.equals("clear")){
			voiceData = voicePath;
		}
	}

	public int getType(){
		return commentType;
	}

	public void setType(Integer type){
		commentType = type;
	}

	public void setLikesCount(Integer count){
		likesCount = count;
	}

	public Integer getLikesCount(){
		return likesCount;
	}

	public void setIsLiked(Boolean isLiked){
		this.isLiked = isLiked;
	}

	public Boolean isLiked(){
		return isLiked;
	}
}

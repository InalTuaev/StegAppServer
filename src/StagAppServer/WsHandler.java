package StagAppServer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArraySet;
import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.OnBinaryMessage;
import org.eclipse.jetty.websocket.WebSocket.OnTextMessage;
import org.eclipse.jetty.websocket.WebSocketHandler;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

public class WsHandler extends WebSocketHandler {
	private static final String STEGAPP_IMG_DIR = "StegApp/media/img/";
	private static final String STEGAPP_IMG_T_DIR = "StegApp/media/img/thumbs/";
	private static final String STEGAPP_VIDEO_DIR = "StegApp/media/video/";
	private static final String STEGAPP_AUDIO_DIR = "StegApp/media/audio/";
	private static final String STEGAPP_PROFILE_PHOTO_DIR = "StegApp/avatars/";
	private static final String STEGAPP_PROFILE_THUMBS_DIR = "StegApp/thumbs/";
	
	public static final int NOTIFICATION_FRIEND = 0;
	public static final int NOTIFICATION_COMMENT = 1;
	public static final int NOTIFICATION_LIKE = 2;
	public static final int NOTIFICATION_GET = 3;
	public static final int NOTIFICATION_SAVE = 4;
	public static final int NOTIFICATION_PRIVATE_STEG = 5;
	
	private volatile HashSet<ChatWebSocket> clientSockets = new HashSet<ChatWebSocket>();
	private Connection dbConnection;
	
	public WsHandler(Connection dbConnection) {
		this.dbConnection = dbConnection;
		sendingStegs(dbConnection);
	}
	
	private class ChatWebSocket implements OnBinaryMessage, OnTextMessage {
			
		public String userId;
		public Connection connection;
		public UserProfile profile;
			 
		@Override
		public void onOpen(Connection arg0) {
			this.connection = arg0;
			this.connection.setMaxIdleTime(300000);
			clientSockets.add(this);
			System.out.println("connected: " + this.connection.toString());
		}
 
		@Override
		public void onClose(int arg0, String arg1) {
			clientSockets.remove(this);
			System.out.println("disconnected: " + this.connection.toString());
		}

	
		@Override
		public void onMessage(byte[] arg0, int arg1, int arg2) {
			StagData stagData;
			String mesType;
			MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(arg0, arg1, arg2);
			try {
				mesType = unpacker.unpackString();
				switch (mesType){
				case "to":
					stagData = unpackMe(unpacker, mesType);
					for(ChatWebSocket con: clientSockets) {
						if(con.userId.equals(stagData.mesReciever))
							con.connection.sendMessage(arg0, arg1, arg2);
					}
					break;
				case "registration":
					String newUserId = unpacker.unpackString();
					String newPaswd = unpacker.unpackString();
					unpacker.close();
					if(DBHandler.newUser(newUserId, newPaswd, dbConnection)){
						this.userId = newUserId;
						this.connection.sendMessage("registration_ok");
					} else {
						this.connection.sendMessage("same_name");
					}
					break;
				case "checkSocialId":
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					MessagePacker packer = MessagePack.newDefaultPacker(baos);
					String newSocId = unpacker.unpackString();
					String oldUserId = DBHandler.checkUserSocialId(newSocId, dbConnection);
					if(oldUserId == null){
						packer.packString("newSocialId");
						System.out.println("newSocialId: " + newSocId + " | userId: " + this.userId);
					} else {
						packer.packString("oldSocialId");
						packer.packString(oldUserId);
						System.out.println("oldSocialId: " + newSocId + " | userId: " + this.userId + " | old: " + oldUserId);
					}
					packer.close();
					this.connection.sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
					baos.close();
					break;
				case "socRegistration":
					String newSocUserId = unpacker.unpackString();
					String newSocPasswd = unpacker.unpackString();
					String newSocIdSoc = unpacker.unpackString();
					unpacker.close();
					if(DBHandler.newUserSocial(newSocUserId, newSocPasswd, newSocIdSoc, dbConnection)){
						this.userId = newSocUserId;
						this.connection.sendMessage("registration_ok");
					} else {
						this.connection.sendMessage("same_name");
					}
					break;
				case "signIn":
					String userId = unpacker.unpackString();
					String paswd = unpacker.unpackString();
					unpacker.close();
					if (DBHandler.signIn(userId, paswd, dbConnection)){
						this.userId = userId;
						this.connection.sendMessage("enter");
						this.profile = DBHandler.getUserProfile(userId, dbConnection);
					} else {
						this.connection.sendMessage("not_registered");
					}
					break;
				case "stag":
					stagData = unpackMe(unpacker, mesType);
					DBHandler.addSteg(stagData, dbConnection);
					break;
				case "profileFromServer":
					String profileId = unpacker.unpackString();
					unpacker.close();
					UserProfile sendProfile = DBHandler.getUserProfile(profileId, dbConnection);
					sendProfile(sendProfile, this);
					break;
				case "addStegToWall":
					addStegToWall(unpacker);
					break;
				case "addLike":
					addLike(unpacker);
					break;
				case "addComment":
					addComment(unpacker);
					break;
				case "addTextComment":
					addTextComment(unpacker);
					break;
				case "setStegUnsended":
					setStegUnsended(unpacker);
					break;
				case "addGeter":
					addGeter(unpacker);
					break;
				case "addFriend":
					addFriend(unpacker);
					break;
				case "removeFriend": 
					removeFriend(unpacker);
					break;
				case "markNewsSended":
					markNewsSended(unpacker);
					break;
				case "markAllStegNotificationsSended":
					markAllStegNoificationsSended(unpacker);
					break;
				case "markPrivateStegSended":
					markPrivateStegSended(unpacker);
					break;
				case "rejectCommonSteg":
					rejectCommonSteg(unpacker);
					break;
				case "deleteSteg":
					deleteSteg(unpacker);
					break;
				case "deleteIncomeSteg":
					deleteIncomeSteg(unpacker);
					break;
				case "deleteStegAdm":
					deleteStegAdm(unpacker);
					break;
				case "removeStegFromWall":
					removeStegFromWall(unpacker);
					break;
				case "removeLike":
					removeLikes(unpacker);
					break;
				case "stegPlea":
					stegPlea(unpacker);
					break;
				case "profileSearch":
					profileSearch(unpacker);
					break;
				case "getSubscribers":
					getSubscribers(unpacker);
					break;
				case "getFriends":
					getFriends(unpacker);
					break;
				case "addToBL":
					addToBlackList(unpacker);
					break;
				case "removeFromBL":
					removeFromBlackList(unpacker);
					break;
				case "setCoordinates":
					setCoordinates(unpacker);
					break;
				case "stegRequest":
					stegRequest(unpacker);
					break;
				}
			unpacker.close();
			} catch (IOException e){
				e.printStackTrace();
			}
		}
		
		public StagData unpackMe (MessageUnpacker unpacker, String mesType) throws IOException {
			StagData stagData = new StagData();
			stagData.mesType = mesType;
			switch (stagData.mesType) {
			case "stag":
				stagData.mesSender = unpacker.unpackString();
				stagData.mesReciever = unpacker.unpackString();
				stagData.stagType = unpacker.unpackInt();
				stagData.lifeTime = unpacker.unpackInt();
				stagData.anonym = unpacker.unpackBoolean();
				stagData.filter = unpacker.unpackInt();
				if ((stagData.stagType & 1) != 0) { // Text
					stagData.mesText = unpacker.unpackString();
				}
				if((stagData.stagType & 8) != 0) {
					stagData.voiceDataFile = unpacker.unpackString();
				}
				if (((stagData.stagType & 2) != 0) || ((stagData.stagType & 4) != 0)) { //photo or video
					stagData.cameraDataFile = unpacker.unpackString();
				}
				unpacker.close();
				break;
			}
			return stagData;
		}

		public void addStegToWall(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String profileId = unpacker.unpackString();
			DBHandler.stegToWall(stegId, profileId, dbConnection);
			
			String toUserId = DBHandler.getStegSenderId(stegId, dbConnection);
			sendNotification(NOTIFICATION_SAVE, toUserId, profileId);
		}
		
		public void stegRequest(MessageUnpacker unpacker) throws IOException{
			String profileId = unpacker.unpackString();
			StegRequestItem srItem = DBHandler.stegRequset(profileId, dbConnection);
			
			sendStegItem(srItem, this);
		}
		
		public void addLike(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String profileId = unpacker.unpackString();
			if(DBHandler.addLike(stegId, profileId, dbConnection)){
				String toUserId = DBHandler.getStegSenderId(stegId, dbConnection);
				if(!toUserId.equals(profileId)){
					sendNotification(NOTIFICATION_LIKE, toUserId, profileId);
				}
			}
		}
		
		public void addComment(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String profileId = unpacker.unpackString();
			String text = unpacker.unpackString();
			DBHandler.addComment(stegId, profileId, text, dbConnection);
			String toUserId = DBHandler.getStegSenderId(stegId, dbConnection);
			if(!toUserId.equals(profileId)){
				sendNotification(NOTIFICATION_COMMENT, toUserId, profileId);
			}
		}
		
		public void addTextComment(MessageUnpacker unpacker) throws IOException{
			CommentData comment = new CommentData();
			comment.stegId = unpacker.unpackInt();
			comment.profileId = unpacker.unpackString();
			Integer commentType = unpacker.unpackInt();
			if((commentType & CommentData.COMMENT_TEXT_MASK) != 0){
				comment.setText(unpacker.unpackString());
			}
			if(((commentType & CommentData.COMMENT_IMAGE_MASK) != 0)||((commentType & CommentData.COMMENT_VIDEO_MASK) != 0)){
			}
			if((commentType & CommentData.COMMENT_VOICE_MASK) != 0){
			}
			comment.setType(commentType);
			DBHandler.addComment(comment, dbConnection);
			String toUserId = DBHandler.getStegSenderId(comment.stegId, dbConnection);
			if(!toUserId.equals(comment.profileId)){
				sendNotification(NOTIFICATION_COMMENT, toUserId, comment.profileId);
			}
		}
		
		public void setStegUnsended(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			DBHandler.setStegUnrecieved(stegId, dbConnection);
		}
		
		public void addGeter(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String profileId = unpacker.unpackString();
			DBHandler.addGeter(stegId, profileId, dbConnection);
			
			String toUserId = DBHandler.getStegSenderId(stegId, dbConnection);
			sendNotification(NOTIFICATION_GET, toUserId, profileId);
		}
		
		public void addFriend(MessageUnpacker unpacker) throws IOException{
			String friendId = unpacker.unpackString();
			DBHandler.addFriend(userId, friendId, dbConnection);

			sendNotification(NOTIFICATION_FRIEND, friendId, userId);
		}
		
		public void removeFriend(MessageUnpacker unpacker) throws IOException{
			String friendId = unpacker.unpackString();
			DBHandler.removeFriend(userId, friendId, dbConnection);
		}
		
		public void addToBlackList(MessageUnpacker unpacker) throws IOException{
			String blackProfileId = unpacker.unpackString();
			DBHandler.addToBlackList(userId, blackProfileId, dbConnection);
		}
		
		public void removeFromBlackList(MessageUnpacker unpacker) throws IOException{
			String blackProfileId = unpacker.unpackString();
			DBHandler.removeFromBlackList(userId, blackProfileId, dbConnection);
		}
		
		public void setCoordinates(MessageUnpacker unpacker) throws IOException{
			DBHandler.setUserCoordinates(unpacker.unpackString(), unpacker.unpackString(), 
										unpacker.unpackDouble(), unpacker.unpackDouble(), dbConnection);
		}
		
		public void markNewsSended(MessageUnpacker unpacker) throws IOException{
			Integer newsId = unpacker.unpackInt();
			DBHandler.markNewsSended(newsId, dbConnection);
		}
		
		public void markAllStegNoificationsSended(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String ownerId = unpacker.unpackString();
			DBHandler.markAllStegNotificationsSended(ownerId, stegId, dbConnection);
		}
		
		public void markPrivateStegSended(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			DBHandler.markPrivetStegSended(stegId, dbConnection);
		}
		
		public void rejectCommonSteg(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			DBHandler.markUnrecievedSteg(stegId, dbConnection);
		}
		
		public void deleteSteg(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String profileId = unpacker.unpackString();
			DBHandler.deleteSteg(stegId, profileId, dbConnection);
		}
		
		public void deleteIncomeSteg(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String profileId = unpacker.unpackString();
			DBHandler.deleteIncomeSteg(stegId, profileId, dbConnection);
		}
		
		public void deleteStegAdm(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			DBHandler.deleteStegAdm(stegId, dbConnection);
		}
		
		public void removeStegFromWall(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String ownerId = unpacker.unpackString();
			DBHandler.removeStegFromWall(stegId, ownerId, dbConnection);
		}
		
		public void removeLikes(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String profileId = unpacker.unpackString();
			DBHandler.removeLike(stegId, profileId, dbConnection);
		}
		
		public void stegPlea(MessageUnpacker unpacker) throws IOException{
			String text;
			String eMail;
			String pleaer = unpacker.unpackString();
			Integer stegId = unpacker.unpackInt();
			Boolean isText = unpacker.unpackBoolean();
			if(isText){
				text = unpacker.unpackString();
			} else {
				text = "no plea text";
			}
			Boolean isEMail = unpacker.unpackBoolean();
			if(isEMail){
				eMail = unpacker.unpackString();
			} else {
				eMail = "no e-mail";
			}
						
			EmailSender emailSender = new EmailSender();
			String pleaText;
			pleaText = "From: " + pleaer + "\n\nSteg: " + stegId.toString() + "\n\nEmail: " + eMail + "\n\nText: " + text;
			emailSender.send("Plea", pleaText, "stegapp999@gmail.com", "stegapp999@gmail.com");
			System.out.println("plea: " + pleaText);
		}
		
		public void profileSearch(MessageUnpacker unpacker) throws IOException{
			String searchString = unpacker.unpackString();
			String myCity = unpacker.unpackString();
			ArrayList<String> searchResult = DBHandler.getProfileSearchResult(searchString, myCity, dbConnection);
			
			ByteArrayOutputStream searchResultBaos = new ByteArrayOutputStream();
			MessagePacker packer = MessagePack.newDefaultPacker(searchResultBaos);
			Integer resultSize = searchResult.size();
			packer.packString("profileSearchResult")
				.packArrayHeader(resultSize);
			for(String searchItem : searchResult){
				packer.packString(searchItem);
			}
			packer.close();
			connection.sendMessage(searchResultBaos.toByteArray(), 0, searchResultBaos.toByteArray().length);
		}
		
		public void getSubscribers(MessageUnpacker unpacker) throws IOException{
			String profileId = unpacker.unpackString();
			System.out.println("geSubscribers: " + profileId);
			ArrayList<String> subscribers = DBHandler.getSubscribers(profileId, dbConnection);
			
			ByteArrayOutputStream  subscribersStream = new ByteArrayOutputStream();
			MessagePacker packer = MessagePack.newDefaultPacker(subscribersStream);
			Integer resultSize = subscribers.size();
			packer.packString("subscribersResult");
			packer.packArrayHeader(resultSize);
			for(String item : subscribers){
				packer.packString(item);
			}
			packer.close();
			connection.sendMessage(subscribersStream.toByteArray(), 0, subscribersStream.toByteArray().length);
		}
		
		private void getFriends(MessageUnpacker unpacker) throws IOException{
			String profileId = unpacker.unpackString();
			System.out.println("getFriends: " + profileId);
			ArrayList<String> subscribers = DBHandler.getFriendsList(profileId, dbConnection);
			
			ByteArrayOutputStream  subscribersStream = new ByteArrayOutputStream();
			MessagePacker packer = MessagePack.newDefaultPacker(subscribersStream);
			Integer resultSize = subscribers.size();
			packer.packString("friendsResult");
			packer.packArrayHeader(resultSize);
			for(String item : subscribers){
				packer.packString(item);
			}
			packer.close();
			connection.sendMessage(subscribersStream.toByteArray(), 0, subscribersStream.toByteArray().length);
		}
		
		@Override
		public void onMessage(String arg0) {		
		}
	}
	
	public void sendNotification(Integer type, String toUserId, String fromUserId){
		new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					for(ChatWebSocket socket: clientSockets){
						if(socket.userId != null){
						if(socket.userId.equals(toUserId)){
							String fromUserName = DBHandler.getUserNameFromId(fromUserId, dbConnection);
							ByteArrayOutputStream profileNameBaos = new ByteArrayOutputStream();
							MessagePacker packer = MessagePack.newDefaultPacker(profileNameBaos);
							packer
								.packString("newsFromServer")
								.packInt(type)
								.packString(fromUserName);
							packer.close();
							socket.connection.sendMessage(profileNameBaos.toByteArray(), 0, profileNameBaos.toByteArray().length);
							break;
						}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public void sendingStegs(Connection dbConnection){
		
		Thread sendingThread = new Thread() {
			public void run(){
				while(!isInterrupted()){
					ArrayList<StagData> stegList = DBHandler.getUnrecievedStegs(dbConnection);
					System.out.println("stegList: " + stegList.size());
					
					for (StagData steg: stegList){
						HashSet<ChatWebSocket> sendSockets = checkWebSockets(clientSockets, steg.mesSender, steg.filter);			
						sendToSomeBody(sendSockets, steg.stegId);
					}
					try {
						sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}					
				}
			}
		};
		sendingThread.start();
	}

	public void sendProfile(UserProfile profile, ChatWebSocket webSocket) throws IOException{
		ByteArrayOutputStream profileBaos = new ByteArrayOutputStream();
		MessagePacker profilePacker = MessagePack.newDefaultPacker(profileBaos);
		profilePacker
		    .packString("profileFromServer")
		    .packString(profile.getId())
			.packString(profile.getName())
			.packString(profile.getSex())
			.packString(profile.getState())
			.packString(profile.getCity())
			.packInt(profile.getAge());
		if(!profile.getPhoto().equals("clear")){
			profilePacker.packString("photo");
			File sendFile = new File(STEGAPP_PROFILE_THUMBS_DIR + profile.getPhoto());
			profilePacker.packString(sendFile.getName().substring(sendFile.getName().lastIndexOf(".")));
			byte[] photoBytes = new byte[(int) sendFile.length()];
			FileInputStream fis = new FileInputStream(sendFile);
			fis.read(photoBytes, 0, photoBytes.length);
			fis.close();
			profilePacker.packBinaryHeader(photoBytes.length);
			profilePacker.writePayload(photoBytes, 0, photoBytes.length);							
		} else profilePacker.packString("clear");
		profilePacker.close();
		webSocket.connection.sendMessage(profileBaos.toByteArray(), 0, profileBaos.toByteArray().length);
	}
	
	public void sendStegItem(StegRequestItem srItem, ChatWebSocket webSocket) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		MessagePacker packer = MessagePack.newDefaultPacker(baos);
		packer.packString("stegRequestResult");
		packer.packInt(srItem.stegId != null ? srItem.stegId : -1);
		packer.packString(srItem.city != null ? srItem.city : "clear");
		packer.close();
		
		webSocket.connection.sendMessage(baos.toByteArray(),  0, baos.toByteArray().length);
	}

	public void sendToSomeBody(HashSet<ChatWebSocket> sendSockets, Integer stegId){
		Random rand = new Random(System.currentTimeMillis());
		System.out.println("sendsockets count: " + sendSockets.size());
		if(sendSockets.size() > 0){
		Integer i = rand.nextInt(sendSockets.size());
	
		for(ChatWebSocket con: sendSockets){
			if(i == 0){
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				MessagePacker packer = MessagePack.newDefaultPacker(baos);
				try {
					packer
						.packString("stegFromServer")
						.packInt(stegId);
					packer.close();
					
					con.connection.sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
					DBHandler.markRecievedSteg(stegId, dbConnection);
					System.out.println("Send steg: " + stegId + " to: " + con.profile.getId() + " sex: " + con.profile.getSex());
				} catch (IOException e) {
					e.printStackTrace();
				}	
				break;
			}
			i--;
		}
		}
	}
	
	public HashSet<ChatWebSocket> checkWebSockets(HashSet<ChatWebSocket> clientSockets, String ownerId, int filter){
		HashSet<ChatWebSocket> sendSockets = new HashSet<>();
		for(Iterator<ChatWebSocket> iterator = clientSockets.iterator(); iterator.hasNext();){
			ChatWebSocket con = iterator.next();
			if(con.profile != null){
				System.out.println("id: " + con.profile.getId() + " sex: " +con.profile.getSex());
				if (!con.profile.getId().equals(ownerId)){
					switch (filter & StagData.STEG_SEX_MASK){
					case StagData.STEG_SEX_MASK_MAN:
						if(con.profile.getSex().equals("man")){
							sendSockets.add(con);
						}
						break;
					case StagData.STEG_SEX_MASK_WOMAN:
						if(con.profile.getSex().equals("woman")){
							sendSockets.add(con);
						}
						break;
					default:
						sendSockets.add(con);
						break;
					}
				}
			}
		}
		return sendSockets;
	}
	
	@Override
	public WebSocket doWebSocketConnect(HttpServletRequest arg0, String arg1) {
		return new ChatWebSocket();
	}
}

package StagAppServer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

class WsHandler extends WebSocketHandler {
	private static final String STEGAPP_IMG_DIR = "StegApp/media/img/";
	private static final String STEGAPP_IMG_T_DIR = "StegApp/media/img/thumbs/";
	private static final String STEGAPP_VIDEO_DIR = "StegApp/media/video/";
	private static final String STEGAPP_AUDIO_DIR = "StegApp/media/audio/";
	private static final String STEGAPP_PROFILE_PHOTO_DIR = "StegApp/avatars/";
	private static final String STEGAPP_PROFILE_THUMBS_DIR = "StegApp/thumbs/";

    private static final int NOTIFICATION_FRIEND = 0;
    private static final int NOTIFICATION_COMMENT = 1;
    private static final int NOTIFICATION_LIKE = 2;
    private static final int NOTIFICATION_GET = 3;
    private static final int NOTIFICATION_SAVE = 4;
	public static final int NOTIFICATION_PRIVATE_STEG = 5;
	
	private volatile CopyOnWriteArraySet<ChatWebSocket> clientSockets = new CopyOnWriteArraySet<>();
    volatile ChatDispatcher chatDispatcher;
	private Connection dbConnection;
	private static volatile WsHandler instance;

    WsHandler(Connection dbConnection) {
        chatDispatcher = new ChatDispatcher();
		this.dbConnection = dbConnection;
		sendingStegs(dbConnection);
		instance = this;
    }

	static WsHandler getInstance(){
			return instance;
	}
	
	class ChatWebSocket implements OnBinaryMessage, OnTextMessage {

       	volatile private String userId;
        volatile private Connection connection;
        volatile private UserProfile profile;
		volatile private Integer chatId = null;
			 
		@Override
		public void onOpen(Connection arg0) {
			this.connection = arg0;
			this.connection.setMaxIdleTime(300000);
			System.out.println("connected: " + this.connection.toString());
		}
 
		@Override
		public void onClose(int arg0, String arg1) {
			clientSockets.remove(this);
			if(chatId != null){
				chatDispatcher.removeListener(chatId, ChatWebSocket.this);
			}
			System.out.println("@" + (this.userId != null ? this.userId : "null") + " disconnected");
		}

        @Override
        public void onMessage(String arg0) {
            switch (arg0){
                case "checkConnection":
                    checkConnection();
                    break;
            }
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
						clientSockets.add(this);
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
					} else {
						packer.packString("oldSocialId");
						packer.packString(oldUserId);
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
                    case "addChatListener":
                        addListenerToChat(unpacker);
                        break;
                    case "removeChatListener":
                        removeListenerFromChat(unpacker);
                        break;
				}
			unpacker.close();
			} catch (IOException e){
				e.printStackTrace();
			}
		}

        private StagData unpackMe (MessageUnpacker unpacker, String mesType) throws IOException {
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

        private void addStegToWall(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String profileId = unpacker.unpackString();
			DBHandler.stegToWall(stegId, profileId, dbConnection);
			
			String toUserId = DBHandler.getStegSenderId(stegId, dbConnection);
			sendNotification(NOTIFICATION_SAVE, toUserId, profileId);
		}

        private void stegRequest(MessageUnpacker unpacker) throws IOException{
			String profileId = unpacker.unpackString();
			StegRequestItem srItem = DBHandler.stegRequset(profileId, dbConnection);
			
			sendStegItem(srItem, this);
		}

        private void addLike(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String profileId = unpacker.unpackString();
			if(DBHandler.addLike(stegId, profileId, dbConnection)){
				String toUserId = DBHandler.getStegSenderId(stegId, dbConnection);
				if(!toUserId.equals(profileId)){
					sendNotification(NOTIFICATION_LIKE, toUserId, profileId);
				}
			}
		}

        private void addComment(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String profileId = unpacker.unpackString();
			String text = unpacker.unpackString();
			Integer msgId = DBHandler.addComment(stegId, profileId, text, dbConnection);
			String toUserId = DBHandler.getStegSenderId(stegId, dbConnection);
			if(!toUserId.equals(profileId)){
				sendNotification(NOTIFICATION_COMMENT, toUserId, profileId);
			}
			System.out.println("send mes to chat: " + stegId);
            chatDispatcher.sendMessage(stegId, msgId, profileId);
		}

        private void addTextComment(MessageUnpacker unpacker) throws IOException{
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

        private void setStegUnsended(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			DBHandler.setStegUnrecieved(stegId, dbConnection);
		}

        private void addGeter(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String profileId = unpacker.unpackString();
			DBHandler.addGeter(stegId, profileId, dbConnection);
			
			String toUserId = DBHandler.getStegSenderId(stegId, dbConnection);
			sendNotification(NOTIFICATION_GET, toUserId, profileId);
		}

        private void addFriend(MessageUnpacker unpacker) throws IOException{
			String friendId = unpacker.unpackString();
			DBHandler.addFriend(userId, friendId, dbConnection);

			sendNotification(NOTIFICATION_FRIEND, friendId, userId);
		}

        private void removeFriend(MessageUnpacker unpacker) throws IOException{
			String friendId = unpacker.unpackString();
			DBHandler.removeFriend(userId, friendId, dbConnection);
		}

        private void addToBlackList(MessageUnpacker unpacker) throws IOException{
			String blackProfileId = unpacker.unpackString();
			DBHandler.addToBlackList(userId, blackProfileId, dbConnection);
		}

        private void removeFromBlackList(MessageUnpacker unpacker) throws IOException{
			String blackProfileId = unpacker.unpackString();
			DBHandler.removeFromBlackList(userId, blackProfileId, dbConnection);
		}

        private void setCoordinates(MessageUnpacker unpacker) throws IOException{
			DBHandler.setUserCoordinates(unpacker.unpackString(), unpacker.unpackString(), 
										unpacker.unpackDouble(), unpacker.unpackDouble(), dbConnection);
		}

        private void markNewsSended(MessageUnpacker unpacker) throws IOException{
			Integer newsId = unpacker.unpackInt();
			DBHandler.markNewsSended(newsId, dbConnection);
		}

        private void markAllStegNoificationsSended(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String ownerId = unpacker.unpackString();
			DBHandler.markAllStegNotificationsSended(ownerId, stegId, dbConnection);
		}

        private void markPrivateStegSended(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			DBHandler.markPrivetStegSended(stegId, dbConnection);
		}

        private void rejectCommonSteg(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			DBHandler.markUnrecievedSteg(stegId, dbConnection);
		}

        private void deleteSteg(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String profileId = unpacker.unpackString();
			DBHandler.deleteSteg(stegId, profileId, dbConnection);
		}

        private void deleteIncomeSteg(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String profileId = unpacker.unpackString();
			DBHandler.deleteIncomeSteg(stegId, profileId, dbConnection);
		}

        private void deleteStegAdm(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			DBHandler.deleteStegAdm(stegId, dbConnection);
		}

        private void removeStegFromWall(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String ownerId = unpacker.unpackString();
			DBHandler.removeStegFromWall(stegId, ownerId, dbConnection);
		}

        private void removeLikes(MessageUnpacker unpacker) throws IOException{
			Integer stegId = unpacker.unpackInt();
			String profileId = unpacker.unpackString();
			DBHandler.removeLike(stegId, profileId, dbConnection);
		}

        private void stegPlea(MessageUnpacker unpacker) throws IOException{
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
		}

        private void profileSearch(MessageUnpacker unpacker) throws IOException{
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

        private void getSubscribers(MessageUnpacker unpacker) throws IOException{
			String profileId = unpacker.unpackString();
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

        private void addListenerToChat(MessageUnpacker unpacker) throws IOException{
            Integer chatId = unpacker.unpackInt();
            chatDispatcher.addListener(chatId, ChatWebSocket.this);
			this.chatId = chatId;
        }

        private void removeListenerFromChat(MessageUnpacker unpacker) throws IOException{
            Integer chatId = unpacker.unpackInt();
            chatDispatcher.removeListener(chatId, ChatWebSocket.this);
			this.chatId = null;
        }

        private void checkConnection(){
            try {
                this.connection.sendMessage("checkConnection");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String getUserId(){
            return this.userId;
        }

        Connection getConnection(){
            return this.connection;
        }
	}

    private void sendNotification(Integer type, String toUserId, String fromUserId){
		new Thread(() -> {
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
		}).start();
	}

    private void sendingStegs(Connection dbConnection){
		
		Thread sendingThread = new Thread() {
			public void run(){
				while(!isInterrupted()){
					ArrayList<StagData> stegList = DBHandler.getUnrecievedStegs(dbConnection);

					for (StagData steg: stegList){
						CopyOnWriteArraySet<ChatWebSocket> sendSockets = checkWebSockets(clientSockets, steg.mesSender, steg.filter);
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
	
	private void sendStegItem(StegRequestItem srItem, ChatWebSocket webSocket) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		MessagePacker packer = MessagePack.newDefaultPacker(baos);
		packer.packString("stegRequestResult");
		packer.packInt(srItem.stegId != null ? srItem.stegId : -1);
		packer.packString(srItem.city != null ? srItem.city : "clear");
		packer.close();
		
		webSocket.connection.sendMessage(baos.toByteArray(),  0, baos.toByteArray().length);
	}

	private void sendToSomeBody(CopyOnWriteArraySet<ChatWebSocket> sendSockets, Integer stegId){
		Random rand = new Random(System.currentTimeMillis());
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
	    			} catch (IOException e) {
		    			e.printStackTrace();
			    	}
				    break;
			    }
			    i--;
		    }
		}
	}
	
	private CopyOnWriteArraySet<ChatWebSocket> checkWebSockets(CopyOnWriteArraySet<ChatWebSocket> clientSockets, String ownerId, int filter){
        CopyOnWriteArraySet<ChatWebSocket> sendSockets = new CopyOnWriteArraySet<>();
		for(ChatWebSocket con : clientSockets){
			if(con.profile != null){
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

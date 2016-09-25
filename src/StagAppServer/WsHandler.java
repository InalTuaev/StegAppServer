package StagAppServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;

import StagAppServer.dataClasses.*;
import StagAppServer.fcm.FcmConnection;
import StagAppServer.fcm.FcmConsts;
import StagAppServer.location.PrizeLocation;
import StagAppServer.location.StegLocation;
import StagAppServer.location.UserLocation;
import org.eclipse.jetty.util.ConcurrentHashSet;
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

    public static final int STEG_LIST_TYPE_WALL_ITEM = 0;
    public static final int STEG_LIST_TYPE_ALL = 1;
    public static final int STEG_LIST_TYPE_INCOME_PRIVATE_ITEM = 2;
    public static final int STEG_LIST_TYPE_OUTCOME_PRIVATE_ITEM = 3;
    public static final int STEG_LIST_TYPE_OUTCOME_COMMON_ITEM = 4;
    public static final int STEG_LIST_TYPE_INCOME_COMMON_ITEM = 5;
    public static final int PROFILE_REFRESH = 6;

    private volatile CopyOnWriteArraySet<ChatWebSocket> clSockets = new CopyOnWriteArraySet<>();
    private final ConcurrentHashSet<ChatWebSocket> clientSockets = new ConcurrentHashSet<>();
    final ChatDispatcher chatDispatcher;
    public final Connection dbConnection;
    private static volatile WsHandler instance;

    WsHandler(Connection dbConnection) {
        System.out.println("wsHandler started");
        chatDispatcher = new ChatDispatcher();
        this.dbConnection = dbConnection;
//		ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
//		scheduledExecutorService.scheduleWithFixedDelay(sendingStegsTask, 0, 20, TimeUnit.SECONDS);
        instance = this;
    }

    public static WsHandler getInstance() {
        return instance;
    }

    class ChatWebSocket implements OnBinaryMessage, OnTextMessage {

        volatile private String userId;
        volatile private Connection connection;
        volatile private Integer chatId = null;

        @Override
        public void onOpen(Connection arg0) {
            this.connection = arg0;
            this.connection.setMaxIdleTime(300000);
            this.connection.setMaxTextMessageSize(2 * 1024);
            this.connection.setMaxBinaryMessageSize(64 * 1024);
        }

        @Override
        public void onClose(int arg0, String arg1) {
            clientSockets.remove(this);
            if (chatId != null) {
                chatDispatcher.removeListener(chatId, ChatWebSocket.this);
            }
            if(userId != null){
                DBHandler.setUserOnline(userId, false, dbConnection);
            }
			System.out.println("@" + (this.userId != null ? this.userId : "null") + " disconnected-----");
        }

        @Override
        public void onMessage(String s) {

        }

        @Override
        public void onMessage(byte[] arg0, int arg1, int arg2) {
            StagData stagData;
            String mesType;
            MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(arg0, arg1, arg2);
            try {
                mesType = unpacker.unpackString();
                switch (mesType) {
                    case "check":
                        checkConnection();
                        break;
                    case "registration":
                        String newUserId = unpacker.unpackString();
                        String newPaswd = unpacker.unpackString();
                        unpacker.close();
                        if (DBHandler.newUser(newUserId, newPaswd, dbConnection)) {
                            this.userId = newUserId;
                            this.connection.sendMessage("registration_ok");
                            clientSockets.add(this);
                            System.out.println("@" + (this.userId != null ? this.userId : "null") + " registered+++++");
                        } else {
                            this.connection.sendMessage("same_name");
                        }
                        break;
                    case "checkSocialId":
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        MessagePacker packer = MessagePack.newDefaultPacker(baos);
                        String newSocId = unpacker.unpackString();
                        String oldUserId = DBHandler.checkUserSocialId(newSocId, dbConnection);
                        if (oldUserId == null) {
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
                        if (DBHandler.newUserSocial(newSocUserId, newSocPasswd, newSocIdSoc, dbConnection)) {
                            this.userId = newSocUserId;
                            this.connection.sendMessage("registration_ok");
                            clientSockets.add(this);
                        } else {
                            this.connection.sendMessage("same_name");
                        }
                        break;
                    case "signIn":
                        String userId = unpacker.unpackString();
                        String paswd = unpacker.unpackString();
                        unpacker.close();
                        if (DBHandler.signIn(userId, paswd, dbConnection)) {
                            this.userId = userId;
                            this.connection.sendMessage("enter");
                            clientSockets.add(this);
                            System.out.println("@" + (this.userId != null ? this.userId : "null") + " connected+++++ : " + connection.getProtocol());
                        } else {
                            this.connection.sendMessage("not_registered");
                        }
                        break;
                    case"signInV2":
                        String userIdv2 = unpacker.unpackString();
                        String paswdv2 = unpacker.unpackString();
                        unpacker.close();
                        if (DBHandler.signIn(userIdv2, paswdv2, dbConnection)) {
                            this.userId = userIdv2;
                            this.connection.sendMessage("enter");
                            this.connection.setMaxIdleTime(2 * 60 * 1000);
                            DBHandler.setUserOnline(userIdv2, true, dbConnection);
                            clientSockets.add(this);
                            System.out.println("@" + (this.userId != null ? this.userId : "null") + " connected!!!!! : " + connection.getProtocol());
                        } else {
                            this.connection.sendMessage("not_registered");
                        }
                        break;
                    case "stag":
                        stagData = unpackSteg(unpacker, mesType);
                        DBHandler.addSteg(stagData, dbConnection);
                        break;
                    case "addStegToWall":
                        addStegToWall(unpacker);
                        break;
                    case "addLike":
                        addLike(unpacker);
                        break;
                    case "addComLike":
                        addComLike(unpacker);
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
                    case "setStegActive":
                        setStegActive(unpacker);
                        break;
                    case "addGeter":
                        addGeter(unpacker);
                        break;
                    case "addReceiver":
                        addReceiver(unpacker);
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
                    case "deleteComment":
                        deleteComment(unpacker);
                        break;
                    case "removeStegFromWall":
                        removeStegFromWall(unpacker);
                        break;
                    case "removeLike":
                        removeLikes(unpacker);
                        break;
                    case "removeComLike":
                        removeCommentLike(unpacker);
                        break;
                    case "stegPlea":
                        stegPlea(unpacker);
                        break;
                    case "commentPlea":
                        commentPlea(unpacker);
                        break;
                    case "profileSearch":
                        profileSearch(unpacker);
                        break;
                    case "getUserLocations":
                        getUserLocations(unpacker);
                        break;
                    case "getPrizeLocations":
                        getPrizeLocations();
                        break;
                    case "addPrizeWinner":
                        addPrizeWinner(unpacker);
                        break;
                    case "addPrizeWinnerContacts":
                        addPrizeWinnerContacts(unpacker);
                        break;
                    case "getStegLocationsAll":
                        getStegLocationsAll(unpacker);
                        break;
                    case "getStegLocationsForProfile":
                        getStegLocationsForProfile(unpacker);
                        break;
                    case "getCurrentStegLocation":
                        getCurrentStegLocation(unpacker);
                        break;
                    case "fcmToken":
                        setFcmToken(unpacker);
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
                    case "addToBLbyStegId":
                        addToBlackListByStegId(unpacker);
                        break;
                    case "removeFromBL":
                        removeFromBlackList(unpacker);
                        break;
                    case "setCoordinates":
                        setCoordinates(unpacker);
                        break;
                    case "setCoordinatesWithState":
                        setCoordinatesWithState(unpacker);
                        break;
                    case "setShowCityEnabled":
                        setUserShowCityEnabled(unpacker);
                        break;
                    case "stegRequestFirst":
                        stegRequest(unpacker);
                        break;
                    case "stegRequestV2":
                        stegRequestV2(unpacker);
                        break;
                    case "addChatListener":
                        addListenerToChat(unpacker);
                        break;
                    case "removeChatListener":
                        removeListenerFromChat(unpacker);
                        break;
                    case "deleteGeter":
                        deleteGeter(unpacker);
                        break;
                    case "getReceivers":
                        getReceivers(unpacker);
                        break;
                    case "getVoters":
                        getVoters(unpacker);
                        break;
                    case "addStegToFav":
                        addStegToFavorites(unpacker);
                        break;
                    case "removeStegFromFav":
                        removeStegFromFavorites(unpacker);
                        break;
                    case "addVote":
                        addVote(unpacker);
                        break;
                    case "removeVote":
                        removeVote(unpacker);
                        break;
                    case "getStatistic":
                        getStatistic(unpacker);
                        break;
                    case "getAccount":
                        getAccount(unpacker);
                        break;
                    case "incMyAccount":
                        incMyAccount(unpacker);
                        break;
                }
                unpacker.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private StagData unpackSteg(MessageUnpacker unpacker, String mesType) throws IOException {
            StagData stagData = new StagData();
            stagData.mesType = mesType;
            switch (stagData.mesType) {
                case "stag":
                case "to":
                    stagData.mesSender = unpacker.unpackString();
                    stagData.mesReciever = unpacker.unpackString();
                    stagData.stagType = unpacker.unpackInt();
                    stagData.lifeTime = unpacker.unpackInt();
                    stagData.anonym = unpacker.unpackBoolean();
                    stagData.filter = unpacker.unpackInt();
                    if ((stagData.stagType & 1) != 0) { // Text
                        stagData.mesText = unpacker.unpackString();
                    }
                    if ((stagData.stagType & 8) != 0) {
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

        private void addStegToWall(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            String profileId = unpacker.unpackString();
            DBHandler.stegToWall(stegId, profileId, dbConnection);
        }

        private void stegRequest(MessageUnpacker unpacker) throws IOException {
            String profileId = unpacker.unpackString();
            StegRequestItem srItem = DBHandler.stegRequest(profileId, dbConnection);
            sendStegItem(srItem, this);
        }

        private void stegRequestV2(MessageUnpacker unpacker) throws IOException {
            String profileId = unpacker.unpackString();
            StegRequestItem srItem = DBHandler.stegRequestV2(profileId, dbConnection);
            sendStegItem(srItem, this);
        }

        private void addLike(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            String profileId = unpacker.unpackString();
            if (DBHandler.addLike(stegId, profileId, dbConnection)) {
                String toUserId = DBHandler.getStegSenderId(stegId, dbConnection);
                if (!toUserId.equals(profileId)) {
                    DBHandler.decStegReceived(stegId, dbConnection);
                    DBHandler.incAccount(toUserId, DBHandler.BONUS_FOR_LIKE, dbConnection);
                    StegSender.getInstance().addStegToQueueLast(stegId);
                    sendNotification(NewsData.NOTIFICATION_LIKE, toUserId, profileId, stegId);
                    FcmConnection.getInstance().sendAccountDelta(toUserId, DBHandler.BONUS_FOR_LIKE, DBHandler.getProfileAccount(toUserId, dbConnection));
                }
            }
        }

        private void addComLike(MessageUnpacker unpacker) throws IOException {
            Integer commentId = unpacker.unpackInt();
            String profileId = unpacker.unpackString();
            String commentOwnerId = unpacker.unpackString();
            if (DBHandler.addCommentLike(commentId, profileId, commentOwnerId, dbConnection)) {
                if (!profileId.equals(commentOwnerId)) {
                    Integer chatId = DBHandler.getCommentedSteg(commentId, dbConnection);
                    if (chatId != null && !chatDispatcher.isProfileInChat(commentOwnerId, chatId)) {
                        sendNotification(NewsData.NOTIFICATION_COMMENT_LIKE, commentOwnerId, profileId, chatId);
                    }
                }
                chatDispatcher.sendLike(chatId, commentId, true, profileId);
            }
        }

        private void addComment(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            String profileId = unpacker.unpackString();
            String text = unpacker.unpackString();
            Integer msgId = DBHandler.addComment(stegId, profileId, text, dbConnection);
            String toUserId = DBHandler.getStegSenderId(stegId, dbConnection);

            if (!toUserId.equals(profileId)) {
                sendNotification(NewsData.NOTIFICATION_COMMENT, toUserId, profileId, stegId);
            }
            chatDispatcher.sendMessage(stegId, msgId, profileId);
        }

        private void addTextComment(MessageUnpacker unpacker) throws IOException {
            CommentData comment = new CommentData();
            comment.stegId = unpacker.unpackInt();
            comment.profileId = unpacker.unpackString();
            Integer commentType = unpacker.unpackInt();
            if ((commentType & CommentData.COMMENT_TEXT_MASK) != 0) {
                comment.setText(unpacker.unpackString());
            }
            if (((commentType & CommentData.COMMENT_IMAGE_MASK) != 0) || ((commentType & CommentData.COMMENT_VIDEO_MASK) != 0)) {
            }
            if ((commentType & CommentData.COMMENT_VOICE_MASK) != 0) {
            }
            comment.setType(commentType);
            DBHandler.addComment(comment, dbConnection);
            String toUserId = DBHandler.getStegSenderId(comment.stegId, dbConnection);
            if (!toUserId.equals(comment.profileId)) {
                sendNotification(NewsData.NOTIFICATION_COMMENT, toUserId, comment.profileId, comment.stegId);
            }
        }

        private void setStegUnsended(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            DBHandler.setStegUnrecieved(stegId, dbConnection);
        }

        private void setStegActive(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            Boolean value = unpacker.unpackBoolean();
            DBHandler.setStegActive(stegId, value, dbConnection);
        }

        private void addGeter(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            String profileId = unpacker.unpackString();
            if (DBHandler.addGeter(stegId, profileId, dbConnection)) {
                DBHandler.decStegReceived(stegId, dbConnection);
                String toUserId = DBHandler.getStegSenderId(stegId, dbConnection);
                sendNotification(NewsData.NOTIFICATION_GET, toUserId, profileId, stegId);
            }
        }

        private void addReceiver(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            String profileId = unpacker.unpackString();
            DBHandler.addReceiver(stegId, profileId, dbConnection);
        }

        private void addFriend(MessageUnpacker unpacker) throws IOException {
            String friendId = unpacker.unpackString();
            DBHandler.addFriend(userId, friendId, dbConnection);

            sendNotification(NewsData.NOTIFICATION_FRIEND, friendId, userId, null);
        }

        private void removeFriend(MessageUnpacker unpacker) throws IOException {
            String friendId = unpacker.unpackString();
            DBHandler.removeFriend(userId, friendId, dbConnection);
        }

        private void addToBlackList(MessageUnpacker unpacker) throws IOException {
            String blackProfileId = unpacker.unpackString();
            DBHandler.addToBlackList(userId, blackProfileId, dbConnection);
        }

        private void addToBlackListByStegId(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            DBHandler.addToBlackListByStegId(userId, stegId, dbConnection);
        }

        private void removeFromBlackList(MessageUnpacker unpacker) throws IOException {
            String blackProfileId = unpacker.unpackString();
            DBHandler.removeFromBlackList(userId, blackProfileId, dbConnection);
        }

        private void setCoordinates(MessageUnpacker unpacker) throws IOException {
            DBHandler.setUserCoordinates(unpacker.unpackString(), unpacker.unpackString(),
                    unpacker.unpackDouble(), unpacker.unpackDouble(), dbConnection);
        }

        private void setUserShowCityEnabled(MessageUnpacker unpacker) throws IOException {
            DBHandler.setUserShowCityEnabled(unpacker.unpackString(), unpacker.unpackBoolean(), dbConnection);
        }

        private void setCoordinatesWithState(MessageUnpacker unpacker) throws IOException {
            DBHandler.setUserCoordinatesWithState(unpacker.unpackString(), unpacker.unpackString(), unpacker.unpackString(),
                    unpacker.unpackDouble(), unpacker.unpackDouble(), dbConnection);
        }

        private void markNewsSended(MessageUnpacker unpacker) throws IOException {
            Integer newsId = unpacker.unpackInt();
            DBHandler.markNewsSended(newsId, dbConnection);
        }

        private void markAllStegNoificationsSended(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            String ownerId = unpacker.unpackString();
            DBHandler.markAllStegNotificationsSended(ownerId, stegId, dbConnection);
        }

        private void markPrivateStegSended(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            DBHandler.markPrivetStegSended(stegId, dbConnection);
        }

        private void rejectCommonSteg(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
//            StegSender.getInstance().addStegToQueueFirst(stegId);
//            System.out.println("rejected: " + stegId);
//			DBHandler.decStegReceived(stegId, dbConnection);
        }

        private void deleteSteg(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            String profileId = unpacker.unpackString();
            DBHandler.deleteSteg(stegId, profileId, dbConnection);
        }

        private void deleteIncomeSteg(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            String profileId = unpacker.unpackString();
            DBHandler.deleteIncomeSteg(stegId, profileId, dbConnection);
        }

        private void deleteComment(MessageUnpacker unpacker) throws IOException {
            Integer commentId = unpacker.unpackInt();
            DBHandler.deleteComment(commentId, dbConnection);
        }

        private void deleteStegAdm(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            DBHandler.deleteStegAdm(stegId, dbConnection);
        }

        private void removeStegFromWall(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            String ownerId = unpacker.unpackString();
            DBHandler.removeStegFromWall(stegId, ownerId, dbConnection);
        }

        private void removeLikes(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            String profileId = unpacker.unpackString();
            DBHandler.removeLike(stegId, profileId, dbConnection);
            String stegOwner = DBHandler.getStegOwner(stegId, dbConnection);
            if (!stegOwner.equals(profileId)) {
                DBHandler.decAccount(stegOwner, DBHandler.BONUS_FOR_LIKE, dbConnection);
                FcmConnection.getInstance().sendAccountNotification(stegOwner, DBHandler.getProfileAccount(stegOwner, dbConnection));
                DBHandler.incStegReceived(stegId, dbConnection);
            }

        }

        private void removeCommentLike(MessageUnpacker unpacker) throws IOException {
            Integer commentId = unpacker.unpackInt();
            String profileId = unpacker.unpackString();
            DBHandler.removeCommentLike(commentId, profileId, dbConnection);

            chatDispatcher.sendLike(chatId, commentId, false, profileId);
        }

        private void stegPlea(MessageUnpacker unpacker) throws IOException {
            String text;
            String eMail;
            String pleaer = unpacker.unpackString();
            Integer stegId = unpacker.unpackInt();
            Boolean isText = unpacker.unpackBoolean();
            if (isText) {
                text = unpacker.unpackString();
            } else {
                text = "no plea text";
            }
            Boolean isEMail = unpacker.unpackBoolean();
            if (isEMail) {
                eMail = unpacker.unpackString();
            } else {
                eMail = "no e-mail";
            }

            EmailSender emailSender = EmailSender.getInstance();
            String pleaText;
            pleaText = "From: " + pleaer + "\n\nSteg: " + stegId.toString() + "\n\nEmail: " + eMail + "\n\nText: " + text;
            emailSender.send("PleaSteg", pleaText, "stegapp777@gmail.com", "stegapp777@gmail.com");
        }

        private void commentPlea(MessageUnpacker unpacker) throws IOException {
            String text;
            String eMail;
            String pleaer = unpacker.unpackString();
            Integer commentId = unpacker.unpackInt();
            Boolean isText = unpacker.unpackBoolean();
            if (isText) {
                text = unpacker.unpackString();
            } else {
                text = "no plea text";
            }
            Boolean isEMail = unpacker.unpackBoolean();
            if (isEMail) {
                eMail = unpacker.unpackString();
            } else {
                eMail = "no e-mail";
            }

            EmailSender emailSender = EmailSender.getInstance();
            String pleaText;
            pleaText = "From: " + pleaer + "\n\nComment: " + commentId.toString() + "\n\nEmail: " + eMail + "\n\nText: " + text;
            emailSender.send("PleaComment", pleaText, "stegapp777@gmail.com", "stegapp777@gmail.com");
        }

        private void profileSearch(MessageUnpacker unpacker) throws IOException {
            String searchString = unpacker.unpackString();
            String myCity = unpacker.unpackString();
            ArrayList<String> searchResult = DBHandler.getProfileSearchResult(searchString, myCity, dbConnection);

            ByteArrayOutputStream searchResultBaos = new ByteArrayOutputStream();
            MessagePacker packer = MessagePack.newDefaultPacker(searchResultBaos);
            Integer resultSize = searchResult.size();
            packer.packString("profileSearchResult")
                    .packArrayHeader(resultSize);
            for (String searchItem : searchResult) {
                packer.packString(searchItem);
            }
            packer.close();
            connection.sendMessage(searchResultBaos.toByteArray(), 0, searchResultBaos.toByteArray().length);
        }

        private void getUserLocations(MessageUnpacker unpacker) throws IOException {
            String profileId = unpacker.unpackString();
            ArrayList<UserLocation> userLocations = DBHandler.getUserLocations(profileId, dbConnection);

            ByteArrayOutputStream resultBaos = new ByteArrayOutputStream();
            MessagePacker packer = MessagePack.newDefaultPacker(resultBaos);
            Integer resultSize = userLocations.size();
            packer.packString("userLocationsResult")
                    .packArrayHeader(resultSize);
            for (UserLocation item : userLocations) {
                packer.packString(item.getProfileId())
                        .packDouble(item.getLongitude())
                        .packDouble(item.getLatitude());
            }
            packer.close();
            connection.sendMessage(resultBaos.toByteArray(), 0, resultBaos.toByteArray().length);
        }

        private void addPrizeWinner(MessageUnpacker unpacker) throws IOException {
            String winnerId= unpacker.unpackString();
            Integer prizeId = unpacker.unpackInt();

            Boolean success = DBHandler.addPrizeWinner(winnerId, prizeId, dbConnection);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MessagePacker packer = MessagePack.newDefaultPacker(baos);
            packer.packString("prizeWinnerAdded")
                    .packBoolean(success)
                    .close();
            connection.sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
        }

        private void addPrizeWinnerContacts(MessageUnpacker unpacker) throws IOException {
            String profileId = unpacker.unpackString();
            String contacts = unpacker.unpackString();
            Integer prizeId = unpacker.unpackInt();

            Boolean success = DBHandler.addPrizeWinnerContacts(profileId, contacts, prizeId, dbConnection);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MessagePacker packer = MessagePack.newDefaultPacker(baos);
            packer.packString("prizeContactsAdded")
                    .packBoolean(success)
                    .close();
            connection.sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
        }

        private void getPrizeLocations() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ArrayList<PrizeLocation> prizes = DBHandler.getPrizeLocations(dbConnection);
            MessagePacker packer = MessagePack.newDefaultPacker(baos);

            packer.packString("prizeLocations")
                    .packArrayHeader(prizes.size());
            for (PrizeLocation prize : prizes){
                packer.packInt(prize.getId())
                        .packInt(prize.getStegId())
                        .packDouble(prize.getLatitude())
                        .packDouble(prize.getLongitude())
                        .packString(prize.getTitle())
                        .packInt(prize.getValue())
                        .packBoolean(prize.isWon());
                if (prize.isWon())
                    packer.packString(prize.getWinnerId());
            }
            packer.close();
            connection.sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
        }

        private void getStegLocationsAll(MessageUnpacker unpacker) throws IOException {
            String profileId = unpacker.unpackString();
            ArrayList<StegLocation> stegLocations = DBHandler.getStegLocationsAll(profileId, dbConnection);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MessagePacker packer = MessagePack.newDefaultPacker(baos);
            Integer len = stegLocations.size();
            packer.packString("stegLocationsAll")
                    .packArrayHeader(len);
            for (StegLocation stegLocation : stegLocations) {
                packer.packInt(stegLocation.getId())
                        .packInt(stegLocation.getStegId())
                        .packDouble(stegLocation.getLatitude())
                        .packDouble(stegLocation.getLongitude())
                        .packString(stegLocation.getTitle())
                        .packInt(stegLocation.getType())
                        .packString(stegLocation.getProfileId());
            }
            packer.close();
            connection.sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
        }

        private void setFcmToken(MessageUnpacker unpacker) throws IOException {
            String profileId = unpacker.unpackString();
            String token = unpacker.unpackString();

            DBHandler.setFcmToken(profileId, token, dbConnection);
        }

        private void getStegLocationsForProfile(MessageUnpacker unpacker) throws IOException {
            String profileId = unpacker.unpackString();
            unpacker.close();
            ArrayList<StegLocation> stegLocations = DBHandler.getLocationStegForProfile(profileId, dbConnection);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MessagePacker packer = MessagePack.newDefaultPacker(baos);
            Integer len = stegLocations.size();
            packer.packString("stegLocationsFroProfile")
                    .packArrayHeader(len);
            for (StegLocation stegLocation : stegLocations) {
                packer.packInt(stegLocation.getId())
                        .packInt(stegLocation.getStegId())
                        .packDouble(stegLocation.getLatitude())
                        .packDouble(stegLocation.getLongitude())
                        .packString(stegLocation.getTitle())
                        .packInt(stegLocation.getType())
                        .packString(stegLocation.getProfileId());
            }
            packer.close();
            connection.sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
        }

        private void getCurrentStegLocation(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            unpacker.close();
            ArrayList<StegLocation> stegLocations = DBHandler.getCurrentStegLocation(stegId, dbConnection);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MessagePacker packer = MessagePack.newDefaultPacker(baos);
            Integer len = stegLocations.size();
            packer.packString("currentStegLocation")
                    .packArrayHeader(len);
            for (StegLocation stegLocation : stegLocations) {
                packer.packInt(stegLocation.getId())
                        .packInt(stegLocation.getStegId())
                        .packDouble(stegLocation.getLatitude())
                        .packDouble(stegLocation.getLongitude())
                        .packString(stegLocation.getTitle())
                        .packInt(stegLocation.getType())
                        .packString(stegLocation.getProfileId());
            }
            packer.close();
            connection.sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
        }

        private void getSubscribers(MessageUnpacker unpacker) throws IOException {
            String profileId = unpacker.unpackString();
            ArrayList<String> subscribers = DBHandler.getSubscribers(profileId, dbConnection);

            ByteArrayOutputStream subscribersStream = new ByteArrayOutputStream();
            MessagePacker packer = MessagePack.newDefaultPacker(subscribersStream);
            Integer resultSize = subscribers.size();
            packer.packString("subscribersResult");
            packer.packArrayHeader(resultSize);
            for (String item : subscribers) {
                packer.packString(item);
            }
            packer.close();
            connection.sendMessage(subscribersStream.toByteArray(), 0, subscribersStream.toByteArray().length);
        }

        private void getFriends(MessageUnpacker unpacker) throws IOException {
            String profileId = unpacker.unpackString();
            ArrayList<String> subscribers = DBHandler.getFriendsList(profileId, dbConnection);

            ByteArrayOutputStream subscribersStream = new ByteArrayOutputStream();
            MessagePacker packer = MessagePack.newDefaultPacker(subscribersStream);
            Integer resultSize = subscribers.size();
            packer.packString("friendsResult");
            packer.packArrayHeader(resultSize);
            for (String item : subscribers) {
                packer.packString(item);
            }
            packer.close();
            connection.sendMessage(subscribersStream.toByteArray(), 0, subscribersStream.toByteArray().length);
        }

        private void getReceivers(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            ArrayList<String> subscribers = DBHandler.getReceiversList(stegId, dbConnection);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MessagePacker packer = MessagePack.newDefaultPacker(baos);
            Integer resultSize = subscribers.size();
            packer.packString("receiversResult");
            packer.packInt(stegId);
            packer.packArrayHeader(resultSize);
            for (String item : subscribers) {
                packer.packString(item);
            }
            packer.close();
            connection.sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
        }

        private void getVoters(MessageUnpacker unpacker) throws IOException {
            Integer pollItemId = unpacker.unpackInt();
            ArrayList<String> voters = DBHandler.getVotersList(pollItemId, dbConnection);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MessagePacker packer = MessagePack.newDefaultPacker(baos);
            Integer resultSize = voters.size();
            packer.packString("votersResult");
            packer.packInt(pollItemId);
            packer.packArrayHeader(resultSize);
            for (String item : voters) {
                packer.packString(item);
            }
            packer.close();
            connection.sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
        }

        private void addListenerToChat(MessageUnpacker unpacker) throws IOException {
            Integer chatId = unpacker.unpackInt();
            chatDispatcher.addListener(chatId, ChatWebSocket.this);
            this.chatId = chatId;
        }

        private void removeListenerFromChat(MessageUnpacker unpacker) throws IOException {
            Integer chatId = unpacker.unpackInt();
            chatDispatcher.removeListener(chatId, ChatWebSocket.this);
            this.chatId = null;
        }

        private void deleteGeter(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            String profileId = unpacker.unpackString();
            DBHandler.deleteGeter(stegId, profileId, dbConnection);
        }

        private void addStegToFavorites(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            String profileId = unpacker.unpackString();
            DBHandler.addStegToFavorite(stegId, profileId, dbConnection);
        }

        private void removeStegFromFavorites(MessageUnpacker unpacker) throws IOException {
            Integer stegId = unpacker.unpackInt();
            String profileId = unpacker.unpackString();
            DBHandler.removeStegFromFavorite(stegId, profileId, dbConnection);
        }

        private void addVote(MessageUnpacker unpacker) throws IOException{
            Integer pollItemId = unpacker.unpackInt();
            String profileId = unpacker.unpackString();
            DBHandler.addVote(pollItemId, profileId, dbConnection);
        }

        private void removeVote(MessageUnpacker unpacker) throws IOException{
            Integer stegId = unpacker.unpackInt();
            String profileId = unpacker.unpackString();
            DBHandler.removeVote(stegId, profileId, dbConnection);
        }

        private void getStatistic(MessageUnpacker unpacker) throws IOException {
            HashMap<String, Integer> stat = DBHandler.getStatistic(dbConnection);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MessagePacker packer = MessagePack.newDefaultPacker(baos);
            packer.packString("statistic");
            packer.packArrayHeader(stat.size());
            Iterator<String> iter = stat.keySet().iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                packer.packString(key);
                packer.packInt(stat.get(key));
            }
            packer.close();
            connection.sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
        }

        private void getAccount(MessageUnpacker unpacker) throws IOException {
            String profileId = unpacker.unpackString();
            float account = DBHandler.getProfileAccount(profileId, dbConnection);

            FcmConnection.getInstance().sendAccountNotification(profileId, account);
        }

        private void incMyAccount(MessageUnpacker unpacker) throws IOException {
            String profileId = unpacker.unpackString();
            float value = unpacker.unpackFloat();

            DBHandler.incAccount(profileId, value, dbConnection);
        }

        private void checkConnection() {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                MessagePacker packer = MessagePack.newDefaultPacker(baos);
                packer.packString("check");
                packer.close();
                this.connection.sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
                packer = null;
                baos = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String getUserId() {
            return this.userId;
        }

        Connection getConnection() {
            return this.connection;
        }
    }

    public void sendNotification(Integer type, String toUserId, String fromUserId, Integer stegId) {
        if (stegId == null)
            stegId = -1;
        String toToken = DBHandler.getProfileToken(toUserId, dbConnection);
        if (toToken != null){
//            send notification via FCM
            Map<String, String> data = new HashMap<>();
            data.put(FcmConsts.NOTIFICATION_TYPE, type.toString());
            data.put(FcmConsts.FORM_USER, fromUserId);
            data.put(FcmConsts.STEG_ID, stegId.toString());
            FcmConnection.getInstance().sendNotification(toToken, data);
        } else {
//            try to send notification via webSocket connection
            try {
                for (ChatWebSocket socket : clientSockets) {
                    if (socket.userId != null) {
                        if (socket.userId.equals(toUserId)) {
                            ByteArrayOutputStream profileNameBaos = new ByteArrayOutputStream();
                            MessagePacker packer = MessagePack.newDefaultPacker(profileNameBaos);
                            packer
                                    .packString("newsFromServer")
                                    .packInt(type)
                                    .packString(fromUserId);
                            if (stegId != null)
                                packer.packInt(stegId);
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
    }

    public void sendBroadcastNotification(Integer stegId) {
        try {
            for (ChatWebSocket socket : clientSockets) {
                ByteArrayOutputStream profileNameBaos = new ByteArrayOutputStream();
                MessagePacker packer = MessagePack.newDefaultPacker(profileNameBaos);
                packer
                        .packString("newsFromServer")
                        .packInt(NewsData.NOTIFICATION_PRIVATE_STEG)
                        .packString("StegApp");
                if (stegId != null)
                    packer.packInt(stegId);
                packer.close();
                socket.connection.sendMessage(profileNameBaos.toByteArray(), 0, profileNameBaos.toByteArray().length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void notifyStegFollowers(Integer stegId, String commentSender) {
        try {
            ArrayList<String> followers = DBHandler.getFollowers(FavoriteItem.TYPE_STEG, stegId, dbConnection);
            for (ChatWebSocket socket : clientSockets) {
                if (socket.userId != null && !socket.userId.equals(commentSender) && followers.contains(socket.userId)) {
                    if (!chatDispatcher.isProfileInChat(socket.userId, stegId)) {
                        ByteArrayOutputStream profileNameBaos = new ByteArrayOutputStream();
                        MessagePacker packer = MessagePack.newDefaultPacker(profileNameBaos);
                        packer
                                .packString("newsFromServer")
                                .packInt(NewsData.NOTIFICATION_FAVORITE_STEG_COMMENT)
                                .packString(commentSender);
                        if (stegId != null)
                            packer.packInt(stegId);
                        packer.close();
                        socket.connection.sendMessage(profileNameBaos.toByteArray(), 0, profileNameBaos.toByteArray().length);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void notifyToRefresh(Integer listType, String profileId) {
        for (ChatWebSocket socket : clientSockets) {
            if (socket.userId.equals(profileId)) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    MessagePacker packer = MessagePack.newDefaultPacker(baos);
                    packer.packString("notifyToRefresh")
                            .packInt(listType);
                    packer.close();
                    socket.connection.sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    private Runnable sendingStegsTask = new Runnable() {
        @Override
        public void run() {
            ArrayList<StagData> stegList = DBHandler.getUnreceivedStegs(dbConnection);

            for (StagData steg : stegList) {
                ConcurrentHashSet<ChatWebSocket> sendSockets = checkWebSockets(clientSockets, steg);
                String city = DBHandler.getStegSenderCity(steg.stegId, dbConnection);
                sendToSomeBody(sendSockets, steg, city);
            }
        }
    };

    private void sendStegItem(StegRequestItem srItem, ChatWebSocket webSocket) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessagePacker packer = MessagePack.newDefaultPacker(baos);
        if (srItem != null) {
            packer.packString("stegRequestResult");
            packer.packInt(srItem.getStegId());
            packer.packString(srItem.getCity() != null ? srItem.getCity() : "clear");
            packer.packString(srItem.getSenderName());
            packer.packInt(srItem.getStegMode());
        } else {
            packer.packString("stegRequestNull");
        }
        packer.close();

        webSocket.connection.sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
    }

    private void sendToSomeBody(ConcurrentHashSet<ChatWebSocket> sendSockets, StagData steg, String city) {
        Random rand = new Random(System.currentTimeMillis());
        if (sendSockets.size() > 0) {
            Integer i = rand.nextInt(sendSockets.size());

            for (ChatWebSocket con : sendSockets) {
                if (i == 0) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    MessagePacker packer = MessagePack.newDefaultPacker(baos);
                    try {
                        packer
                                .packString("stegFromServer")
                                .packInt(steg.stegId);
                        packer.packString(city != null ? city : "clear");
                        packer.close();

                        con.connection.sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                i--;
            }
        }
    }

    private ConcurrentHashSet<ChatWebSocket> checkWebSockets(ConcurrentHashSet<ChatWebSocket> clientSockets, StagData steg) {
        ConcurrentHashSet<ChatWebSocket> sendSockets = new ConcurrentHashSet<>();
        for (ChatWebSocket con : clientSockets) {
            if (con.userId != null && !con.userId.equals(steg.mesSender) && DBHandler.checkReceiver(steg, con.userId, dbConnection)) {
                sendSockets.add(con);
            }
        }
        return sendSockets;
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest arg0, String arg1) {
        return new ChatWebSocket();
    }
}

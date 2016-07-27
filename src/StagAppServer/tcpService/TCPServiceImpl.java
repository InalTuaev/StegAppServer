package StagAppServer.tcpService;


import StagAppServer.*;
import StagAppServer.messageSystem.Address;
import StagAppServer.messageSystem.MessageSystem;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;

public class TCPServiceImpl implements TCPService, Runnable{

    private static final String STEGAPP_IMG_DIR = "StegApp/media/img/";
    private static final String STEGAPP_IMG_T_DIR = "StegApp/media/img/thumbs/";
    private static final String STEGAPP_VIDEO_DIR = "StegApp/media/video/";
    private static final String STEGAPP_AUDIO_DIR = "StegApp/media/audio/";
    private static final String STEGAPP_PROFILE_PHOTO_DIR = "StegApp/avatars/";
    private static final String STEGAPP_PROFILE_THUMBS_DIR = "StegApp/thumbs/";

    private static final Integer kB32 = 32*1024;

    private final Connection dbConnection;
    private final MessageSystem ms;
    private final Address address = new Address();

    public TCPServiceImpl(Connection dbConnection, MessageSystem ms){
        this.dbConnection = dbConnection;
        this.ms = ms;
        ms.addService(this);
        ms.getAddressService().registerTcpService(this);
    }

    @Override
    public void run() {
        try {
            while (true) {
                ms.execForAbonent(this);
            }
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    @Override
    public void handleRequest(Socket socket) {
        try{
            socket.setTcpNoDelay(true);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            String what = in.readUTF();
            switch(what){
                case STEG_TO_SERVER:
                    stegToServer(in);
                    break;
                case STEG_FROM_SERVER:
                    stegFromServer(in, out);
                    break;
                case STEG_REQUEST:
                    stegRequest(in, out);
                    break;
                case PROFILE_FROM_SERVER:
                    profileFromServer(in, out);
                    break;
                case PROFILE_IMG_FROM_SERVER:
                    profileImgFromServer(in, out);
                    break;
                case PROFILE_TO_SERVER:
                    profileToServer(in);
                    break;
                case PROFILE_TO_SERVER_NO_IMG_NO_GEO:
                    profileToServerNoImg(in);
                    break;
                case PROFILE_IMG_TO_SERVER:
                    profileImgToServer(in);
                    break;
                case WALL_ITEMS_FROM_SERVER:
                    wallItemsFromServer(in, out);
                    break;
                case COMMENT_ITEMS_FROM_SERVER:
                    commentItemsFromServer(in, out);
                    break;
                case COMMENT_FROM_SERVER:
                    commentFromServer(in, out);
                    break;
                case COMMENT_REQUEST:
                    commentRequest(in, out);
                    break;
                case COMMENT_TO_SERVER:
                    commentToServer(in, out);
                    break;
                case LIKES_FROM_SERVER:
                    likesFromServer(in, out);
                    break;
                case COMMENT_LIKES_FROM_SERVER:
                    commentLikesFromServer(in, out);
                case NOTIFICATION_ITEMS_FROM_SERVER:
                    notificationItemsFromServer(in, out);
                    break;
                case PROFILE_CORRESPONDENCE_FROM_SERVER:
                    profileCorrespondenceFromServer(in, out);
                    break;
                case INCOME_PRIVATE_ITEMS_FROM_SERVER:
                    incomePrivateItemsFromServer(in, out);
                    break;
                case OUTCOME_PRIVATE_ITEMS_FROM_SERVER:
                    outcomePrivateItemsFromServer(in, out);
                    break;
                case OUTCOME_COMMON_ITEMS_FROM_SERVER:
                    outcomeCommonItemsFromServer(in, out);
                    break;
                case INCOME_COMMON_ITEMS_FROM_SERVER:
                    incomeCommonItemsFromServer(in, out);
                    break;
                case PROFILE_SENT_ITEMS:
                    profileSentItems(in, out);
                    break;
                case SAVERS_FROM_SERVER:
                    saversFromServer(in, out);
                    break;
                case GETERS_FROM_SERVER:
                    getersFromServer(in, out);
                    break;
                case GET_IS_BLACK:
                    getIsBlack(in, out);
                    break;
                case IS_ME_IN_BLACK_LIST:
                    isMeInBlackList(in, out);
                    break;
                case CHECK_PASSWORD:
                    checkPassword(in, out);
                    break;
                case DEL_PROFILE:
                    delProfile(in, out);
                    break;
                case FAVORITES_FROM_SERVER:
                    favoritesFromServer(in, out);
                    break;
            }
            socket.close();
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    private void stegToServer(DataInputStream in) throws IOException{
        Boolean notStopped = true;
        Boolean isAddToMyWall;
        StagData stagData = new StagData();
        while(notStopped){
            File imgFile = null;
            String imgExt = null;
            File stagFile;
            String what = in.readUTF();
            switch(what){
                case "fileToServer":
                    Integer fileType = in.readInt();
                    FileOutputStream dos = null;
                    String fileExt = in.readUTF();
                    switch(fileType){
                        case 1:
                            imgExt = fileExt;
                            imgFile = new File(STEGAPP_IMG_DIR + System.currentTimeMillis() + imgExt);
                            stagData.cameraDataFile = imgFile.getName();
                            dos = new FileOutputStream(imgFile);
                            break;
                        case 2:
                            stagFile = new File(STEGAPP_VIDEO_DIR + System.currentTimeMillis() + fileExt);
                            stagData.cameraDataFile = stagFile.getName();
                            dos = new FileOutputStream(stagFile);
                            break;
                        case 3:
                            stagFile = new File(STEGAPP_AUDIO_DIR + System.currentTimeMillis() + fileExt);
                            stagData.voiceDataFile = stagFile.getName();
                            dos = new FileOutputStream(stagFile);
                            break;
                    }
                    int fileSize = in.readInt();

                    readImgFile(dos, in, imgFile, imgExt, fileSize);
                    dos.close();
                    break;
                case "stagData":
                    int stagDataSize = in.readInt();
                    byte[] stagDataBytes = new byte[stagDataSize];
                    in.read(stagDataBytes);
                    MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(stagDataBytes);
                    stagData.mesType = unpacker.unpackString();
                    stagData.mesSender = unpacker.unpackString();
                    stagData.mesReciever = unpacker.unpackString();
                    stagData.stagType = unpacker.unpackInt();
                    if((stagData.stagType & 1) != 0){
                        stagData.mesText = unpacker.unpackString();
                    }
                    stagData.lifeTime = unpacker.unpackInt();
                    stagData.anonym = unpacker.unpackBoolean();
                    stagData.filter = unpacker.unpackInt();
                    isAddToMyWall = unpacker.unpackBoolean();
                    unpacker.close();
                    Integer newStegId = DBHandler.addSteg(stagData, dbConnection);
                    Integer listType;
                    if(isAddToMyWall){
                        DBHandler.incStegReceived(newStegId, dbConnection);
                        DBHandler.stegToWall(newStegId, stagData.mesSender, dbConnection);
                        listType = WsHandler.STEG_LIST_TYPE_WALL_ITEM;
                    } else {
                        if(!stagData.mesReciever.equals("common")){
                            listType = WsHandler.STEG_LIST_TYPE_OUTCOME_PRIVATE_ITEM;
                            if(stagData.anonym){
                                DBHandler.addNews(NewsData.NOTIFICATION_TYPE_PRIVATE_STEG, "clear", stagData.mesReciever, newStegId, dbConnection);
                            } else {
                                DBHandler.addNews(NewsData.NOTIFICATION_TYPE_PRIVATE_STEG, stagData.mesSender, stagData.mesReciever, newStegId, dbConnection);
                            }
                            String sender;
                            if (stagData.anonym){
                                sender = "anonym";
                            } else {
                                sender = stagData.mesSender;
                            }
                            WsHandler.getInstance().sendNotification(NewsData.NOTIFICATION_PRIVATE_STEG, stagData.mesReciever, sender, stagData.stegId);
                        } else {
                            listType = WsHandler.STEG_LIST_TYPE_OUTCOME_COMMON_ITEM;
                        }
                    }
                    WsHandler.getInstance().notifyToRefresh(listType, stagData.mesSender);
                    notStopped = false;
                    break;
            }
        }
    }

    private void stegRequest(DataInputStream in, DataOutputStream out) throws Exception {
        int stegId = in.readInt();
        String userId = in.readUTF();
        StagData steg = DBHandler.getSteg(stegId, userId, dbConnection);

        if (steg == null)
            throw new Exception("NULL Steg Exception! stegId: " + stegId + " == null!!! ");

//        Отправляем клиенту информацию о стеге
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessagePacker packer = MessagePack.newDefaultPacker(baos);
        packer
                .packInt(steg.stegId)
                .packString(steg.mesType)
                .packString(steg.mesSender)
                .packString(steg.senderName)
                .packString(steg.mesReciever)
                .packInt(steg.stagType);
        if((steg.stagType & 1) != 0){
            packer.packString(steg.mesText);
        }
        packer.packInt(steg.lifeTime)
                .packBoolean(steg.anonym)
                .packInt(steg.filter)
                .packInt(steg.likes)
                .packInt(steg.gets)
                .packInt(steg.saves)
                .packInt(steg.comments)
                .packInt(steg.likes)
                .packLong(steg.date.getTime())
                .packLong(steg.time.getTime())
                .packBoolean(steg.liked)
                .packBoolean(steg.isActive())
                .packBoolean(steg.isFavorite());
        packer.close();
        out.writeInt(baos.toByteArray().length);
        out.flush();
        out.write(baos.toByteArray(), 0, baos.toByteArray().length);
        out.flush();

//        Если нет файлов для отправки завершаем работу функции
        if ((steg.stagType & StagData.STEG_MEDIA_CONTENT_MASK) == 0)
            return;

//        Отправляем клиенту файлы если он их запросит

//        Ложим все файлы в список
        ArrayList<StagFile> fileList = new ArrayList<>();

        if((steg.stagType & StagData.STEG_CONTENT_IMG_MASK) != 0) {
            fileList.add(new StagFile(steg.cameraDataFile, StagFile.STEG_FILE_TYPE_IMG));
        }
        if((steg.stagType & StagData.STEG_CONTENT_VIDEO_MASK) != 0) {
            fileList.add(new StagFile(steg.cameraDataFile, StagFile.STEG_FILE_TYPE_VIDEO));
        }
        if((steg.stagType & StagData.STEG_CONTENT_AUDIO_MASK) != 0) {
            fileList.add(new StagFile(steg.voiceDataFile, StagFile.STEG_FILE_TYPE_AUDIO));
        }
//        Отправляем клиенту количество готовых к отправке файлов
        out.writeInt(fileList.size());
        out.flush();

        for (StagFile stegFile : fileList){
            out.writeInt(stegFile.getType());
            out.flush();
            out.writeUTF(stegFile.getFilePath().substring(stegFile.getFilePath().lastIndexOf(".")));
            out.flush();

//            Отправлять ли файл
            Boolean getFile = in.readBoolean();

            if (getFile){
                FileInputStream dis = null;
                switch(stegFile.getType()){
                    case StagFile.STEG_FILE_TYPE_IMG:
                        out.writeInt((int) new File(STEGAPP_IMG_T_DIR + stegFile.getFilePath()).length());
                        out.flush();
                        dis = new FileInputStream(STEGAPP_IMG_T_DIR + stegFile.getFilePath());
                        break;
                    case StagFile.STEG_FILE_TYPE_VIDEO:
                        out.writeInt((int) new File(STEGAPP_VIDEO_DIR + stegFile.getFilePath()).length());
                        out.flush();
                        dis = new FileInputStream(STEGAPP_VIDEO_DIR + stegFile.getFilePath());
                        break;
                    case StagFile.STEG_FILE_TYPE_AUDIO:
                        out.writeInt((int) new File(STEGAPP_AUDIO_DIR + stegFile.getFilePath()).length());
                        out.flush();
                        dis = new FileInputStream(STEGAPP_AUDIO_DIR + stegFile.getFilePath());
                        break;
                }

                Integer len;
                while (true){
                    byte[] buffer = new byte[kB32];
                    len = dis.read(buffer);
                    if(len == -1) {
                        break;
                    }
                    out.write(buffer, 0, len);
                    out.flush();
                }
                dis.close();
            }
        }
    }

    private void stegFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        int stagId = in.readInt();
        String userId = in.readUTF();
        StagData stagData = DBHandler.getSteg(stagId, userId, dbConnection);

        if (stagData == null)
            System.out.println("steg == null!  - " + Integer.toString(stagId) + " - " + userId);

        ArrayList<StagFile> fileList = new ArrayList<>();

        if((stagData.stagType & 2) != 0) {
            fileList.add(new StagFile(stagData.cameraDataFile, 1));
        }
        if((stagData.stagType & 4) != 0) {
            fileList.add(new StagFile(stagData.cameraDataFile, 2));
        }
        if((stagData.stagType & 8) != 0) {
            fileList.add(new StagFile(stagData.voiceDataFile, 3));
        }

        for(StagFile sFile: fileList){
            out.writeUTF("fileFromServer");
            out.flush();
            out.writeInt(sFile.getType());
            out.flush();
            out.writeUTF(sFile.getFilePath().substring(sFile.getFilePath().lastIndexOf(".")));
            out.flush();
            FileInputStream dis = null;
            switch(sFile.getType()){
                case 1:
                    out.writeInt((int) new File(STEGAPP_IMG_T_DIR + sFile.getFilePath()).length());
                    out.flush();
                    dis = new FileInputStream(STEGAPP_IMG_T_DIR + sFile.getFilePath());
                    break;
                case 2:
                    out.writeInt((int) new File(STEGAPP_VIDEO_DIR + sFile.getFilePath()).length());
                    out.flush();
                    dis = new FileInputStream(STEGAPP_VIDEO_DIR + sFile.getFilePath());
                    break;
                case 3:
                    out.writeInt((int) new File(STEGAPP_AUDIO_DIR + sFile.getFilePath()).length());
                    out.flush();
                    dis = new FileInputStream(STEGAPP_AUDIO_DIR + sFile.getFilePath());
                    break;
            }

            Integer len;
            while (true){
                byte[] buffer = new byte[kB32];
                len = dis.read(buffer);
                if(len == -1) {
                    break;
                }
                out.write(buffer, 0, len);
                out.flush();
            }
            dis.close();
        }

        out.writeUTF("stagData");
        out.flush();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessagePacker packer = MessagePack.newDefaultPacker(baos);
        packer
                .packInt(stagData.stegId)
                .packString(stagData.mesType)
                .packString(stagData.mesSender)
                .packString(stagData.senderName)
                .packString(stagData.mesReciever)
                .packInt(stagData.stagType);
        if((stagData.stagType & 1) != 0){
            packer.packString(stagData.mesText);
        }
        packer.packInt(stagData.lifeTime)
                .packBoolean(stagData.anonym)
                .packInt(stagData.filter)
                .packInt(stagData.likes)
                .packInt(stagData.gets)
                .packInt(stagData.saves)
                .packInt(stagData.comments)
                .packInt(stagData.likes)
                .packLong(stagData.date.getTime())
                .packLong(stagData.time.getTime())
                .packBoolean(stagData.liked)
                .packBoolean(stagData.isActive())
                .packBoolean(stagData.isFavorite());
        packer.close();
        out.writeInt(baos.toByteArray().length);
        out.flush();
        out.write(baos.toByteArray(), 0, baos.toByteArray().length);
        out.flush();
    }

    private void profileToServer(DataInputStream in) throws IOException {
        UserProfile recProfile = new UserProfile();
        String what = in.readUTF();
        switch(what) {
            case "userProfile":
                Integer len = in.readInt();
                byte[] profileBytes = new byte[len];
                in.read(profileBytes, 0, len);
                MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(profileBytes);
                recProfile.setId(unpacker.unpackString());
                recProfile.setName(unpacker.unpackString());
                recProfile.setSex(unpacker.unpackString());
                recProfile.setState(unpacker.unpackString());
                recProfile.setCity(unpacker.unpackString());
                recProfile.setAge(unpacker.unpackInt());
                if(unpacker.hasNext()){
                    recProfile.setCoordinates(unpacker.unpackDouble(), unpacker.unpackDouble());
                }
                unpacker.close();

                DBHandler.addUserInfo(recProfile, dbConnection);

                String isPhoto = in.readUTF();
                if(isPhoto.equals("profilePhoto")){
                    String recUserId = in.readUTF();
                    String fileExt = in.readUTF();
                    Integer fileSize = in.readInt();
                    File newFile = new File(STEGAPP_PROFILE_PHOTO_DIR + recUserId + fileExt);
                    FileOutputStream dos = new FileOutputStream(newFile);

                    try {
                        readFile(dos, in, fileSize);

                        dos.close();
                        System.out.println("photo added: " + newFile.getAbsolutePath());
                        // Write in DB
                        DBHandler.addUserPhoto(recUserId, recUserId + fileExt, dbConnection);

                        // Creating Thumbnail image
                        ImageIO.write(resizeImg(newFile, 120, 120), fileExt.substring(1), new File(STEGAPP_PROFILE_THUMBS_DIR + recUserId + fileExt));

                    } catch (EOFException e) {
                        dos.close();
                    }
                }
                break;
        }
    }

    private void profileToServerNoImg(DataInputStream in) throws IOException {
        UserProfile recProfile = new UserProfile();

        Integer len = in.readInt();
        byte[] profileBytes = new byte[len];
        in.read(profileBytes, 0, len);
        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(profileBytes);
        recProfile.setId(unpacker.unpackString());
        recProfile.setName(unpacker.unpackString());
        recProfile.setSex(unpacker.unpackString());
        recProfile.setAge(unpacker.unpackInt());
        unpacker.close();

        DBHandler.addUserInfoNoImgNoGeo(recProfile, dbConnection);
    }

    private void profileImgToServer(DataInputStream in) throws IOException {
        String profileId = in.readUTF();
        String fileExt = in.readUTF();
        Integer fileSize = in.readInt();

        File imgFile = new File(STEGAPP_PROFILE_PHOTO_DIR + profileId + fileExt);
        FileOutputStream fos = new FileOutputStream(imgFile);

        try{
            readFile(fos, in, fileSize);

            // Write in DB
            if (imgFile.length() > 0)
                DBHandler.addUserPhoto(profileId, profileId + fileExt, dbConnection);

            // Creating Thumbnail image
            ImageIO.write(resizeImg(imgFile, 120, 120), fileExt.substring(1), new File(STEGAPP_PROFILE_THUMBS_DIR + profileId + fileExt));
            WsHandler.getInstance().notifyToRefresh(WsHandler.PROFILE_REFRESH, profileId);
        } catch (EOFException e){
            e.printStackTrace();
        }
    }

    private void profileFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        Boolean isBlack = false;
        String userId = in.readUTF();
        String profileId = in.readUTF();
        UserProfile profileToSend = DBHandler.getUserProfile(userId, dbConnection);

        if(!profileId.equals(userId)){
            profileToSend.setIsFriend(DBHandler.isFriend(profileId, userId, dbConnection));
            isBlack = DBHandler.isBlack(profileId, userId, dbConnection);
        }
        out.writeBoolean(!profileToSend.getPhoto().equals("clear"));
        out.flush();
        ByteArrayOutputStream profileBaos = new ByteArrayOutputStream();
        MessagePacker profilePacker = MessagePack.newDefaultPacker(profileBaos);
        profilePacker
                .packString(profileToSend.getName())
                .packString(profileToSend.getSex())
                .packString(profileToSend.getState())
                .packString(profileToSend.getCity())
                .packInt(profileToSend.getAge())
                .packDouble(profileToSend.getLongitude())
                .packDouble(profileToSend.getLatitude())
                .packBoolean(profileToSend.getIsFriend())
                .packBoolean(isBlack);


        if(!profileToSend.getPhoto().equals("clear")){
            File sendFile = new File(STEGAPP_PROFILE_THUMBS_DIR + profileToSend.getPhoto());
            profilePacker.packString(sendFile.getName().substring(sendFile.getName().lastIndexOf(".")));
            byte[] photoBytes = new byte[(int) sendFile.length()];
            FileInputStream fis = new FileInputStream(sendFile);
            fis.read(photoBytes, 0, photoBytes.length);
            fis.close();
            profilePacker.packBinaryHeader(photoBytes.length);
            profilePacker.writePayload(photoBytes, 0, photoBytes.length);
        } else {
            profilePacker.packString("clear");
        }
        profilePacker.close();
        out.writeInt(profileBaos.toByteArray().length);
        out.flush();

        out.write(profileBaos.toByteArray(), 0, profileBaos.toByteArray().length);
        out.flush();
        profileBaos.close();
    }

    private void profileImgFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        String profileId = in.readUTF();
        UserProfile profile = DBHandler.getUserProfile(profileId, dbConnection);

        File imgFile = new File(STEGAPP_PROFILE_PHOTO_DIR + profile.getPhoto());
        FileInputStream fis = new FileInputStream(imgFile);

        out.writeLong(imgFile.length());

        Integer len;
        while (true){
            byte[] buffer = new byte[kB32];
            len = fis.read(buffer);
            if(len == -1) {
                break;
            }
            out.write(buffer, 0, len);
            out.flush();
        }
        fis.close();
    }

    private void getIsBlack(DataInputStream in, DataOutputStream out) throws IOException {
        String myProfileId = in.readUTF();
        String blackProfileId = in.readUTF();
        Boolean isBlack = DBHandler.isBlack(myProfileId, blackProfileId, dbConnection);
        out.writeBoolean(isBlack);
        out.flush();
    }

    private void isMeInBlackList(DataInputStream in, DataOutputStream out) throws IOException {
        String myProfileId = in.readUTF();
        String blackProfileId = in.readUTF();
        Boolean isMeInBlackList = DBHandler.isMeInBlackList(myProfileId, blackProfileId, dbConnection);
        out.writeBoolean(isMeInBlackList);
        out.flush();
    }

    private void checkPassword(DataInputStream in, DataOutputStream out) throws IOException {
        String profileId = in.readUTF();
        String password = in.readUTF();
        Boolean check = DBHandler.checkPassword(profileId, password, dbConnection);
        out.writeBoolean(check);
        out.flush();
    }

    private void delProfile(DataInputStream in, DataOutputStream out) throws IOException {
        String profileId = in.readUTF();
        Boolean res = DBHandler.deleteProfile(profileId, dbConnection);
        out.writeBoolean(res);
        out.flush();
    }

    private void wallItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        String profileId = in.readUTF();
        ArrayList<StegItem> stegItems = DBHandler.getWallItems(profileId, dbConnection);
        Integer count = stegItems.size();
        out.writeInt(count);
        out.flush();
        for(StegItem stegItem: stegItems){
            out.writeInt(stegItem.getStegId());
            out.writeUTF(stegItem.getMesSender());
            out.writeBoolean(stegItem.isAnonym());
            out.writeInt(stegItem.getLikes());
            out.writeInt(stegItem.getComments());
            out.writeBoolean(stegItem.isLiked());
            out.flush();
        }
    }

    private void incomeCommonItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        String profileId = in.readUTF();
        ArrayList<StegItem> stegItems = DBHandler.getIncomeCommonItems(profileId, dbConnection);
        out.writeInt(stegItems.size());
        out.flush();
        for(StegItem stegItem: stegItems){
            out.writeInt(stegItem.getStegId());
            out.writeUTF(stegItem.getMesSender());
            out.writeBoolean(stegItem.isAnonym());
            out.writeInt(stegItem.getLikes());
            out.writeInt(stegItem.getComments());
            out.writeBoolean(stegItem.isLiked());
            out.flush();
        }
    }

    private void profileSentItems(DataInputStream in, DataOutputStream out) throws IOException {
        String requestor = in.readUTF();
        String profileId = in.readUTF();
        ArrayList<StegItem> stegItems = DBHandler.getProfileSentItems(requestor, profileId, dbConnection);
        out.writeInt(stegItems.size());
        out.flush();
        for(StegItem stegItem: stegItems){
            out.writeInt(stegItem.getStegId());
            out.writeUTF(stegItem.getMesSender());
            out.writeBoolean(stegItem.isAnonym());
            out.writeInt(stegItem.getLikes());
            out.writeInt(stegItem.getComments());
            out.writeBoolean(stegItem.isLiked());
            out.flush();
        }
    }

    private void outcomePrivateItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        String profileId = in.readUTF();
        ArrayList<StegItem> stegItems = DBHandler.getOutcomePrivateItems(profileId, dbConnection);
        Integer count = stegItems.size();
        out.writeInt(count);
        out.flush();
        for(StegItem stegItem: stegItems){
            out.writeInt(stegItem.getStegId());
            out.writeUTF(stegItem.getMesSender());
            out.writeBoolean(stegItem.isAnonym());
            out.writeInt(stegItem.getLikes());
            out.writeInt(stegItem.getComments());
            out.writeBoolean(stegItem.isLiked());
            out.writeBoolean(stegItem.isSended());
            out.flush();
        }
    }

    private void outcomeCommonItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        String profileId = in.readUTF();
        ArrayList<StegItem> stegItems = DBHandler.getOutcomeCommonItems(profileId, dbConnection);
        Integer count = stegItems.size();
        out.writeInt(count);
        out.flush();
        for(StegItem stegItem: stegItems){
            out.writeInt(stegItem.getStegId());
            out.flush();
            out.writeUTF(stegItem.getMesSender());
            out.flush();
            out.writeBoolean(stegItem.isAnonym());
            out.flush();
            out.writeInt(stegItem.getLikes());
            out.flush();
            out.writeInt(stegItem.getComments());
            out.flush();
            out.writeBoolean(stegItem.isLiked());
            out.flush();
            Integer recievers = stegItem.getRecieverCount();
            out.writeInt(recievers);
            out.flush();
            if(recievers > 4)
                recievers = 4;
            out.writeInt(recievers);
            out.flush();
            int i = 0;
            for(Map.Entry<String, UserProfile> entry : stegItem.recieverIds.entrySet()){
                i++;
                if(i > 4) break;
                out.writeUTF(entry.getKey());
            }
            out.flush();
        }
    }

    private void profileCorrespondenceFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        String profileId = in.readUTF();
        String userId = in.readUTF();

        ArrayList<StegItem> stegItems = DBHandler.getProfileCorrespondenceItems(profileId, userId, dbConnection);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessagePacker packer = MessagePack.newDefaultPacker(baos);
        packer.packArrayHeader(stegItems.size());
        for (StegItem item : stegItems){
            packer.packInt(item.getStegId())
                    .packString(item.getMesSender())
                    .packBoolean(item.isAnonym())
                    .packInt(item.getLikes())
                    .packInt(item.getComments())
                    .packBoolean(item.isLiked())
                    .packBoolean(item.isSended());
        }
        packer.close();
        out.writeInt(baos.toByteArray().length);
        out.write(baos.toByteArray(), 0, baos.toByteArray().length);
        out.flush();
    }

    private void incomePrivateItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        String profileId = in.readUTF();
        ArrayList<StegItem> stegItems = DBHandler.getIncomePrivateItems(profileId, dbConnection);
        Integer count = stegItems.size();
        out.writeInt(count);
        out.flush();
        for(StegItem stegItem: stegItems){
            out.writeInt(stegItem.getStegId());
            out.writeUTF(stegItem.getMesSender());
            out.writeBoolean(stegItem.isAnonym());
            out.writeInt(stegItem.getLikes());
            out.writeInt(stegItem.getComments());
            out.writeBoolean(stegItem.isLiked());
            out.writeBoolean(stegItem.isSended());
            out.flush();
        }
    }

    private void commentItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        Integer stegId = in.readInt();
        ArrayList<CommentItem> items = DBHandler.getCommentsItems(stegId, dbConnection);
        Integer count = items.size();
        out.writeInt(count);
        out.flush();
        for(CommentItem comment : items){
            ByteArrayOutputStream commentBaos = new ByteArrayOutputStream();
            MessagePacker commentPacker = MessagePack.newDefaultPacker(commentBaos);
            commentPacker
                    .packInt(comment.getId())
                    .packInt(comment.getStegId())
                    .packInt(comment.getType())
                    .packString(comment.getProfileId())
                    .packString(comment.getText())
                    .packLong(comment.getDate().getTime())
                    .packLong(comment.getTime().getTime());
            commentPacker.close();

            int len = commentBaos.toByteArray().length;
            out.writeInt(len);
            out.flush();
            out.write(commentBaos.toByteArray(), 0, len);
            out.flush();
        }
    }

    private void commentRequest(DataInputStream in, DataOutputStream out) throws IOException {
        int commentId = in.readInt();
        String profileId = in.readUTF();

        CommentData comment = DBHandler.getComment(commentId, profileId, dbConnection);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessagePacker packer = MessagePack.newDefaultPacker(baos);
        packer
                .packInt(comment.id)
                .packInt(comment.stegId)
                .packInt(comment.getType())
                .packString(comment.profileId)
                .packLong(comment.date.getTime())
                .packLong(comment.time.getTime())
                .packString(comment.getText())
                .packInt(comment.getLikesCount())
                .packBoolean(comment.isLiked());
        packer.close();
        out.writeInt(baos.toByteArray().length);
        out.flush();
        out.write(baos.toByteArray(), 0, baos.toByteArray().length);
        out.flush();

        File file = null;
        switch (comment.getType() & CommentData.COMMENT_MEDIA_CONTENT_MASK){
            case CommentData.COMMENT_IMAGE_MASK:
                file = new File(STEGAPP_IMG_T_DIR + comment.getImgData());
                break;
            case CommentData.COMMENT_VIDEO_MASK:
                file = new File(STEGAPP_VIDEO_DIR + comment.getVideoData());
                break;
            case CommentData.COMMENT_VOICE_MASK:
                file = new File(STEGAPP_AUDIO_DIR + comment.getVoiceData());
                break;
        }

        if (file != null){
            out.writeUTF(file.getName().substring(file.getName().lastIndexOf(".")));
            out.flush();

            Boolean getFile = in.readBoolean();

            if (getFile){
                out.writeInt((int) file.length());
                out.flush();
                FileInputStream fis = new FileInputStream(file);

                Integer len;
                while (true) {
                    byte[] buffer = new byte[kB32];
                    len = fis.read(buffer);
                    if (len == -1) {
                        break;
                    }
                    out.write(buffer, 0, len);
                    out.flush();
                }
                fis.close();
            }
        }
    }

    private void commentFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        int commentId = in.readInt();
        String profileId = in.readUTF();

        CommentData comment = DBHandler.getComment(commentId, profileId, dbConnection);

        ArrayList<StagFile> fileList = new ArrayList<>();

        if((comment.getType() & 2) != 0) {
            fileList.add(new StagFile(comment.getImgData(), 1));
        }
        if((comment.getType() & 4) != 0) {
            fileList.add(new StagFile(comment.getVideoData(), 2));
        }
        if((comment.getType() & 8) != 0) {
            fileList.add(new StagFile(comment.getVoiceData(), 3));
        }

        for(StagFile sFile: fileList){
            out.writeUTF("fileFromServer");
            out.flush();
            out.writeInt(sFile.getType());
            out.flush();
            out.writeUTF(sFile.getFilePath().substring(sFile.getFilePath().lastIndexOf(".")));
            out.flush();
            FileInputStream dis = null;
            switch(sFile.getType()){
                case 1:
                    out.writeInt((int) new File(STEGAPP_IMG_T_DIR + sFile.getFilePath()).length());
                    out.flush();
                    dis = new FileInputStream(STEGAPP_IMG_T_DIR + sFile.getFilePath());
                    break;
                case 2:
                    out.writeInt((int) new File(STEGAPP_VIDEO_DIR + sFile.getFilePath()).length());
                    out.flush();
                    dis = new FileInputStream(STEGAPP_VIDEO_DIR + sFile.getFilePath());
                    break;
                case 3:
                    out.writeInt((int) new File(STEGAPP_AUDIO_DIR + sFile.getFilePath()).length());
                    out.flush();
                    dis = new FileInputStream(STEGAPP_AUDIO_DIR + sFile.getFilePath());
                    break;
            }

            Integer len;
            while (true){
                byte[] buffer = new byte[kB32];
                len = dis.read(buffer);
                if(len == -1) {
                    break;
                }
                out.write(buffer, 0, len);
                out.flush();
            }
            dis.close();
        }

        out.writeUTF("commentData");
        out.flush();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessagePacker packer = MessagePack.newDefaultPacker(baos);
        packer
                .packInt(comment.id)
                .packInt(comment.stegId)
                .packInt(comment.getType())
                .packString(comment.profileId)
                .packLong(comment.date.getTime())
                .packLong(comment.time.getTime())
                .packString(comment.getText())
                .packInt(comment.getLikesCount())
                .packBoolean(comment.isLiked());
        packer.close();
        out.writeInt(baos.toByteArray().length);
        out.flush();
        out.write(baos.toByteArray(), 0, baos.toByteArray().length);
        out.flush();
    }

    private void commentToServer(DataInputStream in, DataOutputStream out) throws IOException {
        Boolean notStopped = true;
        CommentData commentData = new CommentData();
        while(notStopped){
            File imgFile = null;
            String imgExt = null;
            File stagFile = null;
            String what = in.readUTF();
            switch(what){
                case "fileToServer":
                    Integer fileType = in.readInt();
                    FileOutputStream dos = null;
                    String fileExt = in.readUTF();
                    switch(fileType){
                        case 1:
                            imgExt = fileExt;
                            imgFile = new File(STEGAPP_IMG_DIR + System.currentTimeMillis() + imgExt);
                            commentData.setImgData(imgFile.getName());
                            dos = new FileOutputStream(imgFile);
                            break;
                        case 2:
                            stagFile = new File(STEGAPP_VIDEO_DIR + System.currentTimeMillis() + fileExt);
                            commentData.setVideoData(stagFile.getName());
                            dos = new FileOutputStream(stagFile);
                            break;
                        case 3:
                            stagFile = new File(STEGAPP_AUDIO_DIR + System.currentTimeMillis() + fileExt);
                            commentData.setVoiceData(stagFile.getName());
                            dos = new FileOutputStream(stagFile);
                            break;
                    }
                    int fileSize = in.readInt();

                    readImgFile(dos, in, imgFile, imgExt, fileSize);

                    dos.close();
                    break;
                case "commentData":
                    int commentDataSize = in.readInt();
                    byte[] commentDataBytes = new byte[commentDataSize];
                    in.read(commentDataBytes);
                    MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(commentDataBytes);
                    commentData.setType(unpacker.unpackInt());
                    commentData.stegId = unpacker.unpackInt();
                    commentData.profileId = unpacker.unpackString();
                    if((commentData.getType() & CommentData.COMMENT_TEXT_MASK) != 0){
                        commentData.setText(unpacker.unpackString());
                    } else {
                        commentData.setText("clear");
                    }
                    unpacker.close();
                    DBHandler.addComment(commentData, dbConnection);
                    notStopped = false;
                    break;
            }
        }
    }

    private void likesFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        Integer stegId = in.readInt();
        ArrayList<String> likers = DBHandler.getLikes(stegId, dbConnection);
        ByteArrayOutputStream likeBaos = new ByteArrayOutputStream();
        MessagePacker likePacker = MessagePack.newDefaultPacker(likeBaos);
        likePacker.packArrayHeader(likers.size());
        for(String liker : likers){
            likePacker.packString(liker);
        }
        likePacker.close();
        out.writeInt(likeBaos.toByteArray().length);
        out.write(likeBaos.toByteArray(), 0, likeBaos.toByteArray().length);
    }

    private void commentLikesFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        Integer commentId = in.readInt();
        ArrayList<String> likers = DBHandler.getCommentLikes(commentId, dbConnection);
        ByteArrayOutputStream likeBaos = new ByteArrayOutputStream();
        MessagePacker likePacker = MessagePack.newDefaultPacker(likeBaos);
        likePacker.packArrayHeader(likers.size());
        for(String liker : likers){
            likePacker.packString(liker);
        }
        likePacker.close();
        out.writeInt(likeBaos.toByteArray().length);
        out.write(likeBaos.toByteArray(), 0, likeBaos.toByteArray().length);
    }

    private void notificationItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        String owner = in.readUTF();
        ArrayList<NewsData> news = DBHandler.getNews(owner, dbConnection);
        out.writeInt(news.size());
        out.flush();
        for(NewsData n: news){
            ByteArrayOutputStream newsBaos = new ByteArrayOutputStream();
            MessagePacker newsPacker = MessagePack.newDefaultPacker(newsBaos);
            newsPacker
                    .packInt(n.id)
                    .packString(n.type)
                    .packBoolean(n.sended)
                    .packString(n.profileId);
            if(!n.type.equals("friend"))
                newsPacker.packInt(n.stegId);
            newsPacker
                    .packLong(n.date.getTime())
                    .packLong(n.time.getTime());

            newsPacker.close();

            int len = newsBaos.toByteArray().length;
            out.writeInt(len);
            out.flush();
            out.write(newsBaos.toByteArray(), 0, len);
            out.flush();
        }
    }

    private void favoritesFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        String profileId = in.readUTF();
        ArrayList<FavoriteItem> favorites = DBHandler.getFavorites(profileId, dbConnection);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessagePacker packer = MessagePack.newDefaultPacker(baos);
        packer.packArrayHeader(favorites.size());


        for (FavoriteItem item : favorites){
            packer
                    .packInt(item.getId())
                    .packInt(item.getFavId())
                    .packString(item.getType());
        }
        packer.close();

        int len = baos.toByteArray().length;
        out.writeInt(len);
        out.flush();
        out.write(baos.toByteArray(), 0, len);
        out.flush();
    }

    private void saversFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        Integer stegId = in.readInt();
        ArrayList<LikeData> savers = DBHandler.getSavers(stegId, dbConnection);
        Integer count = savers.size();
        out.writeInt(count);
        out.flush();
        for(LikeData saver : savers){
            ByteArrayOutputStream saverBaos = new ByteArrayOutputStream();
            MessagePacker saverPacker = MessagePack.newDefaultPacker(saverBaos);
            saverPacker
                    .packInt(saver.id)
                    .packInt(saver.stegId)
                    .packString(saver.profileId)
                    .packString(saver.profileName);

            if(!saver.profileImg.equals("clear")){
                saverPacker.packString("photo");
                File sendFile = new File(STEGAPP_PROFILE_THUMBS_DIR + saver.profileImg);
                saverPacker.packString(sendFile.getName().substring(sendFile.getName().lastIndexOf(".")));
                byte[] photoBytes = new byte[(int) sendFile.length()];
                FileInputStream fis = new FileInputStream(sendFile);
                fis.read(photoBytes, 0, photoBytes.length);
                fis.close();
                saverPacker.packBinaryHeader(photoBytes.length);
                saverPacker.writePayload(photoBytes, 0, photoBytes.length);
            } else saverPacker.packString("clear");
            saverPacker.close();

            int len = saverBaos.toByteArray().length;
            out.writeInt(len);
            out.flush();
            out.write(saverBaos.toByteArray(), 0, len);
            out.flush();
        }
    }

    private void getersFromServer(DataInputStream in, DataOutputStream out) throws IOException {
        Integer stegId = in.readInt();
        ArrayList<LikeData> geters = DBHandler.getGeters(stegId, dbConnection);
        Integer count = geters.size();
        out.writeInt(count);
        out.flush();
        for(LikeData geter : geters){
            ByteArrayOutputStream geterBaos = new ByteArrayOutputStream();
            MessagePacker geterPacker = MessagePack.newDefaultPacker(geterBaos);
            geterPacker
                    .packInt(geter.id)
                    .packInt(geter.stegId)
                    .packString(geter.profileId)
                    .packString(geter.profileName);

            if(!geter.profileImg.equals("clear")){
                geterPacker.packString("photo");
                File sendFile = new File(STEGAPP_PROFILE_THUMBS_DIR + geter.profileImg);
                geterPacker.packString(sendFile.getName().substring(sendFile.getName().lastIndexOf(".")));
                byte[] photoBytes = new byte[(int) sendFile.length()];
                FileInputStream fis = new FileInputStream(sendFile);
                fis.read(photoBytes, 0, photoBytes.length);
                fis.close();
                geterPacker.packBinaryHeader(photoBytes.length);
                geterPacker.writePayload(photoBytes, 0, photoBytes.length);
            } else geterPacker.packString("clear");
            geterPacker.close();

            int len = geterBaos.toByteArray().length;
            out.writeInt(len);
            out.flush();
            out.write(geterBaos.toByteArray(), 0, len);
            out.flush();
        }
    }

    //    ********************************************************************

    private BufferedImage resizeImg(File bigImgFile, int width, int height) throws IOException {
        float dx, dy;
        int genX, genY;

        BufferedImage bigImg = ImageIO.read(bigImgFile);
        dx = ((float) width)/bigImg.getWidth();
        dy = ((float) height)/bigImg.getHeight();

        if(bigImg.getWidth()<=width && bigImg.getHeight()<=height){
            genX = bigImg.getWidth();
            genY = bigImg.getHeight();
        } else {
            if(dx <= dy){
                genX = width;
                genY = (int) (dx*bigImg.getHeight());
            } else {
                genX = (int) (dy*bigImg.getWidth());
                genY = height;
            }
        }

        BufferedImage smallImg = new BufferedImage(genX, genY, BufferedImage.TYPE_INT_RGB);
        smallImg.createGraphics().drawImage(ImageIO.read(bigImgFile).getScaledInstance(genX, genY, Image.SCALE_SMOOTH), 0, 0, null);
        return smallImg;
    }

    private void readImgFile(FileOutputStream dos, DataInputStream in, File file, String fileExt, Integer fileSize){
        try {
            readFile(dos, in, fileSize);

            // Creating Thumbnail image
            if(file != null){
                ImageIO.write(resizeImg(file, 960, 960), fileExt.substring(1), new File(STEGAPP_IMG_T_DIR + file.getName()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readFile(FileOutputStream dos, DataInputStream in, Integer fileSize) throws IOException{
        int i = 0;
        Integer len = 0;
        byte[] buffer;
        while(i < fileSize){
            buffer = new byte[kB32];
            len = in.read(buffer, 0, Math.min(fileSize - i, kB32));
            i = len + i;
            dos.write(buffer, 0, len);
            dos.flush();
        }
    }

    @Override
    public Address getAddress() {
        return address;
    }
}

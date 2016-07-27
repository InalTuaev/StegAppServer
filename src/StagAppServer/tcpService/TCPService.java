package StagAppServer.tcpService;


import StagAppServer.messageSystem.Abonent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public interface TCPService extends Abonent{

/**    Requests from client that must be handled by {@code handleRequest(Socket socket)} */

    String STEG_TO_SERVER = "stegToServer";
    String STEG_FROM_SERVER = "stegFromServer";
    String STEG_REQUEST = "stegRequest";
    String PROFILE_FROM_SERVER = "profileFromServer";
    String PROFILE_IMG_FROM_SERVER = "profileImgFromServer";
    String PROFILE_TO_SERVER = "profileToServer";
    String PROFILE_TO_SERVER_NO_IMG_NO_GEO = "profileToSrvrNoImgNoGeo";
    String PROFILE_IMG_TO_SERVER = "profileImgToServer";
    String WALL_ITEMS_FROM_SERVER = "wallItemsFromServer";
    String COMMENT_ITEMS_FROM_SERVER = "commentItemsFromServer";
    String COMMENT_FROM_SERVER = "commentFromServer";
    String COMMENT_REQUEST = "commentRequest";
    String COMMENT_TO_SERVER = "commentToServer";
    String LIKES_FROM_SERVER = "likesFromServer";
    String COMMENT_LIKES_FROM_SERVER = "comLikesFromServer";
    String NOTIFICATION_ITEMS_FROM_SERVER = "notificationItemsFromServer";
    String PROFILE_CORRESPONDENCE_FROM_SERVER = "profileCorrespondenceFromServer";
    String INCOME_PRIVATE_ITEMS_FROM_SERVER = "incomePrivateItemsFromServer";
    String OUTCOME_PRIVATE_ITEMS_FROM_SERVER = "outcomePrivateItemsFromServer";
    String OUTCOME_COMMON_ITEMS_FROM_SERVER = "outcomeCommonItemsFromServer";
    String INCOME_COMMON_ITEMS_FROM_SERVER = "incomeCommonItemsFromServer";
    String PROFILE_SENT_ITEMS = "profileSentItems";
    String SAVERS_FROM_SERVER = "saversFromServer";
    String GETERS_FROM_SERVER = "getersFromServer";
    String GET_IS_BLACK = "getIsBlack";
    String IS_ME_IN_BLACK_LIST = "isMeInBL";
    String CHECK_PASSWORD = "checkPassword";
    String DEL_PROFILE = "delProfile";
    String FAVORITES_FROM_SERVER = "favoritesFromServer";


    /**    Method that handle clients TCP requests  */
    void handleRequest(Socket socket);

////    Stegs
//    void stegToServer(DataInputStream in) throws IOException;
//    void stegRequest(DataInputStream in, DataOutputStream out) throws Exception;
//    void stegFromServer(DataInputStream in, DataOutputStream out) throws IOException;
//
////    Profile
//    void profileToServer(DataInputStream in) throws IOException;
//    void profileToServerNoImg(DataInputStream in) throws IOException;
//    void profileImgToServer(DataInputStream in) throws IOException;
//    void profileFromServer(DataInputStream in, DataOutputStream out) throws IOException;
//    void profileImgFromServer(DataInputStream in, DataOutputStream out) throws IOException;
//    void getIsBlack(DataInputStream in, DataOutputStream out) throws IOException;
//    void isMeInBlackList(DataInputStream in, DataOutputStream out) throws IOException;
//    void checkPassword(DataInputStream in, DataOutputStream out) throws IOException;
//    void delProfile(DataInputStream in, DataOutputStream out) throws IOException;
//
////    Steg Lists
//    void wallItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException;
//    void incomeCommonItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException;
//    void outcomePrivateItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException;
//    void outcomeCommonItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException;
//
//    void profileCorrespondenceFromServer(DataInputStream in, DataOutputStream out) throws IOException;
//    void profileSentItems(DataInputStream in, DataOutputStream out) throws IOException;
//    void incomePrivateItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException;
//
//
////    Comments
//    void commentItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException;
//    void commentRequest(DataInputStream in, DataOutputStream out) throws IOException;
//    void commentFromServer(DataInputStream in, DataOutputStream out) throws IOException;
//    void commentToServer(DataInputStream in, DataOutputStream out) throws IOException;
//
////    Likes
//    void likesFromServer(DataInputStream in, DataOutputStream out) throws IOException;
//    void commentLikesFromServer(DataInputStream in, DataOutputStream out) throws IOException;
//
////    News
//    void notificationItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException;
//
////    Favorites
//    void favoritesFromServer(DataInputStream in, DataOutputStream out) throws IOException;
//
//    void saversFromServer(DataInputStream in, DataOutputStream out) throws IOException;
//    void getersFromServer(DataInputStream in, DataOutputStream out) throws IOException;

}

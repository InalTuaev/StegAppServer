package StagAppServer.fcm;


public class FcmConsts {
    public static final String BROADCAST_TOPIC = "/topics/broadcast";

    public static final String NOTIFICATION_TYPE = "notificationType";
    public static final String NOTIFICATION_TYPE_STEG_COME_IN = "stegComeIn";
    public static final String NOTIFICATION_TYPE_UPDATE_APP = "update";
    public static final String FORM_USER = "fromUser";
    public static final String STEG_ID = "stegId";
    public static final String UPDATE_TEXT = "updateText";
    public static final String STEG_SENDER_ID = "senderId";
    public static final String STEG_SENDER_CITY = "senderCity";
    public static final String STEG_MODE = "stegMode";

    public static final String COLLAPSE_KEY_NOTIFICATION = "notification";
    public static final String COLLAPSE_KEY_STEG = "steg";
    public static final String COLLAPSE_KEY_UPDATE = "update";

    public static final String TOKEN = "token";

    public static final int ONE_MINUTE = 60;
    public static final int ONE_HOUR = ONE_MINUTE * 60;
    public static final int ONE_DAY = ONE_HOUR * 24;
    public static final int ONE_WEEK = ONE_DAY * 7;
    public static final int MAX_TIME = ONE_WEEK * 4;
}

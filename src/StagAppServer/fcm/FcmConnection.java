package StagAppServer.fcm;


import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Notification;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public class FcmConnection {
    private static final String databaseUri = "https://stegapp-b24be.firebaseio.com/";
    private static final String serverKey = "AIzaSyCEH8rYPGEughCvexn1Nh7qt9yT8eP07J0";

    private static FcmConnection instance;

    private Sender sender;

    private FcmConnection() {
        initServer();
        sender = new Sender(serverKey);
    }

    public static FcmConnection getInstance(){
        if (instance == null){
            synchronized (FcmConnection.class) {
                if (instance == null)
                instance = new FcmConnection();
            }
        }
        return instance;
    }

    private void initServer(){
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setServiceAccount(new FileInputStream("/home/inal/fcmCredentials/stegAppCreds.json"))
                    .setDatabaseUrl(databaseUri)
                    .build();
            FirebaseApp.initializeApp(options);
        } catch (FileNotFoundException e){
            e.printStackTrace();
        }
    }

    public void sendNotification(String toToken, Map<String, String> data){
        Message.Builder builder = new Message.Builder();
        for (String key : data.keySet()){
            builder.addData(key, data.get(key));
        }
        builder.priority(Message.Priority.NORMAL);
        builder.collapseKey(FcmConsts.COLLAPSE_KEY_NOTIFICATION);
        builder.timeToLive(FcmConsts.ONE_DAY * 2);
        Message msg = builder.build();

        try {
            Result res = sender.send(msg, toToken, 5);
            System.out.println("Result: " + res.toString());
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void sendNewStegNotification(String toToken, Integer stegId, String senderId, String senderCity, Integer stegMode){
        Message.Builder builder = new Message.Builder();
        builder.addData(FcmConsts.NOTIFICATION_TYPE, FcmConsts.NOTIFICATION_TYPE_STEG_COME_IN);
        builder.addData(FcmConsts.STEG_ID, stegId.toString());
        builder.addData(FcmConsts.STEG_SENDER_ID, senderId);
        builder.addData(FcmConsts.STEG_SENDER_CITY, senderCity);
        builder.addData(FcmConsts.STEG_MODE, stegMode.toString());

        builder.priority(Message.Priority.NORMAL);
        builder.collapseKey(FcmConsts.COLLAPSE_KEY_NOTIFICATION);
        builder.timeToLive(FcmConsts.ONE_MINUTE * 10);

        Message msg = builder.build();
        try {
            sender.send(msg, toToken, 1);
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}

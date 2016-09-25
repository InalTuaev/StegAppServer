package StagAppServer;


import StagAppServer.fcm.FcmConnection;
import StagAppServer.fcm.FcmConsts;

import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;

public class StegSender implements Runnable {

    public static final long SEC20 = 1000 * 20;
    public static final long ONE_SEC = 1000;
    public static final long ONE_MIN = ONE_SEC * 60;

    private static StegSender instance;
    private Boolean interrupted = false;
    private Connection dbConnection;
    private final ConcurrentLinkedDeque<Integer> stegsQueue;

    private StegSender() {
        this.dbConnection = WsHandler.getInstance().dbConnection;
        stegsQueue = new ConcurrentLinkedDeque<>();
//        stegsQueue.addAll(DBHandler.getUnrecievedStegIds(dbConnection));
    }

    public static StegSender getInstance() {
        if (instance == null) {
            synchronized (StegSender.class) {
                if (instance == null)
                    instance = new StegSender();
            }
        }
        return instance;
    }

    public void interrupt() {
        interrupted = true;
    }

    @Override
    public void run() {
        while (!interrupted) {
            try {
                Thread.sleep(SEC20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            ArrayList<Integer> notSendedStegs = new ArrayList<>();
            Set<String> usersSet = new HashSet<>();
            stegsQueue.addAll(DBHandler.getUnrecievedStegIds(dbConnection));
            while (!stegsQueue.isEmpty()) {
                Integer stegId = stegsQueue.pollFirst();
                Map<String, String> receiverMap = DBHandler.getReceiverForSteg(stegId, dbConnection);
                if (receiverMap.size() > 0) {
                    String token = receiverMap.get(FcmConsts.TOKEN);
                    String senderName = receiverMap.get(FcmConsts.STEG_SENDER_ID);
                    String senderCity = receiverMap.get(FcmConsts.STEG_SENDER_CITY);
                    Integer stegMode = Integer.parseInt(receiverMap.get(FcmConsts.STEG_MODE));
                    if (!usersSet.contains(token)) {
                        FcmConnection.getInstance().sendNewStegNotification(token, stegId, senderName, senderCity, stegMode);
//                        System.out.println("inced by: " + token);
//                        DBHandler.incStegReceived(stegId, dbConnection);
                        usersSet.add(token);
                    }
                } else {
//                    notSendedStegs.add(stegId);
                }
            }
//            Collections.reverse(notSendedStegs);
//            stegsQueue.addAll(notSendedStegs);
//            System.out.println("---------------------------------------------\n++queue: " + stegsQueue.size() + " notSended: " + notSendedStegs.size());
        }
    }

    public void addStegToQueueFirst(Integer stegId) {
        stegsQueue.addFirst(stegId);
    }

    public void addStegToQueueLast(Integer stegId) {
        stegsQueue.addLast(stegId);
    }

    public void addStegToQueueLast(Integer stegId, Integer count) {
        for (int i = 0; i < count; i++)
            stegsQueue.addLast(stegId);
    }

    public void addStegToQueueFirst(Integer stegId, Integer count) {
        for (int i = 0; i < count; i++)
            stegsQueue.addFirst(stegId);
    }

    public void removeAllrecords(Integer stegId) {
        stegsQueue.removeIf(new Predicate<Integer>() {
            @Override
            public boolean test(Integer integer) {
                return (integer.equals(stegId));
            }
        });
    }
}

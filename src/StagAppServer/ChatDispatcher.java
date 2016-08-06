package StagAppServer;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

class ChatDispatcher {

    private volatile ConcurrentHashMap<Integer, ConcurrentHashMap<String, WsHandler.ChatWebSocket>> chatContainer;

    ChatDispatcher(){
        chatContainer = new ConcurrentHashMap<>();
    }

    void addListener(Integer chatId, WsHandler.ChatWebSocket listener){
        if(chatContainer.containsKey(chatId)){
            chatContainer.get(chatId).put(listener.getUserId(), listener);
        } else {
            ConcurrentHashMap<String, WsHandler.ChatWebSocket> newListenerMap = new ConcurrentHashMap<>();
            newListenerMap.put(listener.getUserId(), listener);
            chatContainer.put(chatId, newListenerMap);
        }
    }

    void removeListener(Integer chatId, WsHandler.ChatWebSocket listener){
        if(chatContainer.containsKey(chatId)){
            chatContainer.get(chatId).remove(listener.getUserId());
            if (chatContainer.get(chatId).size() == 0){
                chatContainer.remove(chatId);
            }
        }
    }

    void sendMessage(Integer chatId, Integer msgId, String senderId){
        StagData steg = DBHandler.getSteg(chatId, senderId, WsHandler.getInstance().dbConnection);

        ArrayList<String> notificationList = new ArrayList<>();

        if (steg.mesReciever.equals("common")){
            notificationList = DBHandler.getStegNotifiers(steg.stegId, WsHandler.getInstance().dbConnection);
        } else {
            notificationList.add(steg.mesSender);
            if (!steg.isDeleted())
                notificationList.add(steg.mesReciever);
        }

        if(chatContainer.containsKey(chatId)){
            try {
                ByteArrayOutputStream message = createMessage(chatId, msgId, senderId);
                Iterator<String> iter = chatContainer.get(chatId).keySet().iterator();
                while (iter.hasNext()) {
                    String chatProfile = iter.next();

                    WsHandler.ChatWebSocket socket = chatContainer.get(chatId).get(chatProfile);
                    socket.getConnection().sendMessage(message.toByteArray(), 0, message.toByteArray().length);

                    notificationList.remove(chatProfile);  // unsubscribe profile from push notification
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        String commentSender = senderId;
        if (senderId.equals(steg.mesSender) && steg.anonym)
            commentSender = "clear";

        for (String profileId : notificationList){
            if (!profileId.equals("common") && !profileId.equals(senderId)){
                WsHandler.getInstance().sendNotification(NewsData.NOTIFICATION_COMMENT, profileId, commentSender, chatId);
                DBHandler.justAddCommentNews(NewsData.NOTIFICATION_TYPE_COMMENT, commentSender, profileId, steg.stegId, WsHandler.getInstance().dbConnection, false);
            }
        }
    }

    void sendLike(Integer chatId, Integer msgId, Boolean liked, String liker){
        if (chatContainer.containsKey(chatId)){
            try{
                ByteArrayOutputStream message = createLike(chatId, msgId, liked);
                Iterator<String> iter = chatContainer.get(chatId).keySet().iterator();
                while (iter.hasNext()){
                    String chatProfile = iter.next();
                    if (!chatProfile.equals(liker)) {
                        WsHandler.ChatWebSocket socket = chatContainer.get(chatId).get(chatProfile);
                        socket.getConnection().sendMessage(message.toByteArray(), 0, message.toByteArray().length);
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    Boolean isProfileInChat (String profileId, Integer chatId){
        if (chatContainer.containsKey(chatId)){
            ConcurrentHashMap<String, WsHandler.ChatWebSocket> chat = chatContainer.get(chatId);
            return chat.containsKey(profileId);
        } else
            return false;
    }

    private ByteArrayOutputStream createMessage(Integer chatId, Integer msgId, String senderId) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessagePacker packer = MessagePack.newDefaultPacker(baos);
            packer.packString("newChatMes")
                    .packInt(chatId)
                    .packInt(msgId)
                    .packString(senderId);
            packer.close();
        return baos;
    }

    private ByteArrayOutputStream createLike(Integer chatId, Integer msgId, Boolean liked) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessagePacker packer = MessagePack.newDefaultPacker(baos);
        packer.packString("chatMesLiked")
                .packInt(chatId)
                .packInt(msgId)
                .packBoolean(liked);
        packer.close();
        return baos;
    }

    private void iterateMap(ConcurrentHashMap<Integer, ConcurrentHashMap<String, WsHandler.ChatWebSocket>> set){
        Iterator<Integer> parentIter = set.keySet().iterator();
        while(parentIter.hasNext()){
            Integer chatIterator = parentIter.next();
            Iterator<String> childIter = set.get(chatIterator).keySet().iterator();
            while(childIter.hasNext()){

            }
        }
    }
}

package StagAppServer;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
            System.out.println("added chatId: " + chatId + " profileId: " + listener.getUserId());
        } else {
            ConcurrentHashMap<String, WsHandler.ChatWebSocket> newListenerMap = new ConcurrentHashMap<>();
            newListenerMap.put(listener.getUserId(), listener);
            chatContainer.put(chatId, newListenerMap);
            System.out.println("new chatId: " + chatId + " profileId: " + listener.getUserId());
        }
        iterateMap(chatContainer);
    }

    void removeListener(Integer chatId, WsHandler.ChatWebSocket listener){
        if(chatContainer.containsKey(chatId)){
            chatContainer.get(chatId).remove(listener.getUserId());
            if (chatContainer.get(chatId).size() == 0){
                chatContainer.remove(chatId);
            }
        }
        iterateMap(chatContainer);
    }

    void sendMessage(Integer chatId, Integer msgId, String senderId){
        if(chatContainer.containsKey(chatId)){
            try {
                ByteArrayOutputStream message = createMessage(chatId, msgId, senderId);
                Iterator<String> iter = chatContainer.get(chatId).keySet().iterator();
                while (iter.hasNext()) {
                    String chatProfile = iter.next();
                    System.out.print("chat: " + chatId);
                    WsHandler.ChatWebSocket socket = chatContainer.get(chatId).get(chatProfile);
                    if (!socket.getUserId().equals(senderId)) {
                        socket.getConnection().sendMessage(message.toByteArray(), 0, message.toByteArray().length);
                        System.out.println(" +");
                    } else {
                        System.out.println(" -");
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }
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

    private void iterateMap(ConcurrentHashMap<Integer, ConcurrentHashMap<String, WsHandler.ChatWebSocket>> set){
        Iterator<Integer> parentIter = set.keySet().iterator();
        while(parentIter.hasNext()){
            Integer chatIterator = parentIter.next();
            System.out.println("* " + chatIterator + " +++++++++++++++++++++++++++");
            Iterator<String> childIter = set.get(chatIterator).keySet().iterator();
            while(childIter.hasNext()){
                System.out.println("    *- " + childIter.next() + "+ + + + + + +");
            }
        }
    }
}
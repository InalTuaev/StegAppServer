package StagAppServer.waveChats;


import StagAppServer.WsHandler;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class WaveChatsDispatcher {
    private static WaveChatsDispatcher instance = new WaveChatsDispatcher();
    private final ConcurrentHashMap<String, WaveChat> waveChats = new ConcurrentHashMap<>();
    private final ConcurrentHashSet<WsHandler.ChatWebSocket> chatCandidates = new ConcurrentHashSet<>();

    private WaveChatsDispatcher() {
    }

    public static WaveChatsDispatcher getInstance(){
        return instance;
    }

    public void addCandidate(WsHandler.ChatWebSocket userSocket){
        chatCandidates.add(userSocket);
    }

    public void removeCandidate(WsHandler.ChatWebSocket userSocket){
        chatCandidates.remove(userSocket);
    }

    public void addWaveChat(String chatName){
        waveChats.put(chatName, new WaveChat(chatName));
    }

    public void addWaveChat(String chatName, String password){
        waveChats.put(chatName, new WaveChat(chatName, password));
    }

    public void addUserToChat(String chatName, WsHandler.ChatWebSocket userSocket, String password){
        waveChats.get(chatName).addUser(userSocket, password);
    }

    public void removeUserFromWaveChat(String chatName, WsHandler.ChatWebSocket userSocket){
        waveChats.get(chatName).removeUser(userSocket);
    }

    public void removeUserFromWaveChat(WsHandler.ChatWebSocket userSocket){
        waveChats.forEach((chatName, waveChat) -> waveChat.removeUser(userSocket));
    }

    public void sendTextMessage(String chatName, String sender, String mes){
        waveChats.get(chatName).sendTextMessage(mes, sender);
        sendNotificationsToCandidates(chatName);
    }

    private void sendNotificationsToCandidates(String chatName){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessagePacker packer = MessagePack.newDefaultPacker(baos);
        try {
            packer.packString("waveChatNotes")
                    .packString(chatName)
                    .close();
        } catch (IOException e){
            e.printStackTrace();
            return;
        }
        chatCandidates.forEach((userSocket) -> {
            try {
                userSocket.getConnection().sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
            } catch (IOException e){
                e.printStackTrace();
            }
        });
    }
}

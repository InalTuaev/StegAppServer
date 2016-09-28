package StagAppServer.waveChats;


import StagAppServer.WsHandler;

import java.util.concurrent.ConcurrentHashMap;

public class WaveChatsDispatcher {
    private static WaveChatsDispatcher instance = new WaveChatsDispatcher();
    private final ConcurrentHashMap<String, WaveChat> waveChats = new ConcurrentHashMap<>();

    private WaveChatsDispatcher() {
    }

    public static WaveChatsDispatcher getInstance(){
        return instance;
    }

    public void addWaveChat(String chatName){
        waveChats.put(chatName, new WaveChat(chatName));
        System.out.println("AddWaveChat: " + chatName + " length: " + waveChats.size());
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

    public void sendTextMessage(String chatName, String sender, String mes){
        waveChats.get(chatName).sendTextMessage(mes, sender);
    }
}

package StagAppServer.waveChats;

import StagAppServer.WsHandler;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class WaveChat {
    private final String chatName;
    private final String password;
    private final Boolean commonChat;

    private final ConcurrentHashSet<WsHandler.ChatWebSocket> userSet = new ConcurrentHashSet<>();

    public WaveChat(String chatName, String password) {
        this.chatName = chatName;
        this.password = password;
        this.commonChat = true;
    }

    public WaveChat(String chatName) {
        this.chatName = chatName;
        this.password = null;
        this.commonChat = false;
    }

    public boolean addUser(WsHandler.ChatWebSocket user, String password) {
            userSet.add(user);
            System.out.println("Added USer To WaveChat: " + chatName + " : " + user.getUserId());
            return true;
    }

    public void removeUser(WsHandler.ChatWebSocket user) {
        userSet.remove(user);
        System.out.println("Removed USer From WaveChat: " + chatName + " : " + user.getUserId());
    }

    public void sendTextMessage(String mes, String sender) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessagePacker packer = MessagePack.newDefaultPacker(baos);
        try {
            packer.packString("waveChat")
                    .packString(chatName)
                    .packString(sender)
                    .packString(mes)
                    .close();
            for (WsHandler.ChatWebSocket userSocket : userSet) {
                userSocket.getConnection().sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
                System.out.println("MEssage sended To : " + chatName + " : " + userSocket.getUserId() + " from: " + sender);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

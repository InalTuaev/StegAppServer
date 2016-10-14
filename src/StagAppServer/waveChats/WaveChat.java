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
        sendUsersCount(userSet.size(), user.getUserId(), true);
        return true;
    }

    public boolean removeUser(WsHandler.ChatWebSocket user) {
        if (userSet.remove(user)) {
            sendUsersCount(userSet.size(), user.getUserId(), false);
            return true;
        }
        return false;
    }

    public void sendUsersCount(Integer count, String who, Boolean add) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessagePacker packer = MessagePack.newDefaultPacker(baos);
        try {
            packer.packString("waveChatCount")
                    .packString(chatName)
                    .packInt(count)
                    .packBoolean(add)
                    .packString(who)
                    .close();
            for (WsHandler.ChatWebSocket userSocket : userSet) {
                userSocket.getConnection().sendMessage(baos.toByteArray(), 0, baos.toByteArray().length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

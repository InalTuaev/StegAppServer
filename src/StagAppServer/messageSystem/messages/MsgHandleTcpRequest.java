package StagAppServer.messageSystem.messages;


import StagAppServer.messageSystem.Abonent;
import StagAppServer.messageSystem.Address;
import StagAppServer.tcpService.TCPService;

import java.net.Socket;

public class MsgHandleTcpRequest extends MsgToTcpService{
    private Socket socket;

    public MsgHandleTcpRequest(Address to, Socket socket) {
        super(to);
        this.socket = socket;
    }

    @Override
    void exec(TCPService tcpService) {
        tcpService.handleRequest(socket);
    }
}

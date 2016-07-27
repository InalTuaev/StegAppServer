package StagAppServer.messageSystem.messages;


import StagAppServer.messageSystem.Abonent;
import StagAppServer.messageSystem.Address;
import StagAppServer.tcpService.TCPService;

public abstract class MsgToTcpService extends Msg{
    public MsgToTcpService(Address to) {
        super(to);
    }

    @Override
    public Address getTo() {
        return super.getTo();
    }

    @Override
    public void exec(Abonent abonent) {
        if (abonent instanceof TCPService){
            exec((TCPService) abonent);
        }
    }

    abstract void exec(TCPService tcpService);
}

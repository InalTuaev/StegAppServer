package StagAppServer.messageSystem.messages;

import StagAppServer.messageSystem.Abonent;
import StagAppServer.messageSystem.Address;

public abstract class Msg {
    final private Address to;

    public Msg(Address to){
        this.to = to;
    }

    public Address getTo(){
        return to;
    }

    public abstract void exec(Abonent abonent);
}

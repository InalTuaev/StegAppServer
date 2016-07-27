package StagAppServer.messageSystem;


import StagAppServer.messageSystem.messages.AddressService;
import StagAppServer.messageSystem.messages.Msg;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageSystem {

    private final Map<Address, LinkedBlockingQueue<Msg>> messages = new HashMap<>();
    private final AddressService addressService = new AddressService();

    public MessageSystem(){
    }

    public AddressService getAddressService(){
        return addressService;
    }

    public void addService(Abonent abonent){
        messages.put(abonent.getAddress(), new LinkedBlockingQueue<>());
    }

    public void sendMessage(Msg message){
        Queue<Msg> messageQueue = messages.get(message.getTo());
        messageQueue.add(message);
    }

    public void execForAbonent(Abonent abonent) throws InterruptedException{
        LinkedBlockingQueue<Msg> messageQueue = messages.get(abonent.getAddress());
        while (!messageQueue.isEmpty()){
            Msg message = messageQueue.take();
            message.exec(abonent);
        }
    }
}

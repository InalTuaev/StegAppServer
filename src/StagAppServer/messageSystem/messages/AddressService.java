package StagAppServer.messageSystem.messages;

import StagAppServer.messageSystem.Address;
import StagAppServer.tcpService.TCPService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AddressService {
    private final List<Address> tcpServices = new ArrayList<>();
    private final AtomicInteger tcpServicesCounter = new AtomicInteger();

    public synchronized Address getTcpServiceAddress(){
        int index = tcpServicesCounter.incrementAndGet();
        if (index >= tcpServices.size()){
            index = 0;
        }
        return tcpServices.get(index);
    }

    public void registerTcpService(TCPService tcpService){
        tcpServices.add(tcpService.getAddress());
    }
}

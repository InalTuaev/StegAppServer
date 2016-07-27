package StagAppServer;


import StagAppServer.messageSystem.Address;
import StagAppServer.messageSystem.MessageSystem;
import StagAppServer.messageSystem.messages.MsgHandleTcpRequest;
import StagAppServer.tcpService.TCPServiceImpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

class FileTransferHandler extends Thread {
	private static final int TCP_SOCKET_PORT = 8088;
	private static final int TCP_SERVICES_THREAD_POOL_NUMBER = 30;
	private ServerSocket serverSocket;
	private final Connection dbConnection;
//	private ExecutorService executorService;
	private final MessageSystem messageSystem;
	private final List<Thread> tcpServicesThreadPool;
	
	
	FileTransferHandler (Connection dbConnection){
//		executorService = Executors.newCachedThreadPool();
		this.dbConnection = dbConnection;
		messageSystem = new MessageSystem();
		tcpServicesThreadPool = new ArrayList<>();

		try {
			serverSocket = new ServerSocket(TCP_SOCKET_PORT);
			System.out.println("serverSocket created");
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < TCP_SERVICES_THREAD_POOL_NUMBER; i++){
			Thread tcpThread = new Thread(new TCPServiceImpl(dbConnection, messageSystem));
			tcpThread.setName("TcpService: " + Integer.toString(i));
			tcpServicesThreadPool.add(tcpThread);
		}
		 tcpServicesThreadPool.forEach((thread -> thread.start()));
	}
	
	public void run() {
		while(!this.isInterrupted()) {
			try {
				Socket socket = serverSocket.accept();
				Address tcpServiceAddres = messageSystem.getAddressService().getTcpServiceAddress();
				messageSystem.sendMessage(new MsgHandleTcpRequest(tcpServiceAddres, socket));
//				executorService.submit(new StagHandler(socket, dbConnection));
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

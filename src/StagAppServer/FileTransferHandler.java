package StagAppServer;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class FileTransferHandler extends Thread {
	private static final int TCP_SOCKET_PORT = 8088;
	private ServerSocket serverSocket;	
	private Connection dbConnection;
	private ExecutorService executorService;
	
	
	
	public FileTransferHandler (Connection dbConnection){
		executorService = Executors.newCachedThreadPool();
		this.dbConnection = dbConnection;
		try {
			serverSocket = new ServerSocket(TCP_SOCKET_PORT);
			System.out.println("serverSocket created");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		while(!this.isInterrupted()) {
			try {
				Socket socket = serverSocket.accept();
				executorService.submit(new StagHandler(socket, dbConnection));
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

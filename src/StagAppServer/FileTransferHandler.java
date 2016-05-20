package StagAppServer;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;

public class FileTransferHandler extends Thread {
	Integer port = 8088;
	private ServerSocket serverSocket;	
	Connection dbConnection;
	
	
	
	public FileTransferHandler (Connection dbConnection){
		this.dbConnection = dbConnection;
		try {
			serverSocket = new ServerSocket(port);
			System.out.println("serverSocket created");
			start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		while(!this.isInterrupted()) {
			try {
				Socket socket = serverSocket.accept();
				StagHandler.handleStag(socket, dbConnection);
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

package StagAppServer;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.eclipse.jetty.server.Server;


public class WsProxy {
	private static final String STEGAPP_IMG_DIR = "StegApp/media/img/";
	private static final String STEGAPP_IMG_T_DIR = "StegApp/media/img/thumbs/";
	private static final String STEGAPP_VIDEO_DIR = "StegApp/media/video/";
	private static final String STEGAPP_AUDIO_DIR = "StegApp/media/audio/";
	private static final String STEGAPP_PROFILE_PHOTO_DIR = "StegApp/avatars/";
	private static final String STEGAPP_PROFILE_THUMBS_DIR = "StegApp/thumbs/";
	
 public static void main(String[] args) throws Exception {
	int port = 8087;
	File path = new File(STEGAPP_IMG_DIR);
	if (!path.exists()) {
		path.mkdirs();
		path = new File(STEGAPP_VIDEO_DIR);
		path.mkdirs();
		path = new File(STEGAPP_AUDIO_DIR);
		path.mkdirs();
		path = new File(STEGAPP_PROFILE_PHOTO_DIR);
		path.mkdirs();
		path = new File(STEGAPP_PROFILE_THUMBS_DIR);
		path.mkdirs();
		path = new File(STEGAPP_IMG_T_DIR);
		path.mkdirs();
		path = null;
	}
	Class.forName("org.postgresql.Driver");
	Connection dbConnection = null;
	try{
		dbConnection = DriverManager.getConnection("jdbc:postgresql://188.225.77.207:5432/stegdatabase", "stegserver", "revresgets");
	} catch (SQLException e) {
		e.printStackTrace();
		System.out.println("not connected to DB!");
	}

	Server jetty = new Server(port);
	jetty.setHandler(new WsHandler(dbConnection));
	jetty.start();
	FileTransferHandler fileTransferHandler = new FileTransferHandler(dbConnection);
 }
}

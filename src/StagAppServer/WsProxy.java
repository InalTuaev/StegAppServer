package StagAppServer;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import StagAppServer.fcm.FcmConnection;
import StagAppServer.servlets.MyHttpServlet;
import StagAppServer.waveChats.WaveChatsDispatcher;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;


public class WsProxy {
	private static final int WEBSOCKET_PORT = 8087;
	private static final int HTTP_PORT = 8080;

	private static final String STEGAPP_IMG_DIR = "StegApp/media/img/";
	private static final String STEGAPP_IMG_T_DIR = "StegApp/media/img/thumbs/";
	private static final String STEGAPP_VIDEO_DIR = "StegApp/media/video/";
	private static final String STEGAPP_AUDIO_DIR = "StegApp/media/audio/";
	private static final String STEGAPP_PROFILE_PHOTO_DIR = "StegApp/avatars/";
	private static final String STEGAPP_PROFILE_THUMBS_DIR = "StegApp/thumbs/";

    public static void main(String[] args) throws Exception {
    	FcmConnection fcmConnection = FcmConnection.getInstance();
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
	    }
	    Class.forName("org.postgresql.Driver");
	    Connection dbConnection = null;
	    try{
		    dbConnection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/stegdatabase", "stegserver", "revresgets");
		    dbConnection.setAutoCommit(true);
	    } catch (SQLException e) {
		    e.printStackTrace();
		    System.out.println("not connected to DB!");
	    }

        WsHandler wsHandler = new WsHandler(dbConnection);
        Server wsServer = new Server(WEBSOCKET_PORT);
        wsServer.setHandler(wsHandler);
        wsServer.start();

		FileTransferHandler fileTransferHandler = new FileTransferHandler(dbConnection);
		fileTransferHandler.start();

		Server httpServer = new Server(HTTP_PORT);

		ServletHandler httpServletHandler = new ServletHandler();
		httpServletHandler.addServletWithMapping(MyHttpServlet.class, "/emailValidation");
		httpServer.setHandler(httpServletHandler);
		httpServer.start();

		WaveChatsDispatcher.getInstance().addWaveChat("Splash");
	}
}

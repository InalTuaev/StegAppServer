package StagAppServer;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

public class StagHandler {
	private static final String STEGAPP_IMG_DIR = "StegApp/media/img/";
	private static final String STEGAPP_IMG_T_DIR = "StegApp/media/img/thumbs/";
	private static final String STEGAPP_VIDEO_DIR = "StegApp/media/video/";
	private static final String STEGAPP_AUDIO_DIR = "StegApp/media/audio/";
	private static final String STEGAPP_PROFILE_PHOTO_DIR = "StegApp/avatars/";
	private static final String STEGAPP_PROFILE_THUMBS_DIR = "StegApp/thumbs/";
	
	private static final Integer kB64 = 64*1024;
	

	public static void handleStag(Socket socket, Connection dbConnection){
		Thread handleThread = new Thread() {
			public void run(){
				try {
					socket.setTcpNoDelay(true);
					DataInputStream in = new DataInputStream(socket.getInputStream());
					DataOutputStream out = new DataOutputStream(socket.getOutputStream());
					String where = in.readUTF();
					switch(where){
					case "stegToServer":
						stegToServer(in, dbConnection);
						break;
					case "stegFromServer":
						stegFromServer(in, out);
						break;
					case "stegFromServerWithoutImg":
//						stegFromServerWithoutImg(in, out);
						break;
					case "profileFromServer":
						profileFromServer(in, out);
						break;
					case "profileImgFromServer":
						profileImgFromServer(in, out);
						break;
					case "profileToServer":
						profileToServer(in);
						break;
					case "wallFromServer":
						wallFromServer(in, out);
						break;
					case "wallItemsFromServer":
						wallItemsFromServer(in, out);
						break;
					case "commentsFromServer":
						commentsFromServer(in, out);
						break;
					case "commentItemsFromServer":
						commentItemsFromServer(in, out);
						break;
					case "commentFromServer":
						commentFromServer(in, out);
						break;
					case "commentToServer":
						commentToServer(in, out);
						break;
					case "likesFromServer":
						likesFromServer(in, out);
						break;
					case "newsFromServer":
						newsFromServer(in, out);
						break;
					case "notificationItemsFromServer":
						notificationItemsFromServer(in, out);
						break;
					case "incomePrivateFromServer":
						incomePrivateFromServer(in, out);
						break;
					case "incomePrivateItemsFromServer":
						incomePrivateItemsFromServer(in, out);
						break;
					case "outcomePrivateFromServer":
						outcomePrivateFromServer(in, out);
						break;
					case "outcomePrivateItemsFromServer":
						outcomePrivateItemsFromServer(in, out);
						break;
					case "outcomeCommonFromServer":
						outcomeCommonFromServer(in, out);
						break;
					case "outcomeCommonItemsFromServer":
						outcomeCommonItemsFromServer(in, out);
						break;
					case "incomeCommonItemsFromServer":
						incomeCommonItemsFromServer(in, out);
						break;
					case "saversFromServer":
						saversFromServer(in, out);
						break;
					case "getersFromServer":
						getersFromServer(in, out);
						break;
					case "friendsFromServer":
						friendsFromServer(in, out);
						break;
					case "getIsBlack":
						getIsBlack(in, out);
						break;
					case "isMeInBL":
						isMeInBlackList(in, out);
						break;
					case "checkPassword":
						checkPassword(in, out);
						break;
					case "delProfile":
						delProfile(in, out);
						break;
					}
					out.close();
					in.close();
					socket.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			public void stegToServer(DataInputStream in, Connection dbConnection) throws IOException{
				Boolean notStopped = true;
				Boolean isAddToMyWall = false;
				StagData stagData = new StagData();
				while(notStopped){
					File imgFile = null;
					String imgExt = null;
					File stagFile = null;
					String what = in.readUTF();
					switch(what){
					case "fileToServer":
						Integer fileType = in.readInt();
						FileOutputStream dos = null;
						System.out.println("fileType: " + fileType);
						String fileExt = in.readUTF();
						switch(fileType){
						case 1:
							imgExt = fileExt;
							imgFile = new File(STEGAPP_IMG_DIR + System.currentTimeMillis() + imgExt);
							stagData.cameraDataFile = imgFile.getName();
							dos = new FileOutputStream(imgFile);
							break;
						case 2:
							stagFile = new File(STEGAPP_VIDEO_DIR + System.currentTimeMillis() + fileExt);
							stagData.cameraDataFile = stagFile.getName();
							dos = new FileOutputStream(stagFile);
							break;
						case 3:
							stagFile = new File(STEGAPP_AUDIO_DIR + System.currentTimeMillis() + fileExt);
							stagData.voiceDataFile = stagFile.getName();
							dos = new FileOutputStream(stagFile);
							break;
						}
						int fileSize = in.readInt();
						
						int i = 0;
						Integer len = 0;
						try {
							byte[] buffer;
							while(i < fileSize){
								buffer = new byte[kB64];									
								len = in.read(buffer, 0, Math.min(fileSize - i, kB64));
								i += len;
								dos.write(buffer, 0, len);
								dos.flush();
							}
							// Creating Thumbnail image
							if(imgFile != null){
								ImageIO.write(resizeImg(imgFile, 960, 960), imgExt.substring(1), new File(STEGAPP_IMG_T_DIR + imgFile.getName()));
							}
														
						} catch (EOFException e) {
							dos.close();
						}
						dos.close();
						break;
					case "stagData":
						int stagDataSize = in.readInt();
						byte[] stagDataBytes = new byte[stagDataSize];
						in.read(stagDataBytes);
						MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(stagDataBytes);
						stagData.mesType = unpacker.unpackString();
						stagData.mesSender = unpacker.unpackString();
						stagData.mesReciever = unpacker.unpackString();
						stagData.stagType = unpacker.unpackInt();
						if((stagData.stagType & 1) != 0){
							stagData.mesText = unpacker.unpackString();
						}
						stagData.lifeTime = unpacker.unpackInt();
						stagData.anonym = unpacker.unpackBoolean();
						stagData.filter = unpacker.unpackInt();
						isAddToMyWall = unpacker.unpackBoolean();
						unpacker.close();							
						Integer newStegId = DBHandler.addSteg(stagData, dbConnection);
						if(isAddToMyWall){
							DBHandler.markRecievedSteg(newStegId, dbConnection);
							DBHandler.stegToWall(newStegId, stagData.mesSender, dbConnection);
						} else {
							if(!stagData.mesReciever.equals("common")){
								if(stagData.anonym){
									DBHandler.addNews("privateSteg", "clear", stagData.mesReciever, newStegId, dbConnection);
								} else {
									DBHandler.addNews("privateSteg", stagData.mesSender, stagData.mesReciever, newStegId, dbConnection);
								}
							}
						}
						notStopped = false;
						break;
					}
				}
			}


			public void stegFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				int stagId = in.readInt();
				String userId = in.readUTF();
				StagData stagData = DBHandler.getSteg(stagId, userId, dbConnection);
									
				ArrayList<StagFile> fileList = new ArrayList<>();
				
				if((stagData.stagType & 2) != 0) {
					fileList.add(new StagFile(stagData.cameraDataFile, 1));
				}
				if((stagData.stagType & 4) != 0) {
					fileList.add(new StagFile(stagData.cameraDataFile, 2));
				}
				if((stagData.stagType & 8) != 0) {
					fileList.add(new StagFile(stagData.voiceDataFile, 3));
				}	
				
				for(StagFile sFile: fileList){
					out.writeUTF("fileFromServer");
					out.flush();
					out.writeInt(sFile.getType());
					out.flush();
					out.writeUTF(sFile.getFilePath().substring(sFile.getFilePath().lastIndexOf(".")));
					out.flush();
					FileInputStream dis = null;
					switch(sFile.getType()){
					case 1:
						out.writeInt((int) new File(STEGAPP_IMG_T_DIR + sFile.getFilePath()).length());
						out.flush();
						dis = new FileInputStream(STEGAPP_IMG_T_DIR + sFile.getFilePath());
						break;
					case 2:
						out.writeInt((int) new File(STEGAPP_VIDEO_DIR + sFile.getFilePath()).length());
						out.flush();
						dis = new FileInputStream(STEGAPP_VIDEO_DIR + sFile.getFilePath());
						break;
					case 3:
						out.writeInt((int) new File(STEGAPP_AUDIO_DIR + sFile.getFilePath()).length());
						out.flush();
						dis = new FileInputStream(STEGAPP_AUDIO_DIR + sFile.getFilePath());
						break;
					}
					
					Integer len;
					while (true){
						byte[] buffer = new byte[kB64];
						len = dis.read(buffer);
						if(len == -1) {
							break;
						}
						out.write(buffer, 0, len);
						out.flush();
					}
					dis.close();
				}
				
				out.writeUTF("stagData");
				out.flush();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				MessagePacker packer = MessagePack.newDefaultPacker(baos);
				packer
					.packInt(stagData.stegId)
					.packString(stagData.mesType)
					.packString(stagData.mesSender)
					.packString(stagData.senderName)
					.packString(stagData.mesReciever)
					.packInt(stagData.stagType);
				if((stagData.stagType & 1) != 0){
					packer.packString(stagData.mesText);
				}
				packer.packInt(stagData.lifeTime)
					.packBoolean(stagData.anonym)
					.packInt(stagData.filter)
					.packInt(stagData.likes)
					.packInt(stagData.gets)
					.packInt(stagData.saves)
					.packInt(stagData.comments)
					.packInt(stagData.likes)
					.packLong(stagData.date.getTime())
					.packLong(stagData.time.getTime())
					.packBoolean(stagData.liked);
				packer.close();
				out.writeInt(baos.toByteArray().length);
				out.flush();
				out.write(baos.toByteArray(), 0, baos.toByteArray().length);
				out.flush();
									
			}
		
			public void profileToServer(DataInputStream in) throws IOException{
				UserProfile recProfile = new UserProfile();
				String what = in.readUTF();
				switch(what) {
				case "userProfile":
					Integer len = in.readInt();
					byte[] profileBytes = new byte[len];
					in.read(profileBytes, 0, len);
					MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(profileBytes);
					recProfile.setId(unpacker.unpackString());
					recProfile.setName(unpacker.unpackString());
					recProfile.setSex(unpacker.unpackString());
					recProfile.setState(unpacker.unpackString());
					recProfile.setCity(unpacker.unpackString());
					recProfile.setAge(unpacker.unpackInt());
					if(unpacker.hasNext()){
						recProfile.setCoordinates(unpacker.unpackDouble(), unpacker.unpackDouble());
					}
					unpacker.close();
					
					DBHandler.addUserInfo(recProfile, dbConnection);
					System.out.println("profile '" + recProfile.getId() + "' added!");
					
					String isPhoto = in.readUTF();
					if(isPhoto.equals("profilePhoto")){
					String recUserId = in.readUTF();
					String fileExt = in.readUTF();
					Integer fileSize = in.readInt();
					System.out.println("PROFILE_PHOTO: " + recUserId + "/" + fileExt + "/" + fileSize);
					File newFile = new File(STEGAPP_PROFILE_PHOTO_DIR + recUserId + fileExt);
					FileOutputStream dos = new FileOutputStream(newFile);
					int i = 0;
					try {
						byte[] buffer;
						while(i < fileSize){
							buffer = new byte[kB64];
							len = in.read(buffer, 0, Math.min(fileSize - i, kB64));
							i += len;
							dos.write(buffer, 0, len);
							dos.flush();
						}
					} catch (EOFException e) {
						dos.close();
					}
					dos.close();
					// Write in DB
					DBHandler.addUserPhoto(recUserId, recUserId + fileExt, dbConnection);
					
					// Creating Thumbnail image
					ImageIO.write(resizeImg(newFile, 120, 120), fileExt.substring(1), new File(STEGAPP_PROFILE_THUMBS_DIR + recUserId + fileExt));											
					System.out.println(recUserId + " add avatar");
					} 
					break;
				}
			}
			
			public void profileFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				Boolean isBlack = false;
				String userId = in.readUTF();
				String profileId = in.readUTF();
				UserProfile sendProfile = DBHandler.getUserProfile(userId, dbConnection);
				if(!profileId.equals(userId)){
					sendProfile.setIsFriend(DBHandler.isFriend(profileId, userId, dbConnection));
					isBlack = DBHandler.isBlack(profileId, userId, dbConnection);
				}
				out.writeBoolean(!sendProfile.getPhoto().equals("clear"));
				out.flush();
				ByteArrayOutputStream profileBaos = new ByteArrayOutputStream();
				MessagePacker profilePacker = MessagePack.newDefaultPacker(profileBaos);
				profilePacker
					.packString(sendProfile.getName())
					.packString(sendProfile.getSex())
					.packString(sendProfile.getState())
					.packString(sendProfile.getCity())
					.packInt(sendProfile.getAge())
					.packDouble(sendProfile.getLongitude())
					.packDouble(sendProfile.getLatitude())
					.packBoolean(sendProfile.getIsFriend())
					.packBoolean(isBlack);
				
				
				if(!sendProfile.getPhoto().equals("clear")){
					File sendFile = new File(STEGAPP_PROFILE_THUMBS_DIR + sendProfile.getPhoto());
					profilePacker.packString(sendFile.getName().substring(sendFile.getName().lastIndexOf(".")));
					byte[] photoBytes = new byte[(int) sendFile.length()];
					FileInputStream fis = new FileInputStream(sendFile);
					fis.read(photoBytes, 0, photoBytes.length);
					fis.close();
					profilePacker.packBinaryHeader(photoBytes.length);
					profilePacker.writePayload(photoBytes, 0, photoBytes.length);							
				} else {
					profilePacker.packString("clear");
				}
				profilePacker.close();
				out.writeInt(profileBaos.toByteArray().length);
				out.flush();
				
				out.write(profileBaos.toByteArray(), 0, profileBaos.toByteArray().length);
				out.flush();
				profileBaos.close();
			}
			
			public void profileImgFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				String profileId = in.readUTF();
				UserProfile profile = DBHandler.getUserProfile(profileId, dbConnection);
				
				File imgFile = new File(STEGAPP_PROFILE_PHOTO_DIR + profile.getPhoto());
				FileInputStream fis = new FileInputStream(imgFile);
				
				out.writeLong(imgFile.length());
				
				Integer len;
				while (true){
					byte[] buffer = new byte[kB64];
					len = fis.read(buffer);
					if(len == -1) {
						break;
					}
					out.write(buffer, 0, len);
					out.flush();
				}
				fis.close();
				
			}
			
			public void wallFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				String profileId = in.readUTF();
				ArrayList<StagData> stegList = DBHandler.getWall(profileId, dbConnection);
				Integer count = stegList.size();
				out.writeInt(count);
				out.flush();
				for(StagData steg: stegList){
					ArrayList<StagFile> fileList = new ArrayList<>();
					if((steg.stagType & 2) != 0) {
						fileList.add(new StagFile(steg.cameraDataFile, 1));
					}
					if((steg.stagType & 4) != 0) {
						fileList.add(new StagFile(steg.cameraDataFile, 2));
					}
					if((steg.stagType & 8) != 0) {
						fileList.add(new StagFile(steg.cameraDataFile, 3));
					}	
					
					for(StagFile sFile: fileList){
						out.writeUTF("fileFromServer");
						out.flush();
						out.writeInt(steg.stegId);
						out.flush();
						out.writeInt(sFile.getType());
						out.flush();
						out.writeUTF(sFile.getFilePath().substring(sFile.getFilePath().lastIndexOf(".")));
						out.flush();
						out.writeInt((int) new File(STEGAPP_IMG_T_DIR + sFile.getFilePath()).length());
						out.flush();
						
						FileInputStream dis = new FileInputStream(STEGAPP_IMG_T_DIR + sFile.getFilePath());
						Integer len;
						while (true){
							byte[] buffer = new byte[kB64];
							len = dis.read(buffer);
							if(len == -1) {
								break;
							}
							out.write(buffer, 0, len);
							out.flush();
						}
						dis.close();
					}
					
					out.writeUTF("stagData");
					out.flush();
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					MessagePacker packer = MessagePack.newDefaultPacker(baos);
					packer
						.packInt(steg.stegId)
						.packString(steg.mesType)
						.packString(steg.mesSender)
						.packString(steg.senderName)
						.packString(steg.mesReciever)
						.packInt(steg.stagType);
					if((steg.stagType & 1) != 0){
						packer.packString(steg.mesText);
					}
					packer.packInt(steg.lifeTime)
						.packBoolean(steg.anonym)
						.packInt(steg.filter)
						.packInt(steg.comments)
						.packInt(steg.likes)
						.packLong(steg.date.getTime())
						.packLong(steg.time.getTime())
						.packBoolean(steg.liked);
					packer.close();
					Integer length = baos.toByteArray().length;
					out.writeInt(length);
					out.flush();
					out.write(baos.toByteArray(), 0, length);
					out.flush();
				}
			}
			
			public void wallItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				String profileId = in.readUTF();
				ArrayList<StegItem> stegItems = DBHandler.getWallItems(profileId, dbConnection);
				Integer count = stegItems.size();
				out.writeInt(count);
				out.flush();
				for(StegItem stegItem: stegItems){								
					out.writeInt(stegItem.getStegId());
					out.writeUTF(stegItem.getMesSender());
					out.writeBoolean(stegItem.isAnonym());
					out.writeInt(stegItem.getLikes());
					out.writeInt(stegItem.getComments());
					out.writeBoolean(stegItem.isLiked());
					out.flush();
				}
			}

			public void incomePrivateFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				String profileId = in.readUTF();
				ArrayList<StagData> stegList = DBHandler.getIncomePrivateStegs(profileId, dbConnection);
				Integer count = stegList.size();
				out.writeInt(count);
				out.flush();
				for(StagData steg: stegList){
					ArrayList<StagFile> fileList = new ArrayList<>();
					if((steg.stagType & 2) != 0) {
						fileList.add(new StagFile(steg.cameraDataFile, 1));
					}
					if((steg.stagType & 4) != 0) {
						fileList.add(new StagFile(steg.cameraDataFile, 2));
					}
					if((steg.stagType & 8) != 0) {
						fileList.add(new StagFile(steg.cameraDataFile, 3));
					}	
					
					for(StagFile sFile: fileList){
						out.writeUTF("fileFromServer");
						out.flush();
						out.writeInt(steg.stegId);
						out.flush();
						out.writeInt(sFile.getType());
						out.flush();
						out.writeUTF(sFile.getFilePath().substring(sFile.getFilePath().lastIndexOf(".")));
						out.flush();
						out.writeInt((int) new File(STEGAPP_IMG_T_DIR + sFile.getFilePath()).length());
						out.flush();
						
						FileInputStream dis = new FileInputStream(STEGAPP_IMG_T_DIR + sFile.getFilePath());
						Integer len;
						while (true){
							byte[] buffer = new byte[kB64];
							len = dis.read(buffer);
							if(len == -1) {
								break;
							}
							out.write(buffer, 0, len);
							out.flush();
						}
						dis.close();
					}
					
					out.writeUTF("stagData");
					out.flush();
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					MessagePacker packer = MessagePack.newDefaultPacker(baos);
					packer
						.packInt(steg.stegId)
						.packString(steg.mesType)
						.packString(steg.mesSender)
						.packString(steg.mesReciever)
						.packInt(steg.stagType);
					if((steg.stagType & 1) != 0){
						packer.packString(steg.mesText);
					}
					packer.packInt(steg.lifeTime)
						.packBoolean(steg.anonym)
						.packInt(steg.filter)
						.packInt(steg.comments)
						.packInt(steg.likes)
						.packLong(steg.date.getTime())
						.packLong(steg.time.getTime())
						.packBoolean(steg.sended);
					packer.close();
					Integer length = baos.toByteArray().length;
					out.writeInt(length);
					out.flush();
					out.write(baos.toByteArray(), 0, length);
					out.flush();
				}
			}
			
			public void incomeCommonItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				String profileId = in.readUTF();
				ArrayList<StegItem> stegItems = DBHandler.getIncomeCommonItems(profileId, dbConnection);
				out.writeInt(stegItems.size());
				out.flush();
				for(StegItem stegItem: stegItems){								
					out.writeInt(stegItem.getStegId());
					out.writeUTF(stegItem.getMesSender());
					out.writeBoolean(stegItem.isAnonym());
					out.writeInt(stegItem.getLikes());
					out.writeInt(stegItem.getComments());
					out.writeBoolean(stegItem.isLiked());
					out.flush();
				}
			}
			
			public void incomePrivateItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				String profileId = in.readUTF();
				ArrayList<StegItem> stegItems = DBHandler.getIncomePrivateItems(profileId, dbConnection);
				Integer count = stegItems.size();
				out.writeInt(count);
				out.flush();
				for(StegItem stegItem: stegItems){								
					out.writeInt(stegItem.getStegId());
					out.writeUTF(stegItem.getMesSender());
					out.writeBoolean(stegItem.isAnonym());
					out.writeInt(stegItem.getLikes());
					out.writeInt(stegItem.getComments());
					out.writeBoolean(stegItem.isLiked());
					out.flush();
				}
			}

			public void outcomePrivateFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				String profileId = in.readUTF();
				ArrayList<StagData> stegList = DBHandler.getOutcomePrivateStegs(profileId, dbConnection);
				Integer count = stegList.size();
				out.writeInt(count);
				out.flush();
				for(StagData steg: stegList){
					ArrayList<StagFile> fileList = new ArrayList<>();
					if((steg.stagType & 2) != 0) {
						fileList.add(new StagFile(steg.cameraDataFile, 1));
					}
					if((steg.stagType & 4) != 0) {
						fileList.add(new StagFile(steg.cameraDataFile, 2));
					}
					if((steg.stagType & 8) != 0) {
						fileList.add(new StagFile(steg.cameraDataFile, 3));
					}	
					
					for(StagFile sFile: fileList){
						out.writeUTF("fileFromServer");
						out.flush();
						out.writeInt(steg.stegId);
						out.flush();
						out.writeInt(sFile.getType());
						out.flush();
						out.writeUTF(sFile.getFilePath().substring(sFile.getFilePath().lastIndexOf(".")));
						out.flush();
						out.writeInt((int) new File(STEGAPP_IMG_T_DIR + sFile.getFilePath()).length());
						out.flush();
						
						FileInputStream dis = new FileInputStream(STEGAPP_IMG_T_DIR + sFile.getFilePath());
						Integer len;
						while (true){
							byte[] buffer = new byte[kB64];
							len = dis.read(buffer);
							if(len == -1) {
								break;
							}
							out.write(buffer, 0, len);
							out.flush();
						}
						dis.close();
					}
					
					out.writeUTF("stagData");
					out.flush();
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					MessagePacker packer = MessagePack.newDefaultPacker(baos);
					packer
						.packInt(steg.stegId)
						.packString(steg.mesType)
						.packString(steg.mesSender)
						.packString(steg.mesReciever)
						.packInt(steg.stagType);
					if((steg.stagType & 1) != 0){
						packer.packString(steg.mesText);
					}
					packer.packInt(steg.lifeTime)
						.packBoolean(steg.anonym)
						.packInt(steg.filter)
						.packInt(steg.comments)
						.packInt(steg.likes)
						.packInt(steg.gets)
						.packInt(steg.saves)
						.packLong(steg.date.getTime())
						.packLong(steg.time.getTime());
					packer.close();
					Integer length = baos.toByteArray().length;
					out.writeInt(length);
					out.flush();
					out.write(baos.toByteArray(), 0, length);
					out.flush();
				}
			}

			public void outcomePrivateItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				String profileId = in.readUTF();
				ArrayList<StegItem> stegItems = DBHandler.getOutcomePrivateItems(profileId, dbConnection);
				Integer count = stegItems.size();
				out.writeInt(count);
				out.flush();
				for(StegItem stegItem: stegItems){								
					out.writeInt(stegItem.getStegId());
					out.writeUTF(stegItem.getMesSender());
					out.writeBoolean(stegItem.isAnonym());
					out.writeInt(stegItem.getLikes());
					out.writeInt(stegItem.getComments());
					out.writeBoolean(stegItem.isLiked());
					out.flush();
				}
			}

			public void outcomeCommonFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				String profileId = in.readUTF();
				ArrayList<StagData> stegList = DBHandler.getOutcomeCommonStegs(profileId, dbConnection);
				Integer count = stegList.size();
				out.writeInt(count);
				out.flush();
				for(StagData steg: stegList){
					ArrayList<StagFile> fileList = new ArrayList<>();
					if((steg.stagType & 2) != 0) {
						fileList.add(new StagFile(steg.cameraDataFile, 1));
					}
					if((steg.stagType & 4) != 0) {
						fileList.add(new StagFile(steg.cameraDataFile, 2));
					}
					if((steg.stagType & 8) != 0) {
						fileList.add(new StagFile(steg.cameraDataFile, 3));
					}	
					
					for(StagFile sFile: fileList){
						out.writeUTF("fileFromServer");
						out.flush();
						out.writeInt(steg.stegId);
						out.flush();
						out.writeInt(sFile.getType());
						out.flush();
						out.writeUTF(sFile.getFilePath().substring(sFile.getFilePath().lastIndexOf(".")));
						out.flush();
						out.writeInt((int) new File(STEGAPP_IMG_T_DIR + sFile.getFilePath()).length());
						out.flush();
						
						FileInputStream dis = new FileInputStream(STEGAPP_IMG_T_DIR + sFile.getFilePath());
						Integer len;
						while (true){
							byte[] buffer = new byte[kB64];
							len = dis.read(buffer);
							if(len == -1) {
								break;
							}
							out.write(buffer, 0, len);
							out.flush();
						}
						dis.close();
					}
					
					out.writeUTF("stagData");
					out.flush();
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					MessagePacker packer = MessagePack.newDefaultPacker(baos);
					packer
						.packInt(steg.stegId)
						.packString(steg.mesType)
						.packString(steg.mesSender)
						.packString(steg.mesReciever)
						.packInt(steg.stagType);
					if((steg.stagType & 1) != 0){
						packer.packString(steg.mesText);
					}
					packer.packInt(steg.lifeTime)
						.packBoolean(steg.anonym)
						.packInt(steg.filter)
						.packInt(steg.comments)
						.packInt(steg.likes)
						.packInt(steg.gets)
						.packInt(steg.saves)
						.packLong(steg.date.getTime())
						.packLong(steg.time.getTime());
					packer.close();
					Integer length = baos.toByteArray().length;
					out.writeInt(length);
					out.flush();
					out.write(baos.toByteArray(), 0, length);
					out.flush();
				}
			}
			
			public void outcomeCommonItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				String profileId = in.readUTF();
				ArrayList<StegItem> stegItems = DBHandler.getOutcomeCommonItems(profileId, dbConnection);
				Integer count = stegItems.size();
				out.writeInt(count);
				out.flush();
				for(StegItem stegItem: stegItems){								
					out.writeInt(stegItem.getStegId());
					out.writeUTF(stegItem.getMesSender());
					out.writeBoolean(stegItem.isAnonym());
					out.writeInt(stegItem.getLikes());
					out.writeInt(stegItem.getComments());
					out.writeBoolean(stegItem.isLiked());
					Integer recievers = stegItem.getRecieverCount();
					out.writeInt(recievers);
					if(recievers > 4)
						recievers = 4;
					out.writeInt(recievers);
					int i = 0;
					for(Entry<String, UserProfile> entry : stegItem.recieverIds.entrySet()){
						i++;
						out.writeUTF(entry.getKey());
						if(i > 4) break;
					}
					out.flush();
				}
			}

			public void commentsFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				Integer stegId = in.readInt();
				ArrayList<CommentData> comments = DBHandler.getComments(stegId, dbConnection);
				Integer count = comments.size();
				out.writeInt(count);
				out.flush();
				for(CommentData comment : comments){
					ByteArrayOutputStream commentBaos = new ByteArrayOutputStream();
					MessagePacker commentPacker = MessagePack.newDefaultPacker(commentBaos);
					commentPacker
					    .packInt(comment.id)
					    .packInt(comment.stegId)
					    .packString(comment.profileId)
					    .packString(comment.getText())
					    .packLong(comment.date.getTime())
					    .packLong(comment.time.getTime());
						
//					if(!comment.profileImg.equals("clear")){
//						commentPacker.packString("photo");
//						File sendFile = new File(STEGAPP_PROFILE_THUMBS_DIR + comment.profileImg);
//						commentPacker.packString(sendFile.getName().substring(sendFile.getName().lastIndexOf(".")));
//						byte[] photoBytes = new byte[(int) sendFile.length()];
//						FileInputStream fis = new FileInputStream(sendFile);
//						fis.read(photoBytes, 0, photoBytes.length);
//						fis.close();
//						commentPacker.packBinaryHeader(photoBytes.length);
//						commentPacker.writePayload(photoBytes, 0, photoBytes.length);							
//					} else commentPacker.packString("clear");
					commentPacker.close();
					
					int len = commentBaos.toByteArray().length;
					out.writeInt(len);
					out.flush();
					out.write(commentBaos.toByteArray(), 0, len);
					out.flush();
				}
			}
			
			public void commentItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				Integer stegId = in.readInt();
				ArrayList<CommentItem> items = DBHandler.getCommentsItems(stegId, dbConnection);
				Integer count = items.size();
				out.writeInt(count);
				out.flush();
				for(CommentItem comment : items){
					ByteArrayOutputStream commentBaos = new ByteArrayOutputStream();
					MessagePacker commentPacker = MessagePack.newDefaultPacker(commentBaos);
					commentPacker
					    .packInt(comment.getId())
					    .packInt(comment.getStegId())
					    .packInt(comment.getType())
					    .packString(comment.getProfileId())
					    .packString(comment.getText())
					    .packLong(comment.getDate().getTime())
					    .packLong(comment.getTime().getTime());
					commentPacker.close();
					
					int len = commentBaos.toByteArray().length;
					out.writeInt(len);
					out.flush();
					out.write(commentBaos.toByteArray(), 0, len);
					out.flush();
				}
			}
			
			public void commentFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				int commentId = in.readInt();
				
				CommentData comment = DBHandler.getComment(commentId, dbConnection);
									
				ArrayList<StagFile> fileList = new ArrayList<>();
				
				if((comment.getType() & 2) != 0) {
					fileList.add(new StagFile(comment.getImgData(), 1));
				}
				if((comment.getType() & 4) != 0) {
					fileList.add(new StagFile(comment.getVideoData(), 2));
				}
				if((comment.getType() & 8) != 0) {
					fileList.add(new StagFile(comment.getVoiceData(), 3));
				}	
				
				for(StagFile sFile: fileList){
					out.writeUTF("fileFromServer");
					out.flush();
					out.writeInt(sFile.getType());
					out.flush();
					out.writeUTF(sFile.getFilePath().substring(sFile.getFilePath().lastIndexOf(".")));
					out.flush();
					FileInputStream dis = null;
					switch(sFile.getType()){
					case 1:
						out.writeInt((int) new File(STEGAPP_IMG_T_DIR + sFile.getFilePath()).length());
						out.flush();
						dis = new FileInputStream(STEGAPP_IMG_T_DIR + sFile.getFilePath());
						break;
					case 2:
						out.writeInt((int) new File(STEGAPP_VIDEO_DIR + sFile.getFilePath()).length());
						out.flush();
						dis = new FileInputStream(STEGAPP_VIDEO_DIR + sFile.getFilePath());
						break;
					case 3:
						out.writeInt((int) new File(STEGAPP_AUDIO_DIR + sFile.getFilePath()).length());
						out.flush();
						dis = new FileInputStream(STEGAPP_AUDIO_DIR + sFile.getFilePath());
						break;
					}
					
					Integer len;
					while (true){
						byte[] buffer = new byte[kB64];
						len = dis.read(buffer);
						if(len == -1) {
							break;
						}
						out.write(buffer, 0, len);
						out.flush();
					}
					dis.close();
				}
				
				out.writeUTF("commentData");
				out.flush();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				MessagePacker packer = MessagePack.newDefaultPacker(baos);
				packer
					.packInt(comment.id)
					.packInt(comment.stegId)
					.packInt(comment.getType())
					.packString(comment.profileId)
					.packLong(comment.date.getTime())
					.packLong(comment.time.getTime())
					.packString(comment.getText());
				packer.close();
				out.writeInt(baos.toByteArray().length);
				out.flush();
				out.write(baos.toByteArray(), 0, baos.toByteArray().length);
				out.flush();
			}
			
			public void commentToServer(DataInputStream in, DataOutputStream out) throws Exception{
				Boolean notStopped = true;
				CommentData commentData = new CommentData();
				while(notStopped){
					File imgFile = null;
					String imgExt = null;
					File stagFile = null;
					String what = in.readUTF();
					switch(what){
					case "fileToServer":
						Integer fileType = in.readInt();
						FileOutputStream dos = null;
						System.out.println("fileType: " + fileType);
						String fileExt = in.readUTF();
						switch(fileType){
						case 1:
							imgExt = fileExt;
							imgFile = new File(STEGAPP_IMG_DIR + System.currentTimeMillis() + imgExt);
							commentData.setImgData(imgFile.getName());
							dos = new FileOutputStream(imgFile);
							break;
						case 2:
							stagFile = new File(STEGAPP_VIDEO_DIR + System.currentTimeMillis() + fileExt);
							commentData.setVideoData(stagFile.getName());
							dos = new FileOutputStream(stagFile);
							break;
						case 3:
							stagFile = new File(STEGAPP_AUDIO_DIR + System.currentTimeMillis() + fileExt);
							commentData.setVoiceData(stagFile.getName());
							dos = new FileOutputStream(stagFile);
							break;
						}
						int fileSize = in.readInt();
						
						int i = 0;
						Integer len = 0;
						try {
							byte[] buffer;
							while(i < fileSize){
								buffer = new byte[kB64];									
								len = in.read(buffer, 0, Math.min(fileSize - i, kB64));
								i += len;
								dos.write(buffer, 0, len);
								dos.flush();
							}
							// Creating Thumbnail image
							if(imgFile != null){
								ImageIO.write(resizeImg(imgFile, 960, 960), imgExt.substring(1), new File(STEGAPP_IMG_T_DIR + imgFile.getName()));
							}
														
						} catch (EOFException e) {
							dos.close();
						}
						dos.close();
						break;
					case "commentData":
						int commentDataSize = in.readInt();
						byte[] commentDataBytes = new byte[commentDataSize];
						in.read(commentDataBytes);
						MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(commentDataBytes);
						commentData.setType(unpacker.unpackInt());
						commentData.stegId = unpacker.unpackInt();
						commentData.profileId = unpacker.unpackString();
						if((commentData.getType() & CommentData.COMMENT_TEXT_MASK) != 0){
							commentData.setText(unpacker.unpackString());
						} else {
							commentData.setText("clear");
						}
						unpacker.close();							
						DBHandler.addComment(commentData, dbConnection);
						notStopped = false;
						break;
					}
				}
			}

			public void likesFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				Integer stegId = in.readInt();
				ArrayList<LikeData> likes = DBHandler.getLikes(stegId, dbConnection);
				Integer count = likes.size();
				out.writeInt(count);
				out.flush();
				for(LikeData like : likes){
					ByteArrayOutputStream likeBaos = new ByteArrayOutputStream();
					MessagePacker likePacker = MessagePack.newDefaultPacker(likeBaos);
					likePacker
					    .packInt(like.id)
					    .packInt(like.stegId)
					    .packString(like.profileId)
					    .packString(like.profileName)
					    .packLong(like.date.getTime())
					    .packLong(like.time.getTime());
						
					if(!like.profileImg.equals("clear")){
						likePacker.packString("photo");
						File sendFile = new File(STEGAPP_PROFILE_THUMBS_DIR + like.profileImg);
						likePacker.packString(sendFile.getName().substring(sendFile.getName().lastIndexOf(".")));
						byte[] photoBytes = new byte[(int) sendFile.length()];
						FileInputStream fis = new FileInputStream(sendFile);
						fis.read(photoBytes, 0, photoBytes.length);
						fis.close();
						likePacker.packBinaryHeader(photoBytes.length);
						likePacker.writePayload(photoBytes, 0, photoBytes.length);							
					} else likePacker.packString("clear");
					likePacker.close();
					
					int len = likeBaos.toByteArray().length;
					out.writeInt(len);
					out.flush();
					out.write(likeBaos.toByteArray(), 0, len);
					out.flush();
				}
			}
			
			public void saversFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				Integer stegId = in.readInt();
				ArrayList<LikeData> savers = DBHandler.getSavers(stegId, dbConnection);
				Integer count = savers.size();
				out.writeInt(count);
				out.flush();
				for(LikeData saver : savers){
					ByteArrayOutputStream saverBaos = new ByteArrayOutputStream();
					MessagePacker saverPacker = MessagePack.newDefaultPacker(saverBaos);
					saverPacker
					    .packInt(saver.id)
					    .packInt(saver.stegId)
					    .packString(saver.profileId)
					    .packString(saver.profileName);
						
					if(!saver.profileImg.equals("clear")){
						saverPacker.packString("photo");
						File sendFile = new File(STEGAPP_PROFILE_THUMBS_DIR + saver.profileImg);
						saverPacker.packString(sendFile.getName().substring(sendFile.getName().lastIndexOf(".")));
						byte[] photoBytes = new byte[(int) sendFile.length()];
						FileInputStream fis = new FileInputStream(sendFile);
						fis.read(photoBytes, 0, photoBytes.length);
						fis.close();
						saverPacker.packBinaryHeader(photoBytes.length);
						saverPacker.writePayload(photoBytes, 0, photoBytes.length);							
					} else saverPacker.packString("clear");
					saverPacker.close();
					
					int len = saverBaos.toByteArray().length;
					out.writeInt(len);
					out.flush();
					out.write(saverBaos.toByteArray(), 0, len);
					out.flush();
				}
			}
			
			public void getersFromServer(DataInputStream in, DataOutputStream out) throws IOException{
				Integer stegId = in.readInt();
				ArrayList<LikeData> geters = DBHandler.getGeters(stegId, dbConnection);
				Integer count = geters.size();
				out.writeInt(count);
				out.flush();
				for(LikeData geter : geters){
					ByteArrayOutputStream geterBaos = new ByteArrayOutputStream();
					MessagePacker geterPacker = MessagePack.newDefaultPacker(geterBaos);
					geterPacker
					    .packInt(geter.id)
					    .packInt(geter.stegId)
					    .packString(geter.profileId)
					    .packString(geter.profileName);
						
					if(!geter.profileImg.equals("clear")){
						geterPacker.packString("photo");
						File sendFile = new File(STEGAPP_PROFILE_THUMBS_DIR + geter.profileImg);
						geterPacker.packString(sendFile.getName().substring(sendFile.getName().lastIndexOf(".")));
						byte[] photoBytes = new byte[(int) sendFile.length()];
						FileInputStream fis = new FileInputStream(sendFile);
						fis.read(photoBytes, 0, photoBytes.length);
						fis.close();
						geterPacker.packBinaryHeader(photoBytes.length);
						geterPacker.writePayload(photoBytes, 0, photoBytes.length);							
					} else geterPacker.packString("clear");
					geterPacker.close();
					
					int len = geterBaos.toByteArray().length;
					out.writeInt(len);
					out.flush();
					out.write(geterBaos.toByteArray(), 0, len);
					out.flush();
				}
			}
			
			public void notificationItemsFromServer(DataInputStream in, DataOutputStream out) throws IOException {
				String owner = in.readUTF();
				ArrayList<NewsData> news = DBHandler.getNews(owner, dbConnection);
				out.writeInt(news.size());
				out.flush();
				for(NewsData n: news){
					ByteArrayOutputStream newsBaos = new ByteArrayOutputStream();
					MessagePacker newsPacker = MessagePack.newDefaultPacker(newsBaos);
					newsPacker
						.packInt(n.id)
					    .packString(n.type)
					    .packBoolean(n.sended)
					    .packString(n.profileId);
					if(!n.type.equals("friend"))
					    newsPacker.packInt(n.stegId);
					newsPacker
				    .packLong(n.date.getTime())
				    .packLong(n.time.getTime());
														
					newsPacker.close();
					
					int len = newsBaos.toByteArray().length;
					out.writeInt(len);
					out.flush();
					out.write(newsBaos.toByteArray(), 0, len);
					out.flush();
				}
			}
			
			public void newsFromServer(DataInputStream in, DataOutputStream out) throws IOException {
				String owner = in.readUTF();
				ArrayList<NewsData> news = DBHandler.getNews(owner, dbConnection);
				out.writeInt(news.size());
				out.flush();
				for(NewsData n: news){
					ByteArrayOutputStream newsBaos = new ByteArrayOutputStream();
					MessagePacker newsPacker = MessagePack.newDefaultPacker(newsBaos);
					newsPacker
					    .packString(n.type)
					    .packString(n.profileId)
					    .packString(n.profileName);
					if(!n.type.equals("friend"))
					    newsPacker.packInt(n.stegId);
					newsPacker
					    .packLong(n.date.getTime())
					    .packLong(n.time.getTime())
					    .packBoolean(n.sended)
					    .packInt(n.id);
						
					if(!n.profileImg.equals("clear")){
						newsPacker.packString("photo");
						File profileFile = new File(STEGAPP_PROFILE_THUMBS_DIR + n.profileImg);
						newsPacker.packString(profileFile.getName().substring(profileFile.getName().lastIndexOf(".")));
						byte[] photoBytes = new byte[(int) profileFile.length()];
						FileInputStream fis = new FileInputStream(profileFile);
						fis.read(photoBytes, 0, photoBytes.length);
						fis.close();
						newsPacker.packBinaryHeader(photoBytes.length);
						newsPacker.writePayload(photoBytes, 0, photoBytes.length);							
					} else newsPacker.packString("clear");
					
					if (!n.type.equals("friend")){
						if(!n.stegImg.equals("clear")){
							newsPacker.packString("photo");
							File stegFile = new File(STEGAPP_IMG_T_DIR + n.stegImg);
							newsPacker.packString(stegFile.getName().substring(stegFile.getName().lastIndexOf(".")));
							byte[] stegBytes = new byte[(int) stegFile.length()];
							FileInputStream sFis = new FileInputStream(stegFile);
							sFis.read(stegBytes, 0, stegBytes.length);
							sFis.close();
							newsPacker.packBinaryHeader(stegBytes.length);
							newsPacker.writePayload(stegBytes, 0, stegBytes.length);	
						} else newsPacker.packString("clear");
					}
					
					newsPacker.close();
					
					int len = newsBaos.toByteArray().length;
					out.writeInt(len);
					out.flush();
					out.write(newsBaos.toByteArray(), 0, len);
					out.flush();
				}
			}
			
			public void friendsFromServer(DataInputStream in, DataOutputStream out) throws IOException {
				String profileId = in.readUTF();
				ArrayList<UserProfile> friendsList = DBHandler.getFriends(profileId, dbConnection);
				out.writeInt(friendsList.size());
				out.flush();
				for(UserProfile friend: friendsList){
					ByteArrayOutputStream friendsBaos = new ByteArrayOutputStream();
					MessagePacker friendsPacker = MessagePack.newDefaultPacker(friendsBaos);
					friendsPacker
					    .packString(friend.getId())
					    .packString(friend.getName());
						
					if(!friend.getPhoto().equals("clear")){
						friendsPacker.packString("photo");
						File profileFile = new File(STEGAPP_PROFILE_THUMBS_DIR + friend.getPhoto());
						friendsPacker.packString(profileFile.getName().substring(profileFile.getName().lastIndexOf(".")));
						byte[] photoBytes = new byte[(int) profileFile.length()];
						FileInputStream fis = new FileInputStream(profileFile);
						fis.read(photoBytes, 0, photoBytes.length);
						fis.close();
						friendsPacker.packBinaryHeader(photoBytes.length);
						friendsPacker.writePayload(photoBytes, 0, photoBytes.length);							
					} else {
						friendsPacker.packString("clear");
					}
					friendsPacker
						.packString(friend.getSex())
						.packString(friend.getCity())
						.packString(friend.getState())
						.packInt(friend.getAge());
									
					friendsPacker.close();
					
					int len = friendsBaos.toByteArray().length;
					out.writeInt(len);
					out.flush();
					out.write(friendsBaos.toByteArray(), 0, len);
					out.flush();
				}
			}
			
			public void getIsBlack(DataInputStream in, DataOutputStream out) throws IOException{
				String myProfileId = in.readUTF();
				String blackProfileId = in.readUTF();
				Boolean isBlack = DBHandler.isBlack(myProfileId, blackProfileId, dbConnection);
				out.writeBoolean(isBlack);
				out.flush();
			}
			
			public void isMeInBlackList(DataInputStream in, DataOutputStream out) throws IOException{
				String myProfileId = in.readUTF();
				String blackProfileId = in.readUTF();
				Boolean isMeInBlackList = DBHandler.isMeInBlackList(myProfileId, blackProfileId, dbConnection);
				out.writeBoolean(isMeInBlackList);
				out.flush();
			}
			
			public void checkPassword(DataInputStream in, DataOutputStream out) throws IOException{
				String profileId = in.readUTF();
				String password = in.readUTF();
				Boolean check = DBHandler.checkPassword(profileId, password, dbConnection);
				out.writeBoolean(check);
				out.flush();
			}
			
			public void delProfile(DataInputStream in, DataOutputStream out) throws IOException{
				String profileId = in.readUTF();
				Boolean res = DBHandler.deleteProfile(profileId, dbConnection);
				out.writeBoolean(res);
				out.flush();
			}
			
			public BufferedImage resizeImg(File bigImgFile, int width, int height) throws IOException{
				float dx, dy;
				int genX, genY;
								
				BufferedImage bigImg = ImageIO.read(bigImgFile);
				dx = ((float) width)/bigImg.getWidth();
				dy = ((float) height)/bigImg.getHeight();
				
				if(bigImg.getWidth()<=width && bigImg.getHeight()<=height){
					genX = bigImg.getWidth();
					genY = bigImg.getHeight();
				} else {
					if(dx <= dy){
						genX = width;
						genY = (int) (dx*bigImg.getHeight());
					} else {
						genX = (int) (dy*bigImg.getWidth());
						genY = height;
					}
				}
								
				BufferedImage smallImg = new BufferedImage(genX, genY, BufferedImage.TYPE_INT_RGB);
				smallImg.createGraphics().drawImage(ImageIO.read(bigImgFile).getScaledInstance(genX, genY,Image.SCALE_SMOOTH), 0, 0, null);
				return smallImg;
			}
		};
		handleThread.start();
	}
}

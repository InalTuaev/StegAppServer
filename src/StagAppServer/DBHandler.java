package StagAppServer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Random;

public class DBHandler {
	
	public static Boolean newUser(String userId, String paswd, Connection dbConnection){
		Boolean res = false;
		try {
			if(!checkUser(userId, dbConnection)){
				PreparedStatement insertUserToDB = dbConnection.prepareStatement("INSERT INTO users " +
					"(user_id, user_name, user_paswd) VALUES (?, ?, ?)");
				insertUserToDB.setString(1, userId);
				insertUserToDB.setString(2, userId);
				insertUserToDB.setString(3, paswd);
				insertUserToDB.executeUpdate();
				insertUserToDB.close();
				res = true;
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			res = false;
		}
		return res;
	}
	
	public static Boolean newUserSocial(String userId, String passwd, String socId, Connection dbConnection){
		Boolean res = false;
		try{
			if(!checkUser(userId, dbConnection)){
				PreparedStatement insertUserToDB = dbConnection.prepareStatement("INSERT INTO users " +
					"(user_id, user_name, user_paswd, user_soc_id) VALUES (?, ?, ?, ?);");
				insertUserToDB.setString(1, userId);
				insertUserToDB.setString(2, userId);
				insertUserToDB.setString(3, passwd);
				insertUserToDB.setString(4, socId);
				insertUserToDB.executeUpdate();
				insertUserToDB.close();
				res = true;
			}
		} catch (SQLException e){
			e.printStackTrace();
			res = false;
		}
		return res;
	}
	
	public static String checkUserSocialId(String userSocId, Connection dbConnection){
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT user_id FROM users WHERE user_soc_id = ?;");
			statement.setString(1, userSocId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()){
				String res = rs.getString(1);
				statement.close();
				rs.close();
				return res;
			}
			statement.close();
			rs.close();
		} catch (SQLException e){
			e.printStackTrace();
			return null;
		}
		return null;
	}
	
	public static UserProfile getUserProfile(String userId, Connection dbConnection){
		UserProfile user = null;
		try {
			PreparedStatement statement = dbConnection.prepareStatement("SELECT user_id, user_name, user_sex, " + 
										"user_state, user_city, user_age, latitude, longitude, user_photo FROM users " + 
										"WHERE user_id = ?");
			statement.setString(1, userId);
			ResultSet resultSet = statement.executeQuery();
			
			while(resultSet.next()){
				user = new UserProfile();
				user.setId(userId);
				String name = resultSet.getString("user_name");
				if(name != null) user.setName(name);
				String sex = resultSet.getString("user_sex");
				if(sex != null) user.setSex(sex);
				String state = resultSet.getString("user_state");
				if (state != null) user.setState(state);
				String city = resultSet.getString("user_city");
				if (city != null) user.setCity(city);
				Integer age = resultSet.getInt("user_age");
				if (age != null) user.setAge(age);
				Double latitude = resultSet.getDouble("latitude");
				Double longitude = resultSet.getDouble("longitude");
				if(latitude != null && latitude != null){
					user.setCoordinates(longitude, latitude);
				}
				String photo = resultSet.getString("user_photo");
				if(photo != null) user.setPhoto(photo);
			}
			statement.close();
			resultSet.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return user;
	}

	public static String getStegSenderId(Integer stegId, Connection dbConnection){
		String id = null;
		PreparedStatement statement = null;
		try{
			statement = dbConnection.prepareStatement("SELECT sender FROM stegs WHERE steg_id = ?");
			statement.setInt(1, stegId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				id = rs.getString(1);
			}
			rs.close();
			statement.close();
		} catch(Exception e){
			e.printStackTrace();
		}
		return id;
	}
	
	public static String getUserNameFromId(String userId, Connection dbConnection){
		String name = null;
		PreparedStatement statement = null;
		try{
			statement = dbConnection.prepareStatement("SELECT user_name FROM users WHERE user_id = ?;");
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()){
				name = rs.getString(1);
			}
			rs.close();
			statement.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return name;
	}

	public static Boolean checkUser(String userId, Connection dbConnection){
		Boolean res = false;
		ResultSet rs = null;
		PreparedStatement statement = null;
		try{
			statement = dbConnection.prepareStatement("SELECT * FROM users WHERE user_id = ?");
			statement.setString(1, userId);
			rs = statement.executeQuery();
			while(rs.next()){
				res = true;
			}
			rs.close();
			statement.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		
		return res;
	}

	public static Boolean checkPassword(String profileId, String passwd, Connection dbConnection){
		Boolean res = false;
		System.out.println("userId: " + profileId + " paswd: " + passwd);
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT * FROM users WHERE user_id = ? AND user_paswd = ?");
			statement.setString(1, profileId);
			statement.setString(2, passwd);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				res = true;
			}
			statement.close();
			rs.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		return res;
	}

	public static Boolean deleteProfile(String profileId, Connection dbConnection){
		Boolean res = false;
		try{
			String delString = 
					"DELETE FROM comments " +
							"WHERE (comments.steg_id IN (SELECT stegs.steg_id FROM stegs WHERE stegs.sender = ? OR stegs.reciever = ?)) " +
							"OR comments.profile_id = ?; " +
					"DELETE  FROM gets " +
						"WHERE (gets.steg_id IN (SELECT stegs.steg_id FROM stegs WHERE stegs.sender = ? OR stegs.reciever = ?)) " +
						"OR gets.profile_id = ?; " +
					"DELETE  FROM likes " +
						"WHERE (likes.steg_id IN (SELECT stegs.steg_id FROM stegs WHERE stegs.sender = ? OR stegs.reciever = ?))" +
						"OR likes.profile_id = ?; " +
					"DELETE  FROM wall " +
						"WHERE (wall.steg_id IN (SELECT stegs.steg_id FROM stegs WHERE stegs.sender = ? OR stegs.reciever = ?)) " +
						"OR wall.owner = ?; " +
					"DELETE  FROM news " +
						"WHERE (news.steg_id IN (SELECT stegs.steg_id FROM stegs WHERE stegs.sender = ? OR stegs.reciever = ?)) " +
						"OR news.profile_id = ? " +
						"OR news.owner_id = ?; " +
					"DELETE  FROM stegs WHERE stegs.sender = ? OR stegs.reciever = ?; " +
					"DELETE  FROM friends WHERE profile_id = ? OR friend_id = ?; " +
					"DELETE  FROM blacklist WHERE my_profile_id = ? OR black_profile_id = ?; " +
					"DELETE  FROM users WHERE user_id = ?;";
				
			PreparedStatement statement = dbConnection.prepareStatement(delString);
			for(int i = 1; i < 24; i++)
				statement.setString(i, profileId);
			statement.executeUpdate();
			res = true;
		} catch (SQLException e){
			e.printStackTrace();
			res = false;
		}
		return res;
	}

	public static Boolean signIn(String userId, String paswd, Connection dbConnection){
		Boolean res = false;
		System.out.println("userId: " + userId + " paswd: " + paswd);
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT * FROM users WHERE user_id = ? AND user_paswd = ?");
			statement.setString(1, userId);
			statement.setString(2, paswd);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				res = true;
			}
			statement.close();
			rs.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		return res;
	}

	public static Boolean addUserInfo(UserProfile user, Connection dbConnection){
		Boolean res = false;
		try{
			PreparedStatement statement = dbConnection.prepareStatement("UPDATE users SET " + 
								"user_name = ?, user_sex = ?, user_state = ?, user_city = ?, "+ 
								"user_age = ?, latitude = ?, longitude = ? WHERE user_id = ?;");
			statement.setString(1, user.getName());
			statement.setString(2, user.getSex());
			statement.setString(3, user.getState());
			statement.setString(4, user.getCity());
			statement.setInt(5, user.getAge());
			if(user.getLatitude() != null){
				statement.setDouble(6, user.getLatitude());
				statement.setDouble(7, user.getLongitude());
			} else {
				statement.setNull(6, Types.NUMERIC);
				statement.setNull(7, Types.NUMERIC);
			}
			statement.setString(8, user.getId());
			statement.executeUpdate();
			res = true;
			statement.close();
		}catch (Exception e){
			e.printStackTrace();
		}
		return res;
	}
	
	public static void setUserCoordinates(String userId, String city, Double longitude, Double latitude, Connection dbConnection){
		String query;
		if(!city.equals("clear")){
			query = "UPDATE users SET " +
					"longitude = ?, latitude = ?, user_city = ? WHERE user_id = ?;";
		} else {
			query = "UPDATE users SET " +
					"longitude = ?, latitude = ? WHERE user_id = ?;";
		}
		try{
			PreparedStatement st = dbConnection.prepareStatement(query);
			st.setDouble(1, longitude);
			st.setDouble(2, latitude);
			if(!city.equals("clear")){
				st.setString(3,city);
				st.setString(4, userId);
			} else {
				st.setString(3, userId);
			}
			st.executeUpdate();
			st.close();
		} catch (SQLException e){
			e.printStackTrace();
		}
	}

	public static Boolean addUserPhoto(String userId, String photo, Connection dbConnection){
		Boolean res = false;
		try {
			PreparedStatement statement = dbConnection.prepareStatement("UPDATE users SET user_photo = ? WHERE user_id = ?");
			statement.setString(1, photo);
			statement.setString(2, userId);
			System.out.println("aadUserPhoto: " + statement.executeUpdate());
			res = true;
			statement.close();
		}catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	public static Boolean setUserValid(String userId, Connection dbConnection){
		Boolean res = false;
		try{
			PreparedStatement statement = dbConnection.prepareStatement("UPDATE users SET user_valid = 'true' WHERE user_id = ?");
			statement.setString(1, userId);
			System.out.println("setUserValid: " + statement.executeUpdate());
			statement.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		return res;
	}

	public static Integer addSteg(StagData stagData, Connection dbConnection){
		Integer res = null;
		try{
			PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO stegs " +
						"(sender, reciever, type, life_time, filter, text, voice_path, camera_path, anonym) " +
						"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING steg_id");
			statement.setString(1, stagData.mesSender);
			statement.setString(2, stagData.mesReciever);
			statement.setInt(3, stagData.stagType);
			statement.setInt(4, stagData.lifeTime);
			statement.setInt(5, stagData.filter);
			statement.setString(6, stagData.mesText);
			statement.setString(7, stagData.voiceDataFile);
			statement.setString(8, stagData.cameraDataFile);
			statement.setBoolean(9, stagData.anonym);
			
			ResultSet rs = statement.executeQuery();
			
			while (rs.next()){
				res = rs.getInt(1);
			}
			rs.close();
			statement.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return res;
	}

	public static StagData getSteg(int stagId, String userId, Connection dbConnection){
		StagData stag = null;
		try{
			PreparedStatement statement = dbConnection.prepareStatement(" SELECT stegs.steg_id, stegs.sender," + 
					" stegs.reciever, stegs.type, stegs.life_time," + 
					" stegs.filter, stegs.text, stegs.voice_path," + 
					" stegs.camera_path, stegs.anonym, stegs.date_," +
					" stegs.time_, users.user_name" +  
					" FROM stegs RIGHT OUTER JOIN users ON (stegs.sender = users.user_id)" + 
					" WHERE stegs.steg_id = " + stagId + ";");
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				stag = new StagData();
				stag.stegId = rs.getInt(1);
				stag.mesSender = rs.getString(2);
				stag.mesReciever = rs.getString(3);
				stag.stagType = rs.getInt(4);
				stag.lifeTime = rs.getInt(5);
				stag.filter = rs.getInt(6);
				stag.mesText = rs.getString(7);
				stag.voiceDataFile = rs.getString(8);
				stag.cameraDataFile = rs.getString(9);
				stag.anonym = rs.getBoolean(10);
				stag.date = rs.getDate(11);
				stag.time = rs.getTime(12);
				stag.senderName = rs.getString(13);
				
				PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
				likeStatement.setInt(1, stag.stegId);
				ResultSet likeRs = likeStatement.executeQuery();
				while(likeRs.next()){
					stag.likes = likeRs.getInt(1);
				}
			
				PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
				commentStatement.setInt(1, stag.stegId);
				ResultSet commentRs = commentStatement.executeQuery();
				while(commentRs.next()){
					stag.comments = commentRs.getInt(1);
				}
				
				PreparedStatement getsStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM gets WHERE steg_id = ?;");
				getsStatement.setInt(1, stag.stegId);
				ResultSet getsRs = getsStatement.executeQuery();
				while(getsRs.next()){
					stag.gets = getsRs.getInt(1);
				}
				getsStatement.close();
				getsRs.close();
				
				PreparedStatement savesStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM wall WHERE steg_id = ?;");
				savesStatement.setInt(1, stag.stegId);
				ResultSet savesRs = savesStatement.executeQuery();
				while(savesRs.next()){
					stag.saves = savesRs.getInt(1);
				}
				savesStatement.close();
				savesRs.close();
				
				PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
				likedStatement.setInt(1, stag.stegId);
				likedStatement.setString(2, userId);
				ResultSet likedRs = likedStatement.executeQuery();
				stag.liked = false;
				while(likedRs.next()){
					stag.liked = true;
				}
				likedRs.close();
				likedStatement.close();
			}
			rs.close();
			statement.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stag;
	}

	public static ArrayList<StagData> getIncomePrivateStegs(String profileId, Connection dbConnection){
		ArrayList<StagData> stegList = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT steg_id, sender, reciever, type, life_time,"
	+ " filter, text, voice_path, camera_path,"
	+ " anonym, date_, time_, sended"
	+ " FROM stegs"
	+ " WHERE reciever = ? AND life_time = 0 AND reciever != sender ORDER BY steg_id DESC;");
			statement.setString(1, profileId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				StagData steg = new StagData();
				steg.stegId = rs.getInt(1);
				steg.mesSender = rs.getString(2);
				steg.mesReciever = rs.getString(3);
				steg.stagType = rs.getInt(4);
				steg.lifeTime = rs.getInt(5);
				steg.filter = rs.getInt(6);
				steg.mesText = rs.getString(7);
				steg.voiceDataFile = rs.getString(8);
				steg.cameraDataFile = rs.getString(9);
				steg.anonym = rs.getBoolean(10);
				steg.date = rs.getDate(11);
				steg.time = rs.getTime(12);
				steg.sended = rs.getBoolean(13);
				
				PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
				likeStatement.setInt(1, steg.stegId);
				ResultSet likeRs = likeStatement.executeQuery();
				while(likeRs.next()){
					steg.likes = likeRs.getInt(1);
				}
			
				PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
				commentStatement.setInt(1, steg.stegId);
				ResultSet commentRs = commentStatement.executeQuery();
				while(commentRs.next()){
					steg.comments = commentRs.getInt(1);
				}
				stegList.add(steg);
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return stegList;
	}
	
	public static ArrayList<StegItem> getIncomeCommonItems(String profileId, Connection dbConnection){
		ArrayList<StegItem> stegItems = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT stegs.steg_id, stegs.sender, stegs.anonym"
					+ " FROM stegs JOIN gets ON stegs.steg_id = gets.steg_id"
					+ " WHERE stegs.reciever = ? AND gets.profile_id = ? ORDER BY stegs.steg_id DESC;");
			statement.setString(1, "common");
			statement.setString(2, profileId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				Integer stegId = rs.getInt(1);
				String mesSender = rs.getString(2);
				Boolean anonym = rs.getBoolean(3);
								
				StegItem stegItem = new StegItem(stegId, mesSender, anonym);
							
				PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
				likeStatement.setInt(1, stegId);
				ResultSet likeRs = likeStatement.executeQuery();
				while(likeRs.next()){
					stegItem.setLikes(likeRs.getInt(1));
				}
				likeRs.close();
				likeStatement.close();
								
				PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
				likedStatement.setInt(1, stegId);
				likedStatement.setString(2, profileId);
				ResultSet likedRs = likedStatement.executeQuery();
				while(likedRs.next()){
					stegItem.setLiked(true);
				}
				likedRs.close();
				likedStatement.close();
							
				PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
				commentStatement.setInt(1, stegId);
				ResultSet commentRs = commentStatement.executeQuery();
				while(commentRs.next()){
					stegItem.setComments(commentRs.getInt(1));
				}
				commentRs.close();
				commentStatement.close();
				stegItems.add(stegItem);
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return stegItems;
	}
	
	public static ArrayList<StegItem> getIncomePrivateItems(String profileId, Connection dbConnection){
		ArrayList<StegItem> stegItems = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT steg_id, sender, anonym"
	+ " FROM stegs"
	+ " WHERE reciever = ? AND reciever != sender ORDER BY steg_id DESC;");
			statement.setString(1, profileId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				Integer stegId = rs.getInt(1);
				String mesSender = rs.getString(2);
				Boolean anonym = rs.getBoolean(3);
				
				StegItem stegItem = new StegItem(stegId, mesSender, anonym);
				
				PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
				likeStatement.setInt(1, stegId);
				ResultSet likeRs = likeStatement.executeQuery();
				while(likeRs.next()){
					stegItem.setLikes(likeRs.getInt(1));
				}
				likeRs.close();
				likeStatement.close();
				
				PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
				likedStatement.setInt(1, stegId);
				likedStatement.setString(2, profileId);
				ResultSet likedRs = likedStatement.executeQuery();
				while(likedRs.next()){
					stegItem.setLiked(true);
				}
				likedRs.close();
				likedStatement.close();
			
				PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
				commentStatement.setInt(1, stegId);
				ResultSet commentRs = commentStatement.executeQuery();
				while(commentRs.next()){
					stegItem.setComments(commentRs.getInt(1));
				}
				commentRs.close();
				commentStatement.close();
				stegItems.add(stegItem);
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return stegItems;
	}

	public static ArrayList<StagData> getOutcomePrivateStegs(String profileId, Connection dbConnection){
		ArrayList<StagData> stegList = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT stegs.steg_id, stegs.sender, stegs.reciever, stegs.type, stegs.life_time,"
	+ " stegs.filter, stegs.text, stegs.voice_path, stegs.camera_path,"
	+ " stegs.anonym, stegs.date_, stegs.time_"
	+ " FROM stegs"
	+ " WHERE sender = ? AND reciever != ? AND reciever != sender ORDER BY steg_id DESC;");
			statement.setString(1, profileId);
			statement.setString(2, "common");
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				StagData steg = new StagData();
				steg.stegId = rs.getInt(1);
				steg.mesSender = rs.getString(2);
				steg.mesReciever = rs.getString(3);
				steg.stagType = rs.getInt(4);
				steg.lifeTime = rs.getInt(5);
				steg.filter = rs.getInt(6);
				steg.mesText = rs.getString(7);
				steg.voiceDataFile = rs.getString(8);
				steg.cameraDataFile = rs.getString(9);
				steg.anonym = rs.getBoolean(10);
				steg.date = rs.getDate(11);
				steg.time = rs.getTime(12);
				
				PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
				likeStatement.setInt(1, steg.stegId);
				ResultSet likeRs = likeStatement.executeQuery();
				while(likeRs.next()){
					steg.likes = likeRs.getInt(1);
				}
				likeRs.close();
				likeStatement.close();
			
				PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
				commentStatement.setInt(1, steg.stegId);
				ResultSet commentRs = commentStatement.executeQuery();
				while(commentRs.next()){
					steg.comments = commentRs.getInt(1);
				}
				commentRs.close();
				commentRs.close();
				
				PreparedStatement getsStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM gets WHERE steg_id = ?;");
				getsStatement.setInt(1, steg.stegId);
				ResultSet getsRs = getsStatement.executeQuery();
				while(getsRs.next()){
					steg.gets = getsRs.getInt(1);
				}
				getsStatement.close();
				getsRs.close();
				
				PreparedStatement savesStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM wall WHERE steg_id = ?;");
				savesStatement.setInt(1, steg.stegId);
				ResultSet savesRs = savesStatement.executeQuery();
				while(savesRs.next()){
					steg.saves = savesRs.getInt(1);
				}
				savesStatement.close();
				savesRs.close();
				
				stegList.add(steg);
			}
			rs.close();
			statement.close();
			
		} catch (Exception e){
			e.printStackTrace();
		}
	
		return stegList;
	}

	public static ArrayList<StegItem> getOutcomePrivateItems(String profileId, Connection dbConnection){
		ArrayList<StegItem> stegItems = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT stegs.steg_id, stegs.sender, stegs.anonym"
	+ " FROM stegs"
	+ " WHERE sender = ? AND reciever != ? AND reciever != sender AND life_time = 0 ORDER BY steg_id DESC;");
			statement.setString(1, profileId);
			statement.setString(2, "common");
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				Integer stegId = rs.getInt(1);
				String mesSender = rs.getString(2);
				Boolean anonym = rs.getBoolean(3);
								
				StegItem stegItem = new StegItem(stegId, mesSender, anonym);
				
				PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
				likeStatement.setInt(1, stegId);
				ResultSet likeRs = likeStatement.executeQuery();
				while(likeRs.next()){
					stegItem.setLikes(likeRs.getInt(1));
				}
				likeRs.close();
				likeStatement.close();
				
				PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
				likedStatement.setInt(1, stegId);
				likedStatement.setString(2, profileId);
				ResultSet likedRs = likedStatement.executeQuery();
				while(likedRs.next()){
					stegItem.setLiked(true);
				}
				likedRs.close();
				likedStatement.close();
			
				PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
				commentStatement.setInt(1, stegId);
				ResultSet commentRs = commentStatement.executeQuery();
				while(commentRs.next()){
					stegItem.setComments(commentRs.getInt(1));
				}
				commentRs.close();
				commentStatement.close();
				
				stegItems.add(stegItem);
			}
			rs.close();
			statement.close();
			
		} catch (Exception e){
			e.printStackTrace();
		}
	
		return stegItems;
	}

	public static ArrayList<StagData> getOutcomeCommonStegs(String profileId, Connection dbConnection){
		ArrayList<StagData> stegList = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT steg_id, sender, reciever, type, life_time,"
	+ " filter, text, voice_path, camera_path,"
	+ " anonym, date_, time_"
	+ " FROM stegs"
	+ " WHERE sender = ? AND reciever = ? AND life_time = 0 ORDER BY steg_id DESC;");
			statement.setString(1, profileId);
			statement.setString(2, "common");
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				StagData steg = new StagData();
				steg.stegId = rs.getInt(1);
				steg.mesSender = rs.getString(2);
				steg.mesReciever = rs.getString(3);
				steg.stagType = rs.getInt(4);
				steg.lifeTime = rs.getInt(5);
				steg.filter = rs.getInt(6);
				steg.mesText = rs.getString(7);
				steg.voiceDataFile = rs.getString(8);
				steg.cameraDataFile = rs.getString(9);
				steg.anonym = rs.getBoolean(10);
				steg.date = rs.getDate(11);
				steg.time = rs.getTime(12);
				
				PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
				likeStatement.setInt(1, steg.stegId);
				ResultSet likeRs = likeStatement.executeQuery();
				while(likeRs.next()){
					steg.likes = likeRs.getInt(1);
				}
				likeRs.close();
				likeStatement.close();
			
				PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
				commentStatement.setInt(1, steg.stegId);
				ResultSet commentRs = commentStatement.executeQuery();
				while(commentRs.next()){
					steg.comments = commentRs.getInt(1);
				}
				commentRs.close();
				commentStatement.close();
				
				PreparedStatement getsStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM gets WHERE steg_id = ?;");
				getsStatement.setInt(1, steg.stegId);
				ResultSet getsRs = getsStatement.executeQuery();
				while(getsRs.next()){
					steg.gets = getsRs.getInt(1);
				}
				getsStatement.close();
				getsRs.close();
				
				PreparedStatement savesStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM wall WHERE steg_id = ?;");
				savesStatement.setInt(1, steg.stegId);
				ResultSet savesRs = savesStatement.executeQuery();
				while(savesRs.next()){
					steg.saves = savesRs.getInt(1);
				}
				savesStatement.close();
				savesRs.close();
				
				stegList.add(steg);
			}
			rs.close();
			statement.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		return stegList;
	}
	
	public static ArrayList<StegItem> getOutcomeCommonItems(String profileId, Connection dbConnection){
		ArrayList<StegItem> stegItems = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT steg_id, sender, anonym"
	+ " FROM stegs"
	+ " WHERE sender = ? AND reciever = ? AND life_time = 0 ORDER BY steg_id DESC;");
			statement.setString(1, profileId);
			statement.setString(2, "common");
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				Integer stegId = rs.getInt(1);
				String mesSender = rs.getString(2);
				Boolean anonym = rs.getBoolean(3);			
				
				StegItem stegItem = new StegItem(stegId, mesSender, anonym);
								
				PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
				likeStatement.setInt(1, stegId);
				ResultSet likeRs = likeStatement.executeQuery();
				while(likeRs.next()){
					stegItem.setLikes(likeRs.getInt(1));
				}
				likeRs.close();
				likeStatement.close();
				
				PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
				likedStatement.setInt(1, stegId);
				likedStatement.setString(2, profileId);
				ResultSet likedRs = likedStatement.executeQuery();
				while(likedRs.next()){
					stegItem.setLiked(true);
				}
				likedRs.close();
				likedStatement.close();
			
				PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
				commentStatement.setInt(1, stegId);
				ResultSet commentRs = commentStatement.executeQuery();
				while(commentRs.next()){
					stegItem.setComments(commentRs.getInt(1));
				}
				commentRs.close();
				commentStatement.close();
				
				ArrayList<LikeData> geters = DBHandler.getGeters(stegItem.getStegId(), dbConnection);
				stegItem.setRecieverCount(geters.size());
				for (LikeData getersItem : geters){
					stegItem.recieverIds.put(getersItem.profileId, null);
				}
				stegItems.add(stegItem);
			}
			rs.close();
			statement.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		return stegItems;
	}

	public static Integer getUnrecievedSteg(Connection dbConnection){
		ArrayList<Integer> stagId = new ArrayList<Integer>();
		try {
			PreparedStatement statement = dbConnection.prepareStatement("SELECT steg_id, sender FROM stegs WHERE recieved = 0 AND reciever = ?");
			statement.setString(1, "common");
			ResultSet rs = statement.executeQuery();
			Random rand = new Random(System.currentTimeMillis());
			while(rs.next()){
				stagId.add(rs.getInt("steg_id")); 	
			}
			if (stagId.size() > 0){
				Integer r = rand.nextInt(stagId.size());
				rs.close();
				statement.close();
				return stagId.get(r);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static ArrayList<StagData> getUnrecievedStegs(Connection dbConnection){
		ArrayList<StagData> stegList = new ArrayList<StagData>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT steg_id, filter, sender" +
		" FROM stegs WHERE recieved = 0 AND reciever = 'common'");
			ResultSet rs = statement.executeQuery();
			while (rs.next()){
				StagData steg = new StagData();
				steg.setStegId(rs.getInt(1));
				steg.setFilter(rs.getInt(2));
				steg.setMesSender(rs.getString(3));
				stegList.add(steg);
			}
			rs.close();
			statement.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		return stegList;
	}

	public static void setStegUnrecieved(Integer stegId, Connection dbConnection){
		PreparedStatement statement;
		try {
			statement = dbConnection.prepareStatement("UPDATE stegs SET recieved = 0 WHERE steg_id = ?");
			statement.setInt(1, stegId);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void deleteIncomeSteg(Integer stegId, String profileId, Connection dbConnection){
		PreparedStatement statement;
		String sqlQuery;
		try{
			sqlQuery = 	"BEGIN;"+
						" UPDATE stegs SET reciever = 'clear' WHERE steg_ig = ? AND reciever = ?;" +
						" DELETE FROM wall "+
						"COMMIT;";
			statement = dbConnection.prepareStatement(sqlQuery);
			statement.setInt(1, stegId);
			statement.setString(2, profileId);
			statement.executeUpdate();
		} catch (SQLException e){
			e.printStackTrace();
		}
	}
	
	public static void deleteSteg(Integer stegId, String profileId, Connection dbConnection){
		try {
			Boolean check = false;
			PreparedStatement checkStatement = dbConnection.prepareStatement("SELECT * FROM stegs WHERE steg_id = ? AND sender = ?;");
			checkStatement.setInt(1, stegId);
			checkStatement.setString(2, profileId);
			ResultSet rs = checkStatement.executeQuery();
			while (rs.next()) {
				check = true;
			}
			if(check){
				PreparedStatement statement = dbConnection.prepareStatement(
					"BEGIN;"
					+ " DELETE FROM likes WHERE steg_id = ?;"
					+ " DELETE FROM comments WHERE steg_id = ?;"
					+ " DELETE FROM news WHERE steg_id = ?;"
					+ " DELETE FROM wall WHERE steg_id = ?;"
					+ " DELETE FROM gets WHERE steg_id = ?;"
					+ " DELETE FROM stegs WHERE steg_id = ?;"
					+ " COMMIT;");
				statement.setInt(1, stegId);
				statement.setInt(2, stegId);
				statement.setInt(3, stegId);
				statement.setInt(4, stegId);
				statement.setInt(5, stegId);
				statement.setInt(6, stegId);
				statement.executeUpdate();
				statement.close();
			}
			rs.close();
			checkStatement.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void deleteStegAdm(Integer stegId, Connection dbConnection){
		try {
			PreparedStatement statement = dbConnection.prepareStatement(
				"BEGIN;"
					+ " DELETE FROM likes WHERE steg_id = ?;"
					+ " DELETE FROM comments WHERE steg_id = ?;"
					+ " DELETE FROM news WHERE steg_id = ?;"
					+ " DELETE FROM wall WHERE steg_id = ?;"
					+ " DELETE FROM gets WHERE steg_id = ?;"
					+ " DELETE FROM stegs WHERE steg_id = ?;"
					+ " COMMIT;");
			statement.setInt(1, stegId);
			statement.setInt(2, stegId);
			statement.setInt(3, stegId);
			statement.setInt(4, stegId);
			statement.setInt(5, stegId);
			statement.setInt(6, stegId);
			statement.executeUpdate();
			statement.close();

			statement.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public static void markUnrecievedSteg(Integer stegId, Connection dbConnection){
		try{
			PreparedStatement statement = dbConnection.prepareStatement("UPDATE stegs SET recieved = recieved - 1 WHERE steg_id = ?;");
			statement.setInt(1, stegId);
			statement.executeUpdate();
			statement.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void markRecievedSteg(Integer stegId, Connection dbConnection){
		try{
			PreparedStatement statement = dbConnection.prepareStatement("UPDATE stegs SET recieved = recieved + 1 WHERE steg_id = ?;");
			statement.setInt(1, stegId);
			statement.executeUpdate();
			statement.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void markPrivetStegSended(Integer stegId, Connection dbConnection) {
		try{
			PreparedStatement statement = dbConnection.prepareStatement("UPDATE stegs SET sended = true WHERE steg_id = ?;");
			statement.setInt(1, stegId);
			statement.executeUpdate();
			statement.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getStegOwner(Integer stegId, Connection dbConnection){
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT sender FROM stegs WHERE steg_id = " + stegId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				return rs.getString("sender");
			}
			rs.close();
			statement.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void stegToWall(Integer stegId, String userId, Connection dbConnection){
		try{
			Boolean check = true;
			PreparedStatement checkStatement = dbConnection.prepareStatement("SELECT * FROM wall WHERE steg_id = ? AND owner = ?;");
			checkStatement.setInt(1, stegId);
			checkStatement.setString(2, userId);
			ResultSet checkRs = checkStatement.executeQuery();
			while (checkRs.next()){
				check = false;
			}
			checkRs.close();
			checkStatement.close();
			if(check){
				PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO wall(steg_id, owner) VALUES (?, ?)");
				statement.setInt(1, stegId);
				statement.setString(2, userId);
				statement.executeUpdate();
				statement.close();
			
				DBHandler.addNews("save", userId, null, stegId, dbConnection);
			}
			checkRs.close();
			checkStatement.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static StegRequestItem stegRequset(String reqProfileId, Connection dbConnection){
		StegRequestItem srItem = new StegRequestItem();
		try {
			PreparedStatement statement = dbConnection.prepareStatement("SELECT * FROM "
					+ "(SELECT DISTINCT stegs.steg_id, users.user_city "
					+ "FROM stegs LEFT JOIN users ON (stegs.sender = users.user_id) "
					+ "LEFT JOIN wall ON (stegs.steg_id = wall.steg_id) "
					+ "LEFT JOIN gets ON (stegs.steg_id  = gets.steg_id) "
					+ "WHERE stegs.reciever = ? "
					+ "AND stegs.sender != ? "
					+ "AND (wall.owner != ? OR wall.owner IS NULL) "
					+ "AND (gets.profile_id != ? OR gets.profile_id IS NULL)) sub "
					+ "ORDER BY random() LIMIT 1;");
			statement.setString(1, "common");
			statement.setString(2, reqProfileId);
			statement.setString(3, reqProfileId);
			statement.setString(4, reqProfileId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				srItem = new StegRequestItem(rs.getInt(1), rs.getString(2));
				break;
			}
			rs.close();
			statement.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		return srItem;
	}
	
	public static void removeStegFromWall(Integer stegId, String ownerId, Connection dbConnection){
		try{
			PreparedStatement statement = dbConnection.prepareStatement("DELETE FROM wall WHERE steg_id = ? AND owner = ?");
			statement.setInt(1, stegId);
			statement.setString(2, ownerId);
			statement.executeUpdate();
			statement.close();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ArrayList<StagData> getWall(String userId, Connection dbConnection){
		ArrayList<StagData> stegList = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT wall.steg_id, stegs.sender, stegs.reciever, stegs.type, stegs.life_time,"
	+ " stegs.filter, stegs.text, stegs.voice_path, stegs.camera_path,"
	+ " stegs.anonym, stegs.date_, stegs.time_"
	+ " FROM wall JOIN stegs ON wall.steg_id = stegs.steg_id"
	+ " WHERE wall.owner = ? ORDER BY wall.steg_id DESC;");
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				StagData steg = new StagData();
				steg.stegId = rs.getInt(1);
				steg.mesSender = rs.getString(2);
				steg.mesReciever = rs.getString(3);
				steg.stagType = rs.getInt(4);
				steg.lifeTime = rs.getInt(5);
				steg.filter = rs.getInt(6);
				steg.mesText = rs.getString(7);
				steg.voiceDataFile = rs.getString(8);
				steg.cameraDataFile = rs.getString(9);
				steg.anonym = rs.getBoolean(10);
				steg.date = rs.getDate(11);
				steg.time = rs.getTime(12);
				
				PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
				likeStatement.setInt(1, steg.stegId);
				ResultSet likeRs = likeStatement.executeQuery();
				while(likeRs.next()){
					steg.likes = likeRs.getInt(1);
				}
				likeRs.close();
				likeStatement.close();
				
				PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
				likedStatement.setInt(1, steg.stegId);
				likedStatement.setString(2, userId);
				ResultSet likedRs = likedStatement.executeQuery();
				while(likedRs.next()){
					steg.liked = true;
				}
				likedRs.close();
				likedStatement.close();
			
				PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
				commentStatement.setInt(1, steg.stegId);
				ResultSet commentRs = commentStatement.executeQuery();
				while(commentRs.next()){
					steg.comments = commentRs.getInt(1);
				}
				commentRs.close();
				commentStatement.close();
				
				PreparedStatement nameStatement = dbConnection.prepareStatement("SELECT user_name FROM users WHERE user_id = ?");
				nameStatement.setString(1, steg.mesSender);
				ResultSet nameRs = nameStatement.executeQuery();
				steg.senderName = steg.mesSender;
				while(nameRs.next()){
					steg.senderName = nameRs.getString(1);
				}
				nameRs.close();
				nameStatement.close();
				stegList.add(steg);
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return stegList;
	}
	
	public static ArrayList<StegItem> getWallItems(String userId, Connection dbConnection){
		ArrayList<StegItem> stegItems = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT wall.steg_id, stegs.sender, stegs.anonym"
	+ " FROM wall JOIN stegs ON wall.steg_id = stegs.steg_id"
	+ " WHERE wall.owner = ? ORDER BY wall.steg_id DESC;");
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				Integer stegId = rs.getInt(1);
				String mesSender = rs.getString(2);
				Boolean anonym = rs.getBoolean(3);		
				StegItem stegItem = new StegItem(stegId, mesSender, anonym);
				
				PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
				likeStatement.setInt(1, stegId);
				ResultSet likeRs = likeStatement.executeQuery();
				while(likeRs.next()){
					stegItem.setLikes(likeRs.getInt(1));
				}
				likeRs.close();
				likeStatement.close();
				
				PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
				likedStatement.setInt(1, stegId);
				likedStatement.setString(2, userId);
				ResultSet likedRs = likedStatement.executeQuery();
				while(likedRs.next()){
					stegItem.setLiked(true);
				}
				likedRs.close();
				likedStatement.close();
			
				PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
				commentStatement.setInt(1, stegId);
				ResultSet commentRs = commentStatement.executeQuery();
				while(commentRs.next()){
					stegItem.setComments(commentRs.getInt(1));
				}
				commentRs.close();
				commentStatement.close();
				
				stegItems.add(stegItem);
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return stegItems;
	}

	public static Boolean addComment(Integer stegId, String profileId, String text, Connection dbConnection){
		Boolean res = false;
		try{
			PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO comments " +
						"(steg_id, profile_id, text) " +
						"VALUES (?, ?, ?)");
			statement.setInt(1, stegId);
			statement.setString(2, profileId);
			statement.setString(3, text);
			
			statement.executeUpdate();
			statement.close();
			
			DBHandler.addNews("comment", profileId, null, stegId, dbConnection);
			res = true;
			
		} catch (Exception e) {
			e.printStackTrace();
			res= false;
		}
		return res;
	}
	
	public static void addComment(CommentData comment, Connection dbConnection){
		try{
			PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO comments " +
						"(steg_id, profile_id, data_type, text, camera_data, voice_data) " +
						"VALUES (?, ?, ?, ?, ?, ?) RETURNING id;");
			statement.setInt(1, comment.stegId);
			statement.setString(2, comment.profileId);
			statement.setInt(3, comment.getType());
			statement.setString(4, comment.getText());
			if((comment.getType() & CommentData.COMMENT_IMAGE_MASK) != 0){
				statement.setString(5, comment.getImgData());
			} else {
				if((comment.getType() & CommentData.COMMENT_VIDEO_MASK) != 0){
					statement.setString(5, comment.getVideoData());
				} else {
					statement.setString(5, "clear");
				}
			}
			if((comment.getType() & CommentData.COMMENT_VOICE_MASK) != 0){
				statement.setString(6, comment.getVoiceData());
			} else {
				statement.setString(6,"clear");
			}
			
			ResultSet rs = statement.executeQuery();
			while (rs.next()){
				Integer res = rs.getInt(1);
				DBHandler.addNews("comment", comment.profileId, null, comment.stegId, dbConnection);
				rs.close();
				statement.close();
				break;
			}			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Boolean addLike(Integer stegId, String profileId, Connection dbConnection){
		Boolean res = false;
		try{
			Boolean check = true;
			PreparedStatement checkStatement = dbConnection.prepareStatement("SELECT profile_id FROM likes WHERE steg_id = ? AND profile_id = ?");
			checkStatement.setInt(1, stegId);
			checkStatement.setString(2, profileId);
			ResultSet rs = checkStatement.executeQuery();
			while(rs.next()){
				check = false;
			}
			if(check){
				PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO likes " +
						"(steg_id, profile_id) " +
						"VALUES (?, ?)");
				statement.setInt(1, stegId);
				statement.setString(2, profileId);			
				statement.executeUpdate();
				statement.close();
				
				DBHandler.addNews("like", profileId, null, stegId, dbConnection);
				
				res = true;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			res= false;
		}
		return res;
	}

	public static void addGeter(Integer stegId, String profileId, Connection dbConnection){
		try{
			Boolean check = true;
			PreparedStatement checkStatement = dbConnection.prepareStatement("SELECT profile_id FROM gets WHERE steg_id = ? AND profile_id = ?");
			checkStatement.setInt(1, stegId);
			checkStatement.setString(2, profileId);
			ResultSet rs = checkStatement.executeQuery();
			while(rs.next()){
				check = false;
			}
			if(check){
				PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO gets " +
						"(steg_id, profile_id) " +
						"VALUES (?, ?)");
				statement.setInt(1, stegId);
				statement.setString(2, profileId);			
				statement.executeUpdate();
				statement.close();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ArrayList<CommentData> getComments(Integer stegId, Connection dbConnection){
		ArrayList<CommentData> commentList = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT comments.id, comments.steg_id, " + 
									"comments.profile_id, users.user_name, users.user_photo, " + 
									"comments.text, comments.date_, comments.time_ " +
									"FROM comments JOIN users ON comments.profile_id = users.user_id " +
									"WHERE comments.steg_id = ? ORDER BY comments.id;");
			statement.setInt(1, stegId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				CommentData comment = new CommentData();
				comment.id = rs.getInt(1);
				comment.stegId = rs.getInt(2);
				comment.profileId = rs.getString(3);
				String profileId = rs.getString(4);
				String profileImg = rs.getString(5);
				String text = rs.getString(6);
				if(text != null)
					comment.setText(text);
				comment.date = rs.getDate(7);
				comment.time = rs.getTime(8);
				commentList.add(comment);
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return commentList;
	}

	public static CommentData getComment(Integer commentId, Connection dbConnection){
		CommentData comment = new CommentData();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT id, steg_id, profile_id, data_type, " + 
								"text, camera_data, voice_data, date_, time_ " +
								"FROM comments WHERE id = ?;");
			statement.setInt(1, commentId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()){
				comment.id = rs.getInt(1);
				comment.stegId = rs.getInt(2);
				comment.profileId = rs.getString(3);
				comment.setType(rs.getInt(4));
				comment.setText(rs.getString(5));
				if((comment.getType() & CommentData.COMMENT_IMAGE_MASK) != 0){
					comment.setImgData(rs.getString(6));
				}
				if((comment.getType() & CommentData.COMMENT_VIDEO_MASK) != 0){
					comment.setVideoData(rs.getString(6));
				}
				comment.setVoiceData(rs.getString(7));
				comment.date = rs.getDate(8);
				comment.time = rs.getTime(9);
				rs.close();
				statement.close();
				return comment;
			}
			rs.close();
			statement.close();
		} catch (SQLException e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static ArrayList<CommentItem> getCommentsItems(Integer stegId, Connection dbConnection){
		ArrayList<CommentItem> items = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT comments.id, comments.steg_id, comments.data_type,  " + 
									"comments.profile_id, " + 
									"comments.text, comments.date_, comments.time_ " +
									"FROM comments " +
									"WHERE comments.steg_id = ? ORDER BY comments.id;");
			statement.setInt(1, stegId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				CommentItem item = new CommentItem();
				item.setId(rs.getInt(1));
				item.setStegId(rs.getInt(2));
				item.setType(rs.getInt(3));
				item.setProfileId(rs.getString(4));
				item.setText(rs.getString(5));					
				item.setDate(rs.getDate(6));
				item.setTime(rs.getTime(7));
				items.add(item);
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return items;
	}
	
	public static ArrayList<LikeData> getLikes(Integer stegId, Connection dbConnection){
		ArrayList<LikeData> likeList = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT likes.id, likes.steg_id, " + 
					"likes.profile_id, users.user_name, users.user_photo, " + 
					"likes.date_, likes.time_ " +
					"FROM likes JOIN users ON likes.profile_id = users.user_id " +
					"WHERE likes.steg_id = ? ORDER BY likes.id DESC;");
			statement.setInt(1, stegId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				LikeData like = new LikeData();
				like.id = rs.getInt(1);
				like.stegId = rs.getInt(2);
				like.profileId = rs.getString(3);
				String profileId = rs.getString(4);
				if (profileId != null)
					like.profileName = profileId;
				String profileImg = rs.getString(5);
				if (profileImg != null)
					like.profileImg = profileImg;
				like.date = rs.getDate(6);
				like.time = rs.getTime(7);
				likeList.add(like);
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return likeList;
	}

	public static ArrayList<LikeData> getSavers(Integer stegId, Connection dbConnection){
		ArrayList<LikeData> saversList = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT wall.id, wall.steg_id, " + 
					"wall.owner, users.user_name, users.user_photo " + 
					"FROM wall JOIN users ON wall.owner = users.user_id " +
					"WHERE wall.steg_id = ? ORDER BY wall.id DESC;");
			statement.setInt(1, stegId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				LikeData saver = new LikeData();
				saver.id = rs.getInt(1);
				saver.stegId = rs.getInt(2);
				saver.profileId = rs.getString(3);
				String profileId = rs.getString(4);
				if (profileId != null)
					saver.profileName = profileId;
				String profileImg = rs.getString(5);
				if (profileImg != null)
					saver.profileImg = profileImg;
				saversList.add(saver);
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return saversList;
	}

	public static ArrayList<LikeData> getGeters(Integer stegId, Connection dbConnection){
		ArrayList<LikeData> getersList = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT gets.id, gets.steg_id, " + 
					"gets.profile_id, users.user_name, users.user_photo " + 
					"FROM gets JOIN users ON gets.profile_id = users.user_id " +
					"WHERE gets.steg_id = ? ORDER BY gets.id DESC;");
			statement.setInt(1, stegId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				LikeData geter = new LikeData();
				geter.id = rs.getInt(1);
				geter.stegId = rs.getInt(2);
				geter.profileId = rs.getString(3);
				String profileId = rs.getString(4);
				if (profileId != null)
					geter.profileName = profileId;
				String profileImg = rs.getString(5);
				if (profileImg != null)
					geter.profileImg = profileImg;
				getersList.add(geter);
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return getersList;
	}
	
	public static void addNews(String type, String profileId, String ownerId, Integer stegId, Connection dbConnection){
		try{
			PreparedStatement statement;
			if(ownerId == null){
				statement = dbConnection.prepareStatement("SELECT sender FROM stegs WHERE steg_id = ?");
				statement.setInt(1, stegId);
				ResultSet senderRs = statement.executeQuery();
				while(senderRs.next()){
					ownerId = senderRs.getString(1);
				}
			}
			
			statement = dbConnection.prepareStatement("INSERT INTO news(type, profile_id, steg_id, owner_id) " + 
						"VALUES(?, ?, ?, ?);");
			statement.setString(1, type);
			statement.setString(2, profileId);
			
			if(stegId != null)
				statement.setInt(3, stegId);
			else 
				statement.setInt(3, 0);
			statement.setString(4, ownerId);
				
			statement.executeUpdate();
				
			statement.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ArrayList<NewsData> getNews(String owner, Connection dbConnection){
		ArrayList<NewsData> news = new ArrayList<>();
		String sql = "SELECT news.type, news.profile_id, users.user_name, " +
					"users.user_photo, news.steg_id, stegs.camera_path, " +
					"news.date_, news.time_, news.sended, news.id " + 
					"FROM news LEFT OUTER JOIN users ON (news.profile_id = users.user_id) " +
					"LEFT OUTER JOIN stegs ON (news.steg_id = stegs.steg_id) " + 
					"WHERE news.owner_id = ? AND news.profile_id != news.owner_id " + 
					"ORDER BY news.id DESC;";
		try {
			PreparedStatement statement = dbConnection.prepareStatement(sql);
			statement.setString(1, owner);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				NewsData n = new NewsData();
				n.type = rs.getString(1);
				n.profileId = rs.getString(2);
				n.profileName = rs.getString(3);
				if(n.profileName == null) n.profileName = "clear";
				n.profileImg = rs.getString(4);
				if(n.profileImg == null) n.profileImg = "clear";
				n.stegId = rs.getInt(5);
				if(n.stegId == null){ 
					n.stegId = 0;
					n.stegImg = "clear";
				} else 
					n.stegImg = rs.getString(6);
				n.date = rs.getDate(7);
				n.time = rs.getTime(8);
				n.sended = rs.getBoolean(9);
				n.id = rs.getInt(10);
				news.add(n);
			}
			rs.close();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return news;
	}

	public static void markNewsSended(Integer newsId, Connection dbConnection) {
		try{
			PreparedStatement statement = dbConnection.prepareStatement("UPDATE news SET sended = true WHERE id = ?;");
			statement.setInt(1, newsId);
			statement.executeUpdate();
			statement.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void markAllStegNotificationsSended(String ownerId, Integer stegId, Connection dbConnection){
		try{
			PreparedStatement statement = dbConnection.prepareStatement("UPDATE news SET sended =  true WHERE owner_id = ? AND steg_id = ?;");
			statement.setString(1, ownerId);
			statement.setInt(2, stegId);
			statement.executeUpdate();
			statement.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	public static void addFriend(String profileId, String friendId, Connection dbConnection){
		if(!profileId.equals(friendId)){
			try{
				Boolean isFriend = false;
				PreparedStatement statement;
				statement = dbConnection.prepareStatement("SELECT friend_id FROM friends WHERE profile_id = ? AND friend_id = ?");
				statement.setString(1, profileId);
				statement.setString(2, friendId);
				ResultSet senderRs = statement.executeQuery();
				while(senderRs.next()){
					isFriend = true;
				}
				
				if(!isFriend){
					statement = dbConnection.prepareStatement("INSERT INTO friends(profile_id, friend_id) " + 
						"VALUES(?, ?);");
					statement.setString(1, profileId);
					statement.setString(2, friendId);
					statement.executeUpdate();
				
					DBHandler.addNews("friend", profileId, friendId, null, dbConnection);
				}
				senderRs.close();
				statement.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	

	public static void removeFriend(String profileId, String friendId, Connection dbConnection){
		try{
			PreparedStatement statement;
			statement = dbConnection.prepareStatement("DELETE FROM friends WHERE profile_id = ? AND friend_id = ?;");
			statement.setString(1, profileId);
			statement.setString(2, friendId);
			statement.executeUpdate();
			
			statement.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void addToBlackList(String myProfileId, String blackProfileId, Connection dbConnection){
		if(!myProfileId.equals(blackProfileId)){
			try{
				Boolean isBlack = false;
				PreparedStatement statement;
				statement = dbConnection.prepareStatement("SELECT black_profile_id FROM blacklist WHERE my_profile_id = ? AND black_profile_id = ?");
				statement.setString(1, myProfileId);
				statement.setString(2, blackProfileId);
				ResultSet rs = statement.executeQuery();
				while(rs.next()){
					isBlack = true;
				}
				
				if(!isBlack){
					statement = dbConnection.prepareStatement("INSERT INTO blacklist(my_profile_id, black_profile_id) " + 
						"VALUES(?, ?);");
					statement.setString(1, myProfileId);
					statement.setString(2, blackProfileId);
					statement.executeUpdate();
				}
				rs.close();
				statement.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void removeFromBlackList(String myProfileId, String blackProfileId, Connection dbConnection){
		try{
			PreparedStatement statement;
			statement = dbConnection.prepareStatement("DELETE FROM blacklist WHERE my_profile_id = ? AND black_profile_id = ?;");
			statement.setString(1, myProfileId);
			statement.setString(2, blackProfileId);
			statement.executeUpdate();
			
			statement.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void removeLike(Integer stegId, String profileId, Connection dbConnection){
		try{
			Boolean check = false;
			PreparedStatement checkStatement = dbConnection.prepareStatement("SELECT profile_id FROM likes WHERE steg_id = ? AND profile_id = ?");
			checkStatement.setInt(1, stegId);
			checkStatement.setString(2, profileId);
			ResultSet rs = checkStatement.executeQuery();
			while(rs.next()){
				check = true;
			}
			if(check){
				PreparedStatement statement = dbConnection.prepareStatement("DELETE FROM likes " +
						"WHERE steg_id = ? AND profile_id = ?;");
				statement.setInt(1, stegId);
				statement.setString(2, profileId);			
				statement.executeUpdate();
				statement.close();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean isFriend(String profileId, String friendId, Connection dbConnection){
		try {
			PreparedStatement statement = dbConnection.prepareStatement("SELECT friend_id FROM friends WHERE profile_id = ? AND friend_id = ?");
			statement.setString(1, profileId);
			statement.setString(2,  friendId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				rs.close();
				statement.close();
				return true;
			}
			rs.close();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
	public static boolean isBlack(String myProfileId, String blackProfileId, Connection dbConnection){
		try {
			PreparedStatement statement = dbConnection.prepareStatement("SELECT black_profile_id FROM blacklist WHERE my_profile_id = ? AND black_profile_id = ?");
			statement.setString(1, myProfileId);
			statement.setString(2,  blackProfileId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				rs.close();
				statement.close();
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
	public static Boolean isMeInBlackList(String myProfileId, String otherProfileId, Connection dbConnection){
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT black_profile_id FROM blacklist WHERE my_profile_id = ? AND black_profile_id = ?;");
			statement.setString(1, otherProfileId);
			statement.setString(2, myProfileId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				rs.close();
				statement.close();
				return true;
			}
		} catch (SQLException e){
			e.printStackTrace();
			return false;
		}
		return false;
	}

	public static ArrayList<UserProfile> getFriends (String profileId, Connection dbConnection){
		ArrayList<UserProfile> friendsList = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT friends.friend_id, users.user_name, users.user_photo, users.user_sex, users.user_city, users.user_state, users.user_age " + 
					"FROM friends JOIN users ON friends.friend_id = users.user_id WHERE friends.profile_id = ?;");
			statement.setString(1, profileId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				UserProfile friend = new UserProfile();
				friend.setId(rs.getString(1));
				friend.setName(rs.getString(2));
				String img = rs.getString(3);
				if(img!=null){
					friend.setPhoto(img);
				} else {
					friend.setPhoto("clear");
				}
				String sex = rs.getString(4);
				if(sex != null){
					friend.setSex(sex);
				}
				String city = rs.getString(5);
				if(city != null){
					friend.setCity(city);
				}
				String state = rs.getString(6);
				if(state != null){
					friend.setState(state);
				}
				Integer age = rs.getInt(7);
				if(age != null){
					friend.setAge(age);
				}
				
				friendsList.add(friend);
			}
			
			rs.close();
			statement.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		return friendsList;
	}
	
	public static ArrayList<String> getProfileSearchResult(String searchString, String myCity, Connection dbConnection){
		ArrayList<String> result = new ArrayList<>();
		try{
			PreparedStatement statement;
			if(searchString.equals("all_search_list")){
				if(!myCity.equals("clear")){
					statement = dbConnection.prepareStatement("SELECT user_id FROM users WHERE user_city = ? ORDER BY random() LIMIT 70;");
					statement.setString(1, myCity);
				} else {
					statement = dbConnection.prepareStatement("SELECT user_id FROM users ORDER BY random() LIMIT 70;");
				}
			} else {
				if(!myCity.equals("clear")){
					statement = dbConnection.prepareStatement("SELECT user_id, " +
										"similarity(?, user_id) + similarity(?, user_name) AS sml " +
										"FROM users WHERE user_city = ? AND (user_id % ? OR user_name % ?) " +
										"ORDER BY sml DESC;");
					statement.setString(1, searchString);
					statement.setString(2, searchString);
					statement.setString(4, searchString);
					statement.setString(5, searchString);
					statement.setString(3, myCity);
				} else {
					statement = dbConnection.prepareStatement("SELECT user_id, " +
										"similarity(?, user_id) + similarity(?, user_name) AS sml " +
										"FROM users WHERE user_id % ? OR user_name % ? " +
										"ORDER BY sml DESC;");
					statement.setString(1, searchString);
					statement.setString(2, searchString);
					statement.setString(3, searchString);
					statement.setString(4, searchString);
				}
				
			}
			ResultSet rs = statement.executeQuery();
			while (rs.next()){
				String searchItem = rs.getString(1);
				result.add(searchItem);
			}
			rs.close();
			statement.close();
		} catch (Exception e){
			e.printStackTrace();
			return result;
		}
		return result;
	}
	
	public static ArrayList<String> getSubscribers(String profileId, Connection dbConnection){
		ArrayList<String> result = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT profile_id FROM friends " + 
																		"WHERE friend_id = ?;");
			statement.setString(1, profileId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				String subscriber = rs.getString(1);
				result.add(subscriber);
			}
			rs.close();
			statement.close();
		} catch (Exception e){
			e.printStackTrace();
			return result;
		}
		return result;
	}
	
	public static ArrayList<String> getFriendsList(String profileId, Connection dbConnection){
		ArrayList<String> result = new ArrayList<>();
		try{
			PreparedStatement statement = dbConnection.prepareStatement("SELECT friend_id FROM friends " + 
																		"WHERE profile_id = ?;");
			statement.setString(1, profileId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				String subscriber = rs.getString(1);
				result.add(subscriber);
			}
			rs.close();
			statement.close();
		} catch (Exception e){
			e.printStackTrace();
			return result;
		}
		return result;
	}
}

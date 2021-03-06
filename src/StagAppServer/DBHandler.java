package StagAppServer;

import StagAppServer.dataClasses.*;
import StagAppServer.dataClasses.polls.Poll;
import StagAppServer.dataClasses.polls.PollItem;
import StagAppServer.fcm.FcmConnection;
import StagAppServer.fcm.FcmConsts;
import StagAppServer.localities.Locality;
import StagAppServer.location.PrizeLocation;
import StagAppServer.location.StegLocation;
import StagAppServer.location.UserLocation;
import StagAppServer.stegsFileManager.StegFileManager;
import com.sun.corba.se.impl.orb.PrefixParserAction;
import jdk.internal.util.xml.impl.Pair;

import java.sql.*;
import java.util.*;
import java.util.function.BooleanSupplier;

public class DBHandler {

    public static final int MAX_RECEIVED_NO_FIELD = 4;
    private static final int MAX_REQUESTED_NUM = 7;

    public static final float BONUS_FOR_LIKE = 1f;
    public static final float BONUS_FOR_COMMENT = 1f;
    public static final float BONUS_FOR_COMMENT_LIKE = 1f;
    public static final float BONUS_FOR_FIRST_STEG = 3f;

    private static final String SQL_GET_RECEIVER_FOR_STEG = "SELECT users.fcm_token, get_steg_sender_city(stegs.steg_id), get_sender_anonym_name(stegs.anonym, stegs.sender), stegs.mode  " +
            "FROM users, stegs " +
            "WHERE stegs.steg_id = ? " +
            "AND users.user_id != stegs.sender " +
            "AND users.fcm_token IS NOT NULL " +
            "AND users.user_id NOT IN(SELECT receives.profile_id FROM receives WHERE receives.steg_id = ?) " +
            "AND ((stegs.filter & " + Integer.toString(StagData.STEG_SEX_MASK) + " = 0) OR(stegs.filter & get_sex_filter_mask(users.user_sex)) != 0) " +
            "ORDER BY random() LIMIT 1;";

    private static final String SQL_STEG_REQUEST = "SELECT stegs.steg_id, get_steg_sender_city(stegs.steg_id), get_sender_anonym_name(stegs.anonym, stegs.sender), stegs.mode  " +
            "FROM stegs " +
            "LEFT JOIN receives ON(stegs.steg_id = receives.steg_id) " +
            "LEFT JOIN users ON (users.user_id = ?) " +
            "WHERE stegs.steg_id NOT IN (SELECT receives.steg_id FROM receives WHERE receives.profile_id = ?) " +
            "AND stegs.sender != ? " +
            "AND stegs.recieved < " + Integer.toString(MAX_REQUESTED_NUM) + " " +
            "AND stegs.reciever = 'common' " +
            "AND stegs.mode = 0" +
            "AND deleted != true " +
            "AND active = true " +
            "AND date_ + time_ > date_trunc('Day', now()) - interval '7 days' " +
            "AND ((stegs.filter & " + Integer.toString(StagData.STEG_SEX_MASK) + " = 0) OR " +
            "(stegs.filter & get_sex_filter_mask(users.user_sex) != 0)) " +
            "ORDER BY random() LIMIT 1;";

    private static final String SQL_STEG_REQUEST_V2 = "SELECT stegs.steg_id, get_steg_sender_city(stegs.steg_id), get_sender_anonym_name(stegs.anonym, stegs.sender), stegs.mode  " +
            "FROM stegs " +
            "LEFT JOIN receives ON(stegs.steg_id = receives.steg_id) " +
            "LEFT JOIN users ON (users.user_id = ?) " +
            "WHERE stegs.steg_id NOT IN (SELECT receives.steg_id FROM receives WHERE receives.profile_id = ?) " +
            "AND stegs.sender != ? " +
            "AND stegs.recieved < " + Integer.toString(MAX_REQUESTED_NUM) + " " +
            "AND stegs.reciever = 'common' " +
            "AND deleted != true " +
            "AND active = true " +
            "AND ((stegs.filter & " + Integer.toString(StagData.STEG_SEX_MASK) + " = 0) OR " +
            "(stegs.filter & get_sex_filter_mask(users.user_sex) != 0)) " +
            "ORDER BY random() LIMIT 1;";

    private static final String SQL_GET_UNRECEIVED_STEGS = "SELECT steg_id, filter, sender, mode " +
            "FROM stegs " +
            "WHERE recieved < " + Integer.toString(MAX_RECEIVED_NO_FIELD) + " " +
            "AND reciever = 'common' " +
            "AND active = TRUE " +
            "AND deleted != TRUE " +
            "AND date_ > date_trunc('Day', now()) - interval '4 days' " +
            "ORDER BY random();";

    private static final String SQL_GET_UNRECEIVED_STEGS_ID = "SELECT steg_id, recieved " +
            "FROM stegs " +
            "WHERE recieved < " + Integer.toString(MAX_RECEIVED_NO_FIELD) + " " +
            "AND reciever = 'common' " +
            "AND active = TRUE " +
            "AND deleted != TRUE " +
//            "AND date_ > date_trunc('Day', now()) - interval '1 days' " +
            "ORDER BY date_ DESC, time_ DESC;";

    private static final String SQL_CHECK_RECEIVERS = "SELECT users.user_id FROM users " +
            "WHERE users.user_id = ? " +
            "AND users.user_sex IN (SELECT * FROM convert_sex_mask_to_text_array(?)) " +
            "AND users.user_id NOT IN (SELECT receives.profile_id " +
            "FROM receives WHERE receives.steg_id = ? );";

    private static final String SQL_GET_STEG_SENDER_CITY = "SELECT users.user_city " +
            "FROM users JOIN stegs ON (stegs.sender = users.user_id) " +
            "WHERE stegs.steg_id = ?;";

    static Boolean newUser(String userId, String paswd, Connection dbConnection) {
        Boolean res = false;
        try {
            if (!checkUser(userId, dbConnection)) {
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

    static Boolean newUserSocial(String userId, String passwd, String socId, Connection dbConnection) {
        Boolean res = false;
        try {
            if (!checkUser(userId, dbConnection)) {
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
        } catch (SQLException e) {
            e.printStackTrace();
            res = false;
        }
        return res;
    }

    static void setUserOnline(String userId, Boolean online, Connection dbConnection) {
        try {
            PreparedStatement st = dbConnection.prepareStatement("UPDATE users SET online = ? WHERE user_id = ?;");
            st.setBoolean(1, online);
            st.setString(2, userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static String checkUserSocialId(String userSocId, Connection dbConnection) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT user_id FROM users WHERE user_soc_id = ?;");
            statement.setString(1, userSocId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String res = rs.getString(1);
                statement.close();
                rs.close();
                return res;
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public static Float getProfileAccount(String userId, Connection dbConnection) {
        float res = 0f;
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT account FROM users WHERE user_id = ?;");
            st.setString(1, userId);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                res = rs.getFloat(1);
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static float incAccount(String profileId, float delta, Connection dbConnection) {
        float res = 0f;
        try {
            PreparedStatement st = dbConnection.prepareStatement("UPDATE users SET account = account + ? WHERE user_id = ? RETURNING account;");
            st.setFloat(1, delta);
            st.setString(2, profileId);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                res = rs.getFloat(1);
            }
            rs.close();
            st.close();
            FcmConnection.getInstance().sendAccountDelta(profileId, delta, res);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static float decAccount(String profileId, float delta, Connection dbConnection) {
        float res = 0f;
        try {
            PreparedStatement st = dbConnection.prepareStatement("UPDATE users SET account = account - ? WHERE user_id = ? RETURNING account;");
            st.setFloat(1, delta);
            st.setString(2, profileId);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                res = rs.getFloat(1);
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    //    returning true if newEmail is already used by other user
    public static Boolean setNewUserEmail(String profileId, String email, String validation_code, Connection dbConnection) {
        try {
            String oldUserId = "clear";
            PreparedStatement st = dbConnection.prepareStatement("SELECT user_id FROM users WHERE user_email = ?;");
            st.setString(1, email);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                oldUserId = rs.getString(1);
            }
            rs.close();
            st.close();
            if (!oldUserId.equals("clear"))
                return false;

            st = dbConnection.prepareStatement("UPDATE users SET user_email='clear' WHERE user_id=?");
            st.setString(1, profileId);
            st.executeUpdate();
            st.close();

            st = dbConnection.prepareStatement("UPDATE email_validation SET email = ?, validation_code = ? WHERE user_id = ?;" +
                    "INSERT INTO email_validation (user_id, email, validation_code)" +
                    "       SELECT ?, ?, ?" +
                    "       WHERE NOT EXISTS (SELECT 1 FROM email_validation WHERE user_id = ?);");
            st.setString(1, email);
            st.setString(2, validation_code);
            st.setString(3, profileId);
            st.setString(4, profileId);
            st.setString(5, email);
            st.setString(6, validation_code);
            st.setString(7, profileId);
            st.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static Map<String, String> forgotPassword(String loginOrEmail, Connection dbConnection) {
        Map<String, String> res = null;
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT user_id, user_email, user_paswd FROM users WHERE user_email != 'clear' AND (user_id = ? OR user_email = ?);");
            st.setString(1, loginOrEmail);
            st.setString(2, loginOrEmail.toLowerCase());
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                res = new HashMap<>();
                res.put("userId", rs.getString(1));
                res.put("email", rs.getString(2));
                res.put("paswd", rs.getString(3));
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return res;
    }

    public static boolean changePassword(String profileId, String oldPassword, String newPassword, Connection dbConnection) {
        try {
            PreparedStatement st = dbConnection.prepareStatement("UPDATE users SET user_paswd = ? WHERE user_id = ? AND user_paswd = ?;");
            st.setString(1, newPassword);
            st.setString(2, profileId);
            st.setString(3, oldPassword);
            int count = st.executeUpdate();
            st.close();
            if (count < 1)
                return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String getNotValidEmail(String profile_id, Connection dbConnection) {
        String res = "clear";
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT email FROM email_validation WHERE user_id = ?;");
            st.setString(1, profile_id);
            ResultSet rs = st.executeQuery();
            if (rs.next())
                res = rs.getString(1);
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static UserProfile getUserProfile(String userId, Connection dbConnection) {
        UserProfile user = new UserProfile();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT user_id, user_name, user_sex, " +
                    "user_state, user_city, user_age, latitude, longitude, user_photo, user_email, status FROM users " +
                    "WHERE user_id = ?;");
            statement.setString(1, userId);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                user = new UserProfile(resultSet.getString("user_id"));
                String name = resultSet.getString("user_name");
                if (name != null) user.setName(name);
                String sex = resultSet.getString("user_sex");
                if (sex != null) user.setSex(sex);
                String state = resultSet.getString("user_state");
                if (state != null) user.setState(state);
                String city = resultSet.getString("user_city");
                if (city != null) user.setCity(city);
                Integer age = resultSet.getInt("user_age");
                if (age != null) user.setAge(age);
                Double latitude = resultSet.getDouble("latitude");
                Double longitude = resultSet.getDouble("longitude");
                if (latitude != null && latitude != null) {
                    user.setCoordinates(longitude, latitude);
                }
                String photo = resultSet.getString("user_photo");
                if (photo != null)
                    user.setPhoto(photo);
                user.setEmail(resultSet.getString("user_email"));
                String status = resultSet.getString("status");
                if (status != null)
                    user.setStatus(status);
            }

            statement.close();
            resultSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return user;
        }

        return user;
    }

    static String getStegSenderId(Integer stegId, Connection dbConnection) {
        String id = null;
        PreparedStatement statement = null;
        try {
            statement = dbConnection.prepareStatement("SELECT sender FROM stegs WHERE steg_id = ?");
            statement.setInt(1, stegId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                id = rs.getString(1);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    static int getCommentsCountForSteg(String profileId, Integer stegId, Connection dbConnection) {
        int res = 0;
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT COUNT(profile_id) FROM comments WHERE profile_id = ? AND steg_id = ?;");
            st.setString(1, profileId);
            st.setInt(2, stegId);
            ResultSet rs = st.executeQuery();
            if (rs.next())
                res = rs.getInt(1);

            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    static String getCommentOwner(Integer commentId, Connection dbConnection) {
        String res = UserProfile.NO_VALUE;
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT stegs.sender FROM stegs WHERE steg_id = (SELECT steg_id FROM comments WHERE id = ?);");
            st.setInt(1, commentId);
            ResultSet rs = st.executeQuery();
            if (rs.next())
                res = rs.getString(1);

            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    static String getUserNameFromId(String userId, Connection dbConnection) {
        String name = null;
        PreparedStatement statement = null;
        try {
            statement = dbConnection.prepareStatement("SELECT user_name FROM users WHERE user_id = ?;");
            statement.setString(1, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                name = rs.getString(1);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return name;
    }

    static Boolean checkUser(String userId, Connection dbConnection) {
        for (String bannedName : Constants.BannedStrings.bannedLogins) {
            if (bannedName.equalsIgnoreCase(userId))
                return true;
        }
        Boolean res = false;
        ResultSet rs = null;
        PreparedStatement statement = null;
        try {
            statement = dbConnection.prepareStatement("SELECT * FROM users WHERE user_id = ?;");
            statement.setString(1, userId);
            rs = statement.executeQuery();
            if (rs.next()) {
                res = true;
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return res;
    }

    public static Boolean checkPassword(String profileId, String passwd, Connection dbConnection) {
        Boolean res = false;
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT * FROM users WHERE user_id = ? AND user_paswd = ?");
            statement.setString(1, profileId);
            statement.setString(2, passwd);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                res = true;
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static Boolean deleteProfile(String profileId, Connection dbConnection) {
        try {
            String delString =
                    "BEGIN; " +
                            "DELETE FROM news WHERE profile_id = ?; " +
                            "DELETE  FROM users WHERE user_id = ?; " +
                            "COMMIT;";

            PreparedStatement statement = dbConnection.prepareStatement(delString);
            statement.setString(1, profileId);
            statement.setString(2, profileId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    static Boolean signIn(String userId, String paswd, Connection dbConnection) {
        Boolean res = false;
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT * FROM users WHERE user_id = ? AND user_paswd = ?");
            statement.setString(1, userId);
            statement.setString(2, paswd);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                res = true;
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static void setUserStatus(String profileId, String status, Connection dbConnection) {
        try {
            PreparedStatement st = dbConnection.prepareStatement("UPDATE users SET status = ? WHERE user_id = ?;");
            st.setString(1, status);
            st.setString(2, profileId);
            st.execute();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Boolean addUserInfo(UserProfile user, Connection dbConnection) {
        if (user.getId().equals("StegApp"))
            return false;

        Boolean res = false;
        try {
            PreparedStatement statement = dbConnection.prepareStatement("UPDATE users SET " +
                    "user_name = ?, user_sex = ?, user_state = ?, user_city = ?, " +
                    "user_age = ?, latitude = ?, longitude = ? WHERE user_id = ?;");
            statement.setString(1, user.getName());
            statement.setString(2, user.getSex());
            statement.setString(3, user.getState());
            statement.setString(4, user.getCity());
            statement.setInt(5, user.getAge());
            if (user.getLatitude() != null) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static void addUserInfoNoImgNoGeo(UserProfile user, Connection dbConnection) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("UPDATE  users SET " +
                    "user_name = ?, user_sex = ?, user_age = ? WHERE user_id = ?;");
            statement.setString(1, user.getName());
            statement.setString(2, user.getSex());
            statement.setInt(3, user.getAge());
            statement.setString(4, user.getId());
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void setUserShowCityEnabled(String userId, Boolean showCity, Connection dbConnection) {
        try {
            PreparedStatement st = dbConnection.prepareStatement("UPDATE users SET show_city = ? WHERE user_id = ?;");
            st.setBoolean(1, showCity);
            st.setString(2, userId);

            st.executeUpdate();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void setUserCoordinates(String userId, String city, Double latitude, Double longitude, Connection dbConnection) {
        String query;
        if (userId.equals("StegApp"))
            return;
        if (!city.equals("clear")) {
            query = "UPDATE users SET " +
                    "latitude = ?, longitude = ?, user_city = ? WHERE user_id = ?;";
        } else {
            query = "UPDATE users SET " +
                    "latitude = ?, longitude = ? WHERE user_id = ?;";
        }
        try {
            PreparedStatement st = dbConnection.prepareStatement(query);
            st.setDouble(1, latitude);
            st.setDouble(2, longitude);
            if (!city.equals("clear")) {
                st.setString(3, city);
                st.setString(4, userId);
            } else {
                st.setString(3, userId);
            }
            st.executeUpdate();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void setUserCoordinatesWithState(String userId, String city, String state, Double latitude, Double longitude, Connection dbConnection) {
        System.out.println("set User Coordinates");
        String query;
        if (userId.equals("StegApp"))
            return;
        if (!city.equals("clear") && !state.equals("clear")) {
            query = "UPDATE users SET " +
                    "latitude = ?, longitude = ?, user_city = ?, user_state = ? WHERE user_id = ?;";
        } else {
            query = "UPDATE users SET " +
                    "latitude = ?, longitude = ? WHERE user_id = ?;";
        }
        try {
            PreparedStatement st = dbConnection.prepareStatement(query);
            st.setDouble(1, latitude);
            st.setDouble(2, longitude);
            if (!city.equals("clear") && !state.equals("clear")) {
                st.setString(3, city);
                st.setString(4, state);
                st.setString(5, userId);
            } else {
                st.setString(3, userId);
            }
            st.executeUpdate();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Boolean addUserPhoto(String userId, String photo, Connection dbConnection) {
        Boolean res = false;
        try {
            PreparedStatement statement = dbConnection.prepareStatement("UPDATE users SET user_photo = ? WHERE user_id = ?");
            statement.setString(1, photo);
            statement.setString(2, userId);
            statement.executeUpdate();
            res = true;
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static Boolean setUserValid(String userId, Connection dbConnection) {
        Boolean res = false;
        try {
            PreparedStatement statement = dbConnection.prepareStatement("UPDATE users SET user_valid = 'true' WHERE user_id = ?");
            statement.setString(1, userId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static Integer createBroadcastSteg(StagData steg, Connection dbConnection) {
        Integer res = null;
        try {
            PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO stegs " +
                    "(sender, reciever, type, life_time, filter, text, voice_path, camera_path, anonym) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING steg_id");
            statement.setString(1, steg.mesSender);
            statement.setString(2, StagData.BROADCAST_STEG);
            statement.setInt(3, steg.stagType);
            statement.setInt(4, steg.lifeTime);
            statement.setInt(5, steg.filter);
            statement.setString(6, steg.mesText);
            statement.setString(7, steg.voiceDataFile);
            statement.setString(8, steg.cameraDataFile);
            statement.setBoolean(9, steg.anonym);

            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                res = rs.getInt(1);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }

    public static Integer addStegV2(StagData steg, Poll poll, Connection dbConnection) {
        Integer res = null;
        try {
            PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO stegs " +
                    "(sender, reciever, type, life_time, filter, text, voice_path, camera_path, anonym, mode) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING steg_id");
            statement.setString(1, steg.mesSender);
            statement.setString(2, steg.mesReciever);
            statement.setInt(3, steg.stagType);
            statement.setInt(4, steg.lifeTime);
            statement.setInt(5, steg.filter);
            statement.setString(6, steg.mesText);
            statement.setString(7, steg.voiceDataFile);
            statement.setString(8, steg.cameraDataFile);
            statement.setBoolean(9, steg.anonym);
            statement.setInt(10, steg.getMode());

            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                res = rs.getInt(1);
            }
            rs.close();
            statement.close();
            if (((steg.getMode() & StagData.POLL_MODE_MASK) != 0) && poll != null) {
                poll.setStegId(res);
                addPoll(poll, dbConnection);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }

    public static Integer addSteg(StagData steg, Connection dbConnection) {
        Integer res = null;
        try {
            PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO stegs " +
                    "(sender, reciever, type, life_time, filter, text, voice_path, camera_path, anonym) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING steg_id");
            statement.setString(1, steg.mesSender);
            statement.setString(2, steg.mesReciever);
            statement.setInt(3, steg.stagType);
            statement.setInt(4, steg.lifeTime);
            statement.setInt(5, steg.filter);
            statement.setString(6, steg.mesText);
            statement.setString(7, steg.voiceDataFile);
            statement.setString(8, steg.cameraDataFile);
            statement.setBoolean(9, steg.anonym);

            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                res = rs.getInt(1);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }

    private static void addPoll(Poll poll, Connection dbConnection) {
        try {
            for (PollItem item : poll.getPollItems()) {
                PreparedStatement st = dbConnection.prepareStatement("INSERT INTO poll_items " +
                        "(steg_id, text) VALUES (?, ?); ");
                st.setInt(1, poll.getStegId());
                st.setString(2, item.getText());
                st.executeUpdate();
                st.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Poll getPoll(Integer stegId, String profileId, Connection dbConnection) {
        Poll poll = new Poll();
        poll.setStegId(stegId);

        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT poll_items.id, poll_items.text, COUNT(votes.profile_id), (SELECT (? IN (SELECT profile_id FROM votes WHERE poll_item_id = poll_items.id))) " +
                    "FROM poll_items LEFT JOIN votes ON (votes.poll_item_id = poll_items.id) " +
                    "WHERE poll_items.steg_id = ? " +
                    "GROUP BY poll_items.id ORDER BY poll_items.id;");
            st.setString(1, profileId);
            st.setInt(2, stegId);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                PollItem item = new PollItem();
                item.setId(rs.getInt(1));
                item.setText(rs.getString(2));
                item.setCount(rs.getInt(3));
                item.setVoted(rs.getBoolean(4));
                poll.addPollItem(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return poll;
    }

    public static void addVote(Integer pollItemId, String profileId, Connection dbConnection) {
        try {
            PreparedStatement st = dbConnection.prepareStatement(
                    "BEGIN; " +
                            "UPDATE votes SET poll_item_id = ? " +
                            "WHERE profile_id = ? " +
                            "AND poll_item_id IN (SELECT poll_items.id FROM poll_items " +
                            "WHERE poll_items.steg_id IN (SELECT steg_id FROM poll_items " +
                            "WHERE id = ?)); " +

                            "INSERT INTO votes (poll_item_id, profile_id) " +
                            "SELECT ?, ? " +
                            "WHERE NOT EXISTS (SELECT poll_item_id, profile_id FROM votes WHERE poll_item_id = ? AND profile_id = ?); " +
                            "COMMIT;");
            st.setInt(1, pollItemId);
            st.setString(2, profileId);
            st.setInt(3, pollItemId);
            st.setInt(4, pollItemId);
            st.setString(5, profileId);
            st.setInt(6, pollItemId);
            st.setString(7, profileId);
            st.executeUpdate();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeVote(Integer stegId, String profileId, Connection dbConnection) {
        try {
            PreparedStatement st = dbConnection.prepareStatement("DELETE FROM votes WHERE profile_id = ? " +
                    "AND poll_item_id IN (SELECT id FROM poll_items WHERE steg_id =?);");
            st.setString(1, profileId);
            st.setInt(2, stegId);
            st.executeUpdate();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static StagData getSteg(int stagId, String userId, Connection dbConnection) {
        StagData stag = null;
        try {
            PreparedStatement statement = dbConnection.prepareStatement(" SELECT stegs.steg_id, stegs.sender," +
                    " stegs.reciever, stegs.type, stegs.life_time," +
                    " stegs.filter, stegs.text, stegs.voice_path," +
                    " stegs.camera_path, stegs.anonym, stegs.date_," +
                    " stegs.time_, stegs.active, stegs.deleted, users.user_name" +
                    " FROM stegs JOIN users ON (stegs.sender = users.user_id)" +
                    " WHERE stegs.steg_id = ?;");
            statement.setInt(1, stagId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
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
                stag.setIsActive(rs.getBoolean(13));
                stag.setIsDeleted(rs.getBoolean(14));
                stag.senderName = rs.getString(15);

                PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
                likeStatement.setInt(1, stag.stegId);
                ResultSet likeRs = likeStatement.executeQuery();
                if (likeRs.next()) {
                    stag.likes = likeRs.getInt(1);
                }

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, stag.stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                if (commentRs.next()) {
                    stag.comments = commentRs.getInt(1);
                }

                PreparedStatement getsStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM gets WHERE steg_id = ?;");
                getsStatement.setInt(1, stag.stegId);
                ResultSet getsRs = getsStatement.executeQuery();
                if (getsRs.next()) {
                    stag.gets = getsRs.getInt(1);
                }
                getsStatement.close();
                getsRs.close();

                PreparedStatement savesStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM wall WHERE steg_id = ?;");
                savesStatement.setInt(1, stag.stegId);
                ResultSet savesRs = savesStatement.executeQuery();
                if (savesRs.next()) {
                    stag.saves = savesRs.getInt(1);
                }
                savesStatement.close();
                savesRs.close();

                PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
                likedStatement.setInt(1, stag.stegId);
                likedStatement.setString(2, userId);
                ResultSet likedRs = likedStatement.executeQuery();
                stag.liked = false;
                if (likedRs.next()) {
                    stag.liked = true;
                }
                likedRs.close();
                likedStatement.close();

                PreparedStatement favStat = dbConnection.prepareStatement("SELECT id FROM favorites WHERE type = ? AND fav_id = ? AND profile_id = ?;");
                favStat.setString(1, FavoriteItem.TYPE_STEG);
                favStat.setInt(2, stagId);
                favStat.setString(3, userId);
                ResultSet favRs = favStat.executeQuery();
                if (favRs.next()) {
                    stag.setIsFavorite(true);
                }
                favRs.close();
                favStat.close();
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stag;
    }

    public static StagData getStegV2(int stegId, String userId, Connection dbConnection) {
        StagData stag = null;
        try {
            PreparedStatement statement = dbConnection.prepareStatement(" SELECT stegs.steg_id, stegs.sender," +
                    " stegs.reciever, stegs.type, stegs.life_time," +
                    " stegs.filter, stegs.text, stegs.voice_path," +
                    " stegs.camera_path, stegs.anonym, stegs.date_," +
                    " stegs.time_, stegs.active, stegs.deleted, stegs.recieved, users.user_name, stegs.mode" +
                    " FROM stegs JOIN users ON (stegs.sender = users.user_id)" +
                    " WHERE stegs.steg_id = ?;");
            statement.setInt(1, stegId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
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
                stag.setIsActive(rs.getBoolean(13));
                stag.setIsDeleted(rs.getBoolean(14));
                Integer recieved = rs.getInt(15);
                stag.senderName = rs.getString(16);
                stag.setMode(rs.getInt(17));

                stag.setIsActive(stag.isActive() && recieved < 4);


                PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
                likeStatement.setInt(1, stag.stegId);
                ResultSet likeRs = likeStatement.executeQuery();
                if (likeRs.next()) {
                    stag.likes = likeRs.getInt(1);
                }

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, stag.stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                if (commentRs.next()) {
                    stag.comments = commentRs.getInt(1);
                }

                PreparedStatement getsStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM gets WHERE steg_id = ?;");
                getsStatement.setInt(1, stag.stegId);
                ResultSet getsRs = getsStatement.executeQuery();
                if (getsRs.next()) {
                    stag.gets = getsRs.getInt(1);
                }
                getsStatement.close();
                getsRs.close();

                PreparedStatement savesStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM wall WHERE steg_id = ?;");
                savesStatement.setInt(1, stag.stegId);
                ResultSet savesRs = savesStatement.executeQuery();
                if (savesRs.next()) {
                    stag.saves = savesRs.getInt(1);
                }
                savesStatement.close();
                savesRs.close();

                PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
                likedStatement.setInt(1, stag.stegId);
                likedStatement.setString(2, userId);
                ResultSet likedRs = likedStatement.executeQuery();
                stag.liked = false;
                if (likedRs.next()) {
                    stag.liked = true;
                }
                likedRs.close();
                likedStatement.close();

                PreparedStatement favStat = dbConnection.prepareStatement("SELECT id FROM favorites WHERE type = ? AND fav_id = ? AND profile_id = ?;");
                favStat.setString(1, FavoriteItem.TYPE_STEG);
                favStat.setInt(2, stegId);
                favStat.setString(3, userId);
                ResultSet favRs = favStat.executeQuery();
                if (favRs.next()) {
                    stag.setIsFavorite(true);
                }
                favRs.close();
                favStat.close();
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stag;
    }

    static ArrayList<StagData> getIncomePrivateStegs(String profileId, Connection dbConnection) {
        ArrayList<StagData> stegList = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT steg_id, sender, reciever, type, life_time,"
                    + " filter, text, voice_path, camera_path,"
                    + " anonym, date_, time_, sended"
                    + " FROM stegs"
                    + " WHERE reciever = ? AND life_time = 0 AND reciever != sender ORDER BY steg_id DESC;");
            statement.setString(1, profileId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
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
                while (likeRs.next()) {
                    steg.likes = likeRs.getInt(1);
                }

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, steg.stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                while (commentRs.next()) {
                    steg.comments = commentRs.getInt(1);
                }
                stegList.add(steg);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stegList;
    }

    public static ArrayList<StegItem> getProfileSentItems(String requestor, String profileId, Connection dbConnection) {
        ArrayList<StegItem> stegItems = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT steg_id, sender"
                    + " FROM stegs"
                    + " WHERE sender = ? AND reciever = 'common' AND anonym = false ORDER BY steg_id DESC;");
            statement.setString(1, profileId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Integer stegId = rs.getInt(1);
                String mesSender = rs.getString(2);


                StegItem stegItem = new StegItem(stegId, mesSender, false);

                PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
                likeStatement.setInt(1, stegId);
                ResultSet likeRs = likeStatement.executeQuery();
                while (likeRs.next()) {
                    stegItem.setLikes(likeRs.getInt(1));
                }
                likeRs.close();
                likeStatement.close();

                PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
                likedStatement.setInt(1, stegId);
                likedStatement.setString(2, requestor);
                ResultSet likedRs = likedStatement.executeQuery();
                while (likedRs.next()) {
                    stegItem.setLiked(true);
                }
                likedRs.close();
                likedStatement.close();

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                while (commentRs.next()) {
                    stegItem.setComments(commentRs.getInt(1));
                }
                commentRs.close();
                commentStatement.close();
                stegItems.add(stegItem);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stegItems;
    }

    public static ArrayList<StegItem> getIncomeCommonItems(String profileId, Connection dbConnection) {
        ArrayList<StegItem> stegItems = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT stegs.steg_id, stegs.sender, stegs.anonym"
                    + " FROM stegs JOIN gets ON stegs.steg_id = gets.steg_id"
                    + " WHERE (stegs.reciever = ? OR stegs.reciever = ?) AND gets.profile_id = ? ORDER BY gets.id DESC;");
            statement.setString(1, StagData.COMMON_STEG);
            statement.setString(2, StagData.LOCATION_STEG);
            statement.setString(3, profileId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Integer stegId = rs.getInt(1);
                String mesSender = rs.getString(2);
                Boolean anonym = rs.getBoolean(3);

                StegItem stegItem = new StegItem(stegId, mesSender, anonym);

                PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
                likeStatement.setInt(1, stegId);
                ResultSet likeRs = likeStatement.executeQuery();
                while (likeRs.next()) {
                    stegItem.setLikes(likeRs.getInt(1));
                }
                likeRs.close();
                likeStatement.close();

                PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
                likedStatement.setInt(1, stegId);
                likedStatement.setString(2, profileId);
                ResultSet likedRs = likedStatement.executeQuery();
                while (likedRs.next()) {
                    stegItem.setLiked(true);
                }
                likedRs.close();
                likedStatement.close();

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                while (commentRs.next()) {
                    stegItem.setComments(commentRs.getInt(1));
                }
                commentRs.close();
                commentStatement.close();
                stegItems.add(stegItem);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stegItems;
    }

    public static ArrayList<StegItem> getIncomeCommonItemsV2(String profileId, Connection dbConnection) {
        ArrayList<StegItem> stegItems = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT stegs.steg_id, stegs.sender, stegs.anonym"
                    + " FROM stegs JOIN gets ON stegs.steg_id = gets.steg_id"
                    + " WHERE (stegs.reciever = ? OR stegs.reciever = ?) AND gets.profile_id = ? ORDER BY gets.id DESC;");
            statement.setString(1, StagData.COMMON_STEG);
            statement.setString(2, StagData.LOCATION_STEG);
            statement.setString(3, profileId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Integer stegId = rs.getInt(1);
                String mesSender = rs.getString(2);
                Boolean anonym = rs.getBoolean(3);

                StegItem stegItem = new StegItem(stegId, mesSender, anonym);

                PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
                likeStatement.setInt(1, stegId);
                ResultSet likeRs = likeStatement.executeQuery();
                while (likeRs.next()) {
                    stegItem.setLikes(likeRs.getInt(1));
                }
                likeRs.close();
                likeStatement.close();

                PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
                likedStatement.setInt(1, stegId);
                likedStatement.setString(2, profileId);
                ResultSet likedRs = likedStatement.executeQuery();
                while (likedRs.next()) {
                    stegItem.setLiked(true);
                }
                likedRs.close();
                likedStatement.close();

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                while (commentRs.next()) {
                    stegItem.setComments(commentRs.getInt(1));
                }
                commentRs.close();
                commentStatement.close();

                ArrayList<LikeData> receivers = DBHandler.getReceivers(stegItem.getStegId(), dbConnection);
                stegItem.setRecieverCount(receivers.size());
                for (LikeData receiversItem : receivers) {
                    stegItem.recieverIds.put(receiversItem.profileId, null);
                }

                stegItems.add(stegItem);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stegItems;
    }

    public static ArrayList<StegItem> getProfileCorrespondenceItems(String profileId, String userId, Connection dbConnection) {
        ArrayList<StegItem> stegItems = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT steg_id, sender, anonym, sended " +
                    "FROM stegs " +
                    "WHERE ((sender = ?) OR (sender = ?)) " +
                    "AND ((reciever = ?) OR (reciever = ?)) " +
                    "AND deleted = false " +
                    "AND ((sender != ?) OR (anonym = false));");
            statement.setString(1, userId);
            statement.setString(2, profileId);
            statement.setString(3, userId);
            statement.setString(4, profileId);
            statement.setString(5, profileId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Integer stegId = rs.getInt(1);
                String mesSender = rs.getString(2);
                Boolean anonym = rs.getBoolean(3);
                Boolean isSended = rs.getBoolean(4);

                StegItem stegItem = new StegItem(stegId, mesSender, anonym);
                stegItem.setIsSended(isSended);

                PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
                likeStatement.setInt(1, stegId);
                ResultSet likeRs = likeStatement.executeQuery();
                while (likeRs.next()) {
                    stegItem.setLikes(likeRs.getInt(1));
                }
                likeRs.close();
                likeStatement.close();

                PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
                likedStatement.setInt(1, stegId);
                likedStatement.setString(2, profileId);
                ResultSet likedRs = likedStatement.executeQuery();
                while (likedRs.next()) {
                    stegItem.setLiked(true);
                }
                likedRs.close();
                likedStatement.close();

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                while (commentRs.next()) {
                    stegItem.setComments(commentRs.getInt(1));
                }
                commentRs.close();
                commentStatement.close();
                stegItems.add(stegItem);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stegItems;
    }

    public static ArrayList<StegItem> getIncomePrivateItems(String profileId, Connection dbConnection) {
        ArrayList<StegItem> stegItems = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT steg_id, sender, anonym, sended"
                    + " FROM stegs"
                    + " WHERE deleted != TRUE AND (reciever = ? OR reciever = ?) AND reciever != sender ORDER BY steg_id DESC;");
            statement.setString(1, profileId);
            statement.setString(2, StagData.BROADCAST_STEG);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Integer stegId = rs.getInt(1);
                String mesSender = rs.getString(2);
                Boolean anonym = rs.getBoolean(3);
                Boolean isSended = rs.getBoolean(4);

                StegItem stegItem = new StegItem(stegId, mesSender, anonym);
                stegItem.setIsSended(isSended);

                PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
                likeStatement.setInt(1, stegId);
                ResultSet likeRs = likeStatement.executeQuery();
                while (likeRs.next()) {
                    stegItem.setLikes(likeRs.getInt(1));
                }
                likeRs.close();
                likeStatement.close();

                PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
                likedStatement.setInt(1, stegId);
                likedStatement.setString(2, profileId);
                ResultSet likedRs = likedStatement.executeQuery();
                while (likedRs.next()) {
                    stegItem.setLiked(true);
                }
                likedRs.close();
                likedStatement.close();

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                while (commentRs.next()) {
                    stegItem.setComments(commentRs.getInt(1));
                }
                commentRs.close();
                commentStatement.close();
                stegItems.add(stegItem);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stegItems;
    }

    static ArrayList<StagData> getOutcomePrivateStegs(String profileId, Connection dbConnection) {
        ArrayList<StagData> stegList = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT stegs.steg_id, stegs.sender, stegs.reciever, stegs.type, stegs.life_time,"
                    + " stegs.filter, stegs.text, stegs.voice_path, stegs.camera_path,"
                    + " stegs.anonym, stegs.date_, stegs.time_"
                    + " FROM stegs"
                    + " WHERE sender = ? AND reciever != ? AND reciever != sender ORDER BY steg_id DESC;");
            statement.setString(1, profileId);
            statement.setString(2, "common");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
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
                while (likeRs.next()) {
                    steg.likes = likeRs.getInt(1);
                }
                likeRs.close();
                likeStatement.close();

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, steg.stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                while (commentRs.next()) {
                    steg.comments = commentRs.getInt(1);
                }
                commentRs.close();
                commentRs.close();

                PreparedStatement getsStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM gets WHERE steg_id = ?;");
                getsStatement.setInt(1, steg.stegId);
                ResultSet getsRs = getsStatement.executeQuery();
                while (getsRs.next()) {
                    steg.gets = getsRs.getInt(1);
                }
                getsStatement.close();
                getsRs.close();

                PreparedStatement savesStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM wall WHERE steg_id = ?;");
                savesStatement.setInt(1, steg.stegId);
                ResultSet savesRs = savesStatement.executeQuery();
                while (savesRs.next()) {
                    steg.saves = savesRs.getInt(1);
                }
                savesStatement.close();
                savesRs.close();

                stegList.add(steg);
            }
            rs.close();
            statement.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stegList;
    }

    public static ArrayList<StegItem> getOutcomePrivateItems(String profileId, Connection dbConnection) {
        ArrayList<StegItem> stegItems = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT stegs.steg_id, stegs.sender, stegs.anonym, stegs.sended"
                    + " FROM stegs"
                    + " WHERE sender = ? AND reciever != ? AND reciever != ? AND reciever != sender ORDER BY steg_id DESC;");
            statement.setString(1, profileId);
            statement.setString(2, "common");
            statement.setString(3, "location");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Integer stegId = rs.getInt(1);
                String mesSender = rs.getString(2);
                Boolean anonym = rs.getBoolean(3);
                Boolean isSended = rs.getBoolean(4);

                StegItem stegItem = new StegItem(stegId, mesSender, anonym);
                stegItem.setIsSended(isSended);

                PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
                likeStatement.setInt(1, stegId);
                ResultSet likeRs = likeStatement.executeQuery();
                while (likeRs.next()) {
                    stegItem.setLikes(likeRs.getInt(1));
                }
                likeRs.close();
                likeStatement.close();

                PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
                likedStatement.setInt(1, stegId);
                likedStatement.setString(2, profileId);
                ResultSet likedRs = likedStatement.executeQuery();
                while (likedRs.next()) {
                    stegItem.setLiked(true);
                }
                likedRs.close();
                likedStatement.close();

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                while (commentRs.next()) {
                    stegItem.setComments(commentRs.getInt(1));
                }
                commentRs.close();
                commentStatement.close();

                stegItems.add(stegItem);
            }
            rs.close();
            statement.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stegItems;
    }

    static ArrayList<StagData> getOutcomeCommonStegs(String profileId, Connection dbConnection) {
        ArrayList<StagData> stegList = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT steg_id, sender, reciever, type, life_time,"
                    + " filter, text, voice_path, camera_path,"
                    + " anonym, date_, time_"
                    + " FROM stegs"
                    + " WHERE sender = ? AND reciever = ? AND life_time = 0 ORDER BY steg_id DESC;");
            statement.setString(1, profileId);
            statement.setString(2, "common");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
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
                while (likeRs.next()) {
                    steg.likes = likeRs.getInt(1);
                }
                likeRs.close();
                likeStatement.close();

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, steg.stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                while (commentRs.next()) {
                    steg.comments = commentRs.getInt(1);
                }
                commentRs.close();
                commentStatement.close();

                PreparedStatement getsStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM gets WHERE steg_id = ?;");
                getsStatement.setInt(1, steg.stegId);
                ResultSet getsRs = getsStatement.executeQuery();
                while (getsRs.next()) {
                    steg.gets = getsRs.getInt(1);
                }
                getsStatement.close();
                getsRs.close();

                PreparedStatement savesStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM wall WHERE steg_id = ?;");
                savesStatement.setInt(1, steg.stegId);
                ResultSet savesRs = savesStatement.executeQuery();
                while (savesRs.next()) {
                    steg.saves = savesRs.getInt(1);
                }
                savesStatement.close();
                savesRs.close();

                stegList.add(steg);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stegList;
    }

    public static ArrayList<StegItem> getOutcomeCommonItems(String profileId, Connection dbConnection) {
        ArrayList<StegItem> stegItems = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT steg_id, sender, anonym"
                    + " FROM stegs"
                    + " WHERE sender = ? AND (reciever = ? OR reciever = ?) AND life_time = 0 ORDER BY steg_id DESC;");
            statement.setString(1, profileId);
            statement.setString(2, "common");
            statement.setString(3, "location");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Integer stegId = rs.getInt(1);
                String mesSender = rs.getString(2);
                Boolean anonym = rs.getBoolean(3);

                StegItem stegItem = new StegItem(stegId, mesSender, anonym);

                PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
                likeStatement.setInt(1, stegId);
                ResultSet likeRs = likeStatement.executeQuery();
                while (likeRs.next()) {
                    stegItem.setLikes(likeRs.getInt(1));
                }
                likeRs.close();
                likeStatement.close();

                PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
                likedStatement.setInt(1, stegId);
                likedStatement.setString(2, profileId);
                ResultSet likedRs = likedStatement.executeQuery();
                while (likedRs.next()) {
                    stegItem.setLiked(true);
                }
                likedRs.close();
                likedStatement.close();

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                while (commentRs.next()) {
                    stegItem.setComments(commentRs.getInt(1));
                }
                commentRs.close();
                commentStatement.close();

                ArrayList<LikeData> receivers = DBHandler.getReceivers(stegItem.getStegId(), dbConnection);
                stegItem.setRecieverCount(receivers.size());
                for (LikeData receiversItem : receivers) {
                    stegItem.recieverIds.put(receiversItem.profileId, null);
                }
                stegItems.add(stegItem);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stegItems;
    }

    public static Integer getUnrecievedSteg(Connection dbConnection) {
        ArrayList<Integer> stagId = new ArrayList<Integer>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT steg_id, sender FROM stegs WHERE recieved = 0 AND reciever = ?");
            statement.setString(1, "common");
            ResultSet rs = statement.executeQuery();
            Random rand = new Random(System.currentTimeMillis());
            while (rs.next()) {
                stagId.add(rs.getInt("steg_id"));
            }
            if (stagId.size() > 0) {
                Integer r = rand.nextInt(stagId.size());
                rs.close();
                statement.close();
                return stagId.get(r);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static String getStegSenderCity(Integer stegId, Connection dbConnection) {
        try {
            PreparedStatement st = dbConnection.prepareStatement(SQL_GET_STEG_SENDER_CITY);
            st.setInt(1, stegId);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                String res = rs.getString(1);
                rs.close();
                st.close();
                return res;
            }
            rs.close();
            st.close();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static ArrayList<Integer> getUnrecievedStegIds(Connection dbConnection) {
        ArrayList<Integer> stegList = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement(SQL_GET_UNRECEIVED_STEGS_ID);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Integer stegId = rs.getInt(1);
                Integer recieved = rs.getInt(2);
                for (int i = recieved; i < MAX_RECEIVED_NO_FIELD; i++) {
                    stegList.add(stegId);
                }
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stegList;
    }

    static ArrayList<StagData> getUnreceivedStegs(Connection dbConnection) {
        ArrayList<StagData> stegList = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement(SQL_GET_UNRECEIVED_STEGS);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                StagData steg = new StagData();
                steg.setStegId(rs.getInt(1));
                steg.setFilter(rs.getInt(2));
                steg.setMesSender(rs.getString(3));
                steg.setMode(rs.getInt(4));
                stegList.add(steg);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stegList;
    }

    static Boolean checkReceiver(StagData steg, String profileId, Connection dbConnection) {
        PreparedStatement statement;
        try {
            statement = dbConnection.prepareStatement(SQL_CHECK_RECEIVERS);
            statement.setString(1, profileId);
            statement.setInt(2, steg.filter);
            statement.setInt(3, steg.stegId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
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

    static void setStegUnrecieved(Integer stegId, Connection dbConnection) {
        PreparedStatement statement;
        try {
            statement = dbConnection.prepareStatement("UPDATE stegs SET recieved = 0 WHERE steg_id = ?");
            statement.setInt(1, stegId);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    static void setStegActive(Integer stegId, Boolean value, Connection dbConnection) {
        PreparedStatement statement;
        try {
            statement = dbConnection.prepareStatement("UPDATE stegs SET active = ? WHERE steg_id = ?;");
            statement.setBoolean(1, value);
            statement.setInt(2, stegId);
            statement.executeUpdate();
            statement.close();
            if (!value) {
                StegSender.getInstance().removeAllrecords(stegId);
            } else {
                Integer recieved = DBHandler.getRecievedCount(stegId, dbConnection);
                if (recieved < 4)
                    StegSender.getInstance().addStegToQueueLast(stegId, 4 - recieved);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static Integer getRecievedCount(Integer stegId, Connection dbConnection) {
        Integer result = 4;
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT recieved FROM stegs WHERE steg_id = ?;");
            st.setInt(1, stegId);
            ResultSet rs = st.executeQuery();
            if (rs.next())
                result = rs.getInt(1);
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    static void deleteIncomeSteg(Integer stegId, String profileId, Connection dbConnection) {
        PreparedStatement statement;
        String sqlQuery;
        try {
            sqlQuery = " BEGIN; " +
                    "DELETE FROM news WHERE owner_id = ? AND steg_id = ?; " +
                    "UPDATE stegs SET deleted = TRUE WHERE steg_id = ? AND reciever = ?; " +
                    "COMMIT;";
            statement = dbConnection.prepareStatement(sqlQuery);
            statement.setString(1, profileId);
            statement.setInt(2, stegId);
            statement.setInt(3, stegId);
            statement.setString(4, profileId);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static Map<String, String> getReceiverForSteg(Integer stegId, Connection dbConnection) {
        Map<String, String> resultMap = new HashMap<>();
        try {
            PreparedStatement st = dbConnection.prepareStatement(SQL_GET_RECEIVER_FOR_STEG);
            st.setInt(1, stegId);
            st.setInt(2, stegId);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                resultMap.put(FcmConsts.TOKEN, rs.getString(1));
                resultMap.put(FcmConsts.STEG_SENDER_CITY, rs.getString(2));
                resultMap.put(FcmConsts.STEG_SENDER_ID, rs.getString(3));
                resultMap.put(FcmConsts.STEG_MODE, Integer.toString(rs.getInt(4)));
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultMap;
    }

    static void deleteSteg(Integer stegId, String profileId, Connection dbConnection) {
        try {
            Boolean check = false;
            PreparedStatement checkStatement = dbConnection.prepareStatement("SELECT steg_id FROM stegs WHERE steg_id = ? AND sender = ?;");
            checkStatement.setInt(1, stegId);
            checkStatement.setString(2, profileId);
            ResultSet rs = checkStatement.executeQuery();
            while (rs.next()) {
                check = true;
            }
            if (check) {
                StegSender.getInstance().removeAllrecords(stegId);

                PreparedStatement FileSt = dbConnection.prepareStatement("SELECT voice_path, camera_path FROM stegs WHERE steg_id = ?;");
                FileSt.setInt(1, stegId);
                ResultSet fileRs = FileSt.executeQuery();
                while (fileRs.next()) {
                    String file = fileRs.getString(1);
                    if (file != null && !file.equals("clear"))
                        StegFileManager.deleteFile(file, true);
                    file = fileRs.getString(2);
                    if (file != null && !file.equals("clear"))
                        StegFileManager.deleteFile(file, false);
                }
                fileRs.close();
                FileSt.close();

                PreparedStatement statement = dbConnection.prepareStatement(
                        "BEGIN;"
                                + " DELETE FROM favorites WHERE type = ? AND fav_id = ?;"
                                + " DELETE FROM news WHERE steg_id = ?;"
                                + " DELETE FROM stegs WHERE steg_id = ?;"
                                + " COMMIT;");
                statement.setString(1, FavoriteItem.TYPE_STEG);
                statement.setInt(2, stegId);
                statement.setInt(3, stegId);
                statement.setInt(4, stegId);
                statement.executeUpdate();
                statement.close();
            }
            rs.close();
            checkStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    static void deleteStegAdm(Integer stegId, Connection dbConnection) {
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT voice_path, camera_path FROM stegs WHERE steg_id = ?;");
            st.setInt(1, stegId);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                String file = rs.getString(1);
                if (file != null && !file.equals("clear"))
                    StegFileManager.deleteFile(file, true);
                file = rs.getString(2);
                if (file != null && !file.equals("clear"))
                    StegFileManager.deleteFile(file, false);
            }
            rs.close();
            st.close();

            PreparedStatement statement = dbConnection.prepareStatement(
                    "BEGIN;"
                            + " DELETE FROM favorites WHERE type = ? AND fav_id = ?;"
                            + " DELETE FROM news WHERE steg_id = ?;"
                            + " DELETE FROM stegs WHERE steg_id = ?;"
                            + " COMMIT;");
            statement.setString(1, FavoriteItem.TYPE_STEG);
            statement.setInt(2, stegId);
            statement.setInt(3, stegId);
            statement.setInt(4, stegId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    static void deleteComment(Integer commentId, Connection dbConnection) {
        PreparedStatement statement;
        String sqlQuery;
        try {
            sqlQuery = "DELETE FROM comments WHERE id = ?;";
            statement = dbConnection.prepareStatement(sqlQuery);
            statement.setInt(1, commentId);
            statement.executeUpdate();
            statement.close();

            String profileId = DBHandler.getCommentOwner(commentId, dbConnection);
            Integer stegId = DBHandler.getCommentedSteg(commentId, dbConnection);
            String stegOwner = DBHandler.getStegOwner(stegId, dbConnection);
            if (!profileId.equals(stegOwner) && DBHandler.getCommentsCountForSteg(profileId, stegId, dbConnection) < 1) {
                DBHandler.decAccount(stegOwner, BONUS_FOR_COMMENT, dbConnection);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void decStegReceived(Integer stegId, Connection dbConnection) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("UPDATE stegs SET recieved = recieved - 1 WHERE steg_id = ? AND date_ + time_ > date_trunc('Day', now()) - interval '1 day';");
            statement.setInt(1, stegId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void incStegReceived(Integer stegId, Connection dbConnection) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("UPDATE stegs SET recieved = recieved + 1 WHERE steg_id = ?;");
            statement.setInt(1, stegId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void markPrivetStegSended(Integer stegId, Connection dbConnection) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("UPDATE stegs SET sended = true WHERE steg_id = ?;");
            statement.setInt(1, stegId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String getStegOwner(Integer stegId, Connection dbConnection) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT sender FROM stegs WHERE steg_id = " + stegId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getString("sender");
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static String getLocationStegSender(Integer stegId, Connection dbConnection) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT sender, anonym FROM stegs WHERE steg_id = " + stegId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                String sender = rs.getString(1);
                Boolean anonym = rs.getBoolean(2);
                if (anonym)
                    return UserProfile.ANONYM;
                return sender;
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void stegToWall(Integer stegId, String userId, Connection dbConnection) {
        try {
            Boolean check = true;
            PreparedStatement checkStatement = dbConnection.prepareStatement("SELECT * FROM wall WHERE steg_id = ? AND owner = ?;");
            checkStatement.setInt(1, stegId);
            checkStatement.setString(2, userId);
            ResultSet checkRs = checkStatement.executeQuery();
            if (checkRs.next()) {
                check = false;
            }
            checkRs.close();
            checkStatement.close();
            if (check) {
                PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO wall(steg_id, owner) VALUES (?, ?)");
                statement.setInt(1, stegId);
                statement.setString(2, userId);
                statement.executeUpdate();
                statement.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static StegRequestItem stegRequest(String reqProfileId, Connection dbConnection) {
        StegRequestItem srItem = null;
        try {
            PreparedStatement statement = dbConnection.prepareStatement(SQL_STEG_REQUEST_V2);
            statement.setString(1, reqProfileId);
            statement.setString(2, reqProfileId);
            statement.setString(3, reqProfileId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                srItem = new StegRequestItem(rs.getInt(1), rs.getString(2), rs.getString(3));
                srItem.setStegMode(rs.getInt(4));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return srItem;
    }

    static StegRequestItem stegRequestV2(String reqProfileId, Connection dbConnection) {
        StegRequestItem srItem = null;
        try {
            PreparedStatement statement = dbConnection.prepareStatement(SQL_STEG_REQUEST_V2);
            statement.setString(1, reqProfileId);
            statement.setString(2, reqProfileId);
            statement.setString(3, reqProfileId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                srItem = new StegRequestItem(rs.getInt(1), rs.getString(2), rs.getString(3));
                srItem.setStegMode(rs.getInt(4));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (srItem != null) {
            decAccount(reqProfileId, 1f, dbConnection);
        }
        return srItem;
    }

    static void removeStegFromWall(Integer stegId, String ownerId, Connection dbConnection) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("DELETE FROM wall WHERE steg_id = ? AND owner = ?");
            statement.setInt(1, stegId);
            statement.setString(2, ownerId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static ArrayList<StagData> getWall(String userId, Connection dbConnection) {
        ArrayList<StagData> stegList = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT wall.steg_id, stegs.sender, stegs.reciever, stegs.type, stegs.life_time,"
                    + " stegs.filter, stegs.text, stegs.voice_path, stegs.camera_path,"
                    + " stegs.anonym, stegs.date_, stegs.time_"
                    + " FROM wall JOIN stegs ON wall.steg_id = stegs.steg_id"
                    + " WHERE wall.owner = ? ORDER BY wall.id DESC;");
            statement.setString(1, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
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
                while (likeRs.next()) {
                    steg.likes = likeRs.getInt(1);
                }
                likeRs.close();
                likeStatement.close();

                PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
                likedStatement.setInt(1, steg.stegId);
                likedStatement.setString(2, userId);
                ResultSet likedRs = likedStatement.executeQuery();
                while (likedRs.next()) {
                    steg.liked = true;
                }
                likedRs.close();
                likedStatement.close();

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, steg.stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                while (commentRs.next()) {
                    steg.comments = commentRs.getInt(1);
                }
                commentRs.close();
                commentStatement.close();

                PreparedStatement nameStatement = dbConnection.prepareStatement("SELECT user_name FROM users WHERE user_id = ?");
                nameStatement.setString(1, steg.mesSender);
                ResultSet nameRs = nameStatement.executeQuery();
                steg.senderName = steg.mesSender;
                while (nameRs.next()) {
                    steg.senderName = nameRs.getString(1);
                }
                nameRs.close();
                nameStatement.close();
                stegList.add(steg);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stegList;
    }

    public static ArrayList<StegItem> getWallItemsForProfile(String profileId, String myProfileId, Connection dbConnection) {
        ArrayList<StegItem> stegItems = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT wall.steg_id, stegs.sender, stegs.anonym"
                    + " FROM wall JOIN stegs ON wall.steg_id = stegs.steg_id"
                    + " WHERE wall.owner = ?"
                    + "AND ((stegs.filter & " + Integer.toString(StagData.STEG_SEX_MASK) + " = 0) OR "
                    + "(stegs.filter & get_sex_filter_mask_by_user_id(?) != 0)) "
                    + " ORDER BY wall.id DESC;");
            statement.setString(1, profileId);
            statement.setString(2, myProfileId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Integer stegId = rs.getInt(1);
                String mesSender = rs.getString(2);
                Boolean anonym = rs.getBoolean(3);
                StegItem stegItem = new StegItem(stegId, mesSender, anonym);

                PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
                likeStatement.setInt(1, stegId);
                ResultSet likeRs = likeStatement.executeQuery();
                while (likeRs.next()) {
                    stegItem.setLikes(likeRs.getInt(1));
                }
                likeRs.close();
                likeStatement.close();

                PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
                likedStatement.setInt(1, stegId);
                likedStatement.setString(2, myProfileId);
                ResultSet likedRs = likedStatement.executeQuery();
                while (likedRs.next()) {
                    stegItem.setLiked(true);
                }
                likedRs.close();
                likedStatement.close();

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                while (commentRs.next()) {
                    stegItem.setComments(commentRs.getInt(1));
                }
                commentRs.close();
                commentStatement.close();

                stegItems.add(stegItem);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stegItems;
    }

    public static ArrayList<StegItem> getWallItems(String userId, Connection dbConnection) {
        ArrayList<StegItem> stegItems = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT wall.steg_id, stegs.sender, stegs.anonym"
                    + " FROM wall JOIN stegs ON wall.steg_id = stegs.steg_id"
                    + " WHERE wall.owner = ? ORDER BY wall.id DESC;");
            statement.setString(1, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Integer stegId = rs.getInt(1);
                String mesSender = rs.getString(2);
                Boolean anonym = rs.getBoolean(3);
                StegItem stegItem = new StegItem(stegId, mesSender, anonym);

                PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
                likeStatement.setInt(1, stegId);
                ResultSet likeRs = likeStatement.executeQuery();
                while (likeRs.next()) {
                    stegItem.setLikes(likeRs.getInt(1));
                }
                likeRs.close();
                likeStatement.close();

                PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
                likedStatement.setInt(1, stegId);
                likedStatement.setString(2, userId);
                ResultSet likedRs = likedStatement.executeQuery();
                while (likedRs.next()) {
                    stegItem.setLiked(true);
                }
                likedRs.close();
                likedStatement.close();

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                while (commentRs.next()) {
                    stegItem.setComments(commentRs.getInt(1));
                }
                commentRs.close();
                commentStatement.close();

                stegItems.add(stegItem);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stegItems;
    }

    public static ArrayList<StegItem> getWallItemsForProfileV2(String profileId, String myProfileId, Connection dbConnection) {
        ArrayList<StegItem> stegItems = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT wall.steg_id, stegs.sender, stegs.anonym"
                    + " FROM wall JOIN stegs ON wall.steg_id = stegs.steg_id"
                    + " WHERE wall.owner = ?"
                    + "AND ((stegs.filter & " + Integer.toString(StagData.STEG_SEX_MASK) + " = 0) OR "
                    + "(stegs.filter & get_sex_filter_mask_by_user_id(?) != 0)) "
                    + " ORDER BY wall.id DESC;");
            statement.setString(1, profileId);
            statement.setString(2, myProfileId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Integer stegId = rs.getInt(1);
                String mesSender = rs.getString(2);
                Boolean anonym = rs.getBoolean(3);
                StegItem stegItem = new StegItem(stegId, mesSender, anonym);

                PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
                likeStatement.setInt(1, stegId);
                ResultSet likeRs = likeStatement.executeQuery();
                while (likeRs.next()) {
                    stegItem.setLikes(likeRs.getInt(1));
                }
                likeRs.close();
                likeStatement.close();

                PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
                likedStatement.setInt(1, stegId);
                likedStatement.setString(2, myProfileId);
                ResultSet likedRs = likedStatement.executeQuery();
                while (likedRs.next()) {
                    stegItem.setLiked(true);
                }
                likedRs.close();
                likedStatement.close();

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                while (commentRs.next()) {
                    stegItem.setComments(commentRs.getInt(1));
                }
                commentRs.close();
                commentStatement.close();

                ArrayList<LikeData> receivers = DBHandler.getReceivers(stegItem.getStegId(), dbConnection);
                stegItem.setRecieverCount(receivers.size());
                for (LikeData receiversItem : receivers) {
                    stegItem.recieverIds.put(receiversItem.profileId, null);
                }

                stegItems.add(stegItem);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stegItems;
    }

    public static ArrayList<StegItem> getWallItemsV2(String userId, Connection dbConnection) {
        ArrayList<StegItem> stegItems = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT wall.steg_id, stegs.sender, stegs.anonym"
                    + " FROM wall JOIN stegs ON wall.steg_id = stegs.steg_id"
                    + " WHERE wall.owner = ? ORDER BY wall.id DESC;");
            statement.setString(1, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Integer stegId = rs.getInt(1);
                String mesSender = rs.getString(2);
                Boolean anonym = rs.getBoolean(3);
                StegItem stegItem = new StegItem(stegId, mesSender, anonym);

                PreparedStatement likeStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM likes WHERE steg_id = ?;");
                likeStatement.setInt(1, stegId);
                ResultSet likeRs = likeStatement.executeQuery();
                while (likeRs.next()) {
                    stegItem.setLikes(likeRs.getInt(1));
                }
                likeRs.close();
                likeStatement.close();

                PreparedStatement likedStatement = dbConnection.prepareStatement("SELECT steg_id FROM likes WHERE steg_id = ? AND profile_id = ?;");
                likedStatement.setInt(1, stegId);
                likedStatement.setString(2, userId);
                ResultSet likedRs = likedStatement.executeQuery();
                while (likedRs.next()) {
                    stegItem.setLiked(true);
                }
                likedRs.close();
                likedStatement.close();

                PreparedStatement commentStatement = dbConnection.prepareStatement("SELECT COUNT(steg_id) FROM comments WHERE steg_id = ?;");
                commentStatement.setInt(1, stegId);
                ResultSet commentRs = commentStatement.executeQuery();
                while (commentRs.next()) {
                    stegItem.setComments(commentRs.getInt(1));
                }
                commentRs.close();
                commentStatement.close();

                ArrayList<LikeData> receivers = DBHandler.getReceivers(stegItem.getStegId(), dbConnection);
                stegItem.setRecieverCount(receivers.size());
                for (LikeData receiversItem : receivers) {
                    stegItem.recieverIds.put(receiversItem.profileId, null);
                }

                stegItems.add(stegItem);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stegItems;
    }

    static Integer addComment(Integer stegId, String profileId, String text, Connection dbConnection) {
        Integer res = null;
        try {
            PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO comments " +
                    "(steg_id, profile_id, text) " +
                    "VALUES (?, ?, ?) RETURNING comments.id");
            statement.setInt(1, stegId);
            statement.setString(2, profileId);
            statement.setString(3, text);

            ResultSet rs = statement.executeQuery();
            if (rs.next())
                res = rs.getInt(1);
            rs.close();
            statement.close();

            DBHandler.addNews(NewsData.NOTIFICATION_TYPE_COMMENT, profileId, null, stegId, dbConnection);

        } catch (Exception e) {
            e.printStackTrace();
            res = null;
        }
        return res;
    }

    static Boolean addCommentLike(Integer commentId, String profileId, String ownerId, Connection dbConnection) {
        Boolean res = false;
        try {
            Boolean check = true;
            PreparedStatement checkStatement = dbConnection.prepareStatement("SELECT profile_id FROM com_likes WHERE comment_id = ? AND profile_id = ?;");
            checkStatement.setInt(1, commentId);
            checkStatement.setString(2, profileId);
            ResultSet rs = checkStatement.executeQuery();
            while (rs.next()) {
                check = false;
            }
            rs.close();
            checkStatement.close();
            if (check) {
                PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO com_likes " +
                        "(comment_id, profile_id) " +
                        "VALUES (?, ?);");
                statement.setInt(1, commentId);
                statement.setString(2, profileId);
                statement.executeUpdate();
                statement.close();

                Integer stegId = DBHandler.getCommentedSteg(commentId, dbConnection);
                if (stegId != null)
                    DBHandler.addNews(NewsData.NOTIFICATION_TYPE_COM_LIKE, profileId, ownerId, stegId, dbConnection);

                res = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            res = false;
        }
        return res;
    }

    static void removeCommentLike(Integer commentId, String profileId, Connection dbConnection) {
        try {
            Boolean check = false;
            PreparedStatement checkStatement = dbConnection.prepareStatement("SELECT profile_id FROM com_likes WHERE comment_id = ? AND profile_id = ?;");
            checkStatement.setInt(1, commentId);
            checkStatement.setString(2, profileId);
            ResultSet rs = checkStatement.executeQuery();
            while (rs.next()) {
                check = true;
            }
            rs.close();
            checkStatement.close();
            if (check) {
                PreparedStatement statement = dbConnection.prepareStatement("DELETE FROM com_likes " +
                        "WHERE comment_id = ? AND profile_id = ?;");
                statement.setInt(1, commentId);
                statement.setString(2, profileId);
                statement.executeUpdate();
                statement.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addComment(CommentData comment, Connection dbConnection) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO comments " +
                    "(steg_id, profile_id, data_type, text, camera_data, voice_data) " +
                    "VALUES (?, ?, ?, ?, ?, ?) RETURNING id;");
            statement.setInt(1, comment.stegId);
            statement.setString(2, comment.profileId);
            statement.setInt(3, comment.getType());
            statement.setString(4, comment.getText());

            if ((comment.getType() & CommentData.COMMENT_IMAGE_MASK) != 0) {
                statement.setString(5, comment.getImgData());
            } else {
                if ((comment.getType() & CommentData.COMMENT_VIDEO_MASK) != 0) {
                    statement.setString(5, comment.getVideoData());
                } else {
                    statement.setString(5, "clear");
                }
            }

            if ((comment.getType() & CommentData.COMMENT_VOICE_MASK) != 0) {
                statement.setString(6, comment.getVoiceData());
            } else {
                statement.setString(6, "clear");
            }

            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                comment.id = rs.getInt(1);
                WsHandler.getInstance().chatDispatcher.sendMessage(comment.stegId, comment.id, comment.profileId);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Boolean addLike(Integer stegId, String profileId, Connection dbConnection) {
        Boolean res = false;
        try {
            Boolean check = true;
            PreparedStatement checkStatement = dbConnection.prepareStatement("SELECT profile_id FROM likes WHERE steg_id = ? AND profile_id = ?");
            checkStatement.setInt(1, stegId);
            checkStatement.setString(2, profileId);
            ResultSet rs = checkStatement.executeQuery();
            if (rs.next()) {
                check = false;
            }
            rs.close();
            checkStatement.close();
            if (check) {
                PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO likes " +
                        "(steg_id, profile_id) " +
                        "VALUES (?, ?)");
                statement.setInt(1, stegId);
                statement.setString(2, profileId);
                statement.executeUpdate();
                statement.close();


                DBHandler.addNews(NewsData.NOTIFICATION_TYPE_LIKE, profileId, null, stegId, dbConnection);

                res = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            res = false;
        }
        return res;
    }

    static void addReceiver(Integer stegId, String profileId, Connection dbConnection) {
        try {
            Boolean check = true;
            PreparedStatement checkStatement = dbConnection.prepareStatement("SELECT profile_id FROM receives WHERE steg_id = ? AND profile_id = ?;");
            checkStatement.setInt(1, stegId);
            checkStatement.setString(2, profileId);
            ResultSet rs = checkStatement.executeQuery();
            while (rs.next()) {
                check = false;
            }
            rs.close();
            checkStatement.close();

            if (check) {
                PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO receives " +
                        "(steg_id, profile_id) " +
                        "VALUES (?, ?);");
                statement.setInt(1, stegId);
                statement.setString(2, profileId);
                statement.execute();
                statement.close();
                incStegReceived(stegId, dbConnection);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean addGeter(Integer stegId, String profileId, Connection dbConnection) {
        try {
            Boolean check = true;
            PreparedStatement checkStatement = dbConnection.prepareStatement("SELECT profile_id FROM gets WHERE steg_id = ? AND profile_id = ?");
            checkStatement.setInt(1, stegId);
            checkStatement.setString(2, profileId);
            ResultSet rs = checkStatement.executeQuery();
            if (rs.next()) {
                check = false;
            }
            rs.close();
            checkStatement.close();
            if (check) {
                PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO gets " +
                        "(steg_id, profile_id) " +
                        "VALUES (?, ?)");
                statement.setInt(1, stegId);
                statement.setString(2, profileId);
                statement.executeUpdate();
                statement.close();

                stegToWall(stegId, profileId, dbConnection);
                String ownerId = getStegSenderId(stegId, dbConnection);
                addNews(NewsData.NOTIFICATION_TYPE_SAVE, profileId, ownerId, stegId, dbConnection);
            }
            return check;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static ArrayList<CommentData> getComments(Integer stegId, Connection dbConnection) {
        ArrayList<CommentData> commentList = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT comments.id, comments.steg_id, " +
                    "comments.profile_id, users.user_name, users.user_photo, " +
                    "comments.text, comments.date_, comments.time_ " +
                    "FROM comments JOIN users ON comments.profile_id = users.user_id " +
                    "WHERE comments.steg_id = ? ORDER BY comments.id;");
            statement.setInt(1, stegId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                CommentData comment = new CommentData();
                comment.id = rs.getInt(1);
                comment.stegId = rs.getInt(2);
                comment.profileId = rs.getString(3);
                String profileId = rs.getString(4);
                String profileImg = rs.getString(5);
                String text = rs.getString(6);
                if (text != null)
                    comment.setText(text);
                comment.date = rs.getDate(7);
                comment.time = rs.getTime(8);
                commentList.add(comment);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return commentList;
    }

    public static CommentData getComment(Integer commentId, String profileId, Connection dbConnection) {
        CommentData comment = new CommentData();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT id, steg_id, profile_id, data_type, " +
                    "text, camera_data, voice_data, date_, time_ " +
                    "FROM comments WHERE id = ?;");
            statement.setInt(1, commentId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                comment.id = rs.getInt(1);
                comment.stegId = rs.getInt(2);
                comment.profileId = rs.getString(3);
                comment.setType(rs.getInt(4));
                comment.setText(rs.getString(5));
                if ((comment.getType() & CommentData.COMMENT_IMAGE_MASK) != 0) {
                    comment.setImgData(rs.getString(6));
                }
                if ((comment.getType() & CommentData.COMMENT_VIDEO_MASK) != 0) {
                    comment.setVideoData(rs.getString(6));
                }
                comment.setVoiceData(rs.getString(7));
                comment.date = rs.getDate(8);
                comment.time = rs.getTime(9);

//				Likes Count
                PreparedStatement likesCountStatement = dbConnection.prepareStatement("SELECT COUNT (id) FROM com_likes WHERE comment_id = ?;");
                likesCountStatement.setInt(1, commentId);
                ResultSet likesCountRs = likesCountStatement.executeQuery();
                while (likesCountRs.next()) {
                    comment.setLikesCount(likesCountRs.getInt(1));
                }
                likesCountRs.close();
                likesCountStatement.close();

//				is Liked
                PreparedStatement isLikedSt = dbConnection.prepareStatement("SELECT id FROM com_likes WHERE comment_id = ? AND profile_id = ?;");
                isLikedSt.setInt(1, commentId);
                isLikedSt.setString(2, profileId);
                ResultSet isLikedRs = isLikedSt.executeQuery();
                while (isLikedRs.next()) {
                    comment.setIsLiked(true);
                }

                rs.close();
                statement.close();
                return comment;
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ArrayList<CommentItem> getCommentsItems(Integer stegId, Connection dbConnection) {
        ArrayList<CommentItem> items = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT comments.id, comments.steg_id, comments.data_type,  " +
                    "comments.profile_id, " +
                    "comments.text, comments.date_, comments.time_ " +
                    "FROM comments " +
                    "WHERE comments.steg_id = ? ORDER BY comments.id;");
            statement.setInt(1, stegId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
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
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    public static ArrayList<String> getLikes(Integer stegId, Connection dbConnection) {
        ArrayList<String> likeList = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT likes.profile_id " +
                    "FROM likes " +
                    "WHERE likes.steg_id = ? ORDER BY likes.id DESC;");
            statement.setInt(1, stegId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                likeList.add(rs.getString(1));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return likeList;
    }

    public static ArrayList<String> getCommentLikes(Integer commentId, Connection dbConnection) {
        ArrayList<String> likeList = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT profile_id " +
                    "FROM com_likes " +
                    "WHERE comment_id = ? ORDER BY id DESC;");
            statement.setInt(1, commentId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                likeList.add(rs.getString(1));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return likeList;
    }

    public static ArrayList<LikeData> getSavers(Integer stegId, Connection dbConnection) {
        ArrayList<LikeData> saversList = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT wall.id, wall.steg_id, " +
                    "wall.owner, users.user_name, users.user_photo " +
                    "FROM wall JOIN users ON wall.owner = users.user_id " +
                    "WHERE wall.steg_id = ? ORDER BY wall.id DESC;");
            statement.setInt(1, stegId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
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
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return saversList;
    }

    static ArrayList<String> getReceiversList(Integer stegId, Connection dbConnection) {
        ArrayList<String> receiversList = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT profile_id FROM receives WHERE steg_id = ?;");
            statement.setInt(1, stegId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                receiversList.add(rs.getString(1));
            }
            rs.close();
            statement.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return receiversList;
    }

    static ArrayList<String> getVotersList(Integer pollItemId, Connection dbConnection) {
        ArrayList<String> voters = new ArrayList<>();
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT profile_id FROM votes WHERE poll_item_id = ?;");
            st.setInt(1, pollItemId);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                voters.add(rs.getString(1));
            }
            rs.close();
            st.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return voters;
    }

    static ArrayList<LikeData> getReceivers(Integer stegId, Connection dbConnection) {
        ArrayList<LikeData> receiversList = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT receives.id, receives.steg_id, " +
                    "receives.profile_id, users.user_name, users.user_photo " +
                    "FROM receives JOIN users ON receives.profile_id = users.user_id " +
                    "WHERE receives.steg_id = ? ORDER BY receives.id DESC;");
            statement.setInt(1, stegId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                LikeData receiver = new LikeData();
                receiver.id = rs.getInt(1);
                receiver.stegId = rs.getInt(2);
                receiver.profileId = rs.getString(3);
                String profileId = rs.getString(4);
                if (profileId != null)
                    receiver.profileName = profileId;
                String profileImg = rs.getString(5);
                if (profileImg != null)
                    receiver.profileImg = profileImg;
                receiversList.add(receiver);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return receiversList;
    }

    public static ArrayList<LikeData> getGeters(Integer stegId, Connection dbConnection) {
        ArrayList<LikeData> getersList = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT gets.id, gets.steg_id, " +
                    "gets.profile_id, users.user_name, users.user_photo " +
                    "FROM gets JOIN users ON gets.profile_id = users.user_id " +
                    "WHERE gets.steg_id = ? ORDER BY gets.id DESC;");
            statement.setInt(1, stegId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
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
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getersList;
    }

    static void justAddCommentNews(String type, String profileId, String ownerId, Integer stegId, Connection dbConnection, Boolean sended) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO news (type, profile_id, steg_id, owner_id, sended) " +
                    "VALUES (?, ?, ?, ?, ?);");
            statement.setString(1, type);
            statement.setString(2, profileId);
            statement.setInt(3, stegId);
            statement.setString(4, ownerId);
            statement.setBoolean(5, sended);
            statement.execute();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addNews(String type, String profileId, String ownerId, Integer stegId, Connection dbConnection) {
        try {
            PreparedStatement statement;
            if (ownerId == null) {
                ownerId = getStegOwner(stegId, dbConnection);
            }
            Boolean sended = false;
            statement = dbConnection.prepareStatement("INSERT INTO news(type, profile_id, steg_id, owner_id, sended) " +
                    "VALUES(?, ?, ?, ?, ?);");
            statement.setString(1, type);
            statement.setString(2, profileId);

            if (stegId != null) {
                statement.setInt(3, stegId);
                if (type.equals(NewsData.NOTIFICATION_TYPE_COMMENT) && WsHandler.getInstance().chatDispatcher.isProfileInChat(ownerId, stegId)) {
                    sended = true;
                }
            } else {
                statement.setInt(3, 0);
            }
            statement.setString(4, ownerId);
            statement.setBoolean(5, sended);

            statement.execute();


            if (type.equals(NewsData.NOTIFICATION_TYPE_COMMENT) && stegId != null) {
                ArrayList<String> followers = DBHandler.getFollowers(FavoriteItem.TYPE_STEG, stegId, dbConnection);
                for (String follower : followers) {
                    if (!follower.equals(profileId)) {
                        statement = dbConnection.prepareStatement("INSERT INTO news (type, profile_id, steg_id, owner_id, sended)" +
                                "VALUES (?, ?, ?, ?, ?);");
                        statement.setString(1, NewsData.NOTIFICATION_TYPE_FAV_COMMENT);
                        statement.setString(2, profileId);
                        statement.setInt(3, stegId);
                        statement.setString(4, follower);
                        Boolean favSended = false;
                        if (WsHandler.getInstance().chatDispatcher.isProfileInChat(follower, stegId)) {
                            favSended = true;
                        }
                        statement.setBoolean(5, favSended);
                        statement.execute();
                    }
                }
                WsHandler.getInstance().notifyStegFollowers(stegId, profileId);
            }
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static ArrayList<String> getStegNotifiers(Integer stegId, Connection dbConnection) {
        PreparedStatement statement;
        ArrayList<String> profileList = new ArrayList<>();
        try {
            statement = dbConnection.prepareStatement("SELECT DISTINCT users.user_id FROM users " +
                    "LEFT JOIN stegs ON (stegs.steg_id = ?) " +
                    "LEFT JOIN favorites ON (favorites.type = 'steg' AND favorites.fav_id = stegs.steg_id) " +
                    "LEFT JOIN gets ON (gets.steg_id = stegs.steg_id) " +
                    "WHERE users.user_id = stegs.sender " +
                    "OR users.user_id = favorites.profile_id " +
                    "OR users.user_id = gets.profile_id;");
            statement.setInt(1, stegId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                profileList.add(rs.getString(1));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return profileList;
    }

    static Integer getCommentedSteg(Integer commentId, Connection dbConnection) {
        Integer res = null;
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT steg_id FROM comments WHERE id = ?;");
            st.setInt(1, commentId);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                res = rs.getInt(1);
            }
            rs.close();
            st.close();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ArrayList<NewsData> getNews(String owner, Connection dbConnection) {
        ArrayList<NewsData> news = new ArrayList<>();
        String sql = "SELECT news.type, news.profile_id, users.user_name, " +
                "users.user_photo, news.steg_id, stegs.camera_path, " +
                "news.date_, news.time_, news.sended, news.id " +
                "FROM news LEFT OUTER JOIN users ON (news.profile_id = users.user_id) " +
                "LEFT OUTER JOIN stegs ON (news.steg_id = stegs.steg_id) " +
                "WHERE news.owner_id = ? AND news.profile_id != news.owner_id " +
                "ORDER BY news.id DESC LIMIT 600;";
        try {
            PreparedStatement statement = dbConnection.prepareStatement(sql);
            statement.setString(1, owner);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                NewsData n = new NewsData();
                n.type = rs.getString(1);
                n.profileId = rs.getString(2);
                n.profileName = rs.getString(3);
                if (n.profileName == null) n.profileName = "clear";
                n.profileImg = rs.getString(4);
                if (n.profileImg == null) n.profileImg = "clear";
                n.stegId = rs.getInt(5);
                if (n.stegId == null) {
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

    static void markNewsSended(Integer newsId, Connection dbConnection) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("UPDATE news SET sended = true WHERE id = ?;");
            statement.setInt(1, newsId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void markAllStegNotificationsSended(String ownerId, Integer stegId, Connection dbConnection) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("UPDATE news SET sended =  true WHERE owner_id = ? AND steg_id = ?;");
            statement.setString(1, ownerId);
            statement.setInt(2, stegId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void addFriend(String profileId, String friendId, Connection dbConnection) {
        if (!profileId.equals(friendId)) {
            try {
                Boolean isFriend = false;
                PreparedStatement statement;
                statement = dbConnection.prepareStatement("SELECT friend_id FROM friends WHERE profile_id = ? AND friend_id = ?");
                statement.setString(1, profileId);
                statement.setString(2, friendId);
                ResultSet senderRs = statement.executeQuery();
                while (senderRs.next()) {
                    isFriend = true;
                }

                if (!isFriend) {
                    statement = dbConnection.prepareStatement("INSERT INTO friends(profile_id, friend_id) " +
                            "VALUES(?, ?);");
                    statement.setString(1, profileId);
                    statement.setString(2, friendId);
                    statement.executeUpdate();

                    DBHandler.addNews(NewsData.NOTIFICATION_TYPE_FRIEND, profileId, friendId, null, dbConnection);
                }
                senderRs.close();
                statement.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void removeFriend(String profileId, String friendId, Connection dbConnection) {
        try {
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

    public static void addToBlackList(String myProfileId, String blackProfileId, Connection dbConnection) {
        if (!myProfileId.equals(blackProfileId)) {
            try {
                Boolean isBlack = false;
                PreparedStatement statement;
                statement = dbConnection.prepareStatement("SELECT black_profile_id FROM blacklist WHERE my_profile_id = ? AND black_profile_id = ?");
                statement.setString(1, myProfileId);
                statement.setString(2, blackProfileId);
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    isBlack = true;
                }

                if (!isBlack) {
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

    public static void addToBlackListByStegId(String myProfileId, Integer stegId, Connection dbConnection) {
        try {
            String blackId = DBHandler.getStegSenderId(stegId, dbConnection);
            addToBlackList(myProfileId, blackId, dbConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeFromBlackList(String myProfileId, String blackProfileId, Connection dbConnection) {
        try {
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

    public static void removeLike(Integer stegId, String profileId, Connection dbConnection) {
        try {
            Boolean check = false;
            PreparedStatement checkStatement = dbConnection.prepareStatement("SELECT profile_id FROM likes WHERE steg_id = ? AND profile_id = ?");
            checkStatement.setInt(1, stegId);
            checkStatement.setString(2, profileId);
            ResultSet rs = checkStatement.executeQuery();
            while (rs.next()) {
                check = true;
            }
            if (check) {
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

    public static boolean isFriend(String profileId, String friendId, Connection dbConnection) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT friend_id FROM friends WHERE profile_id = ? AND friend_id = ?");
            statement.setString(1, profileId);
            statement.setString(2, friendId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
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

    public static boolean isBlack(String myProfileId, String blackProfileId, Connection dbConnection) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT black_profile_id FROM blacklist WHERE my_profile_id = ? AND black_profile_id = ?");
            statement.setString(1, myProfileId);
            statement.setString(2, blackProfileId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
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

    public static Boolean isMeInBlackList(String myProfileId, String otherProfileId, Connection dbConnection) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT black_profile_id FROM blacklist WHERE my_profile_id = ? AND black_profile_id = ?;");
            statement.setString(1, otherProfileId);
            statement.setString(2, myProfileId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
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

    static ArrayList<UserProfile> getFriends(String profileId, Connection dbConnection) {
        ArrayList<UserProfile> friendsList = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT friends.friend_id, users.user_name, users.user_photo, users.user_sex, users.user_city, users.user_state, users.user_age " +
                    "FROM friends JOIN users ON friends.friend_id = users.user_id WHERE friends.profile_id = ?;");
            statement.setString(1, profileId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                UserProfile friend = new UserProfile(rs.getString(1));
                friend.setName(rs.getString(2));
                String img = rs.getString(3);
                if (img != null) {
                    friend.setPhoto(img);
                } else {
                    friend.setPhoto("clear");
                }
                String sex = rs.getString(4);
                if (sex != null) {
                    friend.setSex(sex);
                }
                String city = rs.getString(5);
                if (city != null) {
                    friend.setCity(city);
                }
                String state = rs.getString(6);
                if (state != null) {
                    friend.setState(state);
                }
                Integer age = rs.getInt(7);
                if (age != null) {
                    friend.setAge(age);
                }

                friendsList.add(friend);
            }

            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return friendsList;
    }


    static ArrayList<String> getProfileSearchResult(String searchString, String myCity, Connection dbConnection) {
        ArrayList<String> result = new ArrayList<>();
        try {
            PreparedStatement statement;
            if (searchString.equals("all_search_list")) {
                if (!myCity.equals(UserProfile.NO_VALUE)) {
                    statement = dbConnection.prepareStatement("SELECT user_id FROM users WHERE searchable = true AND user_city = ? AND user_photo != 'clear' ORDER BY random() LIMIT 70;");
                    statement.setString(1, myCity);
                } else {
                    statement = dbConnection.prepareStatement("SELECT user_id FROM users WHERE searchable = true AND user_photo != 'clear' ORDER BY random() LIMIT 70;");
                }
            } else {
                if (!myCity.equals("clear")) {
                    statement = dbConnection.prepareStatement("SELECT user_id, " +
                            "similarity(?, user_id) AS sml " +
                            "FROM users WHERE user_city = ? AND searchable = true AND user_id % ?  " +
                            "ORDER BY sml DESC;");
                    statement.setString(1, searchString);
                    statement.setString(3, searchString);
                    statement.setString(2, myCity);
                } else {
                    statement = dbConnection.prepareStatement("SELECT user_id, " +
                            "similarity(?, user_id) AS sml " +
                            "FROM users WHERE user_id % ? AND searchable = true " +
                            "ORDER BY sml DESC;");
                    statement.setString(1, searchString);
                    statement.setString(2, searchString);
                }

            }
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String searchItem = rs.getString(1);
                result.add(searchItem);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        }
        return result;
    }

    static ArrayList<String> getSubscribers(String profileId, Connection dbConnection) {
        ArrayList<String> result = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT profile_id FROM friends " +
                    "WHERE friend_id = ?;");
            statement.setString(1, profileId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String subscriber = rs.getString(1);
                result.add(subscriber);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        }
        return result;
    }

    static ArrayList<String> getFriendsList(String profileId, Connection dbConnection) {
        ArrayList<String> result = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT friend_id FROM friends " +
                    "WHERE profile_id = ?;");
            statement.setString(1, profileId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String subscriber = rs.getString(1);
                result.add(subscriber);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        }
        return result;
    }

    static void deleteGeter(Integer stegId, String profileId, Connection dbConnection) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("DELETE FROM gets WHERE steg_id = ? AND profile_id = ?;");
            statement.setInt(1, stegId);
            statement.setString(2, profileId);

            statement.executeUpdate();
            statement.close();
            removeStegFromWall(stegId, profileId, dbConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Boolean isFavorite(Integer favId, String profileId, String type, Connection dbConnection) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT id FROM favorites WHERE type = ? AND fav_id = ? AND profile_id = ?");
            statement.setString(1, type);
            statement.setInt(2, favId);
            statement.setString(3, profileId);

            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                rs.close();
                statement.close();
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return false;
    }

    static void addStegToFavorite(Integer stegId, String profileId, Connection dbConnection) {
        if (!profileId.equals(DBHandler.getStegOwner(stegId, dbConnection)) && !DBHandler.isFavorite(stegId, profileId, FavoriteItem.TYPE_STEG, dbConnection)) {
            try {

                PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO favorites (type, fav_id, profile_id) " +
                        "VALUES(?, ?, ?);");
                statement.setString(1, FavoriteItem.TYPE_STEG);
                statement.setInt(2, stegId);
                statement.setString(3, profileId);
                statement.executeUpdate();

                statement.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void removeStegFromFavorite(Integer stegId, String profileId, Connection dbConnection) {
        try {
            if (DBHandler.isFavorite(stegId, profileId, FavoriteItem.TYPE_STEG, dbConnection)) {
                PreparedStatement statement = dbConnection.prepareStatement("DELETE FROM favorites " +
                        "WHERE type = ? AND fav_id = ? AND profile_id = ?;");
                statement.setString(1, FavoriteItem.TYPE_STEG);
                statement.setInt(2, stegId);
                statement.setString(3, profileId);
                statement.executeUpdate();
                statement.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static ArrayList<String> getFollowers(String type, Integer favId, Connection dbConnection) {
        ArrayList<String> followers = new ArrayList<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT profile_id FROM favorites WHERE type = ? AND fav_id = ?;");
            statement.setString(1, type);
            statement.setInt(2, favId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                followers.add(rs.getString(1));
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return followers;
    }

    public static ArrayList<FavoriteItem> getFavorites(String profileId, Connection dbConnection) {
        ArrayList<FavoriteItem> favoriteList = new ArrayList<>();
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT id, fav_id, type FROM favorites WHERE profile_id = ? ORDER BY id DESC;");
            st.setString(1, profileId);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                Integer id = rs.getInt(1);
                Integer favId = rs.getInt(2);
                String type = rs.getString(3);
                favoriteList.add(new FavoriteItem(id, favId, type));
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return favoriteList;
    }

    static HashMap<String, Integer> getStatistic(Connection dbConnection) {
        HashMap<String, Integer> stat = new HashMap<>();
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT steg_id - 1854 FROM stegs WHERE reciever = ? OR reciever = ? ORDER BY steg_id DESC LIMIT 1;");
            statement.setString(1, "common");
            statement.setString(2, "location");
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                stat.put("stegsCount", rs.getInt(1));
            }
            rs.close();
            statement.close();

            statement = dbConnection.prepareStatement("SELECT (SELECT id from users ORDER BY id DESC LIMIT 1), COUNT (DISTINCT user_city) FROM users;");
            rs = statement.executeQuery();
            if (rs.next()) {
                stat.put("usersCount", rs.getInt(1));
                stat.put("usersCity", rs.getInt(2));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stat;
    }

    static ArrayList<UserLocation> getUserLocations(String profileId, Connection dbConnection) {
        ArrayList<UserLocation> locations = new ArrayList<>();
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT user_id, longitude, latitude FROM users WHERE (longitude != 0 OR latitude != 0) AND user_id != ?;");
            st.setString(1, profileId);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                locations.add(new UserLocation(rs.getString(1), rs.getDouble(2), rs.getDouble(3)));
            }
            rs.close();
            st.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return locations;
    }

    public static void addStegLocation(Integer stegId, Double latitude, Double longitude, String title, Integer type, Connection dbConnection) {
        try {
            PreparedStatement st = dbConnection.prepareStatement("INSERT INTO steg_locations (steg_id, latitude, longitude, title, type, point) VALUES (?, ?, ?, ?, ?, ST_GeographyFromText('SRID=4326;POINT(" + latitude.toString() + " " + longitude.toString() + ")'));");
            st.setInt(1, stegId);
            st.setDouble(2, latitude);
            st.setDouble(3, longitude);
            st.setString(4, title);
            st.setInt(5, type);
            st.execute();
            st.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static ArrayList<StegLocation> getCurrentStegLocation(Integer stegId, Connection dbConnection) {
        ArrayList<StegLocation> locations = new ArrayList<>();
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT id, steg_id, latitude, longitude, title, type FROM steg_locations " +
                    "WHERE steg_id = ?;");
            st.setInt(1, stegId);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                StegLocation location = new StegLocation(rs.getInt(1), rs.getInt(2), rs.getDouble(3), rs.getDouble(4));
                String title = rs.getString(5);
                if (!title.equals(StegLocation.NO_TITLE))
                    location.setTitle(title);

                location.setType(rs.getInt(6));
                location.setProfileId(DBHandler.getLocationStegSender(location.getStegId(), dbConnection));
                locations.add(location);
            }
            rs.close();
            st.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return locations;
    }

    static ArrayList<StegLocation> getCatchedStegLocations(String profileId, Connection dbConnection) {
        ArrayList<StegLocation> locations = new ArrayList<>();
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT id, steg_id, latitude, longitude, title, type FROM steg_locations WHERE steg_id IN (SELECT steg_id FROM gets WHERE profile_id = ?);");
            st.setString(1, profileId);
            ResultSet rs = st.executeQuery();
            while (rs.next()){
                StegLocation location = new StegLocation(rs.getInt(1), rs.getInt(2), rs.getDouble(3), rs.getDouble(4));
                String title = rs.getString(5);
                if (!title.equals(StegLocation.NO_TITLE))
                    location.setTitle(title);
                location.setType(rs.getInt(6));
                location.setProfileId(DBHandler.getLocationStegSender(location.getStegId(), dbConnection));
                locations.add(location);
            }
            rs.close();
            st.close();
        } catch (SQLException e){
            e.printStackTrace();
        }
        return locations;
    }

    static void setFcmToken(String profileId, String token, Connection dbConnection) {
        try {
            PreparedStatement st = dbConnection.prepareStatement("UPDATE users SET fcm_token = NULL WHERE fcm_token = ? AND user_id != ?;");
            st.setString(1, token);
            st.setString(2, profileId);
            st.executeUpdate();
            st.close();

            st = dbConnection.prepareStatement("UPDATE users SET fcm_token = ? WHERE user_id = ?;");
            st.setString(1, token);
            st.setString(2, profileId);
            st.executeUpdate();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getProfileToken(String profileId, Connection dbConnection) {
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT fcm_token FROM users WHERE user_id = ?;");
            st.setString(1, profileId);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    static ArrayList<String> getAllTokens(Connection dbConnection) {
        ArrayList<String> result = new ArrayList<>();
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT fcm_token FROM users WHERE fcm_token IS NOT NULL");
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                result.add(rs.getString(1));
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    static ArrayList<StegLocation> getLocationStegForProfile(String profileId, Connection dbConnection) {
        ArrayList<StegLocation> locations = new ArrayList<>();
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT id, steg_id, latitude, longitude, title, type FROM steg_locations " +
                    "WHERE steg_id IN (SELECT steg_id FROM stegs WHERE sender = ? AND reciever = ?);");
            st.setString(1, profileId);
            st.setString(2, "location");
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                StegLocation location = new StegLocation(rs.getInt(1), rs.getInt(2), rs.getDouble(3), rs.getDouble(4));
                String title = rs.getString(5);
                if (!title.equals(StegLocation.NO_TITLE))
                    location.setTitle(title);

                location.setType(rs.getInt(6));
                location.setProfileId(DBHandler.getLocationStegSender(location.getStegId(), dbConnection));
                locations.add(location);
            }
            rs.close();
            st.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return locations;
    }

    static ArrayList<PrizeLocation> getPrizeLocations(Connection dbConnection) {
        ArrayList<PrizeLocation> prizes = new ArrayList<>();
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT id, steg_id, latitude, longitude, title, value, won, winner_id FROM prize_locations;");
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                Integer id = rs.getInt(1);
                Integer stegId = rs.getInt(2);
                Double latitude = rs.getDouble(3);
                Double longitude = rs.getDouble(4);
                String title = rs.getString(5);
                Integer value = rs.getInt(6);
                Boolean won = rs.getBoolean(7);
                String winnerId = rs.getString(8);

                if (won) {
                    prizes.add(new PrizeLocation(id, stegId, winnerId, latitude, longitude, title, value));
                } else {
                    prizes.add(new PrizeLocation(id, stegId, latitude, longitude, title, value));
                }
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return prizes;
    }

    static boolean addPrizeWinner(String profileId, Integer prizeId, Connection dbConnection) {
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT won, winner_id FROM prize_locations WHERE id = ?;");
            st.setInt(1, prizeId);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                if (rs.getBoolean(1)) {
                    String winnerId = rs.getString(2);
                    if (winnerId != null && !winnerId.equals(profileId)) {
                        rs.close();
                        st.close();
                        return false;
                    }
                }
            }
            st = dbConnection.prepareStatement("UPDATE prize_locations SET won = true, winner_id = ? WHERE id = ?;");
            st.setString(1, profileId);
            st.setInt(2, prizeId);
            st.executeUpdate();
            st.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    static boolean addPrizeWinnerContacts(String profileId, String contacts, Integer prizeId, Connection dbConnection) {

        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT winner_id FROM prize_locations WHERE id = ?;");
            st.setInt(1, prizeId);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                String winnerId = rs.getString(1);
                if (winnerId != null && !winnerId.equals(profileId)) {
                    rs.close();
                    st.close();
                    return false;
                }
            }
            rs.close();
            st.close();
            st = dbConnection.prepareStatement("UPDATE prize_locations SET winner_id = ?, winner_contact = ? WHERE id = ?;");
            st.setString(1, profileId);
            st.setString(2, contacts);
            st.setInt(3, prizeId);
            int updates = st.executeUpdate();
            st.close();

            return (updates > 0);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    static ArrayList<StegLocation> getStegLocationsAll(String profileId, Connection dbConnection) {
        ArrayList<StegLocation> locations = new ArrayList<>();
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT id, steg_id, latitude, longitude, title, type FROM steg_locations " +
                    "WHERE steg_id NOT IN (SELECT steg_id FROM receives WHERE profile_id = ?) " +
                    "AND steg_id NOT IN (SELECT steg_id FROM stegs WHERE sender = ? AND reciever = ?);");
            st.setString(1, profileId);
            st.setString(2, profileId);
            st.setString(3, "location");
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                StegLocation location = new StegLocation(rs.getInt(1), rs.getInt(2), rs.getDouble(3), rs.getDouble(4));
                String title = rs.getString(5);
                if (!title.equals(StegLocation.NO_TITLE))
                    location.setTitle(title);

                location.setType(rs.getInt(6));
                location.setProfileId(DBHandler.getLocationStegSender(location.getStegId(), dbConnection));
                locations.add(location);
            }
            rs.close();
            st.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return locations;
    }

    static Set<String> getUsedFiles(Connection dbConnection) {
        Set<String> usedFiles = new HashSet<>();
        try {
//            FILES FROM COMMENTS
            PreparedStatement st = dbConnection.prepareStatement("SELECT camera_data, voice_data FROM comments;");
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                addUsedFileToList(rs.getString(1), usedFiles);
                addUsedFileToList(rs.getString(2), usedFiles);
            }
            st.close();
            rs.close();

//            FILES FROM STEGS
            st = dbConnection.prepareStatement("SELECT voice_path, camera_path FROM stegs;");
            rs = st.executeQuery();
            while (rs.next()) {
                addUsedFileToList(rs.getString(1), usedFiles);
                addUsedFileToList(rs.getString(2), usedFiles);
            }
            st.close();
            rs.close();

//            FILES FROM USERS
            st = dbConnection.prepareStatement("SELECT user_photo FROM users;");
            rs = st.executeQuery();
            while (rs.next()) {
                addUsedFileToList(rs.getString(1), usedFiles);
            }
            st.close();
            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usedFiles;
    }

    private static void addUsedFileToList(String file, Set<String> set) {
        if (file != null && !file.equals("clear")) {
            set.add(file);
        }
    }

    public static void addEmailToValidTable(String profileId, String email, String validCode, Connection dbConnection) {
        try {
            PreparedStatement st = dbConnection.prepareStatement("INSERT INTO email_validation (user_id, email, validation_code) " +
                    " VALUES (?, ?, ?);");
            st.setString(1, profileId);
            st.setString(2, email);
            st.setString(3, validCode);
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Boolean validateEmail(String validationCode) {
        Connection dbConnection = WsHandler.getInstance().dbConnection;
        Boolean res = false;
        try {
            PreparedStatement st = dbConnection.prepareStatement("UPDATE users SET user_email = (SELECT email FROM email_validation WHERE validation_code = ?) " +
                    "WHERE user_id = (SELECT user_id FROM email_validation WHERE validation_code = ?);");
            st.setString(1, validationCode);
            st.setString(2, validationCode);
            if (st.executeUpdate() > 0)
                res = true;
            st.close();
            if (res) {
                st = dbConnection.prepareStatement("DELETE FROM email_validation WHERE validation_code = ?;");
                st.setString(1, validationCode);
                st.executeUpdate();
                st.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static boolean checkDiscoverer(Locality locality, Connection dbConnection) {
        System.out.println("check Discoverer");
        boolean res = true;
        try {
            PreparedStatement st = dbConnection.prepareStatement("SELECT discoverer_id FROM discoverers WHERE country_hash = ? AND area_hash = ? AND city_hash = ?;");
            st.setInt(1, locality.getCountryHash());
            st.setInt(2, locality.getAreaHash());
            st.setInt(3, locality.getCityHash());
            ResultSet rs = st.executeQuery();

            while (rs.next()) {
                res = false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            res = false;
        }
        return res;
    }

    public static void addDiscoverer(Locality locality, String profileId, Connection dbConnection) {
        System.out.println("add Discoverer");
        try {
            PreparedStatement st = dbConnection.prepareStatement("INSERT INTO discoverers (country, country_hash, area, area_hash, city, city_hash, discoverer_id) VALUES (?, ?, ?, ?, ?, ?, ?);");
            st.setString(1, locality.getCountry());
            st.setInt(2, locality.getCountryHash());
            st.setString(3, locality.getArea());
            st.setInt(4, locality.getAreaHash());
            st.setString(5, locality.getCity());
            st.setInt(6, locality.getCityHash());
            st.setString(7, profileId);
            st.executeUpdate();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static final Runnable privateStegDeleteTask = () -> {
        try {
            Connection dbConnection = WsHandler.getInstance().dbConnection;
            PreparedStatement st = dbConnection.prepareStatement("SELECT steg_id FROM stegs " +
                    "WHERE reciever != 'common' AND reciever != 'location' AND reciever != 'broadcast' " +
                    "AND active = TRUE " +
                    "AND date_ + time_ < date_trunc('Day', now()) - interval '7 days';");
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                deleteStegAdm(rs.getInt(1), dbConnection);
            }
            rs.close();
            st.close();

            Set<String> usedFiles = getUsedFiles(dbConnection);
            StegFileManager.checkUnusedFiles(usedFiles);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    };

}

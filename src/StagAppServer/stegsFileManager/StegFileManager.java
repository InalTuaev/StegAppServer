package StagAppServer.stegsFileManager;


import java.io.File;
import java.util.Set;

public class StegFileManager {

    private static final String STEGAPP_IMG_DIR = "StegApp/media/img/";
    private static final String STEGAPP_IMG_T_DIR = "StegApp/media/img/thumbs/";
    private static final String STEGAPP_VIDEO_DIR = "StegApp/media/video/";
    private static final String STEGAPP_AUDIO_DIR = "StegApp/media/audio/";
    private static final String STEGAPP_PROFILE_PHOTO_DIR = "StegApp/avatars/";
    private static final String STEGAPP_PROFILE_THUMBS_DIR = "StegApp/thumbs/";

    public static void deleteFile(String fileName, Boolean isVoice) {
        String[] FILE_DIRS;
        if (isVoice) {
            FILE_DIRS = new String[]{STEGAPP_AUDIO_DIR};
        } else {
            FILE_DIRS = new String[]{STEGAPP_IMG_DIR, STEGAPP_IMG_T_DIR, STEGAPP_VIDEO_DIR};
        }

        for (String dir : FILE_DIRS) {
            File delFile = new File(dir + fileName);
            if (delFile.exists())
                delFile.delete();
        }
    }

    public static void deleteUserImgFile(String fileName) {
        String[] FILE_DIRS = new String[]{STEGAPP_PROFILE_PHOTO_DIR, STEGAPP_PROFILE_THUMBS_DIR};
        for (String dir : FILE_DIRS) {
            File delFile = new File(dir + fileName);
            if (delFile.exists())
                delFile.delete();
        }
    }

    public static void checkUnusedFiles(Set<String> usedFiles) {
        String[] dirs = new String[]{STEGAPP_IMG_DIR, STEGAPP_IMG_T_DIR, STEGAPP_VIDEO_DIR, STEGAPP_AUDIO_DIR, STEGAPP_PROFILE_PHOTO_DIR, STEGAPP_PROFILE_THUMBS_DIR};
        int allFilesCounter = 0;
        int deletedFilesCounter = 0;
        for (String dir : dirs) {
            File dirFile = new File(dir);
            if (dirFile.isDirectory()) {
                for (File file : dirFile.listFiles()) {
                    allFilesCounter++;
                    if (!usedFiles.contains(file.getName()) && !file.isDirectory()) {
                        deletedFilesCounter++;
                        System.out.println(file.getName() + " deleted");
                        file.delete();
                    }
                }
            }
        }
        System.out.println("allFiles: " + allFilesCounter + " deleted: " + deletedFilesCounter);
    }
}

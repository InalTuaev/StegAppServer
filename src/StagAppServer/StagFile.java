package StagAppServer;

public class StagFile {

    public static final int STEG_FILE_TYPE_IMG = 1;
    public static final int STEG_FILE_TYPE_VIDEO = 2;
    public static final int STEG_FILE_TYPE_AUDIO = 3;

    private String filePath;
    private Integer type;

    public StagFile() {
        filePath = "clear";
        type = -1;
    }
    public StagFile(String filePath, Integer type) {
        this.filePath = filePath;
        this.type = type;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    public void setType(Integer type) {
        this.type = type;
    }
    public String getFilePath() {
        return this.filePath;
    }
    public Integer getType() {
        return this.type;
    }
}
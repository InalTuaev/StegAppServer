package StagAppServer;

public class StagFile {
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
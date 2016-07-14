package StagAppServer;

public class FavoriteItem {

    public static final String TYPE_STEG = "steg";

    private Integer id;
    private Integer favId;
    private String type;

    public FavoriteItem(Integer id, Integer favId, String type){
        this.id = id;
        this.favId = favId;
        this.type = type;
    }

    public Integer getId(){
        return id;
    }

    public Integer getFavId(){
        return favId;
    }

    public String getType(){
        return type;
    }
}

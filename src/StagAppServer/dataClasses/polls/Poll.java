package StagAppServer.dataClasses.polls;

import java.util.ArrayList;

public class Poll {

    private Integer stegId;
    private ArrayList<PollItem> pollItems;

    public Poll(){
        pollItems = new ArrayList<>();
    }

    public Integer getStegId() {
        return stegId;
    }

    public void setStegId(Integer stegId) {
        this.stegId = stegId;
    }

    public void addPollItem(PollItem item){
        pollItems.add(item);
    }

    public ArrayList<PollItem> getPollItems(){
        return pollItems;
    }
}

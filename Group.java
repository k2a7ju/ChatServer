import java.io.*;
import java.util.*;

//1つのグループのクラス
public class Group {
    private String groupName; //グループの名前
    private List<ChatClientHandler> cchList = new ArrayList<ChatClientHandler>(); //グループに属している人のリスト
    
    public Group(String groupName){
	this.setGroupName(groupName);
    }
    public void join(ChatClientHandler cch){
	this.cchList.add(cch);
    }
    public void leave(ChatClientHandler cch){
	this.cchList.remove(cch);
    }
    
    //setter
    public void setGroupName(String groupName){
	this.groupName = groupName;
    }
    
    //getter
    public String getGroupName(){
	return this.groupName;
    }
    public List getCchList(){
	return this.cchList;
    }
}
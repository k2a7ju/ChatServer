public Class Reject {
    
    private String userName;
    private List<ChatClientHandler> rejectList = new ArrayList<ChatClientHandler>();
    
    public Reject(String name){
	this.setUserName(name);
    }
    
    public void addRejectUser(ChatClientHandler cch){
	this.rejectList.add(cch);
    }
    
    public void setUserName(String userName){
	this.userName = uerName;
    }
}
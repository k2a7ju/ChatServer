import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClientHandler extends Thread{
    private Socket socket; //クライアント
    private int clientId; //ユーザに割り振るID番号
    private String userName; //ユーザの名前
    private BufferedReader in;
    private BufferedWriter out;
    private List<ChatClientHandler> clients = new ArrayList<ChatClientHandler>(); //現在ログインしている人のリスト
    private List<ChatClientHandler> rejectList = new ArrayList<ChatClientHandler>(); //ブロックされている人のリスト
    private static List<Group> groupList = new ArrayList<Group>(); //グループのリスト
    private static int clientsFlag[] = new int[1000]; //ログイン情報を管理するためのフラグ
    private static int firstOnlyFlag = 1; //初回だけしかループを回らないようにするためのフラグ

    public ChatClientHandler(Socket socket, List clients){
	//初期化
	initial();
	setSocket(socket);
	
	for(int i = 0; i < 1000; i++){ // 使っていないユーザー名を探す
	    if(clientsFlag[i] == 0){
		setClientId(i+1);
		clientsFlag[i] += 1;
		break;
	    }
	}
	setUserName("undefined" + String.valueOf(clientId)); //ログイン時点で名前を付ける	    
	setClients(clients);
	clients.add(this);//clientリストに追加
	
	/*IPアドレスの取得*/
	InetAddress address = socket.getInetAddress();
	System.out.println("IPアドレス: " + address); 
    }
    
    //スレッドを使うために継承されたメソッド
    public void run(){//このメソッドの部分が並列で実行される。
	try{
	    this.open();
	    while(true){
		String message = receive();
		String[] strings = message.split(" ");
		
		/* helpコマンド */
		if(strings[0].equals("help")){
		    helpCommand(strings);
		}
		/* postコマンド */
		else if(strings[0].equals("post")){
		    postCommand(strings);
		}
		/* tellコマンド */
		else if(strings[0].equals("tell")){
		    tellCommand(strings);
		}
		/* usersコマンド */
		else if(strings[0].equals("users")){
		    usersCommand(strings);
		}
		/* rejectコマンド */
		else if(strings[0].equals("reject")){
		    rejectCommand(strings);
		}
		/*  whoamiコマンド */
		else if(strings[0].equals("whoami")){
		    whoamiCommand(strings);
		}
		/* nameコマンド */
		else if(strings[0].equals("name")){
		    nameCommand(strings);
		}
		/* byeコマンド */
		else if(strings[0].equals("bye")){
		    byeCommand(strings);
		    break;
		}
		
		/* グループの作成するためのコマンド */
		/* createコマンド */
		else if(strings[0].equals("create")){
		    createCommand(strings);
		}
		/* groupコマンド */
		else if(strings[0].equals("groups")){
		    groupsCommand();
		}
		/* joinコマンド */
		else if(strings[0].equals("join")){
		    joinCommand(strings[1]);
		}
		/* leaveコマンド */
		else if(strings[0].equals("leave")){
		    leaveCommand(strings[1]);
		}
		/* membersコマンド */
		else if(strings[0].equals("members")){
		    membersCommand(strings);
		}
		else {
		    String buff = strings[0] + ": unknown command";
		    this.send(buff);
		}
	    }
	} catch(IOException e){
	    e.printStackTrace();
	} finally {
	    try{
		this.close();
	    } catch(IOException e){
		e.printStackTrace();
	    }
	}
    }
    /* コマンド */
    public void postCommand(String[] strings) throws IOException{
	if(strings.length == 1){ //post1文字しか入力されなかった場合
	    try {
		this.send("command format error: post <message>");
	    } catch(IOException e){
		e.printStackTrace();
	    }
	} else{
	    try{
		String string = ""; //送信した人の名前を格納するための変数
		String buff = ""; //メッセージ内容を格納するための変数
		
		if(clients.size() == 1){ //クライアントが自分しかいないとき
		    this.send("no one receie message");
		} else if(clients.size() > 1){//2以上のとき
		    int cnt = 0;
		    //クライアントを検索する
		    for(int i = 0; i < clients.size(); i++){
			ChatClientHandler cch = (ChatClientHandler)clients.get(i);
			if(cch != this){
			    if(cnt > 0){ //コンマを追加
				string += ", ";
			    }
			    cnt++;
			    string += cch.getUserName(); //nameをstring型で格納する
			    buff = addUserNameToMessage(userName, strings[1]); //出力結果に投稿者も表示できるように結合する
			    cch.send(buff);//クライアントにそれぞれ送信する。
			}
		    }
		    this.send(string);//送信できたクライアント名を自分に送信
		}
	    } catch(IOException e){
		e.printStackTrace();
	    }
	}
    }
    public void tellCommand(String[] strings) throws IOException{
	try {
	    ChatClientHandler cch;
	    List arrayList = null;//一時保存するためのリスト
	    if(strings.length == 3){
		for(int i = 0; i < clients.size(); i++){
		    ChatClientHandler buffer = (ChatClientHandler)clients.get(i);
		    if(buffer.userName.equals(strings[1])){
			strings[2] = "["+ this.userName + "->" + buffer.userName + "] " + strings[1]; //出力結果に投稿者も表示できるように結合する
			this.send(this.userName);
			buffer.send(strings[2]);
			return;
		    }
		}
		for(int i = 0; i < groupList.size(); i++){
		    if(groupList.get(i).getGroupName().equals(strings[1])){
			arrayList = groupList.get(i).getCchList();
			break;
		    }
		}
		if(arrayList.size() == 1){
		    this.send("no one receive message");
		    return;
		}
		for(int i = 0; i < arrayList.size(); i++){
		    cch = (ChatClientHandler)arrayList.get(i);
		    cch.send("[" + this.userName + "->(" + strings[1] + ")] " + strings[2]);
		}
	    }else {
		try{
		    this.send("command format error: tell <user | group> <message>");
		}catch(IOException e){
		    e.printStackTrace();
		}
	    }
	} catch(IOException e){
	    e.printStackTrace();
	}
    }
    public void helpCommand(String[] strings) throws IOException{
	try{
	    if(strings.length == 1){
		//理解できるコマンドを返信する
		send("help, name, whoami, users, post, tell,reject, create, join, leave, members,  bye");
	    }
	    else if(strings.length >= 2){
		if(strings[1].equals("help")){
		    this.send("help [command]");
		}
		else if(strings[1].equals("name")){
		    this.send("name <name>");
		}
		else if(strings[1].equals("whoami")){
		    this.send("whoami");
		}
		else if(strings[1].equals("post")){
		    this.send("post <message>");
		}
		else if(strings[1].equals("users")){
		    this.send("users");
		}
		else if(strings[1].equals("tell")){
		    this.send("tell <user | group> <message>");
		}
		else if(strings[1].equals("reject")){
		    this.send("reject [user]");
		}
		else if(strings[1].equals("create")){
		    this.send("create <group name>");
		}
		else if(strings[1].equals("join")){
		    this.send("join <group name>");
		}
		else if(strings[1].equals("leave")){
		    this.send("leave <group name>");
		}
		else if(strings[1].equals("members")){
		    this.send("members <groups>");
		}
		else if(strings[1].equals("bye")){
		    this.send("bye");
		}
	    }
	} catch(IOException e){
	    e.printStackTrace();
	}
    }
    
    public void usersCommand(String[] strings) throws IOException{
	String buffer="";
	int cnt = 0;
	//降順にソート
	ArrayList<String> array = new ArrayList<String>();
	for(int i = 0; i < clients.size(); i++){
	    ChatClientHandler cch = (ChatClientHandler)clients.get(i);
	    array.add(cch.getUserName());
	}
	Collections.sort(array);
	//名前をまとめて出力
	try{	    
	    for(int i = 0; i < clients.size(); i++){
		if(cnt > 0){
		    buffer += ", ";
		}
		cnt++;
		buffer += array.get(i);
	    }
	    this.send(buffer);
	} catch (IOException e){
	    e.printStackTrace();
	}
    }
    public void nameCommand(String[] strings) throws IOException{
	if(strings.length == 2){
	    ChatClientHandler buff = null;
	    for(int i = 0; i < clients.size(); i++){
		buff = (ChatClientHandler)clients.get(i);
		if(strings[1].equals(buff.getUserName())){
		    String string = strings[1] + ": already used";
		    try{
			this.send(string);
		    } catch(IOException e){
			e.printStackTrace();
		    }
		    return;
		}
	    }
	    this.setUserName(strings[1]);
	} else {
	    try{
		this.send("name <new name>");
	    } catch(IOException e){
		e.printStackTrace();
	    }
	}
    }
    public void whoamiCommand(String[] strings)  throws IOException{
	try {
	    ChatClientHandler buffer = this;
	    buffer.send(userName);
	}catch(IOException e){
	    e.printStackTrace();
	}
    }
    public void rejectCommand(String[] strings) throws IOException{ //中途半端にしか実装できていません.
	ChatClientHandler cch = null, buff = null;
	if(strings.length == 2){
	    for(int i = 0; i < clients.size(); i++){
		cch = (ChatClientHandler)clients.get(i);
		if(strings[1].equals(cch.userName)){
		    break;
		}
		else if(i == clients.size() - 1){
		    this.send(strings[1] + " isn't exist.");
		    return;
		}
	    }
	    for(int i = 0; i < rejectList.size(); i++){
		buff = (ChatClientHandler)rejectList.get(i);
		if(cch.userName.equals(buff.userName)){
		    this.send(strings[1] + " is exist in your Reject list.");
		    return;
		}
	    }
	    this.rejectList.add(cch);
	    this.send(" ");
	}
    }
    public void byeCommand(String[] strings) throws IOException{
	clients.remove(this);//リストから削除
	try{    
	    this.send(this.userName + ": bye");
	} catch(IOException e){
	    e.printStackTrace();
	}
	clientsFlag[getClientId() - 1] = 0;
    }
    public boolean createCommand(String[] strings) throws IOException{
	if(strings.length == 2){
	    for(int i = 0;i < this.groupList.size(); i++){
		if(this.groupList.get(i).getGroupName().equals(strings[1])){ //既に同じ名前のグループがあれば作成しない
		    return false;
		}
	    }
	    Group group = new Group(strings[1]);
	    group.join(this); //自分をグループに参加させる
	    this.groupList.add(group); //リストにグループを追加
	    try{
		this.send(strings[1] + ": create group");
	    } catch(IOException e){
		e.printStackTrace();
	    }
	    
	}else{
	    try{
		this.send("command format error: create <group name>");
	    } catch(IOException e){
		e.printStackTrace();
	    }
	}
	return true;
    }
    public void groupsCommand() throws IOException{
	String buffer = "";
	int cnt = 0;
	ArrayList<String> array = new ArrayList<String>(); //グループの名前を一時的に格納するためのリスト
	for(int i = 0; i < groupList.size(); i++){ //グループの名前を追加していく
	    array.add((String)groupList.get(i).getGroupName());
	}
	Collections.sort(array); //ソートする。
	for(int i = 0; i < array.size(); i++){ //出力用文字列を生成
	    if(cnt > 0){
		buffer += ", ";
	    }
	    buffer += array.get(i);
	}
	try{
	    this.send(buffer);
	}catch(IOException e){
	    e.printStackTrace();
	}
    }
    
    public boolean joinCommand(String string) throws IOException{
	Group buff = null;
	for(int i = 0; i < groupList.size(); i++){ //グループを検索
	    if(groupList.get(i).getGroupName().equals(string)){ //見つけたとき
		buff =  this.groupList.get(i);
		buff.join(this); //参加する
		try{
		    this.send(this.userName + ": join group " + string);
		    return true;
		}catch(IOException e){
		    e.printStackTrace();
		}
	    }
	}
	try{
	    this.send(string + "unknown group"); //見つからなかった時のエラー文
	} catch(IOException e){
	    e.printStackTrace();
	}
	return false;
    }
    public boolean leaveCommand(String string) throws IOException{
	Group buff = null;
	for(int i = 0; i < groupList.size(); i++){ //グループを検索
	    if(groupList.get(i).getGroupName().equals(string)){ //見つかったとき
		buff =  this.groupList.get(i);
		buff.leave(this); //グループを抜ける
		this.send(this.userName + ": left group");
		return true;
	    }
	}
	try{
	    this.send(string + ": unknown group");
	} catch(IOException e){
	    e.printStackTrace();
	}
	return false;
    }
    public void membersCommand(String[] strings) throws IOException{
	TreeSet set = new TreeSet(); //名前をソートするために用いる
	if(strings.length == 2){
	    Group buff = null;
	    List<ChatClientHandler>  cchList;
	    String string = "";
	    for(int i = 0; i < groupList.size(); i++){ //グループを検索
		if(this.groupList.get(i).getGroupName().equals(strings[1])){
		    buff = this.groupList.get(i);
		    break;
		}
	    }
	    cchList = buff.getCchList();
	    //TreeSetに名前を入れる
	    for(int i = 0; i < cchList.size(); i++){
		set.add(cchList.get(i).getUserName());
	    }
	    //名前一覧をString型に変換
	    Iterator it = set.iterator();
	    int cnt = 0;
	    while(it.hasNext()){
		if(cnt != 0){
		    string += ",";
		}
		cnt++;
		string += it.next();
	    }
	    try{
		this.send(string);
	    }catch(IOException e){
		e.printStackTrace();
	    }
	} else{
	    try{
		this.send("error");
	    } catch(IOException e){
		e.printStackTrace();
	    }
	}
    }
    //フラグを初期化するメソッド
    public void initial(){
	if(firstOnlyFlag != 0){
	    for(int i = 0; i < 1000; i++){ //フラグを0で初期化
		clientsFlag[i] = 0;
	    }
	    firstOnlyFlag = 0;
	}
    }
    //入力ストリームを開くメソッド
    public void open() throws IOException{ //クライアントとので0たのやり取りを行うストリー拓く開くメソッド
	InputStream socketIn = socket.getInputStream();
	in = new BufferedReader(new InputStreamReader(socketIn));
	
	OutputStream socketOut = socket.getOutputStream();
	out = new BufferedWriter(new OutputStreamWriter(socketOut));
    }
    
    //一行読み取って返すメソッド
    public String receive() throws IOException{ //クライアントからデータを受け取るメソッド
	String line = in.readLine();
	System.out.println("received: " +line);
	return line;
    }
    //userNameとmessageを一行に合体させるメソッド
    public String addUserNameToMessage(String userName, String message){
	String buffer;
	buffer = "[" + userName + "] " + message; //1行に投稿者とコメントを合体する
	return buffer;
    }
    //クライアントに送り返すメソッド
    public void send(String message) throws IOException{
	/*出力*/
	out.write(message);
	out.write("\r\n"); //ネットワークでは改行を\r\nと表されることが多い                  
	out.flush();
    }
    
    //入力ストリーム、出力ストリーム、Socketを閉じるためのメソッド
    public void close() throws IOException{ // クライアントとの接続を閉じるメソッド
	//ストリームとソケットは必ず最後に閉じなければいけない。                                
	if(in != null){
	    try{
		in.close();
	    }catch(IOException e){
		e.printStackTrace();                                                            
	    }
	}
	if(out != null){
	    try{
		out.close();
	    }catch(IOException e){
		e.printStackTrace();
	    }
	}
	if(socket != null){
	    try{
		socket.close();
	    }catch(IOException e){
		e.printStackTrace();                                                            
	    }
	}
    }
    
    //getterメソッド
    public int getClientId(){
	return clientId;
    }
    public String getUserName(){
	return userName;
    }
    //setterメソッド
    public void setClientId(int clientId){
	this.clientId = clientId;
    }
    public void setSocket(Socket socket){
	this.socket = socket;
    }
    public void setUserName(String userName){
	this.userName = userName;
    }
    public void setClients(List clients){
	this.clients = clients;
    }
}
import java.net.*;
import java.io.*;
import java.util.*;

public class ChatServer {
    private final static int PORT_NUMBER = 18080;
    private ServerSocket server;
    private List clients = new ArrayList();
    private List groupList = new ArrayList();
    ChatClientHandler handler = null;
    Group group = null;
    Socket socket;
    
    public void listen() throws IOException{
	try {
	    server = new ServerSocket(PORT_NUMBER);
	    System.out.println("start server on port 18080");
	    while(true){
		/*クライアントのアクセス待ち*/
		socket = server.accept();
		handler = new ChatClientHandler(socket, clients);
		System.out.println("クライアントが接続してきました");
		
		handler.start();
		//handler.close()　を呼び出さないようにする。
	    }
	}catch(IOException e){
	    e.printStackTrace();
	} finally {
	    if(handler != null){
		try{
		    handler.close();
		}catch(IOException e){
		    e.printStackTrace();
		}
	    }
	}
	
    }
    public static void main(String[] args){
	ChatServer chat = new ChatServer();
	try{
	    chat.listen();
	} catch(IOException e){
	    e.printStackTrace();
	}
    }
}
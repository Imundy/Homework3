package udpgroupchat.server;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;

public class ClientEndPoint {
	protected final InetAddress address;
	protected final int port;
	protected String name = "";
	
	private final int uniqueID;
	
	//the queue of messages for this client
	protected ArrayList<String> messages = new ArrayList<String>();
	
	public ClientEndPoint(InetAddress addr, int port) {
		this.address = addr;
		this.port = port;
		Random rand = new Random();
		uniqueID=hashCode()+rand.nextInt(1000);
	}
	
	public void changeName(String newName){
		name = newName;
	}
	
	public String getName(){
		if(name.isEmpty()){
			return "Anonymous";
		}else{
			return name;
		}
		
	}
	
	public int getUniqueID(){
		return uniqueID;
	}

	@Override
	public int hashCode() {
		// the hashcode is the exclusive or (XOR) of the port number and the hashcode of the address object
		return this.port ^ this.address.hashCode();
	}
	
	public void addMessage(String message){
		messages.add(message);
	}
	
	
}

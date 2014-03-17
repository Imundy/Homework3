/**
 * CS 283 Assignment 3
 * @author Ian Mundy
 * 
 * This is a Class for an individual chat group.
 * It maintains a set of clients that are currently registered for this group.
 * 
 */

package udpgroupchat.server;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Group {

	//name of the group
	private String name_;
	private final int MAX_GROUP_SIZE_ = 50;
	private int currentSize_;
	
	//The set of ClientEndPoints in this particular group
	protected final Set<ClientEndPoint> clientEndPoints = Collections
			.synchronizedSet(new HashSet<ClientEndPoint>());
	
	public Group(String name){
		name_ = name;
		currentSize_ =0;
	}
	
	/**
	 * Add the client end point if there is room
	 * @param newClient
	 * @return if the client was able to be added
	 */
	public boolean add(ClientEndPoint newClient){
		if(currentSize_ < MAX_GROUP_SIZE_){
			clientEndPoints.add(newClient);
			currentSize_++;
			return true;
		}else{
			return false;
		}
		
	}
	
	public void remove(ClientEndPoint client){
		clientEndPoints.remove(client);
	}
	
	public int hashCode(){
		return name_.hashCode();
	}
	
}

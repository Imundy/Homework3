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
	private int currentSize;
	
	//The set of ClientEndPoints in this particular group
	protected final Set<ClientEndPoint> clientEndPoints = Collections
			.synchronizedSet(new HashSet<ClientEndPoint>());
	
	public Group(String name){
		name_ = name;
		currentSize = 0;
	}
	
	public boolean add(ClientEndPoint newClient){
		if(currentSize < 50){
			clientEndPoints.add(newClient);
			currentSize++;
			return true;
		}else{
			return false;
		}
		
	}
	
	public void remove(ClientEndPoint client){
		
	}
	
	public int hashCode(){
		return name_.hashCode();
	}
	
}

/*
 * 
 * CS 283 Assignment 3
 * Ian Mundy
 * I am extending provided code
 * This implements the funtionality of all valid commands a client can send the server
 * 
 */

package udpgroupchat.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class WorkerThread extends Thread {

	private DatagramPacket rxPacket;
	private DatagramSocket socket;

	public WorkerThread(DatagramPacket packet, DatagramSocket socket) {
		this.rxPacket = packet;
		this.socket = socket;
	}

	@Override
	public void run() {
		// convert the rxPacket's payload to a string
		String payload = new String(rxPacket.getData(), 0, rxPacket.getLength())
				.trim();

		// dispatch request handler functions based on the payload's prefix

		if (payload.startsWith("REGISTER")) {
			onRegisterRequested(payload);
			return;
		}

		if (payload.startsWith("UNREGISTER")) {
			onUnregisterRequested(payload);
			return;
		}

		if (payload.startsWith("SEND")) {
			//note: I changed this so clients can only send messages within groups
			//There is no mechanism for global messaging, although you could have
			//clients join an allChat group by default
			onSendRequested(payload);
			return;
		}
		
		if (payload.startsWith("MSG")) {
			//sends message to a group
			onMessageRequested(payload);
			return;
		}
		
		if (payload.startsWith("JOIN")){
			//join a group
			onJoinRequested(payload);
			return;
		}
		
		if (payload.startsWith("LEAVE")){
			//Leave a group
			onLeaveRequested(payload);
			return;
		}
		
		if (payload.startsWith("NAME")){
			//Leave a group
			onNameRequested(payload);
			return;
		}
		
		if(payload.startsWith("POLL")){
			onPollRequested(payload);
			return;
		}
		
		if(payload.startsWith("ADDR_CHANGE")){
			onAddrChange(payload);
			return;
		}
		
		if(payload.startsWith("SHUTDOWN")){
			onShutdownRequested(payload);
			return;
		}
		
		if(payload.startsWith("ACK")){
			onACK(payload);
			return;
		}

		//
		// implement other request handlers here...
		//

		// if we got here, it must have been a bad request, so we tell the
		// client about it
		onBadRequest(payload);
	}

	// send a string, wrapped in a UDP packet, to the specified remote endpoint
	public void send(String payload, InetAddress address, int port)
			throws IOException {
		//try to send the data
		DatagramPacket txPacket = new DatagramPacket(payload.getBytes(),
				payload.length(), address, port);
		this.socket.send(txPacket);	

	}

	/**
	 * Registers the client end point in the server class
	 * @param payload
	 */
	private void onRegisterRequested(String payload) {
		// get the address of the sender from the rxPacket
		InetAddress address = this.rxPacket.getAddress();
		// get the port of the sender from the rxPacket
		int port = this.rxPacket.getPort();
		ClientEndPoint clientEndPoint = new ClientEndPoint(address, port);
		// create a client object, and put it in the map that assigns names
		// to client objects
		Server.clientEndPoints.put(clientEndPoint.getUniqueID(), clientEndPoint);
		// note that calling clientEndPoints.add() with the same endpoint info
		// (address and port)
		// multiple times will not add multiple instances of ClientEndPoint to
		// the set, because ClientEndPoint.hashCode() is overridden. See
		// http://docs.oracle.com/javase/7/docs/api/java/util/Set.html for
		// details.

		// tell client we're OK
		try {
			send("REGISTERED WITH UNIQUE ID "+ clientEndPoint.getUniqueID() + "\n", 
					this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Unregisters this clientEndPoint
	 * @param payload
	 */
	private void onUnregisterRequested(String payload) {
		int id = Integer.parseInt(payload.substring("UNREGISTER".length()+1));

		// check if client is in the set of registered clientEndPoints
		if (Server.clientEndPoints.containsKey(id)) {
			// yes, remove it
			Server.clientEndPoints.remove(id);
			try {
				send("UNREGISTERED\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// no, send back a message
			try {
				send("CLIENT NOT REGISTERED\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Globally broadcast a message to all registered endpoints
	 * Message is sent immediately and is not put in the message queues
	 * @param payload
	 */
	private void onSendRequested(String payload) {
		// the message is comes after "SEND" in the payload
		String message = payload.substring("SEND".length() + 1,
				payload.length()).trim();
		for (Object object : Server.clientEndPoints.values().toArray()) {
			try {
				ClientEndPoint clientEndPoint = (ClientEndPoint)object;
				send("MESSAGE: " + message + "\n", clientEndPoint.address,
						clientEndPoint.port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Form of acknowledgement receipt is
	 * ACK <uniqueID>:<message>
	 * Once a message has been acknowledged it is removed from the message list
	 * @param payload
	 */
	private void onACK(String payload){
		if(payload.contains(":")){
			int id = Integer.parseInt(payload.substring("ACK".length()+1,payload.indexOf(':')).trim());
			String message = payload.substring(payload.indexOf(':') + 1, payload.length()).trim();
			ClientEndPoint recipient = (Server.clientEndPoints.get(id));
			if(recipient.messages.contains(message)){
				recipient.messages.remove(message);
				try {
					send("ACKNOWLEDGED: " + message + "\n", this.rxPacket.getAddress(),
							this.rxPacket.getPort());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else{
				onBadRequest(payload);
			}
		}else{
			onBadRequest(payload);
		}
	}
	
	/**
	 * Retrieve all the messages for this particular clientEndPoint
	 */
	private void onPollRequested(String payload){
		int id = Integer.parseInt(payload.substring("POLL".length()+1));
		
		ClientEndPoint recipient = (Server.clientEndPoints.get(id));
		// the message is comes after "SEND" in the payload
		boolean moreMessages = true;
		while(moreMessages){
			//send all messages
			for (int i = 0; i < recipient.messages.size(); i ++) {
				try {
					String message = recipient.messages.get(i);
					send(message + "\n", recipient.address,
							recipient.port);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//sleep for ten seconds
			try{
				synchronized(this){
					//this time is incredibly long because I had to type the acknowledgement all into netcat...
					this.wait(20000);
				}
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			//if the messages haven't been acknowledged, send them again
			if(recipient.messages.size()==0){
				moreMessages = false;
			}
		}
		//clear the message list for good measure
		recipient.messages.clear();
	}
	
	/**
	 * Send a message to a group
	 * The message is added to the message list of all ClientEndPoints in that group
	 * @param payload
	 */
	private void onMessageRequested(String payload) {
		// the message is comes after "SEND" in the payload
		String groupName = payload.substring("MSG".length() + 1,
				payload.indexOf(':')).trim();
		Group group = Server.groupChats.get(groupName);
		
		String message = payload.substring((payload.substring(0,
				payload.indexOf(':'))).length() + 1, payload.length()).trim();
		
		for (ClientEndPoint clientEndPoint : group.clientEndPoints) {
			ClientEndPoint recipient = (Server.clientEndPoints.get(clientEndPoint.getUniqueID()));
			recipient.addMessage(("FROM " + clientEndPoint.getName() + " TO " +
					groupName + ": " + message));
		}
	}
	
	/**
	 * Join a particular group
	 * Creates the group if it does not already exist
	 * Requests should be made as "JOIN <uniqueID>: <groupName>
	 * @param payload
	 */
	private void onJoinRequested(String payload) {
		if(payload.contains(":")){
			int id = Integer.parseInt(payload.substring("JOIN".length()+1,payload.indexOf(':')).trim());
			
			ClientEndPoint recipient = Server.clientEndPoints.get(id);
			
			String groupName = payload.substring(payload.indexOf(':')+1, payload.length()).trim();
			boolean joined;
			
			if(!Server.groupChats.containsKey(groupName)){
				Group requestedGroup = new Group(groupName);
				joined = requestedGroup.add(recipient);
				Server.groupChats.put(groupName, requestedGroup);
			}else{
				Group group = Server.groupChats.get(groupName);
				joined = group.add(recipient);
				Server.groupChats.put(groupName, group);
			}
				
	
			if(joined){
				try {
					send("JOINED: " + groupName + "\n", this.rxPacket.getAddress(),
							this.rxPacket.getPort());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else{
				try {
					send("CANNOT JOIN " + groupName + ": ALREADY FULL\n", this.rxPacket.getAddress(),
							this.rxPacket.getPort());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}else{
			onBadRequest(payload);
		}
	}
	
	
/**
 * Leave the whatever group the user specifies, if that group exists
 * Requests should be made as "LEAVE <uniqueID>: <groupName>
 * @param payload
 */
	private void onLeaveRequested(String payload){
		if(payload.contains(":")){
			int id = Integer.parseInt(payload.substring("LEAVE".length()+1,
														payload.indexOf(':')).trim());
			
			ClientEndPoint recipient = Server.clientEndPoints.get(id);
			
			String groupName = payload.substring(payload.indexOf(':')+1, 
					payload.length()).trim();
			Group requestedGroup = Server.groupChats.get(groupName);
			
	
			if(requestedGroup != null && requestedGroup.clientEndPoints.contains(recipient)){
				requestedGroup.remove(recipient);
				try {
					send("LEFT: " + groupName + "\n", this.rxPacket.getAddress(),
							this.rxPacket.getPort());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else{
				try {
					send("NOT A MEMBER OF: " + groupName + "\n", this.rxPacket.getAddress(),
							this.rxPacket.getPort());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}else{
			onBadRequest(payload);
		}
		

	}
	
	/**
	 * Changes the name of specified ClientEndPoint
	 * @param payload
	 */
	private void onNameRequested(String payload){
		if(payload.contains(":")){
			int id = Integer.parseInt(payload.substring("NAME".length()+1, payload.indexOf(':')).trim());
			
			String newName = payload.substring(payload.indexOf(':')+1, payload.length()).trim();
			ClientEndPoint recipient = Server.clientEndPoints.get(id);
			recipient.changeName(newName);
			
			try {
				send("SUCCESS: Hi " + newName + "\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			onBadRequest(payload);
		}
	}
	
	/**
	 * The format of the request is expected as 
	 * "ADDR_CHANGE UNIQUEID"
	 * If the oldip was not valid, then a Bad Request is sent back
	 * @param payload
	 */
	private void onAddrChange(String payload){
		

		InetAddress address = this.rxPacket.getAddress();
		
		
		String uniqueID = payload.substring("ADDR_CHANGE".length() + 1).trim();

		ClientEndPoint oldClientEndPoint = Server.clientEndPoints.get(uniqueID);
		//if the old ip existed reregister the new one
		if(oldClientEndPoint != null){
			//copy the message queue of the old client
			oldClientEndPoint.changeIP(address);interrupt();
			//put the new clientEndPoint into the array
			Server.clientEndPoints.put(Integer.parseInt(uniqueID), oldClientEndPoint);
			try {
				send("REREGISTERED\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			onBadRequest(payload);
		}

		
	}

	/**
	 * Tell the user the request was invalid
	 */
	private void onBadRequest(String payload) {
		try {
			send("BAD REQUEST\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * When the SHUTDOWN is received, we simply close the socket
	 * Any request still processing will through a socketException.
	 * Broadcasts the server is shutting down to all registered clients
	 */
	private void onShutdownRequested(String payload){
		onSendRequested("SEND SERVER IS SHUTTING DOWN");
		this.socket.close();
	}
	

}

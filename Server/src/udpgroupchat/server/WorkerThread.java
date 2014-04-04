package udpgroupchat.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

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

		if (payload.startsWith("REGISTER")) 
		{
			System.out.println("REGISTER requested");
			onRegisterRequested(payload);
			return;
		}

		if (payload.startsWith("UNREGISTER")) 
		{
			onUnregisterRequested(payload);
			return;
		}

		if (payload.startsWith("SEND")) 
		{
			onSendRequested(payload);
			return;
		}

		if (payload.startsWith("SHUTDOWN"))
		{
			if(rxPacket.getAddress().isLoopbackAddress())
			{
				onShutdownRequested();
			}
			else
			{
				try {
					send("ERROR ONLY LOCALHOST CAN REQUEST SHUTDOWN\n", this.rxPacket.getAddress(),
							this.rxPacket.getPort());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return;
		}
		
		if (payload.startsWith("POLL"))
		{
			onPollRequested(payload);
			return;
		}
		
		if (payload.startsWith("ACK"))
		{
			onAckRequest(payload);
			return;
		}
		
		if (payload.startsWith("JOIN"))
		{
			onJoinRequest(payload);
			return;
		}
		
		if (payload.startsWith("LEGAL"))
		{
			onLegalRequest(payload);
			return;
		}
		
		if (payload.startsWith("QUIT"))
		{
			onQuitRequest(payload);
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
		DatagramPacket txPacket = new DatagramPacket(payload.getBytes(),
				payload.length(), address, port);
		this.socket.send(txPacket);
	}

	private void onLegalRequest(String payload)
	{
		String [] tokens = payload.split(" ");
		String groupName = tokens[1].toUpperCase();
		
		if(Server.serverGroups.get(groupName).size() == 2)
		{
			try {
				send("READY\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		else
		{
			try {
				send("NOT READY\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
	}
	
	private void onAckRequest(String payload)
	{
		String [] tokens = payload.split(" ");
		int id;
		
		if(tokens.length == 2)
		{
			id = Integer.parseInt(tokens[1]);
		}
		else
		{
			try {
				send("ERROR NO ACK PERFORMED--ID REQUIRED\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		ClientEndPoint c = Server.clientEndPoints.get(id);
		
		if(c.hasMessagesPending())
		{
			c.removeOldestMessage();
		}
		

	}
	private void onQuitRequest(String payload)
	{
		String [] tokens = payload.split(" ");
		int id;
		String groupName = "";
		
		if(tokens.length == 3)
		{
			id = Integer.parseInt(tokens[1]);
			groupName = tokens[2].toUpperCase();
		}
		else
		{
			try {
				send("ERROR MUST INCLUDE ID AND GROUPNAME TO QUIT\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		ClientEndPoint c = Server.clientEndPoints.get(id);
		
		if(!Server.serverGroups.containsKey(groupName))
		{
			try {
				send(groupName + " IS NOT A VALID GROUP\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		if(Server.serverGroups.get(groupName).contains(c))
		{
			Server.serverGroups.get(groupName).remove(c);
			try {
				send("CLIENT " + id + " HAS QUIT GROUP " + groupName + "\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			try {
				send("CLIENT " + id + "IS NOT IN " + groupName + "\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}		
	
	public void onPollRequested(String payload)
	{
		String [] tokens = payload.split(" ");
		int id;
		
		if(tokens.length == 2)
		{
			id = Integer.parseInt(tokens[1]);
		}
		else
		{
			try {
				send("ERROR NO POLL PERFORMED--ID REQUIRED\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		ClientEndPoint c = Server.clientEndPoints.get(id);
		String message = "";
		if(c.hasMessagesPending())
		{
			message = c.getOldestMessage();
		}
		else
		{
			message = "NO_MORE_MESSAGES\n";
		}
		
		try {
			send("POLL " + message, this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	private void onJoinRequest(String payload)
	{
		String [] tokens = payload.split(" ");
		String groupName = "";
		int id;
		if(tokens.length == 3)
		{
			id = Integer.parseInt(tokens[1]);
			groupName = tokens[2];
			groupName = groupName.toUpperCase();
		}
		else
		{
			try {
				send("ERROR NO GROUP JOINED--ID AND GROUP NAME REQUIRED\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		
		if(Server.serverGroups.containsKey(groupName))
		{
			if(Server.serverGroups.get(groupName).size() <= 1)
			{
				Server.serverGroups.get(groupName).add(Server.clientEndPoints.get(id));
			}
			else
			{
				try {
					send("TOO MANY\n" + groupName + "\n", this.rxPacket.getAddress(),
							this.rxPacket.getPort());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		else
		{
			ArrayList<ClientEndPoint> clients = new ArrayList<ClientEndPoint>();
			clients.add(Server.clientEndPoints.get(id));
			Server.serverGroups.put(groupName, clients);	
		}
		
		try {
			System.out.println("Joined group: "+groupName);
			send("You joined: " + groupName + "\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void onRegisterRequested(String payload) {
		// get the address of the sender from the rxPacket
		InetAddress address = this.rxPacket.getAddress();
		// get the port of the sender from the rxPacket
		int port = this.rxPacket.getPort();
		
		ClientEndPoint c = new ClientEndPoint(address,port);
		
		
		// create a client object, and put it in the map that assigns ids
		// to client objects
		Server.clientEndPoints.put(c.getID(),c);
	

		// tell client we're OK
		try {
			System.out.println("REGISTER successful with ID"+c.getID());
			send("REGISTERED Hello! Your ID number is: " + c.getID() + "\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void onUnregisterRequested(String payload) {
		
		String [] tokens = payload.split(" ");
		int id;
		if(tokens.length == 2)
		{
			id = Integer.parseInt(tokens[1]);
		}
		else
		{
			try {
				send("ERROR NOT UNREGISTERED--ID REQUIRED\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return;
		}
		
		// check if client is in the map of registered clientEndPoints
		if (Server.clientEndPoints.containsKey(id))
		{
			// yes, remove it
			Server.clientEndPoints.remove(id);
			try 
			{
				send("UNREGISTERED\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		else 
		{
			// no, send back a message
			try 
			{
				send("ERROR CLIENT NOT REGISTERED\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}

	private void onSendRequested(String payload) {
		//payload must be four parts
		String [] tokens = payload.split(" ", 4);
		
		int id = Integer.parseInt(tokens[1]);
		String dest = tokens[2].toUpperCase();
		String message = tokens[3];
		
		for (ClientEndPoint clientEndPoint : Server.serverGroups.get(dest)) {
			try {
				send("MESSAGE from CLIENT " + id + " to " + dest + ": " + message + "\n", clientEndPoint.address,
						clientEndPoint.port);
				System.out.println("Im in sendReuqested----------------");
				System.out.println("Im in sending---------------- "+message);
			} catch (IOException e) {
				clientEndPoint.addMessageToQueue(message);
				e.printStackTrace();
			}
		}
	}

	private void onBadRequest(String payload) {
		try {
			send("BAD REQUEST\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void onShutdownRequested()
	{
		try {
			send("SERVER SHUTDOWN\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		socket.close();
	}

}

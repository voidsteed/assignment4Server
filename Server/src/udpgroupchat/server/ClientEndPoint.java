package udpgroupchat.server;

import java.net.InetAddress;
import java.util.LinkedList;

public class ClientEndPoint {
	protected final InetAddress address;
	protected final int port;
	protected LinkedList<String> messageQueue;
	protected final int id;
	
	public ClientEndPoint(InetAddress addr, int port) {
		this.address = addr;
		this.port = port;
		this.messageQueue = new LinkedList<String>();
		this.id = Server.nextId.getAndIncrement();
	}

	
	public void addMessageToQueue(String msg)
	{
		messageQueue.add(msg);
	}
	
	public boolean hasMessagesPending()
	{
		return !messageQueue.isEmpty();
	}
	
	public String getOldestMessage()
	{
		return messageQueue.element();
	}
	
	public void removeOldestMessage()
	{
		messageQueue.removeFirst();
	}
	
	public int getID()
	{
		return id;
	}
	
}

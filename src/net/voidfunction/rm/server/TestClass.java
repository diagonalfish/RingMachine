package net.voidfunction.rm.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;
import org.jgroups.Address;

import net.voidfunction.rm.common.http.IPAddressClient;
import net.voidfunction.rm.common.jgroups.JGroupsManager;
import net.voidfunction.rm.common.jgroups.JGroupsListener;
import net.voidfunction.rm.common.protocol.RMPacket;

public class TestClass extends JGroupsListener {

	public static void main(String[] args) {
		System.setProperty("java.net.preferIPv4Stack", "true");
		
		/*
		IPAddressServer ipserver = new IPAddressServer(1661);
		try {
			ipserver.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(new IPAddressClient("http://localhost:1661/").getMyIP());
		System.out.println(new IPAddressClient("http://whatismyip.org/").getMyIP());
		System.out.println(new IPAddressClient("http://ifconfig.me/ip").getMyIP());
		System.out.println(new IPAddressClient("http://myip.dnsomatic.com/").getMyIP());
		System.out.println(new IPAddressClient("http://myip.xname.org/").getMyIP());
		*/
		PropertyConfigurator.configure("log4j.properties");
		
		InetAddress myIP = null;
		
		try {
			myIP = InetAddress.getByName("192.168.0.103");
			//myIP = InetAddress.getByName(new IPAddressClient("http://whatismyip.org/").getMyIP());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int myPort = new java.util.Random().nextInt(1000) + 2000;
		JGroupsManager netmgr = null;
		try {
			netmgr = new JGroupsManager(myPort, myIP, "test", "voidfunction.net", 12001);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		netmgr.addListener(new TestClass());
		
		netmgr.connect();
	}
	
	public void onConnect() {
		System.out.println("Connected!");
	}
	
	public void onDisconnect() {
		System.out.println("Disconnected!");
	}

	public void onMessage(Address source, RMPacket message) {
		System.out.println("Got message!");
	}
	
	public void onError(String message) {
		System.out.println("Error! " + message);
	}
	
    public void stateRequested(OutputStream output) throws Exception {
    	System.out.println("Someone asked for our state.");
    }

    public void stateReceived(InputStream input) throws Exception {
    	System.out.println("Got the state from someone.");
    }
    
    public void initialPeers(List<Address> peers) {
    	System.out.println("Got initial peer list:");
    	for(Address addr : peers) {
    		System.out.println("  " + addr.toString());
    	}
    }

    public void onPeerJoin(Address newPeer) {
    	System.out.println("Peer joined: " + newPeer.toString());
    }
    
    public void onPeerLeave(Address lostPeer) {
    	System.out.println("Peer left: " + lostPeer.toString());
    }

    public void onPeerPossibleLeave(Address possibleLostPeer) {
    	System.out.println("Suspect a peer has left: " + possibleLostPeer.toString());
    }
	
}

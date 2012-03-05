package net.voidfunction.rm.common.jgroups;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import net.voidfunction.rm.common.protocol.RMPacket;

import org.jgroups.*;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.Util;

/**
 * JGroups 'client' class; abstracts most details of JGroups away from the rest
 * of the application. Creates channel and protocol stack, provides events to
 * any registered JGroupsListeners.
 */
public class JGroupsManager {

	private JChannel 			jch;
	private int 				publicPort; //Primary port our JGroups system listens on for other peer communication
	private InetAddress 		publicIP; //Our public (external) ip
	private String 				clusterName; //Name of cluster (doesn't matter as long as same for all peers
	private InetSocketAddress 	gossipRouter; //Address of GossipRouter we try to get peers from
	private View 				curView; //List of other peers we should know about
	
	private ArrayList<JGroupsListener> listeners; //Registered listeners
	
	public JGroupsManager(int publicPort, InetAddress publicIP, String clusterName, String gossipHost, int gossipPort) throws Exception {
		this.publicPort = publicPort;
		this.publicIP = publicIP;
		this.clusterName = clusterName;
		gossipRouter = new InetSocketAddress(gossipHost, gossipPort);
		
		listeners = new ArrayList<JGroupsListener>();
	}
	
	/**
	 * Register a new JGroupsListener to receive events
	 * @param newListener
	 */
	public void addListener(JGroupsListener newListener) {
		if (!listeners.contains(newListener))
			listeners.add(newListener);
	}
	
	/**
	 * De-register a given JGroupsListener.
	 * @param listener
	 */
	public void removeListener(JGroupsListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Gets address of our current JChannel.
	 * @return
	 */
	public Address getMyAddress() {
		return (jch == null ? null : jch.getAddress());
	}
	
	/**
	 * Get a list of other active peers in the cluster, not including
	 * ourself.
	 * @return
	 */
	public List<Address> getCurrentPeers() {
		List<Address> peers = new ArrayList<Address>();
		if (curView == null) return peers;
		peers.addAll(curView.getMembers());
		peers.remove(jch.getAddress()); //Remove our address
		return peers;
	}
	
	/**
	 * Initialize the channel and connect to the cluster.
	 */
	public void connect() {
		try {
			reset();
			jch.connect(clusterName);
		} catch (Exception e) {
			for (JGroupsListener listener : listeners) {
				listener.onError("Could not connect: " + e.getClass().getName() + " - " + e.getMessage());
				reset();
			}
		}
	}
	
	/**
	 * Disconnect from the cluster.
	 */
	public void disconnect() {
		jch.disconnect();
		jch = null;
	}
	
	/**
	 * Send a packet to all members of the cluster.
	 * @param packet
	 */
	public void sendMessage(RMPacket packet) {
		Message message = new Message();
		message.setObject(packet);
		send(message);
	}
	
	/**
	 * Send a packet to a particular destination address.
	 * @param packet
	 * @param dest
	 */
	public void sendMessage(RMPacket packet, Address dest) {
		Message message = new Message(dest);
		message.setObject(packet);
		send(message);
	}
	
	/* Private methods */
	
	private void reset() {
		jch = new JChannel(false);
		jch.setDiscardOwnMessages(true);
		
		curView = null;
		
		//Combined listener - forwards events on to JGroupsListeners
		JGroupsCombinedListener clistener = new JGroupsCombinedListener();
		jch.setReceiver(clistener);
		jch.addChannelListener(clistener);
		
		//Initialize protocol stack
		ProtocolStack stack = new ProtocolStack();
		jch.setProtocolStack(stack);
		ArrayList<InetSocketAddress> initial_hosts = new ArrayList<InetSocketAddress>();
		initial_hosts.add(gossipRouter);
		
		//Add protocols
		stack.addProtocol(new TCP().setValue("bind_port", publicPort).setValue("use_send_queues", true)
				.setValue("sock_conn_timeout", 60).setValue("external_addr", publicIP))
			.addProtocol(new TCPGOSSIP().setValue("initial_hosts", initial_hosts))
			.addProtocol(new MERGE2())
			//.addProtocol(new FD()) /* Not needed with FD_SOCK working, too spammy anyway */
			.addProtocol(new FD_SOCK().setValue("external_addr", publicIP))
			.addProtocol(new VERIFY_SUSPECT())
			.addProtocol(new ENCRYPT().setValue("encrypt_entire_message", false).setValue("symInit", 128)
					.setValue("symAlgorithm", "AES/ECB/PKCS5Padding").setValue("asymInit", 512)
					.setValue("asymAlgorithm", "RSA"))
			.addProtocol(new NAKACK2().setValue("use_mcast_xmit", false))
			.addProtocol(new UNICAST2())
			.addProtocol(new STABLE())
			.addProtocol(new GMS().setValue("print_local_addr", false))
			.addProtocol(new UFC())
			.addProtocol(new MFC())
			.addProtocol(new FRAG2())
			.addProtocol(new STATE_TRANSFER())
			.addProtocol(new COMPRESS());
		try {
			stack.init();
		} catch (Exception e) {
			for (JGroupsListener listener : listeners) {
				listener.onError("Error creating JChannel! " + e.getClass().getName() + " - " + e.getMessage());
			}
		}
	}
	
	private void send(Message message) {
		if (jch.isConnected()) {
			try {
				jch.send(message);
			} catch (Exception e) {
				for (JGroupsListener listener : listeners) {
					listener.onError("Error sending packet! " + e.getClass().getName() + " - " + e.getMessage());
				}
			}
		}
		else {
			for (JGroupsListener listener : listeners) {
				listener.onError("Attempted to send message while disconnected!");
			}
		}
	}
	
	private class JGroupsCombinedListener extends ReceiverAdapter implements ChannelListener {

		public void channelClosed(Channel channel) {
			//No-op
		}

		public void channelConnected(Channel channel) {
			for (JGroupsListener listener : listeners) {
				listener.onConnect();
			}
		}

		public void channelDisconnected(Channel channel) {
			//No-op
		}
		
		public void receive(Message message) {
			//Message received, try to cast it to an RMPacket and send an event
			try {
				Object obj = message.getObject();
				RMPacket packet = (RMPacket)obj;
				for (JGroupsListener listener : listeners) {
					listener.onMessage(message.getSrc(), packet);
				}
			} catch (Exception e) {
				for (JGroupsListener listener : listeners) {
					listener.onError("Error receiving packet " + e.getClass().getName() + " - " + e.getMessage());
				}
			}
		}
		
	    public void getState(OutputStream output) throws Exception {
			for (JGroupsListener listener : listeners) {
				listener.stateRequested(output);
			}
	    }

	    public void setState(InputStream input) throws Exception {
			for (JGroupsListener listener : listeners) {
				listener.stateReceived(input);
			}
	    }

	    public void viewAccepted(View newView) {
	    	//New list of peers received from the protocol stack
	    	if (curView == null) {
	    		for (JGroupsListener listener : listeners) {
	    			List<Address> peers = new ArrayList<Address>(newView.getMembers());
	    			peers.remove(jch.getAddress()); //Remove our address
					listener.initialPeers(peers);
				}
	    		curView = newView;
	    		return;
	    	}
	    	for(Address newPeer : Util.newMembers(curView.getMembers(), newView.getMembers())) {
	    		if (newPeer.equals(jch.getAddress())) continue; //Don't trigger for our address, just in case
	    		for (JGroupsListener listener : listeners) {
					listener.onPeerJoin(newPeer);
				}
	    	}
	    	for(Address lostPeer : Util.leftMembers(curView, newView)) {
	    		if (lostPeer.equals(jch.getAddress())) continue;
	    		for (JGroupsListener listener : listeners) {
					listener.onPeerLeave(lostPeer);
				}
	    	}
	    	curView = newView;
	    }

	    public void suspect(Address mbr) {
	    	if (mbr.equals(jch.getAddress())) return; //Don't trigger for our address, just in case
	    	for (JGroupsListener listener : listeners) {
				listener.onPeerPossibleLeave(mbr);
			}
	    }
		
	}
	
}

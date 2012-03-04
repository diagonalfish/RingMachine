package net.voidfunction.rm.common.jgroups;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import net.voidfunction.rm.common.protocol.RMPacket;

import org.jgroups.*;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.Util;

public class JGroupsManager {

	private JChannel jch;
	private int localPort;
	private InetAddress publicIP;
	private String clusterName;
	private InetSocketAddress gossipRouter;
	
	private View curView;
	
	private ArrayList<JGroupsListener> listeners;
	
	public JGroupsManager(int localPort, InetAddress publicIP, String clusterName, String gossipHost, int gossipPort) throws Exception {
		this.localPort = localPort;
		this.publicIP = publicIP;
		this.clusterName = clusterName;
		gossipRouter = new InetSocketAddress(gossipHost, gossipPort);
		
		listeners = new ArrayList<JGroupsListener>();
	}
	
	public void addListener(JGroupsListener newListener) {
		if (!listeners.contains(newListener))
			listeners.add(newListener);
	}
	
	public void removeListener(JGroupsListener listener) {
		listeners.remove(listener);
	}
	
	public void reset() {
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
		stack.addProtocol(new TCP().setValue("bind_port", localPort).setValue("use_send_queues", true)
				.setValue("sock_conn_timeout", 60).setValue("external_addr", publicIP))
			.addProtocol(new TCPGOSSIP().setValue("initial_hosts", initial_hosts))
			.addProtocol(new MERGE2())
			//.addProtocol(new FD())
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
	
	public void connect() {
		try {
			reset();
			jch.connect(clusterName);
		} catch (Exception e) {
			for (JGroupsListener listener : listeners) {
				listener.onError("Could not connect: " + e.getClass().getName() + " - " + e.getMessage());
				listener.onDisconnect();
				reset();
			}
		}
	}
	
	public void disconnect() {
		jch.disconnect();
		//TODO: Anything else needed here?
	}
	
	public void sendMessage(RMPacket packet) {
		Message message = new Message();
		message.setObject(packet);
		send(message);
	}
	
	public void sendMessage(RMPacket packet, Address dest) {
		Message message = new Message(dest);
		message.setObject(packet);
		send(message);
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
			/* might break stuff
			reset();
			for (JGroupsListener listener : listeners) {
				listener.onDisconnect();
			}
			*/
		}

		public void channelConnected(Channel channel) {
			for (JGroupsListener listener : listeners) {
				listener.onConnect();
			}
		}

		public void channelDisconnected(Channel channel) {
			reset();
			for (JGroupsListener listener : listeners) {
				listener.onDisconnect();
			}
		}
		
		public void receive(Message message) {
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
	    	if (curView == null) {
	    		for (JGroupsListener listener : listeners) {
					listener.initialPeers(newView.getMembers());
				}
	    		curView = newView;
	    		return;
	    	}
	    	for(Address newPeer : Util.newMembers(curView.getMembers(), newView.getMembers())) {
	    		for (JGroupsListener listener : listeners) {
					listener.onPeerJoin(newPeer);
				}
	    	}
	    	for(Address lostPeer : Util.leftMembers(curView, newView)) {
	    		for (JGroupsListener listener : listeners) {
					listener.onPeerLeave(lostPeer);
				}
	    	}
	    }

	    public void suspect(Address mbr) {
	    	for (JGroupsListener listener : listeners) {
				listener.onPeerPossibleLeave(mbr);
			}
	    }
		
	}
	
}

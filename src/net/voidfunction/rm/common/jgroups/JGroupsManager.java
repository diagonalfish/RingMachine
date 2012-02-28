package net.voidfunction.rm.common.jgroups;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;

import net.voidfunction.rm.common.protocol.RMPacket;

import org.jgroups.*;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.ProtocolStack;

public class JGroupsManager {

	private JChannel jch;
	private int localPort;
	private InetAddress publicIP;
	
	private String gossipRouter;
	
	private ArrayList<JGroupsListener> listeners;
	
	public JGroupsManager(int localPort, InetAddress publicIP, String gossipHost, int gossipPort) throws Exception {
		this.localPort = localPort;
		this.publicIP = publicIP;
		gossipRouter = gossipHost + "[" + gossipPort + "]";
		
		listeners = new ArrayList<JGroupsListener>();
	}
	
	public void addListener(JGroupsListener newListener) {
		if (!listeners.contains(newListener))
			listeners.add(newListener);
	}
	
	public void removeListener(JGroupsListener listener) {
		listeners.remove(listener);
	}
	
	public void setup() {
		jch = new JChannel(false);
		jch.setDiscardOwnMessages(true);
		
		//Combined listener - forwards events on to JGroupsListeners
		JGroupsCombinedListener clistener = new JGroupsCombinedListener();
		jch.setReceiver(clistener);
		jch.addChannelListener(clistener);
		
		//Initialize protocol stack
		ProtocolStack stack = new ProtocolStack();
		jch.setProtocolStack(stack);
		
		//Add protocols
		stack.addProtocol(new TCP().setValue("bind_port", localPort).setValue("use_send_queues", true)
				.setValue("sock_conn_timeout", 60).setValue("external_addr", publicIP))
			.addProtocol(new TCPGOSSIP().setValue("initial_hosts", gossipRouter))
			.addProtocol(new MERGE2())
			.addProtocol(new FD())
			.addProtocol(new FD_SOCK())
			.addProtocol(new VERIFY_SUSPECT())
			.addProtocol(new ENCRYPT().setValue("encrypt_entire_message", false).setValue("sym_init", 128)
					.setValue("sym_algorithm", "AES/ECB/PKCS5Padding").setValue("asym_init", 512)
					.setValue("asym_algorithm", "RSA"))
			.addProtocol(new NAKACK2().setValue("use_mcast_xmit", false))
			.addProtocol(new UNICAST2())
			.addProtocol(new STABLE())
			.addProtocol(new GMS())
			.addProtocol(new UFC())
			.addProtocol(new MFC())
			.addProtocol(new FRAG2())
			.addProtocol(new STATE_TRANSFER())
			.addProtocol(new COMPRESS());
		try {
			stack.init();
		} catch (Exception e) {
			for (JGroupsListener listener : listeners) {
				//TODO send error
			}
		}
	}
	
	public void connect() {
		
	}
	
	public void sendMessage(RMPacket message) {
		
	}
	
	private class JGroupsCombinedListener extends ReceiverAdapter implements ChannelListener {

		public void channelClosed(Channel channel) {
			//for (JGroupsListener listener : listeners) {
			//}
		}

		public void channelConnected(Channel channel) {
		}

		public void channelDisconnected(Channel channel) {
		}
		
		public void receive(Message message) {
		}
		
	    public void getState(OutputStream output) throws Exception {
	    	
	    }

	    public void setState(InputStream input) throws Exception {
	    	
	    }

	    public void viewAccepted(View view) {
	    	
	    }

	    public void suspect(Address mbr) {
	    	
	    }
		
	}
	
}

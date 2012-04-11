package net.voidfunction.rm.common;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelListener;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.auth.MD5Token;
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

	private Node node;
	
	private JChannel jch;
	private int publicPort; // Primary port our JGroups system listens on for
							// other peer communication
	private String publicIP; // Our public (external) ip
	private InetSocketAddress gossipRouter; // Address of GossipRouter we try to
											// get peers from
	private View curView; // List of other peers we should know about
	private String password;

	private ArrayList<JGroupsListener> listeners; // Registered listeners

	public JGroupsManager(Node node, int publicPort, String publicIP, String gossipHost, int gossipPort, String password) {
		this.node = node;
		this.publicPort = publicPort;
		this.publicIP = publicIP;
		this.password = password;
		gossipRouter = new InetSocketAddress(gossipHost, gossipPort);

		listeners = new ArrayList<JGroupsListener>();
	}

	/**
	 * Register a new JGroupsListener to receive events
	 * 
	 * @param newListener
	 */
	public void addListener(JGroupsListener newListener) {
		if (!listeners.contains(newListener))
			listeners.add(newListener);
	}

	/**
	 * De-register a given JGroupsListener.
	 * 
	 * @param listener
	 */
	public void removeListener(JGroupsListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Gets address of our current JChannel.
	 * 
	 * @return
	 */
	public Address getMyAddress() {
		return (jch == null ? null : jch.getAddress());
	}

	/**
	 * Get a list of other active peers in the cluster, not including ourself.
	 * 
	 * @return
	 */
	public List<Address> getCurrentPeers() {
		List<Address> peers = new ArrayList<Address>();
		if (curView == null)
			return peers;
		peers.addAll(curView.getMembers());
		peers.remove(jch.getAddress()); // Remove our address
		return peers;
	}

	/**
	 * Initialize the channel and connect to the cluster.
	 * @throws Exception 
	 */
	public void connect() throws Exception {
		reset();
		jch.connect("RM");
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
	 * 
	 * @param packet
	 * @throws Exception 
	 */
	public void sendMessage(RMPacket packet) throws Exception {
		Message message = new Message();
		message.setObject(packet);
		send(message);
	}

	/**
	 * Send a packet to a particular destination address.
	 * 
	 * @param packet
	 * @param dest
	 * @throws Exception 
	 */
	public void sendMessage(RMPacket packet, Address dest) throws Exception {
		Message message = new Message(dest);
		message.setObject(packet);
		send(message);
	}

	/* Private methods */

	private void reset() throws Exception {
		jch = new JChannel(false);
		jch.setDiscardOwnMessages(true);

		curView = null;

		// Combined listener - forwards events on to JGroupsListeners
		JGroupsCombinedListener clistener = new JGroupsCombinedListener();
		jch.setReceiver(clistener);
		jch.addChannelListener(clistener);

		// Convert public IP String to InetAddress
		InetAddress publicIPAddr = InetAddress.getByName(publicIP);
		
		// Initialize protocol stack
		ProtocolStack stack = new ProtocolStack();
		jch.setProtocolStack(stack);
		ArrayList<InetSocketAddress> initial_hosts = new ArrayList<InetSocketAddress>();
		initial_hosts.add(gossipRouter);
		
		AUTH authProt = new AUTH();
		MD5Token token = new MD5Token(password, "SHA");
		authProt.setAuthToken(token);
		token.setAuth(authProt);

		// Add protocols
		stack.addProtocol(
				new TCP().setValue("bind_port", publicPort).setValue("use_send_queues", true)
						.setValue("sock_conn_timeout", 60).setValue("external_addr", publicIPAddr)
						.setValue("bind_addr", InetAddress.getByName("0.0.0.0"))
						.setValue("thread_pool_rejection_policy", "run")
						.setValue("oob_thread_pool_rejection_policy", "run"))
				.addProtocol(new TCPGOSSIP().setValue("initial_hosts", initial_hosts))
				.addProtocol(new MERGE2())
				.addProtocol(new FD().setValue("timeout", 15000).setValue("max_tries", 2))
				/* .addProtocol(new FD_SOCK().setValue("external_addr", publicIP)) */
				.addProtocol(new VERIFY_SUSPECT())
				/*.addProtocol(
						new ENCRYPT().setValue("encrypt_entire_message", false).setValue("symInit", 128)
								.setValue("symAlgorithm", "AES/ECB/PKCS5Padding").setValue("asymInit", 512)
								.setValue("asymAlgorithm", "RSA")) */
				.addProtocol(new NAKACK2().setValue("use_mcast_xmit", false).setValue("discard_delivered_msgs", true))
				//.addProtocol(new UNICAST())
				.addProtocol(new STABLE())
				.addProtocol(new GMS().setValue("print_local_addr", true).setValue("view_bundling", true))
				.addProtocol(authProt)
				.addProtocol(new UFC())
				.addProtocol(new MFC())
				.addProtocol(new FRAG2())
				.addProtocol(new COMPRESS());
		
		stack.init();
	}

	private void send(Message message) throws Exception {
		/* TODO: is there a better way to tell whether we're connected? Maybe when we get MASTER_INFO? ... sigh 
		if (jch.isConnected()) {
				jch.send(message);
		} else {
			throw new Exception("Tried to send message while disconnected!");
		}
		*/
		jch.send(message);
	}
	
	public boolean isConnected() {
		return jch.isConnected();
	}

	private class JGroupsCombinedListener extends ReceiverAdapter implements ChannelListener {

		public void channelClosed(Channel channel) {
			// No-op
		}

		public void channelConnected(Channel channel) {
			for (JGroupsListener listener : listeners) {
				listener.onConnect();
			}
		}

		public void channelDisconnected(Channel channel) {
			// No-op
		}

		public void receive(Message message) {
			// Message received, try to cast it to an RMPacket and send an event
			try {
				Object obj = message.getObject();
				RMPacket packet = (RMPacket)obj;
				for (JGroupsListener listener : listeners) {
					listener.onMessage(message.getSrc(), packet);
				}
			} catch (Exception e) {
				// Warn on the console
				node.getLog().warn("Received invalid packet message from peer with address " + message.getSrc().toString());
			}
		}

		public void viewAccepted(View newView) {
			// New list of peers received from the protocol stack
			if (curView == null) {
				for (JGroupsListener listener : listeners) {
					List<Address> peers = new ArrayList<Address>(newView.getMembers());
					peers.remove(jch.getAddress()); // Remove our address
					listener.initialPeers(peers);
				}
				curView = newView;
				return;
			}
			for (Address newPeer : Util.newMembers(curView.getMembers(), newView.getMembers())) {
				if (newPeer.equals(jch.getAddress()))
					continue; // Don't trigger for our address, just in case
				for (JGroupsListener listener : listeners) {
					listener.onPeerJoin(newPeer);
				}
			}
			for (Address lostPeer : Util.leftMembers(curView, newView)) {
				if (lostPeer.equals(jch.getAddress()))
					continue;
				for (JGroupsListener listener : listeners) {
					listener.onPeerLeave(lostPeer);
				}
			}
			curView = newView;
		}

		public void suspect(Address mbr) {
			if (mbr.equals(jch.getAddress()))
				return; // Don't trigger for our address, just in case
			for (JGroupsListener listener : listeners) {
				listener.onPeerPossibleLeave(mbr);
			}
		}

	}

}

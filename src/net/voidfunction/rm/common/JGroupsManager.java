/*
 * --------------------------
 * |    Ring Machine 2      |
 * |                        |
 * |         /---\          |
 * |         |   |          |
 * |         \---/          |
 * |                        |
 * | The Crowdsourced CDN   |
 * --------------------------
 * 
 * Copyright (C) 2012 Eric Goodwin
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package net.voidfunction.rm.common;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.jgroups.*;
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
	private String password; // Password to access the cluster

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
	 * 
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

	// Set the manager to a pre-connect state
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
			.addProtocol(new FD().setValue("timeout", 5000).setValue("max_tries", 2))
			/* Removed because it's spammy and requires an extra port which is a hassle in a WAN environment.
			 * .addProtocol(new FD_SOCK().setValue("external_addr",
			 * publicIP))
			 */
			.addProtocol(new VERIFY_SUSPECT())
			.addProtocol(new NAKACK2().setValue("use_mcast_xmit", false).setValue("discard_delivered_msgs", true))
			// It doesn't seem that we need this.
			// .addProtocol(new UNICAST())
			.addProtocol(new STABLE())
			.addProtocol(new GMS().setValue("print_local_addr", false).setValue("view_bundling", true))
			.addProtocol(authProt).addProtocol(new UFC()).addProtocol(new MFC()).addProtocol(new FRAG2())
			.addProtocol(new COMPRESS());

		stack.init();
	}

	private void send(Message message) throws Exception {
		/*
		 * Is there a better way to tell whether we're connected? Maybe
		 * when we get MASTER_INFO? ... sigh
		 * 
		 * if (jch.isConnected()) {
		 * jch.send(message); } else { throw new
		 * Exception("Tried to send message while disconnected!"); }
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
				node.getLog().warn(
						"Received invalid packet message from peer with address "
								+ message.getSrc().toString() + ": " + message.getObject().toString());
				e.printStackTrace();
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
			// Send out events depending on the contents of the new view
			for (Address newPeer : Util.newMembers(curView.getMembers(), newView.getMembers())) {
				// Newly-connected peers
				if (newPeer.equals(jch.getAddress()))
					continue; // Don't trigger for our address, just in case
				for (JGroupsListener listener : listeners) {
					listener.onPeerJoin(newPeer);
				}
			}
			for (Address lostPeer : Util.leftMembers(curView, newView)) {
				// Lost peers
				if (lostPeer.equals(jch.getAddress()))
					continue;
				for (JGroupsListener listener : listeners) {
					listener.onPeerLeave(lostPeer);
				}
			}
			curView = newView;
		}

		public void suspect(Address mbr) {
			// We suspect a peer might have left. May not be called often with our
			// current protocol stack config.
			if (mbr.equals(jch.getAddress()))
				return; // Don't trigger for our address, just in case
			for (JGroupsListener listener : listeners) {
				listener.onPeerPossibleLeave(mbr);
			}
		}

	}

}

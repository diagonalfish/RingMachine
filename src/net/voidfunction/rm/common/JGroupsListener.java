package net.voidfunction.rm.common;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;


import org.jgroups.Address;

/**
 * Receiver for JGroups events, including connection, disconnection, messages,
 * errors, state receive/request, and peer joins/quits.
 */
public abstract class JGroupsListener {

	public void onConnect() {
	}

	public void onMessage(Address source, RMPacket message) {
	}

	public void initialPeers(List<Address> peers) {
	}

	public void onPeerJoin(Address newPeer) {
	}

	public void onPeerLeave(Address lostPeer) {
	}

	public void onPeerPossibleLeave(Address possibleLostPeer) {
	}

}

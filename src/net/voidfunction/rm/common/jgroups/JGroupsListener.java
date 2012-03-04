package net.voidfunction.rm.common.jgroups;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import net.voidfunction.rm.common.protocol.RMPacket;

import org.jgroups.Address;

/**
 * Receiver for JGroups events, including connection, disconnection, messages,
 * errors, state receive/request, and peer joins/quits.
 */
public abstract class JGroupsListener {

	public void onConnect() {}
	
	public void onDisconnect() {}

	public void onMessage(Address source, RMPacket message) {}
	
	public void onError(String message) {}
	
    public void stateRequested(OutputStream output) throws Exception {}

    public void stateReceived(InputStream input) throws Exception {}
    
    public void initialPeers(List<Address> peers) {}

    public void onPeerJoin(Address newPeer) {}
    
    public void onPeerLeave(Address lostPeer) {}

    public void onPeerPossibleLeave(Address possibleLostPeer) {}

}

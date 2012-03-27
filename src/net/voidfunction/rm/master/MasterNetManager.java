package net.voidfunction.rm.master;

import java.util.List;

import org.jgroups.Address;

import net.voidfunction.rm.common.JGroupsListener;
import net.voidfunction.rm.common.JGroupsManager;
import net.voidfunction.rm.common.RMLog;
import net.voidfunction.rm.common.RMPacket;

public class MasterNetManager extends JGroupsListener {
	
	private MasterNode node;
	private JGroupsManager jgm;
	private MasterPacketHandler packetHandler;
	
	public MasterNetManager(MasterNode node) {
		this.node = node;
		jgm = node.getJGroupsMgr();
		jgm.addListener(this);
		
		packetHandler = new MasterPacketHandler(node);
	}
	
	/* JGroups events */
	
	public void onConnect() {
		RMLog.info("Connected to cluster.");
	}
	
	public void initialPeers(List<Address> peers) {
		RMLog.info("Received initial cluster peer list (" + peers.size() + " peers).");
		for(Address worker : peers) {
			packetSendMasterInfo(worker);
		}
	}
	
	public void onPeerJoin(Address newPeer) {
		RMLog.info("New cluster peer: " + newPeer);
		packetSendMasterInfo(newPeer);
	}
	
	public void onPeerLeave(Address lostPeer) {
		//TODO
	}
	
	public void onMessage(Address source, RMPacket message) {
		packetHandler.handle(source, message);
	}
	
	/* Packet sending functions */
	
	public void packetSendMasterInfo(Address target) {
		RMLog.info("Sending MASTER_INFO to new worker " + target);
		RMPacket packetMaster = new RMPacket(RMPacket.Type.MASTER_INFO);
		packetMaster.setDataVal("httpport", node.getConfig().getInt("port.http", 8080));
		try {
			jgm.sendMessage(packetMaster, target);
		} catch (Exception e) {
			RMLog.severe("Could not send MASTER_INFO to " + target + ": " + e.getClass().getName() + " " + e.getMessage());
		}
	}
	
}
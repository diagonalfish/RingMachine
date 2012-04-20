package net.voidfunction.rm.master;

import java.util.List;

import org.jgroups.Address;

import net.voidfunction.rm.common.*;

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
		node.getLog().info("Connected to cluster.");
	}

	public void initialPeers(List<Address> peers) {
		node.getLog().info("Received initial cluster peer list (" + peers.size() + " peers).");
		for (Address worker : peers) {
			packetSendMasterInfo(worker);
		}
	}

	public void onPeerJoin(Address newPeer) {
		node.getLog().info("Cluster peer joined: " + newPeer);
		packetSendMasterInfo(newPeer);
	}

	public void onPeerLeave(Address lostPeer) {
		node.getLog().info("Cluster peer left: " + lostPeer);
		if (node.getWorkerDirectory().removeWorker(lostPeer))
			node.getLog().info("Removed worker " + lostPeer);
	}

	public void onMessage(Address source, RMPacket message) {
		packetHandler.handle(source, message);
	}

	/* Packet sending functions */

	public void packetSendMasterInfo(Address target) {
		node.getLog().info("Sending MASTER_INFO to new worker " + target);
		RMPacket packetMaster = new RMPacket(RMPacket.Type.MASTER_INFO);
		packetMaster.setDataVal("httpport", node.getConfig().getInt("port.http", 8080));
		try {
			jgm.sendMessage(packetMaster, target);
		} catch (Exception e) {
			node.getLog().severe(
				"Could not send MASTER_INFO to " + target + ": " + e.getClass().getName() + " "
					+ e.getMessage());
		}
	}

}

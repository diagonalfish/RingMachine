package net.voidfunction.rm.worker;

import java.util.List;

import org.jgroups.Address;

import net.voidfunction.rm.common.*;

public class WorkerNetManager extends JGroupsListener {

	private WorkerNode node;
	private JGroupsManager jgm;
	private WorkerPacketHandler packetHandler;

	public WorkerNetManager(WorkerNode node) {
		this.node = node;
		jgm = node.getJGroupsMgr();
		jgm.addListener(this);

		packetHandler = new WorkerPacketHandler(node);
	}

	/* JGroups events */

	public void onConnect() {
		node.getLog().info("Connected to cluster.");
	}

	public void initialPeers(List<Address> peers) {
		node.getLog().info("Received initial cluster peer list (" + peers.size() + " peers).");
		// No further action right now - not really useful to us. Nice to log,
		// though.
	}

	public void onPeerJoin(Address newPeer) {
		node.getLog().info("Cluster peer joined: " + newPeer);
		// No further action
	}

	public void onPeerLeave(Address lostPeer) {
		node.getLog().info("Cluster peer left: " + lostPeer);
		if (lostPeer.equals(node.getMasterAddr())) {
			node.getLog().warn("Master node left the network! Resetting master node information.");
			node.setMasterInfo(null, 0);
		}
	}

	public void onMessage(Address source, RMPacket message) {
		packetHandler.handle(source, message);
	}

	public void packetSendWorkerInfo(Address target) {
		node.getLog().info("Sending WORKER_INFO to master node.");
		RMPacket packetWorker = new RMPacket(RMPacket.Type.WORKER_INFO);
		packetWorker.setDataVal("httphost", node.getPublicIP());
		packetWorker.setDataVal("httpport", node.getConfig().getInt("port.http", 8080));
		try {
			jgm.sendMessage(packetWorker, target);
		} catch (Exception e) {
			node.getLog().severe(
				"Could not send WORKER_INFO to " + target + ": " + e.getClass().getName() + " "
					+ e.getMessage());
		}
	}

}

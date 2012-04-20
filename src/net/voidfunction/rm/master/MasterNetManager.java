package net.voidfunction.rm.master;

import java.util.ArrayList;
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
		RMPacket packet = new RMPacket(RMPacket.Type.MASTER_INFO);
		packet.setDataVal("httpport", node.getConfig().getInt("port.http", 8080));
		sendPacket(target, packet);
	}
	
	public void packetSendYourFiles(Address target, ArrayList<String> fileIds) {
		node.getLog().info("Sending YOUR_FILES to node " + target);
		RMPacket packet = new RMPacket(RMPacket.Type.YOUR_FILES);
		packet.setDataVal("files", fileIds);
		sendPacket(target, packet);
	}
	
	public void packetSendGetFile(Address target, RMFile file) {
		node.getLog().info("Sending GET_FILE to node " + target);
		RMPacket packet = new RMPacket(RMPacket.Type.GET_FILE);
		packet.setDataVal("file", file);
		sendPacket(target, packet);
	}
	
	public void packetSendMayRemoveFile(Address target, String fileId) {
		node.getLog().info("Sending MAY_REMOVE_FILE to node " + target);
		RMPacket packet = new RMPacket(RMPacket.Type.MAY_REMOVE_FILE);
		packet.setDataVal("fileid", fileId);
		sendPacket(target, packet);
	}
	
	public void packetSendDeleteFile(String fileId) {
		node.getLog().info("Broadcasting DELETE_FILE");
		RMPacket packet = new RMPacket(RMPacket.Type.DELETE_FILE);
		packet.setDataVal("fileid", fileId);
		broadcastPacket(packet);
	}
	
	private void sendPacket(Address target, RMPacket packet) {
		try {
			jgm.sendMessage(packet, target);
		} catch (Exception e) {
			node.getLog().severe(
				"Could not send " + packet.getType().toString() + " to " + target + ": " + e.getClass().getName() + " "
				+ e.getMessage());
		}
	}
	
	private void broadcastPacket(RMPacket packet) {
		try {
			jgm.sendMessage(packet);
		} catch (Exception e) {
			node.getLog().severe(
				"Could not broadcast " + packet.getType().toString() + ": " + e.getClass().getName() + " "
					+ e.getMessage());
		}
	}

}

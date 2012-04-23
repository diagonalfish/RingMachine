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

package net.voidfunction.rm.master;

import java.util.ArrayList;
import java.util.List;

import org.jgroups.Address;

import net.voidfunction.rm.common.*;

/**
 * Network overlay/protocol manager for master node. Receives events from JGroups
 * and handles sending packets as needed.
 */
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
		packet.setProperty("httpport", node.getConfig().getInt("port.http", 8080));
		sendPacket(target, packet);
	}
	
	public void packetSendYourFiles(Address target, ArrayList<String> fileIds) {
		node.getLog().info("Sending YOUR_FILES to node " + target);
		RMPacket packet = new RMPacket(RMPacket.Type.YOUR_FILES);
		packet.setProperty("files", fileIds);
		sendPacket(target, packet);
	}
	
	public void packetSendGetFile(Address target, RMFile file) {
		node.getLog().info("Sending GET_FILE to node " + target);
		RMPacket packet = new RMPacket(RMPacket.Type.GET_FILE);
		packet.setProperty("file", file);
		sendPacket(target, packet);
	}
	
	public void packetSendMayRemoveFile(Address target, String fileId) {
		node.getLog().info("Sending MAY_REMOVE_FILE to node " + target);
		RMPacket packet = new RMPacket(RMPacket.Type.MAY_REMOVE_FILE);
		packet.setProperty("fileid", fileId);
		sendPacket(target, packet);
	}
	
	public void packetSendDeleteFile(String fileId) {
		node.getLog().info("Broadcasting DELETE_FILE");
		RMPacket packet = new RMPacket(RMPacket.Type.DELETE_FILE);
		packet.setProperty("fileid", fileId);
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

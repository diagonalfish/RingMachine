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

package net.voidfunction.rm.worker;

import java.util.ArrayList;
import java.util.List;

import org.jgroups.Address;

import net.voidfunction.rm.common.*;

/**
 * Network overlay/protocol manager for worker node. Receives events from JGroups
 * and handles sending packets as needed.
 */
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
		RMPacket packet = new RMPacket(RMPacket.Type.WORKER_INFO);
		packet.setProperty("httphost", node.getPublicIP());
		packet.setProperty("httpport", node.getConfig().getInt("port.http", 8080));
		sendPacket(target, packet);
	}

	public void packetSendMyFiles(Address target) {
		node.getLog().info("Sending MY_FILES to master node.");
		ArrayList<String> fileIds = new ArrayList<String>();
		RMPacket packet = new RMPacket(RMPacket.Type.MY_FILES);
		for (RMFile file : node.getFileRepository().getFileObjects()) {
			fileIds.add(file.getId());
		}
		packet.setProperty("files", fileIds);
		sendPacket(target, packet);
	}
	
	public void packetSendGotFile(Address target, String fileid) {
		node.getLog().info("Sending GOT_FILE to master node.");
		RMPacket packet = new RMPacket(RMPacket.Type.GOT_FILE);
		packet.setProperty("fileid", fileid);
		sendPacket(target, packet);
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

}

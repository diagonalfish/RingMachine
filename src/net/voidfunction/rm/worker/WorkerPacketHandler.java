package net.voidfunction.rm.worker;

import net.voidfunction.rm.common.RMPacket;

import org.jgroups.Address;

public class WorkerPacketHandler {
	private WorkerNode node;
	
	public WorkerPacketHandler(WorkerNode node) {
		this.node = node;
	}
	
	public void handle(Address source, RMPacket packet) {
		RMPacket.Type type = packet.getType();
		switch(type) {
		case MASTER_INFO:
			handle_MASTER_INFO(source, packet);
			break;
		default:
			node.getLog().warn("Received unusable packet of type " + type.name() + " from node " + source + ".");
		}
	}
	
	private void handle_MASTER_INFO(Address source, RMPacket packet) {
		node.getLog().info("Received MASTER_INFO from node " + source + ". Port: " + packet.getInteger("httpport"));
		
		// Store the master node's port
		node.setMasterPort(packet.getInteger("httpport"));
		
		// Inform the Master node about our HTTP ip/port
		node.getNetManager().packetSendWorkerInfo(source);
	}
	
}

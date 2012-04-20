package net.voidfunction.rm.master;

import org.jgroups.Address;

import net.voidfunction.rm.common.RMPacket;

public class MasterPacketHandler {

	private MasterNode node;

	public MasterPacketHandler(MasterNode node) {
		this.node = node;
	}

	public void handle(Address source, RMPacket packet) {
		RMPacket.Type type = packet.getType();
		switch (type) {
		case WORKER_INFO:
			handle_WORKER_INFO(source, packet);
			break;
		default:
			node.getLog().warn(
				"Received unusable packet of type " + type.name() + " from node " + source + ".");
		}
	}

	private void handle_WORKER_INFO(Address source, RMPacket packet) {
		node.getLog().info(
			"Received WORKER_INFO from node " + source + ": " + packet.getString("httphost") + ":"
				+ packet.getInteger("httpport"));
		node.getWorkerDirectory().addWorker(source, packet.getString("httphost"),
			packet.getInteger("httpport"));
	}

}

package net.voidfunction.rm.worker;

import java.io.IOException;
import java.util.List;

import net.voidfunction.rm.common.*;

import org.jgroups.Address;

public class WorkerPacketHandler {
	private WorkerNode node;

	public WorkerPacketHandler(WorkerNode node) {
		this.node = node;
	}

	public void handle(Address source, RMPacket packet) {
		RMPacket.Type type = packet.getType();
		switch (type) {
		case MASTER_INFO:
			handle_MASTER_INFO(source, packet);
			break;
		case YOUR_FILES:
			handle_YOUR_FILES(source, packet);
			break;
		case GET_FILE:
			handle_GET_FILE(source, packet);
			break;
		case DELETE_FILE:
			handle_DELETE_FILE(source, packet);
			break;
		default:
			node.getLog().warn(
				"Received unusable packet of type " + type.name() + " from node " + source + ".");
		}
	}

	private void handle_MASTER_INFO(Address source, RMPacket packet) {
		node.getLog().info(
			"Received MASTER_INFO from node " + source + ". Port: " + packet.getInteger("httpport"));

		// Store the master node's info
		node.setMasterInfo(source, packet.getInteger("httpport"));

		// Inform the Master node about our HTTP ip/port
		node.getNetManager().packetSendWorkerInfo(source);

		// Send MY_FILES packet to master node
		if (node.getFileRepository().getFileCount() > 0)
			node.getNetManager().packetSendMyFiles(source);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void handle_YOUR_FILES(Address source, RMPacket packet) {
		node.getLog().info("Received YOUR_FILES from node " + source + ".");
		if (!source.equals(node.getMasterAddr())) {
			node.getLog().warn("Received YOUR_FILES from a non-master node!");
			return;
		}
		
		List<Object> tempKeepFiles = packet.getList("files");
		if (tempKeepFiles == null) return;
		List<String> keepFiles = (List<String>)(List)tempKeepFiles; // Type erasure...
		try {
			int deleted = node.getFileRepository().removeAllExcept(keepFiles);
			node.getLog().info("Removed " + deleted + " unneeded files.");
		} catch (IOException e) {
			node.getLog().warn("Error removing file: " + e.getMessage());
		}
	}
	
	private void handle_GET_FILE(Address source, RMPacket packet) {
		node.getLog().info("Received GET_FILE from node " + source + ".");
		RMFile file = packet.getFile("file");
		
		FileFetcher fetcher = new FileFetcher(node, file);
		fetcher.start();
	}

	private void handle_DELETE_FILE(Address source, RMPacket packet) {
		node.getLog().info("Received DELETE_FILE from node " + source + ".");
		String fileId = packet.getString("fileid");
		
		if (node.getFileRepository().checkFile(fileId)) {
			node.getLog().info("Deleting file " + fileId + " by request of master node.");
			try {
				node.getFileRepository().removeFile(fileId);
			} catch (IOException e) {
				// Oops :(
				node.getLog().severe("Failed to delete file " + fileId + ": " + e.getMessage());
			}
		}
	}
	
}

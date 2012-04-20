package net.voidfunction.rm.master;

import java.util.ArrayList;
import java.util.List;

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
		case MY_FILES:
			handle_MY_FILES(source, packet);
			break;
		case GOT_FILE:
			handle_GOT_FILE(source, packet);
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
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void handle_MY_FILES(Address source, RMPacket packet) {
		node.getLog().info("Received MY_FILES from node " + source + ".");
		
		ArrayList<String> keepFiles = new ArrayList<String>();
		
		List<Object> tempKeepFiles = packet.getList("files");
		if (tempKeepFiles == null) return;
		List<String> workerFiles = (List<String>)(List)tempKeepFiles; // Type erasure...
		
		// Filter out the files that the worker should keep, and make note of the ones they have.
		for (String workerFile : workerFiles) {
			if (node.getFileRepository().checkFile(workerFile)) {
				keepFiles.add(workerFile);
				node.getWorkerDirectory().addWorkerFile(source, workerFile);
			}
		}
		
		node.getNetManager().packetSendYourFiles(source, keepFiles);
	}
	
	private void handle_GOT_FILE(Address source, RMPacket packet) {
		node.getLog().info("Received GOT_FILE from node " + source + ".");
		
		String fileId = packet.getString("fileid");
		node.getWorkerDirectory().addWorkerFile(source, fileId);
	}

}

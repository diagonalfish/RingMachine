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

import java.io.IOException;
import java.util.List;

import net.voidfunction.rm.common.*;

import org.jgroups.Address;

/**
 * Handles incoming packets for the worker node and performs related tasks.
 * 
 * See RMPacket for details about packet types.
 */
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
	
	private void handle_YOUR_FILES(Address source, RMPacket packet) {
		// Received a filtered list of files back from the master node.
		node.getLog().info("Received YOUR_FILES from node " + source + ".");
		if (!source.equals(node.getMasterAddr())) {
			node.getLog().warn("Received YOUR_FILES from a non-master node!");
			return;
		}
		
		// Keep only the files that are on this list, delete the rest
		List<Object> keepFiles = packet.getList("files");
		if (keepFiles == null) return;
		try {
			int deleted = node.getFileRepository().removeAllExcept(keepFiles);
			node.getLog().info("Removed " + deleted + " unneeded files.");
		} catch (IOException e) {
			node.getLog().warn("Error removing file: " + e.getMessage());
		}
	}
	
	private void handle_GET_FILE(Address source, RMPacket packet) {
		// Master node is asking us to fetch a file and add it to our repository
		node.getLog().info("Received GET_FILE from node " + source + ".");
		RMFile file = packet.getFile("file");
		
		if (node.getFileRepository().checkFile(file.getId()))
			// We already have that file
			node.getNetManager().packetSendGotFile(source, file.getId());
		
		else {
			FileFetcher fetcher = new FileFetcher(node, file);
			fetcher.start();
		}
	}

	private void handle_DELETE_FILE(Address source, RMPacket packet) {
		// Master node is instructing us to immediately delete a file from our repository.
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

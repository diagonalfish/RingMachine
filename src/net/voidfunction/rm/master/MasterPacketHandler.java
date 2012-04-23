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

import net.voidfunction.rm.common.RMPacket;

/**
 * Handles incoming packets for the master node and performs related tasks.
 * 
 * See RMPacket for details about packet types.
 */
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
		// New worker node! Store it in the worker directory for later use
		node.getLog().info(
			"Received WORKER_INFO from node " + source + ": " + packet.getString("httphost") + ":"
			+ packet.getInteger("httpport"));
		node.getWorkerDirectory().addWorker(source, packet.getString("httphost"),
			packet.getInteger("httpport"));
	}
	
	private void handle_MY_FILES(Address source, RMPacket packet) {
		node.getLog().info("Received MY_FILES from node " + source + ".");
		
		ArrayList<String> keepFiles = new ArrayList<String>();
		
		List<Object> workerFiles = packet.getList("files");
		if (workerFiles == null) return;
		
		// Filter out the files that the worker should keep, and make note of the ones they have.
		for (Object workerFileObj : workerFiles) {
			if (!(workerFileObj instanceof String))
				return;
			String workerFile = (String)workerFileObj;
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

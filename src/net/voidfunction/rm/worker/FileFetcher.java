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
import java.net.*;
import java.util.HashMap;

import org.jgroups.Address;

import net.voidfunction.rm.common.*;

/**
 * Threaded HTTP client which retrieves a file from the master node
 * and adds it to our FileRepository upon success.
 */
public class FileFetcher extends Thread {

	// We actively try to prevent multiple fetchers being active for
	// a given file ID
	private static HashMap<String, FileFetcher> activeFetchers;
	
	static {
		activeFetchers = new HashMap<String, FileFetcher>();
	}
	
	private static boolean fetcherStart(String fileId, FileFetcher fetcher) {
		synchronized(activeFetchers) {
			if (activeFetchers.containsKey(fileId))
				return false;
			activeFetchers.put(fileId, fetcher);
			return true;
		}
	}
	
	private static void fetcherEnd(String fileId) {
		synchronized(activeFetchers) {
			activeFetchers.remove(fileId);
		}
	}
	
	private WorkerNode node;
	private RMFile file;
	
	/**
	 * Create a file fetcher that will retrieve the given file from the master node
	 * of the network.
	 * @param node
	 * @param file
	 */
	public FileFetcher(WorkerNode node, RMFile file) {
		this.node = node;
		this.file = file;
	}
	
	public void run() {
		node.getLog().info("Running FileFetcher for file " + file.getId());
		
		if (!fetcherStart(file.getId(), this)) {
			node.getLog().warn("FileFetcher for " + file.getId() + " did not start: fetcher already active for this file ID.");
			fetcherEnd(file.getId());
			return;
		}
		
		// Construct the url
		String masterHost = node.getConfig().getString("master.host", null);
		Address masterAddr = node.getMasterAddr();
		if (masterHost == null || masterAddr == null) {
			node.getLog().warn("FileFetcher for " + file.getId() + " did not start: missing master node data.");
			fetcherEnd(file.getId());
			return;
		}
		int masterPort = node.getMasterPort();
		URL url = null;
		try {
			url = new URL("http://" + masterHost + ":" + masterPort + "/files/" + file.getId() + "/Worker-Download");
		} catch (MalformedURLException e1) {
			// Shouldn't happen :(
			node.getLog().warn("FileFetcher for " + file.getId() + " failed: URL construction failed.");
			fetcherEnd(file.getId());
			return;
		}
		
		// Create a URLConnection and download
		URLConnection conn = null;
		try {
			conn = url.openConnection();
			if (!(conn instanceof HttpURLConnection))
				throw new IOException("Connection creation went awry");
			int code = ((HttpURLConnection)conn).getResponseCode();
			if (code != 200)
				throw new IOException("HTTP status code != 200 (" + code + ")");
			
			// Send the file along to the file repository
			node.getFileRepository().addFile(file, conn.getInputStream());
			
			node.getLog().info("Successfully downloaded file " + file.getId());
		} catch (IOException e) {
			node.getLog().warn("FileFetcher for " + file.getId() + " failed: " + e.getMessage());
			fetcherEnd(file.getId());
			return;
		}
		
		node.getNetManager().packetSendGotFile(node.getMasterAddr(), file.getId());
		fetcherEnd(file.getId());
	}
}

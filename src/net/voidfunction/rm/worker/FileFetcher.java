package net.voidfunction.rm.worker;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;

import org.jgroups.Address;

import net.voidfunction.rm.common.*;

public class FileFetcher extends Thread {

	private static HashMap<String, FileFetcher> activeFetchers;
	
	static {
		activeFetchers = new HashMap<String, FileFetcher>();
	}
	
	public static boolean fetcherStart(String fileId, FileFetcher fetcher) {
		synchronized(activeFetchers) {
			if (activeFetchers.containsKey(fileId))
				return false;
			activeFetchers.put(fileId, fetcher);
			return true;
		}
	}
	
	public static void fetcherEnd(String fileId) {
		synchronized(activeFetchers) {
			activeFetchers.remove(fileId);
		}
	}
	
	private WorkerNode node;
	private RMFile file;
	
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
			url = new URL("http://" + masterHost + ":" + masterPort + "/files/" + file.getId() + "/");
		} catch (MalformedURLException e1) {
			// Shouldn't happen :(
			node.getLog().warn("FileFetcher for " + file.getId() + " failed: URL construction failed.");
			fetcherEnd(file.getId());
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
		}
		
		node.getNetManager().packetSendGotFile(node.getMasterAddr(), file.getId());
		fetcherEnd(file.getId());
	}
}

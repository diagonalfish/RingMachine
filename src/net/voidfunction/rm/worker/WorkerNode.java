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
import java.util.Timer;
import java.util.TimerTask;

import org.jgroups.Address;

import net.voidfunction.rm.common.*;

/**
 * Main class for RingMachine worker node.
 */
public class WorkerNode extends Node {

	private WorkerNetManager netManager;

	private Address masterAddr = null;
	private int masterPort = 0;

	public static void main(String[] args) {
		new WorkerNode().start();
	}

	public WorkerNode() {
		super("worker.properties", "./workerfiles");
	}

	public void start() {
		node.getLog().raw("RingMachine Worker Node v0.1 starting up...");

		// Load file repository
		try {
			fileRep.loadFiles();
		} catch (IOException e) {
			node.getLog().fatal(
				"Could not load FileRepository data file! Check that " + fileRep.getDataFileName()
					+ " is accessible and not corrupted!");
			e.printStackTrace();
			System.exit(1);
		}

		// Grab data we need from the config fi;e
		int P2Pport = config.getInt("port.p2p", 1600);
		String masterHost = config.getString("master.host", null);
		int masterPort = config.getInt("master.port", 0);

		// Get our external IP address
		publicIP = config.getString("ip.public", null);
		if (publicIP == null) {
			node.getLog().info("Getting public IP address...");
			String extURL = config.getString("ip.service", null);
			if (extURL == null) {
				// Use master node's IP server
				node.getLog().info("Contacting master node's IP address server...");
				extURL = "http://" + masterHost + ":" + (masterPort + 2) + "/";
			} else
				// use remote service defined in config
				node.getLog().info("Using remote IP address service...");
			try {
				publicIP = new IPAddressClient(extURL).getMyIP();
			} catch (IOException e) {
				node.getLog()
					.fatal(
						"Failed to retrieve public IP address! " + e.getClass().getName() + ": "
							+ e.getMessage());
				System.exit(1);
			}
		}
		node.getLog().info("Public IP address is " + publicIP);

		// Create JGroupsManager and WorkerNetManager
		String password = config.getString("password", null);
		if (password == null) {
			getLog().fatal(
				"Password not defined! Set password=<pass> in the config file before starting the program.");
			System.exit(1);
		}
		jgm = new JGroupsManager(this, P2Pport, publicIP, masterHost, masterPort, password);
		netManager = new WorkerNetManager(this);

		// Start up JGroups engine and connect to master node
		node.getLog().info("Starting P2P engine...");
		try {
			jgm.connect();
		} catch (Exception e) {
			node.getLog()
				.fatal("Failed to start JGroups! " + e.getClass().getName() + " - " + e.getMessage());
			System.exit(1);
		}
		// Set a timer to check the master node's file list every so often
		Timer filesCheckTimer = new Timer();
		filesCheckTimer.schedule(new MyFilesTask(), 180000, 180000); // 3 minutes
		
		// Create web server
		int httpPort = config.getInt("port.http", 8080);
		getLog().info("Starting HTTP server on port " + httpPort + "...");
		RMHTTPServer httpserver = new RMHTTPServer(httpPort);
		
		// File servlet
		httpserver.addServlet("/files/*", new FileServlet(this, null, null));
		
		// Run web server
		try {
			httpserver.run();
		} catch (Exception e) {
			getLog().fatal("Failed to start HTTP server! " + e.getClass().getName() + " - " + e.getMessage());
			System.exit(1);
		}
		getLog().info("HTTP server started.");

		// Console
		NodeConsole console;
		NodeConsoleHandler handler = new WorkerConsoleHandler();
		try {
			console = new NodeConsole(handler);
			log.setConsole(console);
			console.run();
		} catch (IOException e) {
		}

	}

	/**
	 * Retrieve this node's network manager
	 */
	public WorkerNetManager getNetManager() {
		return netManager;
	}

	/**
	 * Set the master node host and port
	 * @param addr
	 * @param port
	 */
	public synchronized void setMasterInfo(Address addr, int port) {
		masterAddr = addr;
		masterPort = port;
	}

	/**
	 * Get the Address of the master node, for sending packets
	 * @return
	 */
	public synchronized Address getMasterAddr() {
		return masterAddr;
	}

	/**
	 * Get the HTTP server port of the master node.
	 * @return
	 */
	public synchronized int getMasterPort() {
		return masterPort;
	}
	
	// Check with the master node periodically re: file list
	private class MyFilesTask extends TimerTask {
		public void run() {
			if (masterAddr != null && getFileRepository().getFileCount() > 0)
				getNetManager().packetSendMyFiles(masterAddr);
		}
	}

}

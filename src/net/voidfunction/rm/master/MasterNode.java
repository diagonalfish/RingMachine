package net.voidfunction.rm.master;

import java.io.IOException;

import net.voidfunction.rm.common.IPAddressClient;
import net.voidfunction.rm.common.JGroupsManager;
import net.voidfunction.rm.common.Node;
import net.voidfunction.rm.common.RMLog;

/**
 * Main class for RingMachine master node.
 */
public class MasterNode extends Node {

	private RMGossipRouter grouter;
	private IPAddressServer ipserver;

	public static void main(String[] args) {
		new MasterNode().start();
	}

	public MasterNode() {
		super("master.properties");
	}

	public void start() {
		RMLog.raw("RingMachine Master Node v0.1 starting up...");

		// Create utility objects
		// TODO
		
		int baseP2Pport = config.getInt("port.p2p", 1600);

		// Start gossip router
		RMLog.info("Starting gossip router on port " + (baseP2Pport + 1) + "...");
		grouter = new RMGossipRouter(baseP2Pport + 1);
		grouter.setExpiryTime(60000);
		try {
			grouter.start();
		} catch (Exception e) {
			// Apparently we failed to start the gossip router
			RMLog.fatal("Failed to start the gossip router! " + e.getClass().getName() + ": "
					+ e.getMessage());
			System.exit(1);
		}
		RMLog.info("Gossip router started.");

		// Start IP address server
		RMLog.info("Starting IP address server on port " + (baseP2Pport + 2) + "...");
		ipserver = new IPAddressServer(baseP2Pport + 2);
		try {
			ipserver.start();
		} catch (IOException e) {
			// Apparently we failed to start the gossip router
			RMLog.fatal("Failed to start the ip address server! " + e.getClass().getName() + ": "
					+ e.getMessage());
			System.exit(1);
		}
		RMLog.info("IP address server started.");

		// Get our external IP address
		publicIP = config.getString("ip.bind", null);
		if (publicIP == null) {
			RMLog.info("Getting public IP address from remote service...");
			String extURL = config.getString("ip.service", "");
			try {
				publicIP = new IPAddressClient(extURL).getMyIP();
			} catch (IOException e) {
				RMLog.fatal("Failed to retrieve public IP address! " + e.getClass().getName() + ": "
						+ e.getMessage());
				System.exit(1);
			}
		}
		RMLog.info("Public IP address is " + publicIP);
		
		// Create JGroupsManager object, configured to connect to our own gossip router
		jgm = new JGroupsManager(baseP2Pport, publicIP, "localhost", baseP2Pport + 1);
		
		// Set up net manager
		
		// Start JGroups
		try {
			jgm.connect();
		} catch (Exception e) {
			RMLog.fatal("Failed to start JGroups! " + e.getClass().getName() + " - " + e.getMessage());
			System.exit(1);
		}
	}
}

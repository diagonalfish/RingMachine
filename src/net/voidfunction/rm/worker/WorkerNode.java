package net.voidfunction.rm.worker;

import java.io.IOException;

import net.voidfunction.rm.common.IPAddressClient;
import net.voidfunction.rm.common.JGroupsManager;
import net.voidfunction.rm.common.Node;
import net.voidfunction.rm.common.RMLog;

/**
 * Main class for RingMachine worker node.
 */
public class WorkerNode extends Node {

	private WorkerNetManager netManager;
	
	public static void main(String[] args) {
		new WorkerNode().start();
	}

	public WorkerNode() {
		super("worker.properties");
	}

	public void start() {
		RMLog.raw("RingMachine Worker Node v0.1 starting up...");
		
		// Load file repository
		try {
			fileRep.loadFiles();
		} catch (IOException e) {
			RMLog.fatal("Could not load FileRepository data file! Check that " + fileRep.getDataFileName() + 
				" is accessible and not corrupted!");
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
			RMLog.info("Getting public IP address...");
			String extURL = config.getString("ip.service", null);
			if (extURL == null) {
				// Use master node's IP server
				RMLog.info("Contacting master node's IP address server...");
				extURL = "http://" + masterHost + ":" + (masterPort + 2) + "/";
			}
			else // use remote service defined in config
				RMLog.info("Using remote IP address service..."); 
			try {
				publicIP = new IPAddressClient(extURL).getMyIP();
			} catch (IOException e) {
				RMLog.fatal("Failed to retrieve public IP address! " + e.getClass().getName() + ": "
						+ e.getMessage());
				System.exit(1);
			}
		}
		RMLog.info("Public IP address is " + publicIP);
		
		// Create JGroupsManager and WorkerNetManager
		jgm = new JGroupsManager(P2Pport, publicIP, masterHost, masterPort);
		netManager = new WorkerNetManager(this);
		
		// Start up JGroups engine and connect to master node
		RMLog.info("Starting P2P engine...");
		try {
			jgm.connect();
		} catch (Exception e) {
			RMLog.fatal("Failed to start JGroups! " + e.getClass().getName() + " - " + e.getMessage());
			System.exit(1);
		}
	}
	
	public WorkerNetManager getNetManager() {
		return netManager;
	}

}

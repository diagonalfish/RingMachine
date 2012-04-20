package net.voidfunction.rm.worker;

import java.io.IOException;

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
		super("worker.properties");
	}

	public void start() {
		node.getLog().raw("RingMachine Worker Node v0.1 starting up...");
		
		// Load file repository
		try {
			fileRep.loadFiles();
		} catch (IOException e) {
			node.getLog().fatal("Could not load FileRepository data file! Check that " + fileRep.getDataFileName() + 
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
			node.getLog().info("Getting public IP address...");
			String extURL = config.getString("ip.service", null);
			if (extURL == null) {
				// Use master node's IP server
				node.getLog().info("Contacting master node's IP address server...");
				extURL = "http://" + masterHost + ":" + (masterPort + 2) + "/";
			}
			else // use remote service defined in config
				node.getLog().info("Using remote IP address service..."); 
			try {
				publicIP = new IPAddressClient(extURL).getMyIP();
			} catch (IOException e) {
				node.getLog().fatal("Failed to retrieve public IP address! " + e.getClass().getName() + ": "
						+ e.getMessage());
				System.exit(1);
			}
		}
		node.getLog().info("Public IP address is " + publicIP);
		
		// Create JGroupsManager and WorkerNetManager
		String password = config.getString("password", null);
		if (password == null) {
			getLog().fatal("Password not defined! Set password=<pass> in the config file before starting the program.");
			System.exit(1);
		}
		jgm = new JGroupsManager(this, P2Pport, publicIP, masterHost, masterPort, password);
		netManager = new WorkerNetManager(this);
		
		// Start up JGroups engine and connect to master node
		node.getLog().info("Starting P2P engine...");
		try {
			jgm.connect();
		} catch (Exception e) {
			node.getLog().fatal("Failed to start JGroups! " + e.getClass().getName() + " - " + e.getMessage());
			System.exit(1);
		}
		
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
	
	public WorkerNetManager getNetManager() {
		return netManager;
	}
	
	public void setMasterInfo(Address addr, int port) {
		masterAddr = addr;
		masterPort = port;
	}
	
	public Address getMasterAddr() {
		return masterAddr;
	}
	
	public int getMasterPort() {
		return masterPort;
	}

}

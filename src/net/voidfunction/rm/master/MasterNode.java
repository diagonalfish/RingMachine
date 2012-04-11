package net.voidfunction.rm.master;

import java.io.IOException;

import net.voidfunction.rm.common.IPAddressClient;
import net.voidfunction.rm.common.JGroupsManager;
import net.voidfunction.rm.common.Node;
import net.voidfunction.rm.common.NodeConsole;
import net.voidfunction.rm.common.NodeConsoleHandler;
import net.voidfunction.rm.common.RMHTTPServer;

/**
 * Main class for RingMachine master node.
 */
public class MasterNode extends Node {

	private RMGossipRouter grouter;
	private IPAddressServer ipserver;
	
	private MasterNetManager netManager;
	
	// Worker directory and File repository
	private WorkerDirectory workerDir;

	public static void main(String[] args) {
		new MasterNode().start();
	}

	public MasterNode() {
		super("master.properties");
	}

	public void start() {
		getLog().raw("RingMachine Master Node v0.1 starting up...");

		// Load our file repository
		try {
			fileRep.loadFiles();
		} catch (IOException e) {
			getLog().fatal("Could not load FileRepository data file! Check that " + fileRep.getDataFileName() + 
				" is accessible and not corrupted!");
			e.printStackTrace();
			System.exit(1);
		}
		
		// Begin starting our network services
		int baseP2Pport = config.getInt("port.p2p", 1600);

		// Start gossip router
		getLog().info("Starting gossip router on port " + (baseP2Pport) + "...");
		grouter = new RMGossipRouter(baseP2Pport);
		grouter.setExpiryTime(60000);
		try {
			grouter.start();
		} catch (Exception e) {
			// Apparently we failed to start the gossip router
			getLog().fatal("Failed to start the gossip router! " + e.getClass().getName() + ": "
					+ e.getMessage());
			System.exit(1);
		}
		getLog().info("Gossip router started.");

		// Start IP address server
		getLog().info("Starting IP address server on port " + (baseP2Pport + 2) + "...");
		ipserver = new IPAddressServer(baseP2Pport + 2);
		try {
			ipserver.start();
		} catch (IOException e) {
			// Apparently we failed to start the gossip router
			getLog().fatal("Failed to start the ip address server! " + e.getClass().getName() + ": "
					+ e.getMessage());
			System.exit(1);
		}
		getLog().info("IP address server started.");

		// Get our external IP address
		publicIP = config.getString("ip.public", null);
		if (publicIP == null) {
			getLog().info("Getting public IP address from remote service...");
			String extURL = config.getString("ip.service", "");
			try {
				publicIP = new IPAddressClient(extURL).getMyIP();
			} catch (IOException e) {
				getLog().fatal("Failed to retrieve public IP address! " + e.getClass().getName() + ": "
						+ e.getMessage());
				System.exit(1);
			}
		}
		getLog().info("Public IP address is " + publicIP);
		
		// Create a WorkerDirectory
		workerDir = new WorkerDirectory(fileRep);
		
		// Create JGroupsManager object, configured to connect to our own gossip router
		String password = config.getString("password", null);
		if (password == null) {
			getLog().fatal("Password not defined! Set password=<pass> in the config file before starting the program.");
			System.exit(1);
		}
		jgm = new JGroupsManager(this, baseP2Pport + 1, publicIP, "localhost", baseP2Pport, password);
		
		// Set up net listener
		netManager = new MasterNetManager(this);
		
		// Start JGroups
		
		try {
			jgm.connect();
		} catch (Exception e) {
			getLog().fatal("Failed to start JGroups! " + e.getClass().getName() + " - " + e.getMessage());
			System.exit(1);
		}
		
		// Start web server
		int httpPort = config.getInt("port.http", 8080);
		getLog().info("Starting HTTP server on port " + httpPort + "...");
		RMHTTPServer httpserver = new RMHTTPServer(httpPort);
		httpserver.addServlet("/admin/*", new AdminServlet(this, "admintemplates/"));
		try {
			httpserver.run();
		} catch (Exception e) {
			getLog().fatal("Failed to start HTTP server! " + e.getClass().getName() + " - " + e.getMessage());
			System.exit(1);
		}
		getLog().info("HTTP server started.");
		
		// Console
		NodeConsole console;
		NodeConsoleHandler handler = new MasterConsoleHandler();
		try {
			console = new NodeConsole(handler);
			log.setConsole(console);
			console.run();
		} catch (IOException e) {
		}
	
	}
	
	public MasterNetManager getNetManager() {
		return netManager;
	}

	public WorkerDirectory getWorkerDirectory() {
		return workerDir;
	}
	
}

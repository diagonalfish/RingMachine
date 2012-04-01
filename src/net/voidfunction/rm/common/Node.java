package net.voidfunction.rm.common;

import org.apache.log4j.PropertyConfigurator;

/**
 * Represents a generic network node with a configuration and public IP; provides
 * getter for class instance (singleton)
 */
public abstract class Node {

	protected RMConfig config;
	protected String publicIP;
	protected JGroupsManager jgm;
	protected FileRepository fileRep;
	
	protected static Node node;

	public Node(String configFile) {
		config = new RMConfig();
		config.safeLoad(configFile);
		node = this;
		
		fileRep = new FileRepository("./files");
		
		PropertyConfigurator.configure("log4j.properties");
	}
	
	public RMConfig getConfig() {
		return config;
	}
	
	public String getPublicIP() {
		return publicIP;
	}
	
	public JGroupsManager getJGroupsMgr() {
		return jgm;
	}
	
	public FileRepository getFileRepository() {
		return fileRep;
	}

}

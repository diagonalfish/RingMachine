package net.voidfunction.rm.common;

/**
 * Represents a generic network node with a configuration and public IP; provides
 * getter for class instance (singleton)
 */
public abstract class Node {

	protected RMConfig config;
	protected String publicIP;
	protected JGroupsManager jgm;
	
	protected static Node node;

	public Node(String configFile) {
		config = new RMConfig();
		config.safeLoad(configFile);
		node = this;
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
	
	// Static singleton getter
	public static Node getInstance() {
		return node;
	}

}

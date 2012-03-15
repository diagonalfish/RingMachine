package net.voidfunction.rm.common;

/**
 * Represents a generic network node with a configuration.
 */
public abstract class Node {

	protected RMConfig config;

	public Node(String configFile) {
		config = new RMConfig();
		config.safeLoad(configFile);
	}

	public RMConfig getConfig() {
		return config;
	}

}

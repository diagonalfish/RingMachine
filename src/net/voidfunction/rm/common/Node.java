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

package net.voidfunction.rm.common;

import org.apache.log4j.PropertyConfigurator;

/**
 * Represents a generic network node with a configuration, public IP,
 * JGroupsManager, and FileRepository. Used as the base class for both types
 * of nodes.
 */
public abstract class Node {

	protected RMConfig config;
	protected String publicIP;
	protected JGroupsManager jgm;
	protected FileRepository fileRep;

	protected RMLog log;

	protected static Node node;

	/**
	 * Create a new node, with the given config file and which stores
	 * its files in the given fileDir.
	 * @param configFile
	 * @param fileDir
	 */
	public Node(String configFile, String fileDir) {
		config = new RMConfig(this);
		config.safeLoad(configFile);
		log = new RMLog();
		node = this;

		fileRep = new FileRepository(this, fileDir);

		PropertyConfigurator.configure("log4j.properties");
	}

	/**
	 * Return this node's RMConfig object.
	 * @return
	 */
	public RMConfig getConfig() {
		return config;
	}

	/**
	 * Return this node's RMLog object.
	 * @return
	 */
	public RMLog getLog() {
		return log;
	}

	/**
	 * Return this node's public IPV4 address as a String.
	 * @return
	 */
	public String getPublicIP() {
		return publicIP;
	}

	/**
	 * Return this node's JGroupsManager.
	 * @return
	 */
	public JGroupsManager getJGroupsMgr() {
		return jgm;
	}

	/**
	 * Return this node's FileRepository.
	 * @return
	 */
	public FileRepository getFileRepository() {
		return fileRep;
	}

}

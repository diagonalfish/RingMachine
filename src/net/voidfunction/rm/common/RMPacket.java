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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A flexible, serializable packet type which consists of a type and a
 * String->Serializable Map.
 */
public class RMPacket implements Serializable {

	public static enum Type {
		/**
		 * Identifies the sender as a master node on the network. Sent to a
		 * newly-joined peer. Supercedes whatever setting the worker node has
		 * for master.
		 * Fields:
		 * - httpport: int, port number for the master node's http server
		 */
		MASTER_INFO,

		/**
		 * Identifies the sender as a worker node. Sent by a worker to the
		 * master node as soon as the worker finds out who the master is.
		 * Fields:
		 * - httpport: int, port number for the worker node's http server
		 * - httphost: String, worker's public IP or hostname
		 */
		WORKER_INFO,

		/**
		 * Sent by a worker to the master node. Contains a list of file IDs that
		 * the worker has cached. Used by master node to determine which files
		 * should be kept and to quickly get an idea of which files the worker
		 * has already.
		 * Fields:
		 * - files: List\<String\>, list of file IDs
		 */
		MY_FILES,

		/**
		 * Sent by a master to all worker nodes. The master filters the worker's
		 * file list by which files actually still exist, and sends the filtered
		 * list back to the worker, indicating which files the worker has which
		 * actually exist in the master's index. The worker should delete any
		 * files which don't appear on this list.
		 * Fields: 
		 * - files: List\<String\>, list of file IDs that the worker node has which should be kept.
		 */
		YOUR_FILES,

		/**
		 * Sent by a master to a worker. Specifically requests that the worker
		 * grab this file and add it to the files it serves.
		 * Fields:
		 * - file: RMFile, identifies the file to request from the server.
		 */
		GET_FILE,

		/**
		 * Sent by a worker to the master - informs that this node has added
		 * this file to its index. Fields: 
		 * - fileid: String, file ID
		 */
		GOT_FILE,

		/**
		 * Sent by master to worker. Informs that this node no longer needs to
		 * have this file in its index. The worker does not need to act upon
		 * this, but may choose to up until it receives a GET_FILE. When/if it does,
		 * it should send back a matching REMOVED_FILE.
		 * Fields:
		 * - fileid: String, file id
		 */
		MAY_REMOVE_FILE,
		
		/**
		 * Sent by a worker to the master - informs the server that this worker
		 * no longer has this file in its index. Must only be sent after receiving
		 * a MAY_REMOVE_FILE for this file.
		 * Fields:
		 * - fileid: String, file id
		 */
		REMOVED_FILE,

		/**
		 * Sent by a master to all workers. Announces that this file has been
		 * removed from the worker's index and should be deleted immediately. No
		 * reply to this is necessary; the master will assume that the file is
		 * gone from the network after sending this message and can be expected
		 * to no longer forward any requests for it.
		 * Fields:
		 * - fileid: String, file id
		 */
		DELETE_FILE
	}

	private RMPacket.Type type;
	private Map<String, Serializable> data;

	private static final long serialVersionUID = 1L;

	/**
	 * Create an empty packet of the given type.
	 * @param type
	 */
	public RMPacket(RMPacket.Type type) {
		this.type = type;
		data = new HashMap<String, Serializable>();
	}

	/**
	 * Get the type of this packet.
	 * @return
	 */
	public RMPacket.Type getType() {
		return type;
	}

	/**
	 * Set the given data property. 
	 * @param key
	 * @param value
	 */
	public void setProperty(String key, Serializable value) {
		data.put(key.toLowerCase(), value);
	}

	public Integer getInteger(String key) {
		Object d = data.get(key.toLowerCase());
		if (d == null || !(d instanceof Integer))
			return null;
		return (Integer)d;
	}

	public Double getDouble(String key) {
		Object d = data.get(key.toLowerCase());
		if (d == null || !(d instanceof Double))
			return null;
		return (Double)d;
	}

	public String getString(String key) {
		Object d = data.get(key.toLowerCase());
		if (d == null || !(d instanceof String))
			return null;
		return (String)d;
	}

	public Boolean getBoolean(String key) {
		Object d = data.get(key.toLowerCase());
		if (d == null || !(d instanceof Boolean))
			return null;
		return (Boolean)d;
	}

	@SuppressWarnings("unchecked")
	public List<Object> getList(String key) {
		try {
			Object d = data.get(key.toLowerCase());
			if (d == null)
				return null;
			return (List<Object>)d;
		} catch (ClassCastException e) {
			return null;
		}
	}
	
	public RMFile getFile(String key) {
		Object d = data.get(key.toLowerCase());
		if (d == null || !(d instanceof RMFile))
			return null;
		return (RMFile)d;
	}

	/**
	 * Print out information about this packet to the console. Useful
	 * for debugging.
	 */
	public void print() {
		System.out.println("Packet Type: " + type.name());
		System.out.println("Data:");
		for (String key : data.keySet()) {
			System.out.println("\t" + key + ": " + data.get(key).toString());
		}
	}

}

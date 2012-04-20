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

	public RMPacket(RMPacket.Type type) {
		this.type = type;
		data = new HashMap<String, Serializable>();
	}

	public RMPacket.Type getType() {
		return type;
	}

	public boolean hasDataVal(String key) {
		return data.containsKey(key);
	}

	public void setDataVal(String key, Serializable value) {
		data.put(key.toLowerCase(), value);
	}

	public Integer getInteger(String key) {
		try {
			Object d = data.get(key.toLowerCase());
			if (d == null)
				return null;
			return (Integer)d;
		} catch (ClassCastException e) {
			return null;
		}
	}

	public Double getDouble(String key) {
		try {
			Object d = data.get(key.toLowerCase());
			if (d == null)
				return null;
			return (Double)d;
		} catch (ClassCastException e) {
			return null;
		}
	}

	public String getString(String key) {
		try {
			Object d = data.get(key.toLowerCase());
			if (d == null)
				return null;
			return (String)d;
		} catch (ClassCastException e) {
			return null;
		}
	}

	public Boolean getBoolean(String key) {
		try {
			Object d = data.get(key.toLowerCase());
			if (d == null)
				return null;
			return (Boolean)d;
		} catch (ClassCastException e) {
			return null;
		}
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

	public void print() {
		System.out.println("Packet Type: " + type.name());
		System.out.println("Data:");
		for (String key : data.keySet()) {
			System.out.println("\t" + key + ": " + data.get(key).toString());
		}
	}

}

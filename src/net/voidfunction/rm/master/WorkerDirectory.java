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

package net.voidfunction.rm.master;

import java.util.*;

import net.voidfunction.rm.common.*;

import org.jgroups.Address;

/**
 * Stores all information the master node knows about workers in the network,
 * including which files they have and how to reach their web server.
 */
public class WorkerDirectory {

	private HashMap<Address, WorkerData> workers;
	private FileRepository fileRep;

	/**
	 * Creates a new WorkerDirectory. Will use the given FileRepository to
	 * determine which files exist when making decisions.
	 * 
	 * @param fileRep
	 */
	public WorkerDirectory(FileRepository fileRep) {
		workers = new HashMap<Address, WorkerData>();
		this.fileRep = fileRep;
	}

	/* Functions for creating/deleting worker records */

	/**
	 * Creates a new worker record with the given http IP and port. New workers
	 * will have an empty file list.
	 * 
	 * @param addr
	 * @param ip
	 * @param httpPort
	 */
	public void addWorker(Address addr, String ip, int httpPort) {
		if (!workerExists(addr)) {
			WorkerData newData = new WorkerData(ip, httpPort);
			workers.put(addr, newData);
		}
	}

	/**
	 * Removes a worker. Returns whether or not the remove succeeded.
	 * 
	 * @param addr
	 * @return boolean
	 */
	public boolean removeWorker(Address addr) {
		return (workers.remove(addr) != null);
	}

	/**
	 * Returns whether we have a worker record for the given Address.
	 * 
	 * @param addr
	 * @return
	 */
	public boolean workerExists(Address addr) {
		return workers.containsKey(addr);
	}

	/**
	 * Gets a string in the form host:port for the given worker's HTTP server.
	 * 
	 * @param addr
	 * @return
	 */
	public String getWorkerHostAndPort(Address addr) {
		if (workerExists(addr)) {
			WorkerData wdata = workers.get(addr);
			return wdata.getIp() + ":" + wdata.getHttpPort();
		}
		return null;
	}

	/**
	 * Returns number of known workers active in cluster
	 * 
	 * @return
	 */
	public int getWorkerCount() {
		return workers.size();
	}

	/* Functions for workers' files */

	/**
	 * Adds a file with the given ID into the worker's known file list, if it
	 * and the worker both exist
	 * 
	 * @param addr
	 * @param fileId
	 */
	public void addWorkerFile(Address addr, String fileId) {
		RMFile file = fileRep.getFileById(fileId);
		if (file == null || !workerExists(addr))
			return;

		workers.get(addr).addFile(file);

	}

	/**
	 * Removes a file with the given ID from the worker's known file list, if it
	 * and the worker both exist
	 * 
	 * @param addr
	 * @param fileId
	 */
	public void removeWorkerFile(Address addr, String fileId) {
		if (!workers.containsKey(addr))
			return; // TODO: Error logs in this type of situation

		RMFile file = fileRep.getFileById(fileId);
		if (file == null || !workerExists(addr))
			return;

		workers.get(addr).removeFile(file);
	}

	/**
	 * Returns a list of worker addresses that have the given ID. This can then
	 * be selected from to redirect clients to a file.
	 * 
	 * @param fileId
	 * @return
	 */
	public List<Address> getWorkersWithFile(String fileId) {
		List<Address> workerList = new ArrayList<Address>();

		RMFile file = fileRep.getFileById(fileId);
		if (file == null)
			return workerList;

		for (Address addr : workers.keySet()) {
			if (workers.get(addr).hasFile(file))
				workerList.add(addr);
		}

		Collections.shuffle(workerList);
		return workerList;
	}

	/**
	 * Count the workers that have a given file - slightly more efficient for
	 * this purpose than getting a list of them.
	 * 
	 * @param fileId
	 * @return
	 */
	public int countWorkersWithFile(String fileId) {
		RMFile file = fileRep.getFileById(fileId);
		if (file == null)
			return 0;

		int count = 0;
		for (Address addr : workers.keySet()) {
			if (workers.get(addr).hasFile(file))
				count++;
		}
		return count;
	}

	/**
	 * Returns a list of worker addresses that DO NOT have the given ID. This
	 * can then be selected from to find new locations for replicas.
	 * 
	 * @param fileId
	 * @return
	 */
	public List<Address> getWorkersWithoutFile(String fileId) {
		List<Address> workerList = new ArrayList<Address>();

		RMFile file = fileRep.getFileById(fileId);
		if (file == null)
			return workerList;

		for (Address addr : workers.keySet()) {
			if (!workers.get(addr).hasFile(file))
				workerList.add(addr);
		}

		Collections.shuffle(workerList);
		return workerList;
	}

	/**
	 * Removes a file from all worker's lists.
	 * 
	 * @param fileId
	 */
	public void removeFileFromAll(String fileId) {
		RMFile file = fileRep.getFileById(fileId);
		if (file == null)
			return;

		for (WorkerData wdata : workers.values()) {
			wdata.removeFile(file);
		}
	}

	/**
	 * Internal class representing a worker node and its known file list.
	 */
	private class WorkerData {
		private String ip;
		private int httpPort;
		private ArrayList<RMFile> files;

		/**
		 * Returns this node's public IP address.
		 * 
		 * @return
		 */
		public String getIp() {
			return ip;
		}

		/**
		 * Returns this node's http port.
		 * 
		 * @return
		 */
		public int getHttpPort() {
			return httpPort;
		}

		/**
		 * Adds a file to this worker's file list.
		 * 
		 * @param file
		 */
		public void addFile(RMFile file) {
			if (!hasFile(file))
				files.add(file);
		}

		/**
		 * Removes a file from this worker's file list.
		 * 
		 * @param file
		 */
		public void removeFile(RMFile file) {
			files.remove(file);
		}

		/**
		 * Returns whether this worker node has the given file.
		 * 
		 * @param file
		 * @return
		 */
		public boolean hasFile(RMFile file) {
			return files.contains(file);
		}

		/**
		 * Creates a new worker data record.
		 * 
		 * @param ip
		 * @param httpPort
		 */
		public WorkerData(String ip, int httpPort) {
			this.ip = ip;
			this.httpPort = httpPort;
			this.files = new ArrayList<RMFile>();
		}
	}

}

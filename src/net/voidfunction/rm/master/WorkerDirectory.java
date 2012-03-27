package net.voidfunction.rm.master;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.voidfunction.rm.common.FileRepository;
import net.voidfunction.rm.common.RMFile;

import org.jgroups.Address;

/**
 * Stores all information the master node knows about workers in the network,
 * including which files they have and how to reach their web server.
 */
public class WorkerDirectory {

	private HashMap<Address, WorkerData> workers;
	private FileRepository fileRep;

	/**
	 * Creates a new WorkerDirectory. Will use the given FileRepository to determine
	 * which files exist when making decisions.
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
	 * Removes a worker record.
	 * @param addr
	 */
	public void removeWorker(Address addr) {
		workers.remove(addr);
	}
	
	/**
	 * Returns whether we have a worker record for the given Address.
	 * @param addr
	 * @return
	 */
	public boolean workerExists(Address addr) {
		return workers.containsKey(addr);
	}
	
	/**
	 * Gets a string in the form host:port for the given worker's HTTP server.
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

	/* Functions for workers' files */

	/**
	 * Adds a file with the given ID into the worker's known file list, if it
	 * and the worker both exist
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
	 * Removes a file with the given ID from the worker's known file list,
	 * if it and the worker both exist
	 * @param addr
	 * @param fileId
	 */
	public  void removeWorkerFile(Address addr, String fileId) {
		if (!workers.containsKey(addr))
			return; //TODO: Error logs in this type of situation
		
		RMFile file = fileRep.getFileById(fileId);
		if (file == null || !workerExists(addr))
			return;
		
		workers.get(addr).removeFile(file);
	}

	/**
	 * Returns a list of worker addresses that have the given ID. This can then
	 * be selected from to redirect clients to a file.
	 * @param fileId
	 * @return
	 */
	public List<Address> workersWithFile(String fileId) {
		List<Address> workerList = new ArrayList<Address>();

		RMFile file = fileRep.getFileById(fileId);
		if (file == null)
			return workerList;
		
		for(Address addr : workers.keySet()) {
			if (workers.get(addr).hasFile(file))
				workerList.add(addr);
		}
		
		return workerList;
	}
	
	/**
	 * Removes a file from all worker's lists.
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
		 * @return
		 */
		public String getIp() {
			return ip;
		}

		/**
		 * Returns this node's http port.
		 * @return
		 */
		public int getHttpPort() {
			return httpPort;
		}
		
		/**
		 * Adds a file to this worker's file list.
		 * @param file
		 */
		public void addFile(RMFile file) {
			if (!hasFile(file))
				files.add(file);
		}
		
		/**
		 * Removes a file from this worker's file list.
		 * @param file
		 */
		public void removeFile(RMFile file) {
			files.remove(file);
		}
		
		/**
		 * Returns whether this worker node has the given file.
		 * @param file
		 * @return
		 */
		public boolean hasFile(RMFile file) {
			return files.contains(file);
		}
		
		/**
		 * Creates a new worker data record.
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

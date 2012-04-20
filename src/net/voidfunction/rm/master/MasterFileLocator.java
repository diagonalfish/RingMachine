package net.voidfunction.rm.master;

import java.util.List;

import org.jgroups.Address;

import net.voidfunction.rm.common.FileLocator;

public class MasterFileLocator extends FileLocator {
	
	private MasterNode node;
	
	public MasterFileLocator(MasterNode node) {
		this.node = node;
	}
	
	public String locateURL(String fileId, String fileName) {
		List<Address> workers = node.getWorkerDirectory().getWorkersWithFile(fileId);
		if (workers.size() == 0)
			return null;
		
		Address worker = workers.get(0); // List is pre-randomized by the worker directory
		return "http://" + node.getWorkerDirectory().getWorkerHostAndPort(worker) +
			"/files/" + fileId + "/" + fileName;
	}
	
}

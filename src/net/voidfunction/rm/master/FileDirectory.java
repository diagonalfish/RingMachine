package net.voidfunction.rm.master;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import net.voidfunction.rm.common.files.RMFile;

import org.jgroups.Address;

public class FileDirectory {

	private HashMap<Address, ArrayList<RMFile>> peerFiles;

	private HashMap<String, RMFile> allFiles;

	public FileDirectory() {
		peerFiles = new HashMap<Address, ArrayList<RMFile>>();
		allFiles = new HashMap<String, RMFile>();
	}

	public synchronized void addFile(RMFile file) {
		if (!allFiles.containsKey(file))
			allFiles.put(file.getId(), file);
	}

	public synchronized void removeFile(RMFile file) {
		allFiles.remove(file);
		for (ArrayList<RMFile> peerFileList : peerFiles.values()) {
			peerFileList.remove(file); // Remove from all peer lists
		}
	}

	public synchronized RMFile getFileById(String id) {
		return allFiles.get(id);
	}

	public synchronized Collection<RMFile> getAllFiles() {
		return allFiles.values();
	}

	/* Functions for peers' files */

	public synchronized void addPeer(Address peer) {
		if (!peerFiles.containsKey(peer))
			peerFiles.put(peer, new ArrayList<RMFile>());
	}

	public synchronized void removePeer(Address peer) {
		peerFiles.remove(peer);
	}

	public synchronized void addPeerFile(Address peer, String fileId) {
		RMFile file = getFileById(fileId);
		if (file == null)
			return;

		if (!peerFiles.containsKey(peer))
			// Lazy-add ArrayList for peer's files if needed
			peerFiles.put(peer, new ArrayList<RMFile>());

		if (peerFiles.containsKey(peer)) {
			if (!peerFiles.get(peer).contains(file))
				peerFiles.get(peer).add(file);
		}
	}

	public synchronized void removePeerFile(Address peer, String fileId) {
		RMFile file = getFileById(fileId);
		if (file == null)
			return;
		if (peerFiles.containsKey(peer)) {
			if (!peerFiles.get(peer).contains(file))
				peerFiles.get(peer).remove(file);
		}
	}

	public synchronized List<Address> peersWithFile(String fileId) {
		List<Address> peers = new ArrayList<Address>();

		RMFile file = getFileById(fileId);
		if (file == null)
			return peers;

		for (Address peer : peerFiles.keySet()) {
			if (peerFiles.get(peer).contains(file))
				peers.add(peer);
		}
		return peers;
	}

}

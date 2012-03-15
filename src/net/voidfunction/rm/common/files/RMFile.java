package net.voidfunction.rm.common.files;

import java.io.Serializable;

import org.jgroups.util.UUID;

/**
 * Generic representation of a file - contains everything needed to identify it
 * except the data itself.
 */
public class RMFile implements Serializable {

	private static final long serialVersionUID = 3764807165415486377L;

	/* File data */
	private String name, id, mimetype;
	private int size;
	private byte[] sha256hash;

	public RMFile(String name, String id, String mimetype, int size, byte[] hash) {
		this.name = name;
		this.id = id;
		this.mimetype = mimetype;
		this.size = size;
		this.sha256hash = hash;
	}

	/**
	 * Constructor that generates its own UUID for id (appropriate for new file
	 * objects)
	 * 
	 * @param name
	 * @param mimetype
	 * @param size
	 * @param hash
	 */
	public RMFile(String name, String mimetype, int size, byte[] hash) {
		this.name = name;
		this.id = UUID.randomUUID().toString();
		this.mimetype = mimetype;
		this.size = size;
		this.sha256hash = hash;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMimetype() {
		return mimetype;
	}

	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public byte[] getHash() {
		return sha256hash;
	}

	public void setHash(byte[] hash) {
		this.sha256hash = hash;
	}

	public boolean equals(Object o) {
		RMFile otherfile = (RMFile)o;
		return otherfile.id.equals(this.id);
	}

	public RMFile cloneToID(String newID) {
		return new RMFile(name, newID, mimetype, size, sha256hash);
	}

}

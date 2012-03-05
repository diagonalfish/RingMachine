package net.voidfunction.rm.common.files;

import java.io.Serializable;

/**
 * Generic representation of a file - contains everything needed to identify
 * it except the data itself.
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
	
}

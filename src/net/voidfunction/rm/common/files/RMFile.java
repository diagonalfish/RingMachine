package net.voidfunction.rm.common.files;

import java.io.Serializable;

public class RMFile implements Serializable {

	/* File data */
	private String name, id, mimetype;
	private int size;
	private byte[] sha256hash;
	
}

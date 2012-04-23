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

import org.jgroups.util.UUID;

/**
 * Generic representation of a file - contains everything needed to identify it
 * except the data itself.
 */
public class RMFile implements Serializable {

	private static final long serialVersionUID = 3764807165415486377L;

	/* File data */
	private String name, id, mimetype;
	private long size;
	private byte[] sha256hash;

	/**
	 * Constructor - generates an id randomly upon creation.
	 * 
	 * @param name
	 * @param mimetype
	 * @param size
	 * @param hash
	 */
	public RMFile(String name, String mimetype, long size, byte[] hash) {
		this.name = name;
		this.id = UUID.randomUUID().toString();
		this.mimetype = mimetype;
		this.size = size;
		this.sha256hash = hash;
	}

	/**
	 * Return the "friendly" filename of this file (the name the user
	 * downloading the file will expect to see).
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the "friendly" filename of this file.
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the random unique ID of this file as a string.
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * Get the MIME type of this file.
	 * @return
	 */
	public String getMimetype() {
		return mimetype;
	}

	/**
	 * Set the MIME type of this file.
	 * @param mimetype
	 */
	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
	}

	/**
	 * Get the size of this file in bytes.
	 * @return
	 */
	public long getSize() {
		return size;
	}

	/**
	 * Set the size of this file in bytes.
	 * @param size
	 */
	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * Get the SHA-256 hash of this file, as an array of bytes.
	 * @return
	 */
	public byte[] getHash() {
		return sha256hash;
	}

	/**
	 * Set the SHA-256 hash of this file.
	 * @param hash
	 */
	public void setHash(byte[] hash) {
		this.sha256hash = hash;
	}

	public boolean equals(Object o) {
		if (!(o instanceof RMFile))
			return false;
		RMFile otherfile = (RMFile)o;
		return otherfile.id.equals(this.id);
	}

}

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

import java.io.*;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.security.*;

/**
 * Provides a couple of utility functions relating to files, including hashing
 * and mime type determination.
 */
public class FileUtils {

	/**
	 * Calculate the SHA-256 hash for a given file efficiently.
	 * 
	 * @param file
	 * @return Byte array containing the hash
	 * @throws IOException
	 */
	public static byte[] sha256Hash(File file) throws IOException {
		FileInputStream in = new FileInputStream(file);
		return sha256Hash(in);
	}

	public static byte[] sha256Hash(InputStream in) throws IOException {
		MessageDigest sha256 = null;
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			// This should not happen, ever
			return new byte[0];
		}
		DigestInputStream din = new DigestInputStream(in, sha256);
		din.on(true);
		byte[] buffer = new byte[8192];
		while (din.read(buffer) != -1)
			// Read everything into the digest input
			;

		return sha256.digest();
	}

	/**
	 * Get a MIME type for a given filename.
	 * 
	 * @param filename
	 * @return MIME type (string)
	 */
	public static String mimeType(String filename) {
		URLConnection.getFileNameMap();
		FileNameMap fileNameMap = URLConnection.getFileNameMap();
		return fileNameMap.getContentTypeFor(filename);
	}

}

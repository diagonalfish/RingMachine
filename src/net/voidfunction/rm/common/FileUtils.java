package net.voidfunction.rm.common;

import java.io.*;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.security.*;

/**
 * Handy functions for files
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
		while (din.read(buffer) != -1) // Read everything into the digest input
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

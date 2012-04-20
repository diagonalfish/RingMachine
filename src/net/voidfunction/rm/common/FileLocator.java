package net.voidfunction.rm.common;

/**
 * Default implementation of a class to locate a file in the network, used by
 * FileServlet.
 */
public class FileLocator {

	/**
	 * Return a URL that the FileServlet using this FileLocator should redirect
	 * to instead of serving the file ourselves, or null if we should not
	 * redirect.
	 * 
	 * @param fileID
	 * @return
	 */
	public String locateURL(String fileID) {
		return null;
	}

}

package net.voidfunction.rm.common;

/**
 * Interface for object to locate a file in the network, used by FileServlet.
 */
public interface FileLocator {

	/**
	 * Return a URL that the FileServlet using this FileLocator should redirect
	 * to instead of serving the file ourselves, or null if we should not
	 * redirect.
	 * 
	 * @param fileId
	 * @param fileName
	 * @return
	 */
	public String locateURL(String fileId, String fileName);

}

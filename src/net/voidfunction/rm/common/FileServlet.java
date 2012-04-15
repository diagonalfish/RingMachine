package net.voidfunction.rm.common;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

public class FileServlet extends HttpServlet {

	private static final long serialVersionUID = 8461560600264423624L;
	
	private Node node;
	private FileLocator locator;
	
	public FileServlet(Node node, FileLocator locator) {
		this.node = node;
		this.locator = locator;
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.getSession().setMaxInactiveInterval(120);
		
		String[] urlParts = request.getRequestURI().substring(1).split("/");
		if (urlParts.length < 3) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		String fileID = urlParts[1];
		String fileName = urlParts[2];
		
		String logOut = "File " + fileID + " (" + fileName + ") requested by " + request.getRemoteHost() + " [Result: ";
		
		String redirURL = (String)request.getSession().getAttribute("fileURL-" + fileID);
		if (redirURL == null)
			redirURL = locator.locateURL(fileID);
		if (redirURL != null) {
			node.getLog().debug("Found redirect URL: " + redirURL);
			request.getSession().setAttribute("fileURL-" + fileID, redirURL);
			// Redirect to the new URL
			logOut += "Redirect]";
			node.getLog().info(logOut);
			
			response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
			response.setHeader("Location", redirURL);
		}
		else {
			// We have to try to find it ourselves
			RMFile file = node.getFileRepository().getFileById(fileID);
			if (file == null) {
				// File not found.
				logOut += "Not Found]";
				node.getLog().info(logOut);
				
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.getWriter().write("<b>404 Not Found</b><br/>Could not find a file with ID " + fileID);
			}
			else {
				// File found
				logOut += "Found]";
				node.getLog().info(logOut);
				
				// Caching magic - we can safely assume the file won't change for now
				String etag = Hex.encodeHexString(file.getHash());
				response.setHeader("ETag", etag);
				String ifModifiedSince = request.getHeader("If-Modified-Since");
				String ifNoneMatch = request.getHeader("If-None-Match");
				boolean etagMatch = (ifNoneMatch != null) && (ifNoneMatch.equals(etag));
				if (ifModifiedSince != null || etagMatch) {
					response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					response.setHeader("Last-Modified", ifModifiedSince);
				}
				else {
					response.setStatus(HttpServletResponse.SC_OK);
					response.setHeader("Expires", HTTPUtils.getServerTime());
					response.setHeader("Cache-Control", "max-age=3600");
					response.setContentType(file.getMimetype());
					response.setHeader("Content-Length", String.valueOf(file.getSize()));
					InputStream fileIn = node.getFileRepository().getFileData(fileID);
					IOUtils.copyLarge(fileIn, response.getOutputStream());
				}
			}
		}
	}

}

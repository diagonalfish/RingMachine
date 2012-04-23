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

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

/**
 * Servlet whose responsibility it is to act as a download server for all files in the
 * node's FileRepository. For the master node, it provides hooks to redirect the users
 * to worker nodes and a means to track downloads.
 */
public class FileServlet extends HttpServlet {

	private static final long serialVersionUID = 8461560600264423624L;

	private Node node;
	private FileLocator locator;
	private FileDownloadListener dlListener;

	/**
	 * Creates a new FileServlet. locator and dlListener may be null if their
	 * functionality is not needed (e.g. in worker nodes)
	 * @param node
	 * @param locator
	 * @param dlListener
	 */
	public FileServlet(Node node, FileLocator locator, FileDownloadListener dlListener) {
		this.node = node;
		this.locator = locator;
		this.dlListener = dlListener;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.getSession().setMaxInactiveInterval(120);
		response.setHeader("Date", HTTPUtils.getServerTime(0));

		// Parse the filename and the ID out of the URL
		String[] urlParts = request.getRequestURI().substring(1).split("/");
		if (urlParts.length < 2) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		String fileID = urlParts[1];
		String fileName = "";
		if (urlParts.length > 2)
			fileName = urlParts[2];

		String logOut = "File " + fileID + " (" + fileName + ") requested by " + request.getRemoteHost() + " [Result: ";
		
		RMFile file = node.getFileRepository().getFileById(fileID);
		if (file == null) {
			// File  with given ID not found - no redirect for you.
			logOut += "Not found]";
			node.getLog().info(logOut);

			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			response.getWriter().write("<b>404 Not Found</b><br/>Could not find a file with ID " + fileID);
			return;
		}
		
		boolean workerDL = (fileName.equals("Worker-Download"));
		if (workerDL)
			logOut += " (Worker Download) ";
		
		// Let the download listener know, if any, but don't count worker downloads
		if (dlListener != null && !workerDL)
			dlListener.fileDownloaded(file);

		String redirURL = null;
		if (locator != null)
			redirURL = (String)request.getSession().getAttribute("fileURL-" + fileID);
		if (redirURL == null && locator != null)
			redirURL = locator.locateURL(fileID, fileName);
		if (redirURL != null) {
			node.getLog().debug("Found redirect URL: " + redirURL);
			request.getSession().setAttribute("fileURL-" + fileID, redirURL);
			// Redirect to the new URL
			logOut += "Redirect]";
			node.getLog().info(logOut);

			response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
			response.setHeader("Location", redirURL);
		} else {
			// We have to try to find it ourselves

			logOut += "Found locally]";
			node.getLog().info(logOut);

			// Caching magic - we can safely assume the file won't change
			String etag = Hex.encodeHexString(file.getHash());
			response.setHeader("ETag", etag);
			String ifModifiedSince = request.getHeader("If-Modified-Since");
			String ifNoneMatch = request.getHeader("If-None-Match");
			boolean etagMatch = (ifNoneMatch != null) && (ifNoneMatch.equals(etag));
			
			if (ifModifiedSince != null || etagMatch) {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				response.setHeader("Last-Modified", ifModifiedSince);
			} else {
				// Send the HTTP response and file data
				response.setStatus(HttpServletResponse.SC_OK);
				response.setHeader("Expires", HTTPUtils.getServerTime(3600));
				response.setHeader("Cache-Control", "max-age=3600");
				response.setContentType(file.getMimetype());
				response.setHeader("Content-Length", String.valueOf(file.getSize()));
				
				// Stream the file data to the output stream using Apache IOUtils
				InputStream fileIn = node.getFileRepository().getFileData(fileID);
				IOUtils.copyLarge(fileIn, response.getOutputStream());
			}
		}
	}

}

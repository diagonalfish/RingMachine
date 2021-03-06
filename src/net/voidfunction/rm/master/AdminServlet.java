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

package net.voidfunction.rm.master;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.*;

import org.apache.commons.codec.binary.Hex;

import net.sf.jtpl.Template;
import net.voidfunction.rm.common.*;

/**
 * Servlet that provides a simple administration interface for the master node.
 */
@MultipartConfig
public class AdminServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private String templateDir;
	private MasterNode node;

	public AdminServlet(MasterNode node, String templateDir) {
		this.node = node;
		this.templateDir = templateDir;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Set headers to prevent caching of pages
		response.setHeader("Date", HTTPUtils.getServerTime(0));
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		response.setHeader("Expires", HTTPUtils.getServerTime(0));
		response.setHeader("Cache-Control", "no-cache, must-revalidate, max-age=0");
		response.setHeader("Pragma", "no-cache");

		// Retrieve output for the correct page.
		String page = request.getParameter("page");
		if (page == null || page.equals("index"))
			response.getWriter().print(pageIndex(request));
		else if (page.equals("upload"))
			response.getWriter().print(pageUpload());
		else if (page.equals("delete")) {
			pageDelete(request);
			response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
			response.setHeader("Location", ".");
		}

	}

	/* Pages */
	private String pageIndex(HttpServletRequest request) throws FileNotFoundException {
		Template tpl = new Template(new File(templateDir + "index.tpl"));
		tpl.assign("HOST", node.getPublicIP());

		// Status info
		tpl.assign("UPTIME", getDuration(ManagementFactory.getRuntimeMXBean().getUptime()));
		tpl.assign("WORKERCOUNT", String.valueOf(node.getWorkerDirectory().getWorkerCount()));
		tpl.assign("FILECOUNT", String.valueOf(node.getFileRepository().getFileCount()));

		// Files
		Collection<RMFile> files = node.getFileRepository().getFileObjects();
		if (files.size() == 0) {
			tpl.assign("FILEID", "No files.");
			tpl.assign("FILENAME", "");
			tpl.assign("FILETYPE", "");
			tpl.assign("FILESIZE", "");
			tpl.assign("DOWNURL", "");
			tpl.assign("DOWNTXT", "");
			tpl.assign("DELETEURL", "");
			tpl.assign("DELETETXT", "");
			tpl.parse("main.file");
		}
		for (RMFile file : files) {
			tpl.assign("FILEID", file.getId());
			tpl.assign("FILENAME", file.getName());
			tpl.assign("FILETYPE", file.getMimetype());
			tpl.assign("FILESIZE", String.valueOf(file.getSize()));
			tpl.assign("DOWNURL", "http://" + request.getLocalAddr() + ":"
				+ node.getConfig().getInt("port.http", 8080) + "/files/" + file.getId() + "/"
				+ file.getName());
			tpl.assign("DOWNTXT", "Download");
			tpl.assign("DELETEURL", "?page=delete&id=" + file.getId());
			tpl.assign("DELETETXT", "Delete");
			tpl.parse("main.file");
		}

		tpl.parse("main");
		return tpl.out();
	}

	private String pageUpload() throws FileNotFoundException {
		Template tpl = new Template(new File(templateDir + "upload.tpl"));

		tpl.parse("main");
		return tpl.out();
	}
	
	private void pageDelete(HttpServletRequest request) throws IOException {
		String id = request.getParameter("id");
		if (id == null)
			return;
		
		// Delete file locally
		if (!node.getFileRepository().checkFile(id)) // File doesn't exist?
			return;
		
		node.getLog().info("Deleting file " + id + " (via web).");
		
		try {
			node.getFileRepository().removeFile(id);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Now pass that along to all the nodes
		node.getNetManager().packetSendDeleteFile(id);
	}

	/* File upload */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setHeader("Date", HTTPUtils.getServerTime(0));
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);

		Part uploadFile = request.getPart("uploadfile");
		if (uploadFile == null)
			throw new ServletException("Uploaded file is null.");

		// Get some info about the file
		String filename = getFilename(uploadFile);
		String contentType = uploadFile.getContentType();
		long size = uploadFile.getSize();
		byte[] hash = FileUtils.sha256Hash(uploadFile.getInputStream());

		// Create a new file object, add it to the database, and store data
		RMFile newFile = new RMFile(filename, contentType, size, hash);
		node.getFileRepository().addFile(newFile, uploadFile.getInputStream());

		// Output data for interested parties
		Template tpl = new Template(new File(templateDir + "uploadsuccess.tpl"));
		tpl.assign("FILENAME", filename);
		tpl.assign("SIZE", String.valueOf(size));
		tpl.assign("TYPE", contentType);
		tpl.assign("HASH", Hex.encodeHexString(hash));
		tpl.assign("FILEID", newFile.getId());
		tpl.parse("main");
		response.getWriter().print(tpl.out());

		// Delete temp file
		uploadFile.delete();

		// Log
		node.getLog().info("New file added (via web): " + newFile.getId() + " (" + newFile.getName() + ")");
	}

	/* Util */

	// Parse a filename out of the form data.
	// Thanks to: http://stackoverflow.com/a/2424824
	private String getFilename(Part part) {
		for (String cd : part.getHeader("content-disposition").split(";")) {
			if (cd.trim().startsWith("filename"))
				return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
		}
		return null;
	}

	private static String getDuration(long millis) {
		long days = TimeUnit.MILLISECONDS.toDays(millis);
		millis -= TimeUnit.DAYS.toMillis(days);
		long hours = TimeUnit.MILLISECONDS.toHours(millis);
		millis -= TimeUnit.HOURS.toMillis(hours);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
		millis -= TimeUnit.MINUTES.toMillis(minutes);
		long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

		StringBuilder sb = new StringBuilder();
		sb.append(days);
		sb.append(" days, ");
		sb.append(hours);
		sb.append(" hours, ");
		sb.append(minutes);
		sb.append(" minutes, ");
		sb.append(seconds);
		sb.append(" seconds");

		return (sb.toString());
	}

}

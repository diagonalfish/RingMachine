package net.voidfunction.rm.master;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.codec.binary.Hex;

import net.sf.jtpl.Template;
import net.voidfunction.rm.common.HTTPUtils;
import net.voidfunction.rm.common.RMFile;
import net.voidfunction.rm.common.FileUtils;

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
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		response.setHeader("Expires", HTTPUtils.getServerTime());
		response.setHeader("Cache-Control", "no-cache, must-revalidate, max-age=0");
		response.setHeader("Pragma", "no-cache");
		
		String page = request.getParameter("page");
		if (page == null || page.equals("index"))
			response.getWriter().print(pageIndex(request));
		else if (page.equals("upload"))
			response.getWriter().print(pageUpload());
		
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
			tpl.parse("main.file");
		}
		for(RMFile file : files) {
			tpl.assign("FILEID", file.getId());
			tpl.assign("FILENAME", file.getName());
			tpl.assign("FILETYPE", file.getMimetype());
			tpl.assign("FILESIZE", String.valueOf(file.getSize()));
			tpl.assign("DOWNURL", "http://" + request.getLocalAddr() + ":" + node.getConfig().getInt("port.http", 8080) +
					"/files/" + file.getId() + "/" + file.getName());
			tpl.assign("DOWNTXT", "Download");
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
	
	/* File upload */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		
		Part uploadFile = request.getPart("uploadfile");
		if (uploadFile == null) throw new ServletException("Uploaded file is null.");
		
		// Get some info about the file
		String filename = getFilename(uploadFile);
		String contentType = uploadFile.getContentType();
		long size = uploadFile.getSize();
		byte[] hash = FileUtils.sha256Hash(uploadFile.getInputStream());
		
		// Create a new file object, add it to the database, and store data
		RMFile newFile = new RMFile(filename, contentType, size, hash);
		node.getFileRepository().addFile(newFile);
		node.getFileRepository().storeFileData(uploadFile.getInputStream(), newFile.getId());
		
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
	}
	
	
	/* Util */
	
	private String getFilename(Part part) {
		for (String cd : part.getHeader("content-disposition").split(";")) {
			if (cd.trim().startsWith("filename"))
				return cd.substring(cd.indexOf('=') + 1).trim() .replace("\"", "");
		}
		return null;
	}
	
	public static String getDuration(long millis) {
		
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
		
		return(sb.toString());
	}
	
}
package net.voidfunction.rm.master;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jtpl.Template;

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
		
		String page = request.getParameter("page");
		if (page == null || page.equals("index"))
			response.getWriter().print(pageIndex());
		else if (page.equals("upload"))
			response.getWriter().print(pageUpload());
		
	}
	
	/* Pages */
	private String pageIndex() throws FileNotFoundException {
		Template tpl = new Template(new File(templateDir + "index.tpl"));
		tpl.assign("HOST", node.getPublicIP());
		
		//Status info
		tpl.assign("UPTIME", getDuration(ManagementFactory.getRuntimeMXBean().getUptime()));
		tpl.assign("WORKERCOUNT", String.valueOf(node.getWorkerDirectory().getWorkerCount()));
		tpl.assign("FILECOUNT", String.valueOf(node.getFileRepository().getFileCount()));
		
		tpl.parse("main");
		return tpl.out();
	}
	
	private String pageUpload() throws FileNotFoundException {
		Template tpl = new Template(new File(templateDir + "upload.tpl"));
		
		tpl.parse("main");
		return tpl.out();
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

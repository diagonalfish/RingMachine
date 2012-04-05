package net.voidfunction.rm.common;

import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class RMHTTPServer {

	private Server server;
	private ServletContextHandler context;
	
	public RMHTTPServer(int port) {
		server = new Server(port);
		context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
	}
	
	public void addServlet(String path, HttpServlet servlet) {
		context.addServlet(new ServletHolder(servlet), path);
	}
	
	public void run() throws Exception {
		server.start();
	}
	
}

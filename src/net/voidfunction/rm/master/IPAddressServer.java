package net.voidfunction.rm.master;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import net.voidfunction.rm.common.http.HTTPUtils;

import com.sun.net.httpserver.*;

/**
 * Simple web server that returns IP address of connecting client. Used by
 * RingMachine clients to figure out their public IP.
 */
@SuppressWarnings("restriction")
public class IPAddressServer implements HttpHandler {

	private int port;
	private HttpServer server;

	public IPAddressServer(int port) {
		this.port = port;
	}

	public void handle(HttpExchange exch) throws IOException {
		String remoteip = exch.getRemoteAddress().getHostName();

		exch.getResponseHeaders().set("Server", "RM-IPAddressServer/0.1");
		exch.getResponseHeaders().set("Connection", "close");
		exch.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
		exch.getResponseHeaders().set("Date", HTTPUtils.getServerTime());
		exch.sendResponseHeaders(200, remoteip.length());

		OutputStream out = exch.getResponseBody();
		out.write(remoteip.getBytes("UTF-8"));
		out.close();
	}

	public void start() throws IOException {
		server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/", this);
		server.start();
	}

	public void stop() {
		if (server != null)
			server.stop(1);
	}

}

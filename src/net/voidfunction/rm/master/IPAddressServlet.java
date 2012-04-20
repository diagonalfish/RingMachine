package net.voidfunction.rm.master;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.*;

/**
 * Simple servlet that returns IP address of connecting client. Used by
 * RingMachine clients to figure out their public IP.
 */
public class IPAddressServlet extends HttpServlet {

	private static final long serialVersionUID = 8754107147296227757L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
		IOException {
		response.setContentType("text/plain");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().write(request.getRemoteAddr());
	}

}

package whp.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import whp.WHPBroker;

@SuppressWarnings("serial")
public class CRONALIVEServlet extends HttpServlet {
	
	public void doGet( HttpServletRequest req, HttpServletResponse resp ) throws IOException {
		// should test that the request is "isAlive" ...
		WHPBroker whpBroker = new WHPBroker();
		whpBroker.whp();
//		resp.setContentType( "text/plain" );
//		resp.getWriter().println( "whp is alive" );
	}
	
}

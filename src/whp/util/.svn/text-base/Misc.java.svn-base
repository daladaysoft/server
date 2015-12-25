package whp.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Scanner;

import javax.servlet.http.HttpServletRequest;

import org.granite.context.GraniteContext;
import org.granite.messaging.webapp.HttpGraniteContext;

import whp.tls.Context;

/**
 * Static useful methods
 */
public class Misc {
	
	// ===== date services
	
	// date prepared convenient formats
	private static final SimpleDateFormat _dateFormatYMDHMSS = new SimpleDateFormat( "yyyyMMddhhmmssSSS" );
	public static SimpleDateFormat getDateFormatYMDHMSS() { return _dateFormatYMDHMSS; }
	
	private static final SimpleDateFormat _dateFormatHMSS = new SimpleDateFormat( "hh:mm:ss.SSS" );
	public static SimpleDateFormat getDateFormatHMSS() { return _dateFormatHMSS; }
	
	// ===== debug services
	
	// get caller stack information (thread id, class and method) at given level
	public static String getCallerInfoAll( int callStackDepth ) {
		StackTraceElement s  = new Throwable().getStackTrace()[ callStackDepth ];
		Thread t = Thread.currentThread();
		return t.getId() + ":" + s.getClassName() + ", " + s.getMethodName();
	}
	
	// returns the class (without the package if any)
	public static String getSimpleClassName( Object c ) {
		String className = c.getClass().getName();
		int firstChar = className.lastIndexOf('.') + 1;
		return( firstChar > 0 ) ? className.substring( firstChar ) : className;
	}
	
	// ===== HTTP services
	
	// get remote IP
	
	/** Return remote host IP (getRemoteAddr or getRemoteAddr+getRemoteHost if different) from 
	 * current HTTP request. If context provided, and there is an error when attempt to get 
	 * granite context (that appends on local GAE when datastore does not exist ...), log a message
	 * and return message in IP address. */
	public static String httpGetRemoteAddr( Context context ) {
		
		HttpGraniteContext graniteContext;
		HttpServletRequest request = null;
		try {
			graniteContext = ( HttpGraniteContext )GraniteContext.getCurrentInstance();
			request = graniteContext.getRequest();
		} catch (java.lang.ClassCastException e) {
			if( context != null ) context.addLog( e.getMessage() );
		}
		
		// return empty IP if cannot reach request
		if( request == null ) return "";
		
		// else return
		String rAddr = request.getRemoteAddr();
		String rHost = request.getRemoteHost();
		return( rAddr.equals( rHost ) ) ? rAddr : rAddr + "-" + rHost;
	}
	
	// ===== System properties 
	
	private static String serverPrivilege;
	
	/** Return system.property 'whp.serverPrivilege', that must be [0-9] range.
	 * 0 is for development server, 9 for production server.
	 * Set to 0 if out of range. TODO: add message log if not correct value. */
	public static String getServerPrivilege() {
		if( serverPrivilege != null ) return serverPrivilege;
		String v = System.getProperty( "whp.serverPrivilege" );
		int i;
		try { i = Integer.parseInt( v ); }
		catch( NumberFormatException e ) { i = 0; }
		serverPrivilege = ( i >= 0 && i <= 9 ) ? "" + i : "0";
		return serverPrivilege;
	}
	
	// ==== test : read file from WEB-INF
	
	public static String getTestFile( Context context ) {
				
		Scanner sc = null;
		try {
			sc = new Scanner( new BufferedReader( new FileReader( "./WEB-INF/appengine-web.xml" ) ) );
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while( sc.hasNextLine() )
		{
		    String s = sc.nextLine();
		    context.addLog( s );
		}
		sc.close();
		
		return null; 
	}

}

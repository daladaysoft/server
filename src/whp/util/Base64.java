package whp.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.DatatypeConverter;

public class Base64 {
	
	/** Return string s as encoded string. */
	public static String encode( String s ) {
		
		byte[] bs = null;
		String encoded = null;
		
		if( s != null ) {
			try { bs = s.getBytes( "UTF-8" ); } 
			catch ( UnsupportedEncodingException e ) { throw new Error( e.getMessage() ); }
			encoded = DatatypeConverter.printBase64Binary( bs );
		} 
		return encoded;
	}
	
	/** Return encoded as a string. */
	public static String decode( String encoded ) {
		byte[] decoded = DatatypeConverter.parseBase64Binary( encoded );
		return new String( decoded, StandardCharsets.UTF_8 );
	}
}

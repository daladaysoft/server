package whp.mdb;


/** VO cannot be stored, see message for more information. */
public class VOStoreException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public VOStoreException(String message) {  
		super(message); 
	}  
	  
	public VOStoreException(Throwable cause) {  
		super(cause); 
	}  
	
	public VOStoreException(String message, Throwable cause) {  
		super(message, cause); 
	} 

}

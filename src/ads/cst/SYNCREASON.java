package ads.cst;

/** Used by domain syncronization, indicate the reason of sync. See adsDomainSync. */
public enum SYNCREASON {

	LOAD( "load" ),
	TCLOSE( "tclose" ), 
	MOREDATA( "moredata" ), 
	DBREFUSE( "dbRefuse" ), 
	UNLOAD( "unload" ), 
	POLL( "poll" ); 
	 
	private String lib;  
    
    private SYNCREASON( String abreviation ) {  
        this.lib = abreviation ;  
   }  
     
    public String getAbreviation() {  
        return  this.lib ;  
   }  
}

package ads.cst;

/** Used by domain syncronization, indicate the reason of sync. See adsDomainSync. */
public enum SYNCREASON {

	// ===== MUST BE SAME AS SYNCREASON.AS3 =====
	
	LOAD( "load" ),
	TCLOSE( "tclose" ), 
	MOREDATA( "moredata" ), 
	DBREFUSE( "dbRefuse" ), 
	UNLOAD( "unload" ), 
	POLL( "poll" ),
	RESTART( "restart" ),
	PUBSUB( "pubsub" ),
	RESYNC( "resync" );
	 
	private String lib;  
    
    private SYNCREASON( String abreviation ) {  
        this.lib = abreviation ;  
   }  
     
    public String getAbreviation() {  
        return  this.lib ;  
   }  
}

package ads.cst;

/** Transaction read isolation. */
public enum TXRISO {

	UNC( "ReadUncommited" ),
	COM( "ReadCommited" ), 
	REP( "ReadRepeteable" ), 
	SER( "ReadSerializable" ); 
	 
	private String lib;  
    
    private TXRISO( String abreviation ) {  
        this.lib = abreviation ;  
   }  
     
    public String getAbreviation() {  
        return  this.lib ;  
   }  
}
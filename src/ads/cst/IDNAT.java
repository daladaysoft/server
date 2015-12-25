package ads.cst;

/** Constants that define the ID NATURE. 
 * Warning, NEVER change theses values as they are stored. 
 * Max 8 values (0-7), coded on 3 bits into MdbId. 0 reserved for null id. */
public enum IDNAT {
	
	USER	( "U" ),	// ordinal 0
	MDB		( "M" ), 	// ordinal 1
	TX		( "t" ), 	// ordinal 2
	DOMAIN	( "D" ), 	// ordinal 3
	TABLE	( "T" ), 	// ordinal 4
	ROW		( "R" ),	// ordinal 5
	DICT	( "d" ); 	// ordinal 6
	 
	private String key;  
    
    private IDNAT( String abreviation ) {  
        key = abreviation ;  
   }  
     
    public String getKey() {  
        return key;  
   }  
}

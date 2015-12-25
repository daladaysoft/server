package whp.mdb;

import ads.cst.TXRISO;

/** Extends a transaction, manage inherited transaction as a fast buffer for N transactions. */
public class MdbTxRealBuf extends MdbTxReal {
	
	// ===== static
	// ===== serialized
	// ===== transient
	
	public int length;				// length of current transaction
	public int eventsCount;			// count events in transaction
	public int timer;				// initialized when transaction initialized
	
	// ===== accessors		
	// ===== methods
	
	MdbTxRealBuf reset() {
		length = eventsCount = timer = 0;
		_ttype = 0;
		upddate = 0;
		riso = 0;
		tid = userId = null;
		return this;
	}
	
	/** Same as a constructor, but on current transaction object. 
	 * Always set hasDomain true (dedicated to insert domains only for now). 
	 * Set always transaction id as 't0.0' for now. */
	public void initialize( int ttype, TXRISO riso, String userId ) {
		length = eventsCount = 0;
		timer = ( int ) System.currentTimeMillis();
		
		this.setTtype( ttype );
		this.setHasDomain( 1 );	// set always hasDomain true
		this.upddate = System.currentTimeMillis();
		this.riso = riso.ordinal();
		// TODO: generate TID here (server transaction id : t0.id, t for transaction, 0 for server base id, id from unique date)
		this.tid = "t0.0";
		this.userId = userId;
	}
}


//int _ttype;
//public long upddate;
//public int riso;
//public String tid;
//public String userId;
package whp.mdb;

/** Extends an event, manage inherited event as a fast buffer for N events. */
public class MdbTxEventBuf extends MdbTxEvent 
{
	// ===== static
	// ===== serialized
	// ===== transient
	
	MdbTxEventBuf reset() {
		voprev = -1;
		dbStmnt = transport = 0;
		voId = voTableId = voNextId = 0;
		vo = null;
		return this;
	}
	
	// ===== methods
	
	/** Same as a constructor, but on current event object. */
	public void initialize( int dbStmnt, VOBaseFull vo, long voNextId ) {
		
		this.dbStmnt = dbStmnt;
		
		// get a snap shoot of these VO values:
		this.voprev = 0;	// vo._lastEvent is not managed by server (transient);
		this.voId = vo.getId();
		this.voTableId = vo.getTableId();
		
		this.vo = vo;
		this.voNextId = voNextId;
	}
}
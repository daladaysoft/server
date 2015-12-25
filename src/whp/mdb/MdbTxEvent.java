package whp.mdb;

import java.io.IOException;

import org.granite.messaging.amf.io.AMF3Deserializer;
import org.granite.messaging.amf.io.AMF3Serializer;

/** An MDB event. INSERT, REMOVE, UPDATE, MOVEIN events. */
public class MdbTxEvent {
	
	// ===== static
	
	// events type must be contiguous
	public static final int INSERT = 1;
	public static final int UPDATE = 2;
	public static final int REMOVE = 3;
	public static final int MOVEIN = 4;
	
	// transport
	public static final int TRANSPORT_NO = 0;
	public static final int TRANSPORT_VO = 1;
	public static final int TRANSPORT_ID = 2;
	
	// fields fixed addresses, until first variable length, according writeBytes
	public static final int 
		ETYPE_L = 1, 		ETYPE_P 	= 0,
		VOPREV_L = 8, 		VOPREV_P 	= ETYPE_P + ETYPE_L,
							VOID_P 		= VOPREV_P + VOPREV_L;
	
	// ===== serialized
	
	/** Event dbStmnt (INSERT, ...). */
	public int dbStmnt;
	
	/** Previous VO event direct address. -1 if this is the first VO event (should be INSERT). */
	public long voprev;		// address of previous event, else -1
	
	/** VO id. */
	public long voId;
	
	/** VO table id. */
	public long voTableId;
	
	/** Indicate how VO are transported. */
	public int transport;
	
	/** The VO linked to this event. Can be serialized or not according transport. */
	public VOBaseFull vo;
	
	/** VO next id transported by event if any, else null (according transport). */
	public long voNextId;
	
	// ===== transient
	
	/** Add event record to given bytes, at current position. 
	 * @throws IOException */
	public int writeBytes( AMF3Serializer out ) throws IOException {
		
		int size = out.size();
		
		// common to all events
		out.writeByte( dbStmnt );			// event db statement
		out.writeDouble( voprev );			// last event address (from initialize)
		out.writeLong( voId );				// VO id (from initialize)
		out.writeLong( voTableId );			// VO table id (from initialize)
		
		// event type & transport specific
		switch( dbStmnt ) {
			
			case UPDATE:
				out.writeByte( TRANSPORT_VO );		// transport full VO as object
				out.writeObject( vo );				// transported VO
				break;
			
			case INSERT:
				out.writeByte( TRANSPORT_VO );		// transport full VO as object
				out.writeObject( vo );				// transported VO
				break;
			
			case REMOVE:
				out.writeByte( TRANSPORT_NO );		// transport nothing
				break;
			
			case MOVEIN:
				if( voNextId == 0 ) 
					out.writeByte( TRANSPORT_NO );	// transport nothing
				else {
					out.writeByte( TRANSPORT_ID );	// transport id
					out.writeLong( voNextId );		// transported VO next id
				}
				break;
		}
		return out.size() - size;
	}
	
	/** Read event from in stream. Stream change position during read: readBytes MUST read all
	 * data until end event (= must leave stream 'in' positioned on next DEF). 
	 * @throws IOException */
	public void readBytes( AMF3Deserializer in ) throws IOException {
		
		dbStmnt = in.readByte();
		voprev = in.readLong();
		voId = in.readLong();
		voTableId = in.readLong();
		
		switch( dbStmnt ) {
			
			case UPDATE:
				transport = in.readByte();
				vo = (VOBaseFull) in.readObject();
				break;
			
			case INSERT:		
				transport = in.readByte();
				vo = (VOBaseFull) in.readObject();
				break;
			
			case REMOVE:
				transport = in.readByte();
				vo = null;
				break;
			
			case MOVEIN:
				if( transport == TRANSPORT_ID ) voNextId = in.readLong();
				break;
		}
	}
	
}
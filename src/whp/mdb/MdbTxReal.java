package whp.mdb;

import java.io.IOException;

import org.granite.messaging.amf.io.AMF3Deserializer;
import org.granite.messaging.amf.io.AMF3Serializer;

public class MdbTxReal {

	// ===== static
	
	// fields fixed addresses, until first variable length, according writeBytes
	public static final int 
		TTYPE_L = 1, 		TTYPE_P 	= 0,
		TXO_L = 8, 			TXO_P 		= TTYPE_P + TTYPE_L,
		UPDDATE_L = 8, 		UPDDATE_P 	= TXO_P + TXO_L,
		RISO_L = 8, 		RISO_P 		= UPDDATE_P + UPDDATE_L,
							TID_P 		= RISO_P + RISO_L;
	
	// ===== serialized
	
	/** Transaction type, 0 auto, 1 declared (bit 1), hasDomain 0 or 1 (bit 2). */
	int _ttype;
	
	/** Transaction sequence order, set only by server. The sequence order is updated by the server
	 *  as unique atomic value (directly updated by server in its data, and returned to the initiator 
	 *  client that have to update its local stored transaction). */
	public long txo;
	
	/** Transaction date, set as current date when creation is created (local). If transaction
	 * is server shared, the date is updated by the server as unique atomic date (directly 
	 * updated by server in its data, and returned to the initiator client that have to update
	 * its local stored transaction). */
	public long upddate;
	
	/** Tx read isolation. */
	public int riso;
	
	/** Transaction id, unique. */
	public long tid;
	
	/** User id */
	public long userId;
	
	// ===== transient
	
	// ===== getters,setters
	
	public int getTtype() { return( ( _ttype & 1 ) == 1 ) ? 1 : 0; }
	public void setTtype( int v ) {
		if( v == 0 ) { if( ( _ttype & 1 ) == 1 ) _ttype ^= 1; }
		else if( v == 1 ) _ttype |= 1;
		else throw new Error( "ttype 0 or 1" );
	}
	
	public int getHasDomain() { return( ( _ttype & 2 ) == 2 ) ? 1 : 0; }
	public void setHasDomain( int v ) {
		if( v == 0 ) { if( ( _ttype & 2 ) == 2 ) _ttype ^= 2; }
		else if( v == 1 ) _ttype |= 2;
		else throw new Error( "tdomain 0 or 1" );
	}
	
	// ===== methods
	
	/** Add transaction record to given bytes, at current position.
	 * @throws IOException */
	public int writeBytes( AMF3Serializer out ) throws IOException {
		
		int size = out.size();
		
		out.writeByte( _ttype );
		out.writeLong( txo );
		out.writeDouble( upddate );
		out.writeByte( riso );
		out.writeDouble( tid );
		out.writeDouble( userId );
		
		return out.size() - size;
	}
	
	/** Read transaction from in stream. Stream change position during read: readBytes MUST read all
	 * data until end transaction (= must leave stream 'in' positioned on next DEF).
	 * @throws IOException */
	public void readBytes( AMF3Deserializer in ) throws IOException {
		
		_ttype = in.readUnsignedByte();
		txo = in.readLong();
		upddate = ( long ) in.readDouble();
		riso = in.readUnsignedByte();
		tid = ( long ) in.readDouble();
		userId = ( long ) in.readDouble();
	}
}

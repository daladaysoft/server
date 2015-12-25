package whp;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;

import ads.type.BytesOut;
import flex.messaging.io.ArrayCollection;

/** Store returned values (or references) to a client. 
 * Data is returned as an object if unique, or as an ArrayCollection if more than one data returned. 
 * If bytes is used data must be null, if data is used bytes must be null. 
 * Log is an ArrayCollection, that track text messages. 
 * MoreData is true if they are more data to return to the client before the request can be closed. */
public class Answer implements Externalizable
{	
	// structure of serialized message (int ms, message structure, transported as short: 16 values)
	
	public 	static final int RC		 	= 1;
	public 	static final int DATA 		= 2;
	public 	static final int MORE 		= 4;
	public 	static final int TXO 		= 8;
	public 	static final int LUPD 		= 16;
	public 	static final int LOG		= 32;
	public 	static final int ELAPSE		= 64; 
	public 	static final int BYTES		= 128;
	
	// data that can be transported
	
	private int 				ms;						// message structure
	
	public	int					rc = 0;					// server return code (see RLE codes)
	public	Object 				data = null;			// server data if any
	public	boolean				moreData = false;		// server moreData if any
	public	long				txo = 0;				// server txo if any
	public	Date				lastUpd = null;			// server lastUpd if any
	public 	ArrayCollection		log = null;				// server log if any
	public	long				serverElapse = 0;		// session global duration, if != 0
	public	BytesOut 			bytesOut = null;		// server data as Bytes if any
	
	// constructor
	
	public Answer() {}
	
	// miscellaneous (warning, only computed at writeExternal time)
	
	public boolean isRc() 		{ return ( (ms & RC) == 0 ) ? false : true; }
	public boolean isData()		{ return ( (ms & DATA) == 0 ) ? false : true; }
	public boolean isMoreData()	{ return ( (ms & MORE) == 0 ) ? false : true; }
	public boolean isTxo()		{ return ( (ms & TXO) == 0 ) ? false : true; }
	public boolean isLastUpd()	{ return ( (ms & LUPD) == 0 ) ? false : true; }
	public boolean isLog() 		{ return ( (ms & LOG) == 0 ) ? false : true; }
	public boolean isElapse() 	{ return ( (ms & ELAPSE) == 0 ) ? false : true; }
	public boolean isBytes() 	{ return ( (ms & BYTES) == 0 ) ? false : true; }
	
	// ===== interface Externalizable
	
	// never read
	@Override public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {}
	
	/** Only write needed data (transport optimization), following ms (message structure). */
	@Override public void writeExternal( ObjectOutput out ) throws IOException {
		
		ms = 0;
		if( rc > 0 )			ms += RC;
		if( data != null )		ms += DATA;
		if( moreData )			ms += MORE;
		if( txo != 0 )			ms += TXO;
		if( lastUpd != null )	ms += LUPD;
		if( log != null ) 		ms += LOG;
		if( serverElapse != 0 ) ms += ELAPSE;
		if( bytesOut != null ) 	ms += BYTES;
		
		out.writeShort( ms );
		
		if( isRc() ) 		out.writeInt( rc );					// return code, default 0 (OK)
		if( isData() ) 		out.writeObject( data );			// data, default null (no data)
		if( isMoreData() ) 	out.writeBoolean( moreData );		// moreData, default false (no more data)
		if( isTxo() ) 		out.writeLong( txo );				// txo, default 0
		if( isLastUpd() ) 	out.writeObject( lastUpd );			// lastUpd, default null (no data read)
		if( isLog() )		out.writeObject( log );				// log, default null (no log) 
		if( isElapse() )	out.writeInt( (int) serverElapse );	// if server compute request elapse duration
		if( isBytes() ) 	out.writeObject( bytesOut );		// return BytesOut (to return byte[], see BytesOut.writeExternal)
	}
	
}

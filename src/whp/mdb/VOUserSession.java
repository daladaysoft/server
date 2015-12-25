package whp.mdb;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;

import whp.util.Hex;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/** VODUserSession. As light as possible: updated for each secure request. */
public class VOUserSession extends VOBaseFull 
{
	// ===== constants & static variable
	// ===== serialized variable
	
	public	Date		timestamp;
	public	boolean		active;
	public	long		requests;		// request count for the session
	
	// ---- only in GAE entity
	
	public	long 		bid;			// distant base id
	
	// ===== transient variables
	
	// ===== constructor
	
	public VOUserSession() { super(); }
	public VOUserSession( Entity gaeEntity ) { super( gaeEntity ); }
	
	// ===== storage related properties
	
	/** VOUserSession is stored in root domain. */
	public boolean isDomainRootAllowed() { return true; }
	
	public long getUserId() { return getTableId(); }
	
	public String isConsistent() {
		if( getDomainId() != 0 ) return "userSession domainId must be 0-null";
		if( getTableId() == 0 ) return "userSession tableId is 0-null";
		if( getId() == 0 ) return "userSession Id is 0-null";
		return null;
	}
	
	// ===== VO creation
	
//	public VOUserSession create( Date timestamp, boolean active, long requests, long bid ) {
//		
//		this.timestamp 			= timestamp;
//		this.active 			= active;
//		this.requests 			= requests;
//		this.bid	 			= bid;
//		return this;
//	}
	
	// ===== call backed by parent classes
	
	// ===== specific this VO methods
	
	// ===== standard GAE Entities management
	// ----- store, unstore
	// ----- get a valid GAE Entity
	
	/** Read GAE Entity, unserialize it and return a new instanced VO. */
	public static VOUserSession readEntity( Entity gaeEntity ) {
		VOUserSession vo = new VOUserSession( gaeEntity );
		vo.readEntityBase();
		return vo;
	}
	
	// ----- get a valid GAE Key
	
	/** VOUserSession kind is userKind + userId. */
	public static String getGaeKind( long userId ) {
		return Hex.longToHex( userId );
	}
	
	/** VOUserSession key is root entity, kind (userId), userSessionId (userId and session num) */
	public static Key getKeyFromFactory( long userId, long userSessionId ) {
		return KeyFactory.createKey( getGaeKind( userId ), userSessionId );
	}
	
	// ===== serialize GAE Entity
	
	@Override
	protected Entity readEntityBase() {
		
		Entity gaeEntity = super.readEntityBase();
				
		// this data
		timestamp 	= (Date) gaeEntity.getProperty( "ts" );
		active 		= (Boolean) gaeEntity.getProperty( "ac" );
		requests 	= (Long) gaeEntity.getProperty( "rq" );
		bid	 		= (Long) gaeEntity.getProperty( "bid" );
		
		return gaeEntity;
	}
	
	@Override
	protected Entity writeEntity() throws VOStoreException {
		
		// get entity 
		Entity gaeEntity = super.writeEntity();
		
		// this data
		gaeEntity.setUnindexedProperty( "ts", timestamp );
		gaeEntity.setUnindexedProperty( "ac", active );
		gaeEntity.setUnindexedProperty( "rq", requests );
		gaeEntity.setUnindexedProperty( "bid", bid );
		
		return gaeEntity;
	}
	
	// ===== interface Externalizable
	
	@Override
	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
				
		super.readExternal( in );
		
		timestamp		= ( Date ) in.readObject();
		active 			= in.readBoolean();
		requests 		= in.readLong();
		
	}
	
	@Override
	public void writeExternal( ObjectOutput out ) throws IOException {
				
		super.writeExternal( out );
		
		out.writeObject( timestamp );
		out.writeBoolean( active );
		out.writeLong( requests );
		
	}
}

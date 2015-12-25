package whp.mdb;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class VODomain extends VOBaseTable 
{
	// ===== constants & static variable
	
	public static final String kind = "D";
	
	// bit 0 jnal, (1, 2, 3) unused, 4 memsnap, 5 memjnal, 6 local, 7 remote
	public static final int STORAGE_MEMSNAP = 16;
	public static final int STORAGE_MEMORY = 17;
	public static final int STORAGE_LOCAL = 49;
	public static final int STORAGE_REMOTE = 113;
	
	// ===== serialized variable
	
	/** Cache for the tableId of this domain (= its parent domain).
	 * Managed by VOBaseFull, not here: see getTableId and setTableId.  */
	long _domainParentId;
	
	public	Byte			storage;
	public 	boolean			isUnique;
	public 	Byte			risoDefaultId;
	
	// ===== transient variables
	
	// ===== constructor
	
	public VODomain() { super(); }
	public VODomain( Entity gaeEntity ) { super( gaeEntity ); }
	
	// ===== storage related properties
	
	/** VODomain */
	public String isConsistent() {
		if( getDomainId() != 0 ) return "domainId not 0-null";
		if( getTableId() == 0 ) return "tableId is 0-null";
		if( getId() == 0 ) return "id is 0-null";
		return null;
	}
	
	// ===== VO creation
	// ===== call backed by parent classes
	// ===== specific this VO methods
	
	// ===== standard GAE Entities management
	// ----- store, unstore
	// ----- get a valid GAE Entity
	
	/** Read GAE Entity, unserialize it and return a new instanced VO. */
	public static VODomain readEntity( Entity gaeEntity ) {
		
		VODomain vo = new VODomain( gaeEntity );
		vo.readEntityBase();
		return vo;
	}
	
	// ----- GAE Key services
	
	/** VODomain key. */
	public static Key getKeyFromFactory( long domainId ) {
		return KeyFactory.createKey( kind, domainId );
	}
	
	// ===== serialize GAE Entity
	
	@Override
	protected Entity readEntityBase() {
		
		Entity gaeEntity = super.readEntityBase();
				
		// this data
		_domainParentId 		= (Long) gaeEntity.getProperty( "dpId" );
		storage			 		= (Byte) gaeEntity.getProperty( "sto" );
		isUnique		 		= (Boolean) gaeEntity.getProperty( "unique" );
		risoDefaultId			= (Byte) gaeEntity.getProperty( "riso" );
		
		return gaeEntity;
	}
	
	
	@Override
	protected Entity writeEntity() throws VOStoreException {
		
		// get entity 
		Entity gaeEntity = super.writeEntity();
		
		// set this entity properties
		gaeEntity.setProperty( 			"dpId", 		_domainParentId );
		gaeEntity.setUnindexedProperty( "sto", 			storage );
		gaeEntity.setUnindexedProperty( "unique", 		isUnique );
		gaeEntity.setUnindexedProperty( "riso", 		risoDefaultId );
		
		return gaeEntity;
	}
	
	// ===== interface Externalizable
	
	// note: internal _domainParentId is managed as tableId by VOBaseFull

	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
		
		super.readExternal( in );
		storage 					= in.readByte();
		isUnique					= in.readBoolean();
		risoDefaultId				= in.readByte();
	}
	
	public void writeExternal( ObjectOutput out ) throws IOException {
		
		super.writeExternal( out );
		out.writeByte( storage );
		out.writeBoolean( isUnique );
		out.writeByte( risoDefaultId );
	}
	
		
}

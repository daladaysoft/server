package whp.mdb;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/** VOMdb */
public class VOMdb extends VOBaseFull
{
	// ===== constants & static variable
	
	public static final String kind = "M";
	
	// ===== serialized variable
	
//	public 	long			DidCounter;			// domain id
//	public 	long			TidCounter;			// table id
//	public 	long			RidCounter;			// row id
//	public 	long			tidCounter;			// transaction id
//	public 	long			eidCounter;			// event id
//	public 	long			iidCounter;			// item id (unstructured)
	public	String			desc;				// description
	
	/** serialized in Entity, but not for network (see RPC to revoke a VO) */ 
	public 	boolean			revoked = false;	// if true, this MDB cannot login
	
	// ===== transient variables
	
	// ===== constructor
	
	public VOMdb() { super(); }
	public VOMdb( Entity gaeEntity ) { super( gaeEntity ); }
	
	// ===== storage related properties
	
	/** VOMdb is stored in root domain. */
	public boolean isDomainRootAllowed() { return true; }
	
	public String isConsistent() {
		if( getDomainId() != 0 ) return "mdb domainId must be 0-null";
		if( getKind() != kind ) return "mdb kind not " + kind;
		if( getId() == 0 ) return "mdb Id is 0-null";
		return null;
	}
	
	// ===== VO creation
	
//	/** Never used. */	
//	public VOMdb create() { return this; }
	
	// ===== call backed by parent classes
	
	protected boolean isAutoGlobalId() { return false; }
	
	// ===== specific this VO methods
	
	// ===== standard GAE Entities management
	// ----- store, unstore
	// ----- get a valid GAE Entity
	
	/** Read GAE Entity, unserialize it and return a new instanced VO. */
	public static VOMdb readEntity( Entity gaeEntity ) {
		VOMdb vo = new VOMdb( gaeEntity );
		vo.readEntityBase();
		return vo;
	}
	
	// ----- GAE Key services
	
	/** VOMdb key is mdb nature. The given bid must be mdb nature. 
	 * If bid not correct, return a bad refused key, that cannot be found. */
	public static Key getKeyFromFactory( long bid ) {
		if( bid == 0 ) return KeyFactory.createKey( "REFUSED", 1 );
		else return KeyFactory.createKey( kind, bid );
	}

	// ===== serialize GAE Entity
	
	@Override
	protected Entity readEntityBase() {
		
		Entity gaeEntity = super.readEntityBase();
				
		// this data		
//		DidCounter		 	= (Long) gaeEntity.getProperty( "Did" );
//		TidCounter		 	= (Long) gaeEntity.getProperty( "Tid" );
//		RidCounter 			= (Long) gaeEntity.getProperty( "Rid" );
//		tidCounter 			= (Long) gaeEntity.getProperty( "tid" );
//		eidCounter 			= (Long) gaeEntity.getProperty( "eid" );
//		iidCounter 			= (Long) gaeEntity.getProperty( "iid" );
		desc 				= (String) gaeEntity.getProperty( "desc" );
		revoked				= (Boolean) gaeEntity.getProperty( "del" );
		
		return gaeEntity;
	}
	
	@Override
	protected Entity writeEntity() throws VOStoreException {
		
		// get entity 
		Entity gaeEntity = super.writeEntity();
		
		// this data		
//		gaeEntity.setUnindexedProperty( "Did", 		DidCounter );
//		gaeEntity.setUnindexedProperty( "Tid", 		TidCounter );
//		gaeEntity.setUnindexedProperty( "Rid", 		RidCounter );
//		gaeEntity.setUnindexedProperty( "tid", 		tidCounter );
//		gaeEntity.setUnindexedProperty( "eid", 		eidCounter );
//		gaeEntity.setUnindexedProperty( "iid", 		iidCounter );
		gaeEntity.setUnindexedProperty( "desc", 	desc );
		gaeEntity.setUnindexedProperty( "del", 		revoked );
		
		return gaeEntity;
	}
	
	// ===== interface Externalizable
	
	@Override
	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
				
		super.readExternal( in );
		
//		DidCounter 			= in.readInt();
//		TidCounter 			= in.readInt();
//		RidCounter 			= in.readInt();
//		tidCounter 			= in.readInt();
//		eidCounter 			= in.readInt();
//		iidCounter 			= in.readInt();
		desc 				= in.readUTF();
		
	}
	
	@Override
	public void writeExternal( ObjectOutput out ) throws IOException {
				
		super.writeExternal( out );
		
//		out.writeInt( (int) DidCounter );
//		out.writeInt( (int) TidCounter );
//		out.writeInt( (int) RidCounter );
//		out.writeInt( (int) tidCounter );
//		out.writeInt( (int) eidCounter );
//		out.writeInt( (int) iidCounter );
		out.writeUTF( desc );
		
	}
}

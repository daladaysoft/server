package whp.dict;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.google.appengine.api.datastore.Entity;

import whp.mdb.VOBaseTable;
import whp.mdb.VOStoreException;

public class VODictEntities extends VOBaseTable
{
	// ===== constants & static variable
	
	// ===== serialized variable

	// ===== transient variables
	
	// ===== constructor
	
	public VODictEntities() { super(); }
	public VODictEntities( Entity gaeEntity ) { super( gaeEntity ); }
	
	// ===== storage related properties

	// ===== VO creation
	
	// ===== call backed by parent classes
		
	// ===== specific this VO methods
	
	// ===== standard GAE Entities management
	// ----- store, unstore
	// ----- get a valid GAE Entity
	
	/** Read GAE Entity, unserialize it and return a new instanced VO. */
	public static VODictDefEntity readEntity( Entity gaeEntity ) {
		VODictDefEntity vo = new VODictDefEntity( gaeEntity );
		vo.readEntityBase();
		return vo;
	}
	
	// ----- GAE Key services
	
//	/** VODictDefEntity kind is tableId ? */
//	public static String getGaeKind() {
//		return null;
//	}
//	
//	/** VODictDefEntity key is tableId + id ? */
//	public static Key getKeyFromFactory() {
//		return null;
//	}
	
	// ===== serialize GAE Entity
	
	@Override
	protected Entity readEntityBase() {
		
		Entity gaeEntity = super.readEntityBase();
				
		// this data		
		
		return gaeEntity;
	}
	
	@Override
	protected Entity writeEntity() throws VOStoreException {
		
		// get entity 
		Entity gaeEntity = super.writeEntity();
		
		// this data		
		
		return gaeEntity;
	}
	
	// ===== interface Externalizable
	
	@Override
	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
				
		super.readExternal( in );
	}
	
	@Override
	public void writeExternal( ObjectOutput out ) throws IOException {
				
		super.writeExternal( out );
		
	}
}

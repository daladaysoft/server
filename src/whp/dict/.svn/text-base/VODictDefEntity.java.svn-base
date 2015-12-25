package whp.dict;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import whp.mdb.VOBaseTable;
import whp.mdb.VOStoreException;

import com.google.appengine.api.datastore.Entity;

public class VODictDefEntity extends VOBaseTable 
{
	// ===== constants & static variable
	
	public static final long ROW = 1;
	public static final long TABLE = 2;
	public static final long ROWTABLE = 3;
	
	// ===== serialized variable
	
	private int 			type;				// ROW, TABLE, ROWTABLE
	public	String			desc;				// description
	
	// ===== transient variables
	
	// ===== constructor
	
	public VODictDefEntity() { super(); }
	public VODictDefEntity( Entity gaeEntity ) { super( gaeEntity ); }
	
	// ===== storage related properties
	
	public String getTypeLib() {
		String str = "";
		if( (type & ROW) != 0 ) str += "ROW";
		if( (type & TABLE) != 0 ) str += "TABLE";
		return str;
	}
	
	// ===== VO creation
	
	/** Never used. */	
	public VODictDefEntity create() { return this; }
	
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
		type		 		= (Integer) gaeEntity.getProperty( "type" );
		desc 				= (String) gaeEntity.getProperty( "desc" );
		
		return gaeEntity;
	}
	
	@Override
	protected Entity writeEntity() throws VOStoreException {
		
		// get entity 
		Entity gaeEntity = super.writeEntity();
		
		// this data		
		gaeEntity.setUnindexedProperty( "type", 	type );
		gaeEntity.setUnindexedProperty( "desc", 	desc );
		
		return gaeEntity;
	}
	
	// ===== interface Externalizable
	
	@Override
	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
				
		super.readExternal( in );
		
		type 				= in.readShort();
		desc 				= in.readUTF();
	}
	
	@Override
	public void writeExternal( ObjectOutput out ) throws IOException {
				
		super.writeExternal( out );
		
		out.writeShort( type );
		out.writeUTF( desc );
		
	}
}

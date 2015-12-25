package whp.dict;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import whp.mdb.VOBaseFull;
import whp.mdb.VOStoreException;

import com.google.appengine.api.datastore.Entity;

public class VODictDefProp extends VOBaseFull
{
	// ===== constants & static variable
	
	// ===== serialized variable
	
	public int		type;				// 0 public variable, 1 public accessor
	public String 	typeFullName;		// type full class name
	public Boolean 	isComplex;			// should be true if typeName != typeFullName (type is class)
	public Boolean 	isInterface;		// should be complex
	public Boolean 	isDynamic;			// should be only true if complex and not interface
	public int 		visibility;			// always public 0 for now
	public int 		access;				// 0 read only, 1 readwrite
	public String 	declaringFullName;	// declared by full class name
	public String	desc;				// description
	
	// ===== transient variables
	
	// ===== constructor
	
	public VODictDefProp() { super(); }
	public VODictDefProp( Entity gaeEntity ) { super( gaeEntity ); }
	
	// ===== storage related properties
	
	// ===== VO creation
	
	/** Never used. */	
	public VODictDefProp create() { return this; }
	
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
	
//	/** VODictDefProp kind is tableId ? */
//	public static String getGaeKind() {
//		return null;
//	}
//	
//	/** VODictDefProp key is tableId + id ? */
//	public static Key getKeyFromFactory() {
//		return null;
//	}
	
	// ===== serialize GAE Entity
	
	@Override
	protected Entity readEntityBase() {
		
		Entity gaeEntity = super.readEntityBase();
				
		// this data		
		type		 		= (Integer) gaeEntity.getProperty( "type" );
		typeFullName		= (String) gaeEntity.getProperty( "tfn" );
		isComplex		 	= (Boolean) gaeEntity.getProperty( "cplx" );
		isInterface		 	= (Boolean) gaeEntity.getProperty( "intf" );
		isDynamic		 	= (Boolean) gaeEntity.getProperty( "dyn" );
		visibility		 	= (Integer) gaeEntity.getProperty( "vis" );
		access		 		= (Integer) gaeEntity.getProperty( "acs" );
		declaringFullName	= (String) gaeEntity.getProperty( "dfn" );
		desc		 		= (String) gaeEntity.getProperty( "desc" );
		
		return gaeEntity;
	}
	
	@Override
	protected Entity writeEntity() throws VOStoreException {
		
		// get entity 
		Entity gaeEntity = super.writeEntity();
		
		// this data		
		gaeEntity.setUnindexedProperty( "type", 	type );
		gaeEntity.setUnindexedProperty( "tfn", 		typeFullName );
		gaeEntity.setUnindexedProperty( "cplx", 	isComplex );
		gaeEntity.setUnindexedProperty( "intf", 	isInterface );
		gaeEntity.setUnindexedProperty( "dyn", 		isDynamic );
		gaeEntity.setUnindexedProperty( "vis", 		visibility );
		gaeEntity.setUnindexedProperty( "acs", 		access );
		gaeEntity.setUnindexedProperty( "dfn", 		declaringFullName );
		gaeEntity.setUnindexedProperty( "desc", 	desc );
		
		return gaeEntity;
	}
	
	// ===== interface Externalizable
	
	@Override
	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
				
		super.readExternal( in );
		
		type 				= in.readShort();
		typeFullName 		= in.readUTF();
		isComplex 			= in.readBoolean();
		isInterface 		= in.readBoolean();
		isDynamic 			= in.readBoolean();
		visibility 			= in.readShort();
		access 				= in.readShort();
		declaringFullName 	= in.readUTF();
		desc 				= in.readUTF();
		
	}
	
	@Override
	public void writeExternal( ObjectOutput out ) throws IOException {
				
		super.writeExternal( out );
		
		out.writeInt( type );
		out.writeUTF( typeFullName );
		out.writeBoolean( isComplex );
		out.writeBoolean( isInterface );
		out.writeBoolean( isDynamic );
		out.writeShort( visibility );
		out.writeShort( access );
		out.writeUTF( declaringFullName );
		out.writeUTF( desc );
		
	}
}

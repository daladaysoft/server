package whp.mdb;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.google.appengine.api.datastore.Entity;

public abstract class VOTable extends VOBaseTable 
{
	// ===== constants & static variable
	// ===== serialized variable
	
	public	String			qualifier;
	public	String			desc;
	
	// ===== transient variables
	
	// ===== constructor
	
	public VOTable() { super(); }
	public VOTable( Entity gaeEntity ) { super( gaeEntity ); }
	
	// ===== storage related properties
	// ===== VO creation
	// ===== call backed by parent classes
	// ===== specific this VO methods
	
	// ===== standard GAE Entities management
	// ----- store, unstore
	// ----- get a valid GAE Entity
	// ----- GAE Key services
	// ===== serialize GAE Entity
	
	@Override
	protected Entity readEntityBase() {
		
		Entity gaeEntity = super.readEntityBase();
				
		// this data
		qualifier 			= (String) gaeEntity.getProperty( "qn" );
		desc	 			= (String) gaeEntity.getProperty( "desc" );
		
		return gaeEntity;
	}
	
	@Override
	protected Entity writeEntity() throws VOStoreException {
		
		// get entity 
		Entity gaeEntity = super.writeEntity();
		
		// set this entity properties
		gaeEntity.setUnindexedProperty( "qn", 		qualifier );
		gaeEntity.setUnindexedProperty( "desc", 	desc );
		
		return gaeEntity;
	}
	
// ===== interface Externalizable
	
	// note: internal _domainParentId is managed as tableId by VOBaseFull

	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
		
		super.readExternal( in );
		qualifier 			= in.readUTF();
		desc	 			= in.readUTF();
		
	}
	
	public void writeExternal( ObjectOutput out ) throws IOException {
		
		super.writeExternal( out );
		out.writeUTF( qualifier );
		out.writeUTF( desc );
		
	}
	
}

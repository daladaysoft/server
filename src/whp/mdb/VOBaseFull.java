package whp.mdb;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.google.appengine.api.datastore.Entity;

public abstract class VOBaseFull extends VOBaseId implements Externalizable
{
	// ===== constants & static variable
	// ===== serialized variable
	
	public	String			name;
	
	// ===== transient variables
	
	// ===== constructor
	
	public VOBaseFull() { super(); }
	public VOBaseFull( Entity gaeEntity ) { super( gaeEntity ); }
	
	// ===== storage related properties
	// ===== call backed by parent classes
	// ===== specific this VO methods
	
	// ===== standard GAE Entities management
	
	// ----- GAE Key services
	
	// ===== serialize GAE Entity
	
	@Override
	protected Entity readEntityBase() {
		
		// get entity 
		Entity gaeEntity = super.readEntityBase();
		
		name				= (String) gaeEntity.getProperty( "name" );
		
		return gaeEntity;
	}
	
	@Override
	protected Entity writeEntity() throws VOStoreException {
		
		// get entity 
		Entity gaeEntity = super.writeEntity();
		
		// set this entity properties
		gaeEntity.setUnindexedProperty( "name", name );
		
		return gaeEntity;
	}
	
	// ===== interface Externalizable
	
	@Override
	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
		
		super.readExternal( in );
		name = in.readUTF();
	}
	
	@Override
	public void writeExternal( ObjectOutput out ) throws IOException {
		
		super.writeExternal( out );
		out.writeUTF( ( name != null ) ? name : "" );
	}
}

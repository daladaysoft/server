package whp.indd.dom;

import com.google.appengine.api.datastore.Entity;

import whp.mdb.VODomain;

public class VODocument extends VODomain 
{
	// ===== constants & static variable
	// ===== serialized variable
	// ===== transient variables
	
	// ===== constructor
	
	public VODocument() { super(); }
	public VODocument( Entity gaeEntity ) { super( gaeEntity ); }
	
	// ===== storage related properties
	
	// ===== VO creation
	// ===== call backed by parent classes
	// ===== specific this VO methods
	
	// ===== standard GAE Entities management
	// ----- store, unstore
	// ----- get a valid GAE Entity
	// ----- GAE Key services
	// ===== serialize GAE Entity
	
	/** Read GAE Entity, unserialize it and return a new instanced VO. */
	public static VODocument readEntity( Entity gaeEntity ) {
		VODocument vo = new VODocument( gaeEntity );
		vo.readEntityBase();
		return vo;
	}
	
	// ===== interface Externalizable

}

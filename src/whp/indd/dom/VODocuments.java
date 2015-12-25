package whp.indd.dom;

import com.google.appengine.api.datastore.Entity;

import whp.mdb.VODomain;

public class VODocuments extends VODomain 
{
	// ===== constants & static variable
	// ===== serialized variable
	// ===== transient variables
	
	// ===== constructor
	
	public VODocuments() { super(); }
	public VODocuments( Entity gaeEntity ) { super( gaeEntity ); }
	
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
	public static VODocuments readEntity( Entity gaeEntity ) {
		VODocuments vo = new VODocuments( gaeEntity );
		vo.readEntityBase();
		return vo;
	}
	
	// ===== interface Externalizable

}

package whp.mdb;

import com.google.appengine.api.datastore.Entity;

public class VODictionary extends VODomain 
{
	// ===== constants & static variable
	// ===== serialized variable
	// ===== transient variables
	
	// ===== constructor
	
	public VODictionary() { super(); }
	public VODictionary( Entity gaeEntity ) { super( gaeEntity ); }
	
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
	public static VODictionary readEntity( Entity gaeEntity ) {
		VODictionary vo = new VODictionary( gaeEntity );
		vo.readEntityBase();
		return vo;
	}
	
	// ===== interface Externalizable

}

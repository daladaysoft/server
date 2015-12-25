package whp.mdb;

import com.google.appengine.api.datastore.Entity;

public class VODomainRoot extends VODomain 
{
	// ===== constants & static variable
	
	public static final String kind = "D";
	
	// ===== serialized variable
	// ===== transient variables
	
	// ===== constructor
	
	public VODomainRoot() { super(); }
	public VODomainRoot( Entity gaeEntity ) { super( gaeEntity ); }
	
	// ===== storage related properties
	
	/** VODomainRoot */
	public String isConsistent() {
		if( getDomainId() != 0 ) return "domainId not null";
		if( ! MdbId.isIdDomainRoot( getId() ) ) return "id not root domain D0.0";
		if( getTableId() != 0 ) return "tableId is not 0-null";
		return null;
	}
	
	// ===== VO creation
	// ===== call backed by parent classes
	// ===== specific this VO methods
	
	// ===== standard GAE Entities management
	// ----- store, unstore
	// ----- get a valid GAE Entity
	// ----- GAE Key services
	// ===== serialize GAE Entity
	
	/** Read GAE Entity, unserialize it and return a new instanced VO. */
	public static VODomainRoot readEntity( Entity gaeEntity ) {
		VODomainRoot vo = new VODomainRoot( gaeEntity );
		vo.readEntityBase();
		return vo;
	}
	
	// ===== interface Externalizable

}

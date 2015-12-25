package whp.mdb;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.ParseException;

import whp.gae.RLE;
import whp.util.Hex;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public abstract class VOBaseFull extends RLE implements Externalizable
{
	// ===== constants & static variable
	
	private static final double VERSION = 0.01;
	
	// ===== serialized variable
	
	public	double	 		version;
	
	/** Can be VOKey (temporary store domainId, voId, voTableId) if GAE Entity not exists,
	 * that is the case when the VO is just unserialized, else voEntity is Entity, that
	 * contains (domainId, voId, voTableId) in its Key.
	 * Getters for domainId, voId, voTableId manage these situations. */
	private Object		voEntity;
	
	public	String			name;
	
	// ===== transient variables
	
	// ===== constructor
	
	/** Default constructor, without entity. Set voEntity with temporary key VOKey. */
	public VOBaseFull() { 
		voEntity = new VOKey(); 
	}	
	
	/** The GAE Entity is know. Set voEntity as Entity (avoid instantiation of temporary key storage). */
	public VOBaseFull( Entity gaeEntity ) {
		voEntity = gaeEntity;
	}
	
	// ===== storage related properties
	
	// ----- domain
	
	/** Return domainId, 0 if no domainId. Used as GAE Key ancestor if this is not VODomain. */
	public final long getDomainId() { 
		if( voEntity instanceof VOKey ) {
			VOKey vokey = (VOKey) voEntity;
			return vokey.domainId;
		}
		if( voEntity instanceof Entity ) {
			Key key = ((Entity) voEntity).getKey();
			Key parentKey = key.getParent();
			if( parentKey != null ) return parentKey.getId();
		}
		return 0;
	}
	
	/** Return domainId (explained) if this is (VOKey | Entity), else "" (null id). */
	public final String getDomainIdExplained() {
		long id = getDomainId();
		return( id > 0 ) ? MdbId.idExplained( id ) : ""; 
	}
	
	/** Set domainId, return domainId if was not already set, else thow error (domainId is 
	 * write-once, cannot be changed). Used as GAE Key ancestor if this is not VODomain.*/
	public final void setDomainId( long domainId ) {
		if( voEntity instanceof VOKey ) {
			VOKey vokey = (VOKey) voEntity;
			if( vokey.domainId == 0 ) vokey.domainId = domainId;
		}
		else throw new Error( "setDomainId can only be set once, and when voEntity is VOKey" );
	}
	
	/** Allows specialized classes to be stored in root domain. Do not use
	 * this callback on other classes (they should not be allowed to be stored
	 * in the root domain). Not checked for domains that are root entities. */
	public boolean isDomainRootAllowed() { return false; }
	
	// ----- tableKind
	
	/** Return tableId if there is one, else 0. Used as GAE Key Kind. */
	public final long getTableId() { 
		if( voEntity instanceof VOKey ) {
			VOKey vokey = (VOKey) voEntity;
			return vokey.getTableId();
		}
		if( voEntity instanceof Entity ) {
			if( this instanceof VODomain ) return ((VODomain) this)._domainParentId;
			Key key = ((Entity) voEntity).getKey();
			return key.getId();
		}
		return 0;
	}
	
	/** Return kind if there is one, else null. Used as GAE Key Kind. */
	public final String getKind() { 
		if( voEntity instanceof VOKey ) return ((VOKey) voEntity).getKind();
		if( voEntity instanceof Entity ) return ((Entity) voEntity).getKey().getKind();
		return null;
	}
	
	/** Set tableId, if was not already set, else thow error (tableId is write-once, 
	 * cannot be changed). */
	public final void setTableId( long tableId ) {
		if( voEntity instanceof VOKey ) {
			VOKey vokey = (VOKey) voEntity;
			if( vokey.tableKind == null ) vokey.tableKind = Hex.longToHex( tableId );
		}
		else throw new Error( "setTableId can only be set once, and when voEntity is VOKey" );
	}
	
	/** Set kind, if was not already set, else thow error (kind is write-once, 
	 * cannot be changed). */
	public final void setKind( String kind ) {
		if( voEntity instanceof VOKey ) {
			VOKey vokey = (VOKey) voEntity;
			if( vokey.tableKind == null ) vokey.tableKind = kind;
		}
		else throw new Error( "setKind can only be set once, and when voEntity is VOKey" );
	}
	
	/** Return tableId(explained) or kind if this is (VOKey | Entity), else null. */
	public final String getTableKindExplained() {
		long id = getTableId();
		return( id > 0 ) ? MdbId.idExplained( id ) : getKind(); 
	}
	
	// ----- table / kind resolved as ads id long
	
	/** Return tableId as clients see it (from kind hex if set, or from kind translation).
	 * Throw error if kind is not set (not tableId, not valid kind). */
	public final long getTableIdResolved() {
		
		long tableId = 0;
		String kind = null;
		
		tableId = this instanceof VODomain ? ((VODomain) this)._domainParentId : getTableId();
		if( tableId <= 0 ) kind = getKind();
		
		if( kind != null ) {
			switch( kind ) {
				case VOUser.kind:	tableId = MdbId.getIdTableUsers();	break;
				case VOMdb.kind:	tableId = MdbId.getIdTableMdbs();	break;
				default: throw new Error( "Cannot resolve kind '" + kind + "' as tableId." );
			}
		}
		
		if( tableId <= 0 ) throw new Error( "Cannot resolve tableid (kind null, tableId 0)" );
		
		return tableId;
	}
	
	// ----- id
	
	/** Set id, if was not already set, else return 0 (id is write-once, 
	 * cannot be changed). */
	public final long getId() { 
		if( voEntity instanceof VOKey ) return ((VOKey) voEntity).id;
		if( voEntity instanceof Entity ) return ((Entity) voEntity).getKey().getId();
		return 0;
	}
	
	/** Return id (explained) if this is (VOKey | Entity), else "" (null id). */
	public final String getIdExplained() {
		long id = getId();
		return( id > 0 ) ? MdbId.idExplained( id ) : ""; 
	}
	
	/** Set id, if was not already set, else thow error (id is write-once, 
	 * cannot be changed). */
	public final void setId( long id ) {
		if( voEntity instanceof VOKey ) {
			VOKey vokey = (VOKey) voEntity;
			if( vokey.id == 0 ) vokey.id = id;
		}
		else throw new Error( "setId can only be set once, and when voEntity is VOKey" );
	}

	// ----- get info
	
	/** Return key info. Assert a WHP key, and name provided even if long id used (test is necessary). */
	public final String getKeyInfo() {
		if( voEntity == null ) return "null";
		if( voEntity instanceof VOKey ) return ((VOKey) voEntity).getInfo();
		if( voEntity instanceof Entity ) {
			Key key = ((Entity) voEntity).getKey();
			if( key == null ) return "Key null";
			Key keyParent = key.getParent();
			String kindStr = key.getKind();
			String s1 = keyParent == null ? "RootKey" : "domainId " + MdbId.idExplained( keyParent.getId() );
			String s2 = kindStr.length() == 16 ? ", tableId " + MdbId.idExplained( hexToLongNoErr( kindStr ) ) : ", kind " + kindStr;
			String s3 = ", id " + MdbId.idExplained( key.getId() );
			return "DSKey (" + s1 + s2 + s3 + ")";
		}
		return "ERROR, voEntity not null, not VOKey, not Entity...";
	}
	
	// ----- is consistent
	
	/** Return null if isConsistent, else return a message. 
	 * Check here a standard VO (not in root domain) consistency. 
	 * (domains override isConsistent). */
	public String isConsistent() {
		if( getDomainId() == 0 ) return "domainId is 0-null (see setDomainId)";
		if( getTableId() == 0 && getKind() == null ) return "tableId is 0-null and kind is null";
		if( getId() == 0 ) return "id is 0-null";
		return null;
	}
	
	// ===== call backed by parent classes
	
	/** Allows a extend class to declare an id that is global (generated by the 
	 * server) and can be not WHP unique (as could be an GAE auto long id). 
	 * Used by VOUser entities). Default is false. */
	protected boolean isAutoGlobalId() { return false; }
	
	// ===== specific this VO methods
	
	// ===== standard GAE Entities management
	
	// ----- store, unstore
	
	/** Store GAE Entity (create a new or reuse previous). Warning, the domainId MUST 
	 * be set (setDomainId) before store can be used if the VO is a new inserted VO, 
	 * else store will throw VOStoreException. The ds.put() maintains accurate the Key of
	 * the Entity: because this, voEntity get the correct key if set by GAE (auto long id). */
	public void store() throws VOStoreException {
		
		// check isConsistent before attempt to store the VO
		if( isConsistent() != null ) throw new VOStoreException( isConsistent() );
		
		// store GAE entity. This set the id if given by GAE.
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Entity gaeEntity = writeEntity();
		ds.put( gaeEntity );
	}
	
	/** Not implemented. */
	public void unstore() throws VOStoreException {}
	
	// ----- get a valid GAE Entity
	
	/** Create a new GAE entity if not already acquired (and store it in voEntity),
	 * else reuse voEntity. All GAE Key construction rules for inherited classes 
	 * (VOBaseFull, VOBaseTable, VODomain ...) are managed in this method (that 
	 * cannot be override). 
	 * @throws VOStoreException */
	private Entity getEntity() throws VOStoreException {
		
		// if voEntity is already an Entity return it
		if( voEntity instanceof Entity ) return (Entity) voEntity;
		
		// create Entity from temporary (domainId, voId, voTableId) stored in VOKey
		VOKey voKey = (VOKey) voEntity;
		
		// if this is a domain: simple root entity "D" + id
		if( this instanceof VODomain ) {
			
			// save the tableId as parent domain
			((VODomain) this)._domainParentId = voKey.getTableId();
			
			// create GAE Key for a VODomain : kind "D", name id, root Entity (no ancestor)
			voEntity = new Entity( VODomain.kind, voKey.id );
			return (Entity) voEntity;
			
		} 
		
		// set domain to D0.0 automatically if null and allowed
		if( voKey.domainId == 0 && ! isDomainRootAllowed() )
			throw new VOStoreException( "domainId is 0-null on " + voKey.getInfo() );
		
		// if standard Name key	
		if( isAutoGlobalId() == false ) {
	
			// create GAE Key for a VOBaseFull and VOBaseTable (same rules)
			if( voKey.domainId == 0 ) {
				// there is no ancestor nor transactions in domain root
				voEntity = new Entity( voKey.tableKind, voKey.id );
			} else {
				// else VO have their domain as root entity (transactions are available)
				Key parentKey = KeyFactory.createKey( VODomain.kind, voKey.domainId );
				voEntity = new Entity( voKey.tableKind, voKey.id, parentKey );
			}
			return (Entity) voEntity;
			
		}
		
		// if Key is long auto by GAE, and exists (reuse id) NOTE seems never used ?
		if( voKey.id != 0 ) {
			
			// create GAE Key for a VOBaseFull and VOBaseTable (same rules)
			if( voKey.domainId == 0 ) {
				// there is no ancestor nor transactions in domain root
				voEntity = new Entity( voKey.tableKind, voKey.id );
			} else {
				// else VO have their domain as root entity (transactions are available)
				Key parentKey = KeyFactory.createKey( VODomain.kind, voKey.domainId );
				voEntity = new Entity( voKey.tableKind, voKey.id, parentKey );
			}
			return (Entity) voEntity;
		}
		
		// if Key is long auto by GAE, and not exists (id will be available after store)
		if( voKey.domainId == 0 ) {
			// there is no ancestor nor transactions in domain root
			voEntity = new Entity( voKey.tableKind );
		} else {
			// else VO have their domain as root entity (transactions are available)
			Key parentKey = KeyFactory.createKey( VODomain.kind, voKey.domainId );
			voEntity = new Entity( voKey.tableKind, parentKey );
		}
		return (Entity) voEntity;
	
	}
	
	// Template for implement readEntity in extended class VOExample 
	// Read GAE Entity, unserialize it and return a new instanced VO:
	//		public static VOExample readEntity( Entity gaeEntity ) {
	//			VOExample vo = new VOExample( gaeEntity );
	//			vo.readEntityBase();
	//			return vo;
	//		}
	
	// ----- GAE Key services
	
	/** Return GAE Key if exists, else make a new Entity (not stored)
	 * and return Key.
	 * @throws VOStoreException */
	public final Key getKey() throws VOStoreException { 
		return getEntity().getKey();
	}
	
	// ----- services
	
	/** Return hex id as long if valid hex id, else 0-null. */
	static long hexToLongNoErr( String sid ) {
		try { return Hex.hexToLong( sid ); } 
		catch (ParseException e) { return 0; }
	}
	
	// static methods dedicated to specialized classes when entity is reachable from the domain
	// (for now: VODomain, VOUser, VOUserSession, VOMdb):
	// 		public static String getGaeKind( ... )
	// 		public static Key getKeyFromFactory( ... )
	
	// ===== serialize GAE Entity
	
	protected Entity readEntityBase() {
		
		Entity gaeEntity 	= (Entity) voEntity;
		
		version 			= (Double) gaeEntity.getProperty( "v" );
		name				= (String) gaeEntity.getProperty( "name" );
		
		return gaeEntity;
	}
	
	protected Entity writeEntity() throws VOStoreException {
		
		// get entity 
		Entity gaeEntity = getEntity();
		
		// set this entity properties
		gaeEntity.setUnindexedProperty( "v", version );				
		gaeEntity.setUnindexedProperty( "name", name );
		
		return gaeEntity;
	}
	
	// ===== interface Externalizable
	
	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
		
		version = in.readDouble();
		if( Double.isNaN( version ) ) version = 0;
		
		// use setTableId and setId here ? (if voEntity was Entity, not VOKey ?)
		VOKey key = (VOKey) voEntity;
		
		key.tableKind = Hex.longToHex( in.readLong() );
		key.id = in.readLong();
		name = in.readUTF();
	}
	
	public void writeExternal( ObjectOutput out ) throws IOException {
		
		out.writeDouble( VERSION );
		
		out.writeLong( getTableIdResolved() );
		out.writeLong( getId() );
		out.writeUTF( ( name != null ) ? name : "" );
	}
}

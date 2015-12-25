package whp.mdb;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/** VOUser, missing a control on single login key. */
public class VOUser extends VOBaseTable
{
	// ===== constants & static variable
	
	public static final String kind = "U";
	
	// ===== serialized variable
	
	public	String			login;				// login
	public	String			pswd;				// password
	public	long			userDomainId;		// user domain
	public	String			userRoleId;			// user role
	public	String			desc;				// description
	
	/** serialized in Entity, but not for network (see RPC to revoke a VO) */ 
	public 	boolean			revoked = false;	// if true, this user cannot login
	
	// ===== transient variables
	
	// ===== constructor
	
	public VOUser() { super(); }
	public VOUser( Entity gaeEntity ) { super( gaeEntity ); }
	
	// ===== storage related properties
	
	/** VOUser is stored in root domain. */
	public boolean isDomainRootAllowed() { return true; }
	
	public String isConsistent() {
		if( getDomainId() != 0 ) return "user domainId must be 0-null";
		if( getKind() != kind ) return "mdb kind not " + kind;
		// id is auto 
		return null;
	}
	
	// ===== VO creation
	
//	public VOUser create( String name, String login, String pswd, String userDomainId, String userRoleId, String desc ) {
//		
//		this.name 			= name;
//		this.login 			= login;
//		this.pswd 			= pswd;
//		this.userDomainId	= userDomainId;
//		this.userRoleId 	= userRoleId;
//		this.desc 			= desc;
//		
//		return this;
//	}
	
	// ===== call backed by parent classes
	
	protected boolean isAutoGlobalId() { return true; }
	
	// ===== specific this VO methods
	
	// ===== standard GAE Entities management
	// ----- store, unstore
	// ----- get a valid GAE Entity
	
	/** Read GAE Entity, unserialize it and return a new instanced VO. */
	public static VOUser readEntity( Entity gaeEntity ) {
		VOUser vo = new VOUser( gaeEntity );
		vo.readEntityBase();
		return vo;
	}
	
	/** VOUser key is user nature. The given userId must be user nature. 
	 * If usedId not correct, return a bad refused key, that cannot be found. */
	public static Key getKeyFromFactory( long userId ) {
		if( ! MdbId.isIdUserReal( userId ) ) return KeyFactory.createKey( "REFUSED", 1 );
		return KeyFactory.createKey( kind, userId );
	}
	
	// ===== serialize GAE Entity
	
	@Override
	protected Entity readEntityBase() {
		
		Entity gaeEntity = super.readEntityBase();
				
		// this data
		login 				= (String) gaeEntity.getProperty( "login" );
		userDomainId 		= (Long) gaeEntity.getProperty( "domId" );
		pswd 				= (String) gaeEntity.getProperty( "pswd" );
		userRoleId			= (String) gaeEntity.getProperty( "roleId" );
		desc 				= (String) gaeEntity.getProperty( "desc" );
		revoked				= (Boolean) gaeEntity.getProperty( "del" );
		
		return gaeEntity;
	}
	
	@Override
	protected Entity writeEntity() throws VOStoreException {
		
		// get entity 
		Entity gaeEntity = super.writeEntity();
		
		// set this entity properties
		gaeEntity.setProperty( 			"login", 	login );
		gaeEntity.setProperty( 			"domId", 	userDomainId );
		gaeEntity.setUnindexedProperty( "pswd", 	pswd );
		gaeEntity.setUnindexedProperty( "roleId", 	userRoleId );
		gaeEntity.setUnindexedProperty( "desc", 	desc );
		gaeEntity.setUnindexedProperty( "del", 		revoked );
		
		return gaeEntity;
	}
	
	// ===== interface Externalizable
	
	@Override
	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
				
		super.readExternal( in );
		
		login 				= in.readUTF();
		pswd 				= in.readUTF();
		userDomainId 		= in.readLong();
		userRoleId 			= in.readUTF();
		desc 				= in.readUTF();
		
	}
	
	@Override
	public void writeExternal( ObjectOutput out ) throws IOException {
				
		super.writeExternal( out );
		
		out.writeUTF( login );
		out.writeUTF( pswd );
		out.writeLong( userDomainId );
		out.writeUTF( userRoleId );
		out.writeUTF( desc );
		
	}
}

package whp.mdb;

import java.util.Date;
import java.util.LinkedList;

import whp.gae.RLE;
import whp.tls.Context;
import whp.util.Misc;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;

/** Centralize user methods, including login (VOUser, VOUserSession, VOMdb). */
public class AppUser 
{
	// ===== constants & static variable
	// ===== serialized variable
	// ===== transient variables
	
	private Context context;
	
	// ===== instance initializer
	// ===== constructor
	
	public AppUser( Context context ) {
		this.context = context;
	}
	
	// ===== PUBLIC, WHP LOGIN
	
	/** Handle 3 operations in the same RPC: [logout], [Login], [bid], according given parameters.
	 * <ul><li> logout (optional) if logoutUserSession is not null, logout this user. If logout fail, log 
	 * error, but do not quit. If success, the userSession becomes available for this user (from another
	 * desktop for example). </li>
	 * <li> login (optional) use askLogin and askPswd and [askUserSession]. Find user by askLogin, reject 
	 * if not found or askPswd incorrect. If askUserSession provided, attempt to reuse it, else create a new 
	 * one. Do not accept login if max number of sessions for a user is reached (max 10 sessions, 0..9). </li>
	 * <li> bid (mandatory if login) used only if login asked. If bid=0 provided, create and return a 
	 * valid VOMdb (allowed only for 'bidRequest' user), else check the bid provided (user 'bidRequest' forbidden). 
	 * Return null if incorrect or create bid fails. </li></ul>
	 * 
	 * We could have a login request handled only by user session, with a rule like: a server user session lives
	 * for 2x max, a client user session lives for 1x max (1 hour, 1 day, ...). In case of reconnect within this 
	 * time, the client could avoid to transport login and password. Not done for now. 
	 * */
	public void whpLogin( String askLogin, String askPswd, VOUserSession askUserSession,
			VOUserSession logoutUserSession, long bid ) {
		long begTime = System.currentTimeMillis();
		
		// logout if logoutUserSession
		if( logoutUserSession != null ) userLogout( logoutUserSession );
		
		// is login asked
		if( askLogin == null || askPswd == null ) {
			context.setRc( RLE.INF_NOTLOGGED );
			return;
		}
		
		// bid must be provided if login is asked
		if( ! MdbId.isIdMdb( bid ) ) {
			context.setRc( RLE.ERR_LOGIN );
			return;
		}
		
		// check user login, return if not valid
		VOUser user = userLoginCheck( askLogin, askPswd );
		if( user == null ) {
			context.setRc( RLE.ERR_LOGIN );
			return;
		}
		
		// check bid now that asked login is valid
		VOMdb mdb = mdbGetById( askLogin, askPswd, bid );
		if( mdb == null ) {
			context.setRc( RLE.ERR_LOGIN );
			return;
		} 
		
		// if bidRequest, do not get session and return new mdb now
		if( MdbId.isIdMdbRequest( bid ) ) {
			context.addData( mdb );
			return;
		}
		
		// user and bid valid, can now get a valid user session
		VOUserSession userSession = userSessionGet( askUserSession, user.getId(), mdb.getId() );
		if( userSession == null ) {
			context.setRc( RLE.ERR_LOGIN );
			return;
		}
		
		// login successful, return VOUser and VOUserSession
		context.addLog( "Login successful for " + user.name, begTime );
		context.addData( user );
		context.addData( userSession );
	}
	
	// ===== PUBLIC, WHP CREATE USER
	
	/** Create a new login (and a new user). Return new user VO if successful. */
	public void whpUserCreate( String name, String login, long userDomainId, long userDomainParent, 
			String pswd, String userRoleId, String desc ) {
		long begTime = System.currentTimeMillis();
		
		VOUser newUser = new VOUser();
		
		newUser.setDomainId( 		0 );		
		newUser.setKind( 			VOUser.kind );
		// VOUser is id auto
		newUser.name 				= name;
		newUser.login 				= login;
		newUser.pswd 				= pswd;
		newUser.userDomainId 		= userDomainId;
		newUser.userRoleId			= userRoleId;
		newUser.desc				= desc;
		
		// check if login exists
		if( userGetByLogin( newUser.login ) != null ) {
			context.addLog( "error (login " + newUser.login + " already exists)", begTime );
			context.setRc( RLE.ERR_EXIST );
			return;
		}
		
		AppDomain domains = new AppDomain( context );
		
		// check given userDomain
		VODomain newUserDomain = domains.domainGetById( newUser.userDomainId );
		if( newUserDomain == null ) {
			context.addLog( "error (userDomainId not found) ", begTime );
			context.setRc( RLE.ERR_NOTVALID );
			return ;
		}
		
		// verify that logged user can create a user with new user.userDomain
		VOUserSession userSession = context.getUserSession();
		VOUser user = userGetById( userSession.getUserId() ); 
		VODomain userDomain = domains.domainGetById( user.userDomainId );
		
		// check if newUserDomain is in current userDomain path
		LinkedList<VODomain> domainList = domains.domainGetPathList( newUserDomain, userDomain );
		if( domainList.size() < 1 ) {
			context.addLog( "error (newUserDomain not in current userDomain path) ", begTime );
			context.setRc( RLE.ERR_NOTVALID );
			return;
		}
		
		// try to store new user
		try { newUser.store(); }
		catch( VOStoreException e ) { newUser = null; }
		
		// log message for debug
		if( newUser == null ) {
			context.addLog( "Fail to create new user", begTime );
			context.setRc( RLE.ERR_EXECERROR );
			return;
		}
		
		// new user created
		context.addLog( "New user created " + newUser.getId(), begTime );
		context.addData( user );
		context.setRc( RLE.OK );
	}
	
	// ===== PUBLIC, WHP USER STATE
	
	/** userSetState: activate or inactive (revoke) a user in the Datastore. 
	 * A user can't be removed from the Datastore. But it's state can be inactive (login 
	 * refused), or active (login accepted). If a user is already logged during inactivation,
	 * it's sessions remains active, only next login will be refused.
	 * TODO: active sessions not checked
	 * TODO: actions on users should be done in transactions */
	public void whpUserSetState( long userId, boolean revoked ) {
		long begTime = System.currentTimeMillis();
		
		// get user by id
		VOUser user = userGetById( userId );
		if( user == null ) {
			context.addLog( "userId not found", begTime );
			context.setRc( RLE.ERR_NOTFOUND );
			return;
		}
		
		// cannot change user sysadmin
		if( user.login.equals( "sysadmin@whp.com" ) ) {
			context.addLog( "can't change user sysadmin@whp.com", begTime );
			context.setRc( RLE.ERR_INCONSISTENT );
			return;
		}
		
		// set new state
		user.revoked = revoked;
		
		// try to store new state
		try { user.store(); }
		catch( VOStoreException e ) { user = null; }
		
		// log message for debug
		if( user == null ) {
			context.addLog( "Fail to change user state " + userId, begTime );
			context.setRc( RLE.ERR_EXECERROR );
			return;
		}
		
		context.addLog( "revoked to " + revoked  + " done for " + user.login, begTime );
	}
	
	// ===== PACKAGE, USERS SERVICES
	
	// ===== PRIVATE, USERS SERVICES
	
	// ===== user logout by user session
	
	/** Logout a user by set active false. There is no need to check the user (even if it is 
	 * revoked, deactivate the session has no side effect). */
	private void userLogout( VOUserSession logoutUserSession ) {
		
		// get old userSession
		Entity gaeEntity = null;
		try { gaeEntity = context.ds.get( logoutUserSession.getKey() ); }
		catch( VOStoreException e ) {}
		catch( EntityNotFoundException e ) {}
		
		if( gaeEntity != null ) {
			VOUserSession userSession = VOUserSession.readEntity( gaeEntity );
			// release session only if was the same date as old session
			if( userSession.timestamp.equals( logoutUserSession.timestamp ) ) {
				context.addLog( "Logout for " + userSession.getId() );
				userSession.active = false;
				try { userSession.store(); }
				catch( VOStoreException e ) { 
					context.addLog( "Store error " + logoutUserSession.getKeyInfo() );
					userSession = null; 
				}
			}
		}
	}
	
	// ===== login a user by login, check password
	
	/** Search VOUser by login, check password, return null if not found. */
	private VOUser userLoginCheck( String login, String pswd ) {
		
		VOUser user = null;
		
		// get user by login		
		user = userGetByLogin( login );
		
		// check initiator sysadmin if not found
		if( user == null ) user = checkServerInitRequested( login, pswd );
		
		// exit if user not found
		if( user == null ) return null;
		
		// or incorrect password
		if( ! user.pswd.equals( pswd ) ) return null;
		
		// or invalid user
		if( user.revoked == true ) return null;
		
		// successful login check, return user
		return user;
	}
	
	// ===== get user by login
	
	/** Search VOUser by login, return null if not found.
	 * This method MUST be private (user state and password are not checked). */
	@SuppressWarnings("deprecation")
	private VOUser userGetByLogin( String login ) {
		
		VOUser user = null;
		
		// request by login (indexed property on VOUser)
		Query query = new Query( VOUser.kind );
		query.addFilter("login", FilterOperator.EQUAL, login );	
		
		int count = 0;
		Iterable<Entity> gaeEntities = context.ds.prepare( query ).asIterable();
		for( Entity gaeEntity : gaeEntities ) {
			user = VOUser.readEntity( gaeEntity );
			if( ++count > 1 ) {
				context.addLog( "Warning, more than one user for login " + login );
				break;
			}
		}
		
		return user;
	}
	
	// ===== get user by id
	
	/** Search VOUser by id, return null if not found. */
	public VOUser userGetById( long userId ) {
				
		Entity gaeEntity;
		Key key = VOUser.getKeyFromFactory( userId );
		try { gaeEntity = context.ds.get( key ); }
		catch( EntityNotFoundException e ) { gaeEntity = null; }
		return( gaeEntity != null ) ? VOUser.readEntity( gaeEntity ) : null;
	}
	
	// ===== get user session, try to use asked if possible, else try create
	
	/** Get a user session, reuse given VO is any, or create a new one if not provided. 
	 * The bid is mandatory (stored in returned user session).
	 * The userId is mandatory for control. 
	 * If fail, return null. */
	private VOUserSession userSessionGet( VOUserSession askUserSession, long userId, long bid ) {
		long begTime = System.currentTimeMillis();
		
		VOUserSession newUserSession = null;
		
		// check given bid
		if( bid <= 0 ) return null;
		
		// check askUserSession if provided, return if OK
		newUserSession = userSessionReuse( askUserSession, bid );
		if( newUserSession != null ) return newUserSession;
		
		// askUsedSession cannot be used. Try to get or create a new one. 
		Query query = new Query( VOUserSession.getGaeKind( userId ) );
		int sessionId = 1;
		Iterable<Entity> gaeEntities = context.ds.prepare( query ).asIterable();
		for( Entity gaeEntity : gaeEntities ) {
			newUserSession = VOUserSession.readEntity( gaeEntity );
			if( newUserSession.active == false ) break;	// found re-usable
			newUserSession = null;
			if( ++sessionId > 9 ) break;
		}
		
		// not found an inactive session, and max session (10) reached
		if( newUserSession == null && sessionId > 9 ) {
			context.addLog( "More than 10 sessions for userId " + userId + ", can't add a new session.", begTime );
			return null;
		}
		
		// not found an inactive session, create a new one
		if( newUserSession == null ) {
			newUserSession = new VOUserSession();
			newUserSession.setDomainId( 0 );
			newUserSession.setTableId( userId );
			newUserSession.setId( MdbId.getNewIdUserSession( userId,  sessionId ) );
			context.addLog( "Login, session created " + newUserSession.getId() );
		}
		
		// set and store session (new one or inactive found)
		newUserSession.timestamp = new Date();
		newUserSession.active = true;
		newUserSession.requests = 0;
		newUserSession.bid = bid;
		try { newUserSession.store(); }
		catch( VOStoreException e ) { newUserSession = null; }
		
		// log message for debug
		if( newUserSession == null ) context.addLog( "Fail to allocate user session", begTime );
		else context.addLog( "Session allocated " + newUserSession.getId(), begTime );
		
		return newUserSession;
	}
	
	// ===== try to reuse a user session
	
	/** Return user session if success, else null. */
	private VOUserSession userSessionReuse( VOUserSession askUserSession, long bid ) {
		
		VOUserSession newUserSession = null;
		
		// check given askUserSession
		if( askUserSession == null ) return null;
		
		// get askUserSession
		Entity gaeEntity = null;
		try { gaeEntity = context.ds.get( askUserSession.getKey() ); }
		catch( VOStoreException e ) {}
		catch( EntityNotFoundException e ) {}
	
		// if askUserSession found
		if( gaeEntity != null ) {
			newUserSession = VOUserSession.readEntity( gaeEntity );
			
			// re-use askUserSession if same timestamp and bid
			if( ! newUserSession.timestamp.equals( askUserSession.timestamp ) ) return null;
			if( newUserSession.bid != bid ) return null;
			
			// askUserSession valid, becomes newUserSession, set timestamp and reuse it
			context.addLog( "Reuse " + newUserSession.getId() );
			newUserSession.active = true;
			newUserSession.timestamp = new Date();
			newUserSession.bid = bid;
			try { newUserSession.store(); }
			catch( VOStoreException e ) { newUserSession = null; }
		}
		
		// return newUserSession, null if failed
		return newUserSession;
	}
	
	// ===== get remote database VOMdb, or create a new one if bid "0"
	
	/** Get an VOMdb from bid. If bid is 0, create a new one. 
	 * Return null if not found or create failed. 
	 * Else return a valid VOMdb that can be used. */
	private VOMdb mdbGetById( String askLogin, String askPswd, long bid ) {
		
		boolean isBidRequest = MdbId.isIdMdbRequest( bid );
		
		// cancel login if bid not requested by user 'bidRequest', or 'bidRequest' user used with a bid
		if( isBidRequest && ! askLogin.equals( "bidRequest" ) && ! askPswd.equals( askLogin ) ) return null;
		else if( ! isBidRequest && askLogin.equals( "bidRequest" ) && askPswd.equals( askLogin ) ) return null;
			
		// create VOMdb if bid 0
		if( isBidRequest ) {
			
			VOMdb mdb = new VOMdb();
			mdb.setDomainId( 0 );
			mdb.setKind( VOMdb.kind );
			mdb.setId( MdbId.getNewIdMdb( context) );
			mdb.name = "local";
			mdb.desc = Misc.httpGetRemoteAddr( context );
			//mdb.serverPrivilege = Misc.serverPrivilege();
			try { 
				mdb.store(); 
				context.addLog( "new bid created " + mdb.getId() );
			} 
			catch( VOStoreException  e ) { 
				context.addLog( e.getMessage() );
				mdb = null;
			}
			return mdb;
		
		}
		
		// else search VOMdb from bid
		Entity gaeEntity;
		Key key = VOMdb.getKeyFromFactory( bid );
		try { gaeEntity = context.ds.get( key ); }
		catch( EntityNotFoundException e ) { 
			context.addLog( "bid not found " + e.getMessage() );
			gaeEntity = null; 
		}
		
		// return if found or null
		return( gaeEntity != null ) ? VOMdb.readEntity( gaeEntity ) : null;
	}
	
	// ===== check initial condition, initialize if first call to the server
	
	/** Check for server initialization, return user only when bidRequest user requested and not found. 
	 * Return bidRequest user only if initialization done and bidRequest was created. */
	private VOUser checkServerInitRequested( String loginBidRequest, String pswdBidRequest ) {
		
		// refused if not called by bidRequest
		if( ! loginBidRequest.equals( "bidRequest" ) || ! pswdBidRequest.equals( "bidRequest" ) ) return null;
		
		// check if bidRequest already exists, do not initialize
		VOUser user = userGetByLogin( loginBidRequest );
		if( user != null ) return user;
		
		// first call to the server done by sysadmin (should be better tested)
		AppInitialize appInitialize = new AppInitialize( context );
		try { appInitialize.initializeFirst(); } 
		catch( VOStoreException  e ) { context.addLog( e.getMessage() ); }
		
		// retry bidRequest that should now exists, return it
		user = userGetByLogin( loginBidRequest );
		return user;
	}
}

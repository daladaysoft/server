package whp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Date;

import org.granite.messaging.amf.io.AMF3Deserializer;

import whp.boot.VOLibBoot;
import whp.gae.RLE;
import whp.mdb.AppDomain;
import whp.mdb.AppLib;
import whp.mdb.AppUser;
import whp.mdb.MdbId;
import whp.mdb.VOUserSession;
import whp.tls.Context;
import ads.cst.SYNCREASON;
import ads.net.pusher.Pusher;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entities;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;

/**
 * Main remote WHP broker, called by each incoming request by network layer (GraniteDS for now). 
 * Contains only one variable, the context cache. 
 * <li> Prepare context, authenticate requests, return null if not valid. </li>
 * <li> Initialize domainId, userId, usersessionId, HTTP context (remote IP), start time in context. </li>
 */
public class WHPBroker 
{	
	// ===== constants & static variable
	
	/** Elapse max time (20 seconds) */
	public static final int ELAPSE_MAX = 20000;
	
	// ===== serialized variable
	// ===== transient variables
	// ===== instance initializer
	{
	}
	
	// ===== constructor
	
	public WHPBroker() {}
	
	// ===== WHP TEST (is alive)
	
	/** Allows a simple check of the server (no context, no authenticate). */
	public Boolean whp() { return true; }
	
	// ===== WHP LOGIN
	
	/** The only whp RPC that do not check authentication: initialize context itself. 
	 * @see AppUser#whpLogin */
	public Answer whpLogin( String askLogin, String askPswd, VOUserSession askUserSession,
			VOUserSession logoutUserSession, double bidD ) {
		
		long bid = Double.doubleToLongBits( bidD );
		if( ! MdbId.isIdMdb( bid ) ) return null;
			
		Context context = new Context( "whpLogin" ).clean();
		AppUser users = new AppUser( context );
		users.whpLogin( askLogin, askPswd, askUserSession, logoutUserSession, bid );
		return context.returnAnswer();
	}
	
	// ===== WHP USER CREATE
	
	/** @see AppUser#whpUserCreate */
	public Answer whpUserCreate( double usIdD, Date usTimestamp, String name, String login, double userDomainIdD, 
			double userDomainParentD, String pswd, String userRoleId, String desc ) {
		
		long usId = Double.doubleToLongBits( usIdD );
		if( ! MdbId.isIdUserSession( usId ) ) return null;
		long userDomainId = Double.doubleToLongBits( userDomainIdD );
		if( ! MdbId.isIdDomain( userDomainId ) ) return null;
		long userDomainParent = Double.doubleToLongBits( userDomainParentD );
		if( ! MdbId.isIdDomain( userDomainParent ) ) return null;
		
		Context context = authenticate( "whpUserCreate", usId, usTimestamp );
		if( context == null ) return null;
		AppUser users = new AppUser( context );
		users.whpUserCreate( name, login, userDomainId, userDomainParent, pswd, userRoleId, desc );
		return context.returnAnswer();	
	}
	
	// ===== WHP USER SET STATE
	
	/** (warning, was by user login, now by userId) 
	 *@see AppUser#whpUserSetState(String, boolean) */
	public Answer whpUserSetState( double usIdD, Date usTimestamp, double userIdD, boolean revoked ) {
		
		long usId = Double.doubleToLongBits( usIdD );
		if( ! MdbId.isIdUserSession( usId ) ) return null;
		long userId = Double.doubleToLongBits( userIdD );
		if( ! MdbId.isIdUserReal( userId ) ) return null;
		
		Context context = authenticate( "whpUserSetState", usId, usTimestamp );
		if( context == null ) return null;
		AppUser users = new AppUser( context );
		users.whpUserSetState( userId, revoked );
		return context.returnAnswer();
	}
	
	// ===== DOMAIN SYNCHRONIZE
	
	/** @see AppDomain#adsDomainSync(String, Date, byte[]) */
	public Answer adsDomainSync( byte[] headBytes, byte[] txBytes, String end ) {
		
		// if end not null, return null (without any explanation)
		if( end != null ) return null;
				
		long usId = 0;
		Date usTimeStamp = null;
		long domainId = 0;
		@SuppressWarnings("unused") SYNCREASON reason; 
		Date fromDate = null;
		
		// try to get values from head
		boolean isHead = false;
		AMF3Deserializer head = new AMF3Deserializer( new ByteArrayInputStream( headBytes ) );
		try {
			usId = head.readLong();											// read user id
			usTimeStamp = new Date( (long) head.readDouble() );				// read session timestamp
			domainId = head.readLong();										// read domain id
			reason = SYNCREASON.values()[ head.readUnsignedByte() ];		// read reason id
			fromDate = new Date( (long) head.readDouble() );				// read last upd
		} catch( IOException e ) {}
		
		try {
			head.close();
			isHead = true;
		} catch( IOException e ) {}
		
		// if any problem, return null (without any explanation)
		if( isHead == false ) return null;
		
		// try to authenticate, if not valid, return null (without any explanation)
		Context context = authenticate( "adsDomainSync", usId, usTimeStamp );
		if( context == null ) return null;
		
		// we can work on received data
		AppDomain domains = new AppDomain( context );
		context.addLog( "did " + domainId );	
		domains.adsDomainSync( domainId, fromDate, txBytes );
		return context.returnAnswer();
	}
	
	// ===== WHP DOMAIN UNIQUE
	
	/** @see AppDomain#whpDomainGetUnique( String, String, String ) */
	public Answer whpDomainGetUnique( double usIdD, Date usTimestamp, double domainIdD, String domainName, double domainParentIdD ) {
		
		long usId = Double.doubleToLongBits( usIdD );
		if( ! MdbId.isIdUserSession( usId ) ) return null;
		long domainId = Double.doubleToLongBits( domainIdD );
		if( ! MdbId.isIdDomain( domainId ) ) return null;
		long domainParentId = Double.doubleToLongBits( domainParentIdD );
		if( ! MdbId.isIdDomain( domainParentId ) ) return null;
		
		long begTime = System.currentTimeMillis();
		Context context = authenticate( "whpDomainSync", usId, usTimestamp );
		if( context == null ) return null;
		AppDomain domains = new AppDomain( context );
		while( true ) {
			try { 
				domains.whpDomainGetUnique( domainId, domainName, domainParentId ); 
				break;
			}
			catch( ConcurrentModificationException e ) {
				try {
					if( System.currentTimeMillis() - begTime < ELAPSE_MAX ) {
						Thread.sleep( 2000 );
						continue;
					}
					context.addLog( "whpDomainGetUnique, time limit reached in " + domainParentId );
					context.setRc( RLE.ERR_TIMEOUT );
				} catch( InterruptedException e1 ) {
					context.addLog( "whpDomainGetUnique, InterruptedException in " + domainParentId );
					context.setRc( RLE.ERR_INTERNAL );
				}
				break;
			}
		}
		return context.returnAnswer();
	}
		
	// ===== WHP DOMAIN IN PATH
	
	/** @see AppDomain#whpDomainGetPath(String, String) */
	public Answer whpDomainGetPath( double usIdD, Date usTimestamp, double fromDomainIdD, double untilDomainIdD ) {
		
		long usId = Double.doubleToLongBits( usIdD );
		if( ! MdbId.isIdUserSession( usId ) ) return null;
		long fromDomainId = Double.doubleToLongBits( fromDomainIdD );
		if( ! MdbId.isIdDomain( fromDomainId ) ) return null;
		long untilDomainId = Double.doubleToLongBits( untilDomainIdD );
		if( untilDomainId == 0 ) untilDomainId = MdbId.getIdDomainRoot();
		if( ! MdbId.isIdDomain( untilDomainId ) ) return null;
		
		Context context = authenticate( "whpDomainGetPath", usId, usTimestamp );
		if( context == null ) return null;
		AppDomain domains = new AppDomain( context );
		domains.whpDomainGetPath( fromDomainId, untilDomainId );
		return context.returnAnswer();
	}
	
	/** @see #whpDomainGetPath( String, Date, String, String ) */
	public Answer whpDomainGetPath( double usId, Date usTimestamp, double fromDomainId ) {
		return whpDomainGetPath( usId, usTimestamp, fromDomainId, 0 );
	}
	
	// ===== WHP PUSHER TEST
	
	/** Send pusher message, . */
	public Answer whpPusherTest( double usIdD, Date usTimestamp, String channel, String event, String messageToSend ) {
		
		long usId = Double.doubleToLongBits( usIdD );
		if( ! MdbId.isIdUserSession( usId ) ) return null;
		if( channel == null || channel.length() == 0 ) return null;
		if( messageToSend == null || messageToSend.length() == 0 ) return null;
		
		Context context = authenticate( "whpPusherTest", usId, usTimestamp );
		if( context == null ) return null;
		
		context.addLog( "channel " + channel );
		context.addLog( "event " + event );
		context.addLog( "messageToSend " + messageToSend );
		
		Pusher.triggerPush( channel, event, "{\"tx\":\"" + messageToSend + "\"}" );
		
		return context.returnAnswer();
	}
	
	// ===== WHP ALL KIND LIST
	
	/** Return all datastore kinds list. */
	public Answer whpKindList( double usIdD, Date usTimestamp ) {
		
		long usId = Double.doubleToLongBits( usIdD );
		if( ! MdbId.isIdUserSession( usId ) ) return null;
		
		Context context = authenticate( "whpKindList", usId, usTimestamp );
		if( context == null ) return null;
		Query q = new Query( Entities.KIND_METADATA_KIND );
		for( Entity e : context.ds.prepare( q ).asIterable() ) context.addLog( e.getKey().getName() );
		return context.returnAnswer();
	}
	
	// ===== WHP LIB MANAGEMENT
	
	/** Receive a library to add or to update. VOLibBoot must contains the code.
	 * Return nothing or error. */
	public Answer whpLibUpload( double usIdD, Date usTimestamp, VOLibBoot voLib, byte[] data ) {
		
		long usId = Double.doubleToLongBits( usIdD );
		if( ! MdbId.isIdUserSession( usId ) ) return null;
		
		Context context = authenticate( "whpLibUpload", usId, usTimestamp );
		if( context == null ) return null;
		AppLib applib = new AppLib( context );
		applib.whpLibUpload( voLib, data );
		return context.returnAnswer();
	}
	
	/** @see AppLib#whpLibReadSync(String, Date, boolean) */
	public Answer whpLibReadSync( double usIdD, Date usTimestamp, String name, Date fromDate, boolean isVersion ) {
		
		long usId = Double.doubleToLongBits( usIdD );
		if( ! MdbId.isIdUserSession( usId ) ) return null;
		
		Context context = authenticate( "whpLibReadSync", usId, usTimestamp );
		if( context == null ) return null;
		AppLib applib = new AppLib( context );
		applib.whpLibReadSync( name, fromDate, isVersion );
		return context.returnAnswer();
	}
	
	/** Without authenticate for reserved libraries. 
	 * @see AppLib#whpLibReadSync(String, Date, boolean) */
	public Answer whpLibReadSync( String name, Date fromDate, boolean isVersion ) {
		
		if( ! name.startsWith( "WhpRoot_AIR_swf_rel_" ) && 
			! name.startsWith( "WhpRoot_AIR_swf_dbg_" ) &&
			
			! name.startsWith( "WhpAppRoot_AIR_swf_rel_" ) && 
			! name.startsWith( "WhpAppRoot_AIR_swf_dbg_" ) &&
			! name.startsWith( "WhpAppRoot_BWR_swf_rel_" ) && 
			! name.startsWith( "WhpAppRoot_BWR_swf_dbg_" ) && 
			! name.startsWith( "WhpAppRoot_CSX_swf_rel_" ) && 
			! name.startsWith( "WhpAppRoot_CSX_swf_dbg_" ) ) return null;
		
		Context context = new Context( "whpLibDownloadRaw" ).clean();			// RPC without authenticate
		if( context == null ) return null;
		AppLib applib = new AppLib( context );
		applib.whpLibReadSync( name, fromDate, isVersion );
		return context.returnAnswer();
	}
	
	// ===== AUTHENTICATE, used by all WHP requests (except whpLogin)
	
	/** Called by each incoming request (except whpLogin). 
	 * For this reason, authenticate load and initiate a new context.
	 * Check usId and usTimestamp with user session, and prepare context with user session data.
	 * Return context if success, else null (and silently without log message). */
	private Context authenticate( String rpcName, long usId, Date usTimestamp ) {
		
		// initiate context must be done before any operation
		Context context = new Context( rpcName ).clean();
		
		// extract userId and sessionId from usId
		long userId = MdbId.getUserFromUserSession( usId );
		
		// get sessionKey (userid (user only), usId( user and session num)
		Key sessionKey = VOUserSession.getKeyFromFactory( userId, usId );
		
		// search user session in datastore (will be in cache in future)
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Entity gaeEntity = null;
		try { gaeEntity = ds.get( sessionKey ); }
		catch( EntityNotFoundException e ) {}
		
		// failed if session not found
		if( gaeEntity == null ) return null;
		
		// get VOUserSession
		VOUserSession userSession = VOUserSession.readEntity( gaeEntity );
		
		/** TODO: enhance security level with setting new usTimestamp = current server date,
		 * returned by context (and client must always set the good value).Use with
		 * memcache (not datastore) to be faster ... return to actual mode if memcache
		 * cleared (= control with datastore without timestamp control in this case). 
		 * Set timestamp as long (not as date), this is useless and transport unused data.
		 * */
		
		// check timestamp
		if( ! usTimestamp.equals( userSession.timestamp ) ) return null;
		
		// authenticate successful, set context 
		context.setUserSession( userSession );

		return context;
	}
	
}

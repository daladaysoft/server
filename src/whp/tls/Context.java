package whp.tls;

import java.util.Date;

import whp.Answer;
import whp.VOLog;
import whp.WHPBroker;
import whp.mdb.VOUserSession;
import ads.type.BytesOut;
import ads.type.LongAds;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;

import flex.messaging.io.ArrayCollection;

/** Context, store data during a request execution.
 * Provided by {@link TLFactory#getContext()}, that is called by the 
 * broker instance initializer on each request {@link WHPBroker}. */
public class Context {
	
	// ===== constants & static variable
	
	private static int		idCounter;
	public int				id;
	
	// ===== serialized variable
	// ===== transient variables
	
	/** Answer object, will be returned to remote. */
	private Answer	 		answer;
	
	/** convenient datastore service */
	public DatastoreService ds;
	
	// ----- if authenticate success
	
	// RPC name that initiate this context
	@SuppressWarnings("unused")
	private String 			rpcName;
	
	// user informations
	public 	String 			userId;						// loaded if session authenticated
	public 	VOUserSession 	userSession;				// loaded if session authenticated
	public 	String			userDomainId;				// connected user domain id
	
	// unique date and sharded counters
//	private	long			uniqueDate;					// update unique date in whp transaction
//	private	long			uniqueDateIncrement;		// uniqueDate + 1 (n times)
//	private	long			uniqueDateIncrementCount;	// increment use count (max SHARDED_INTERVAL) 
	
	// ===== instance initializer, data that can be alive across RPC
	{
		// initialize only once
		ds = DatastoreServiceFactory.getDatastoreService();
		id = idCounter++;	// TODO: circular counter (uint ?)
	}
	
	// ===== constructor
	
	public Context( String rpcName ) {
		this.rpcName = rpcName;
		//System.out.println( rpcName + " new context " + id ); 
	}
	
	// ===== private methods
	
	// ===== global context management
	
	/** Clean and set the context before use it. Automatically done by WHPBroker on each incoming RPC
	 * that use authenticate, and done RPC that use context and not authenticate.
	 * This is necessary, because the context is allocated by thread local, that depends on J2EE pool
	 * thread management, that can reuse a thread and thus, do not allocate a new context on starting
	 * RPC. */
	public Context clean() {
		
		// answer is the part of context that will be returned to the remote 
		answer = new Answer();
		
		answer.serverElapse = System.currentTimeMillis(); 
		answer.log = null;		// lazy initialization
		answer.moreData = false;
		answer.rc = 0;
		
		// user session store user, session and bid informations
		userSession = null;
		
		// reset unique date (next use of unique date will load from DLAtom)
//		resetUniqueDate();
		
		return this;
	}
	
	/** Return answer to the remote RPC. Note that the context is not removed here from thread 
	 * local, nor cleaned, because it's instanced (if J2EE thread pool start a new thread for
	 * us), or cleaned (if J2EE thread pool reuse an old thread) by WHPBroker instantiation.  */
	public final Answer returnAnswer() {
		
		answer.serverElapse = System.currentTimeMillis() - answer.serverElapse;
		return answer;
	}
	
	// ===== context services
	
	// add data array
	
	/** Add a data to previous pending data. */
	public void addData( Object o ) {
		if( ! (o instanceof java.io.Externalizable) && ! (o instanceof java.io.Serializable) )
			throw new Error( "Answer accept only Externalizable or Serializable objects (received: " + o.getClass() + ")" );	
		if( o instanceof ArrayCollection ) throw new Error( "Answer can't add an array. See setData()" );
		if( answer.bytesOut != null ) throw new Error( "Answer data cannot be used if bytes used." );
		
		Object data = answer.data;
		// if data null, data = o
		if( data == null ) data = o;
		else { 
			// if data is not array, set an array with previous data value
			if( ! (data instanceof ArrayCollection) ) {
				Object tmp = data;
				data = new ArrayCollection();
				((ArrayCollection)data).add( tmp );
			}
			// add o to data array
			((ArrayCollection)data).add( o );
		}
		answer.data = data;	
	}
	
	// set BytesOut
	
	/** set byte array to return (throw error if there were a previously byte array). */
	public void setBytesOut( BytesOut bytes ) {
		if( answer.bytesOut != null ) throw new Error( "Answer bytes cannot be overloaded." );
		if( answer.data != null ) throw new Error( "Answer bytes cannot be used if data used." );
		answer.bytesOut = bytes;
	}
	
	// moreData
	
	/** set moreData */
	public void setMoreData( boolean v ) { answer.moreData = v; }
	
	// txo 
	
	/** set TXO (last tx order) */
	public void setTxo( long v ) { answer.txo = v; }
	public long getTxo() { return answer.txo; }
	
	// lastUpd 
	
	/** set LUPD (last update date) */
	public void setLastUpd( long v ) { answer.lastUpd = new Date( v ); }
	public void setLastUpd( Date v ) { answer.lastUpd = v; }
	public Date getLastUpd() { return answer.lastUpd; }
	
	// RC, return code
	
	/** set return code (rc) */
	public int setRc( int v ) { return answer.rc = v; }
	public int getRc() { return answer.rc; }
	
	// log
	
	public void addLog( VOLog o ) {
		if( answer.log == null ) answer.log = new ArrayCollection();
		answer.log.add( o );	
	}
	
	// session authenticate
	
	/** Il semble manquer le userId et userDomainId dans authenticate
	 * (en tout cas il me semble que ces valeurs ne sont pas charg�es dans contexte) */
	
	public void setUserSession( VOUserSession userSession ) { 
		this.userSession = userSession; 
	}
	public VOUserSession getUserSession() { 
		return userSession; 
	}
	
	public long getBid() { 
		return userSession.bid;
	}
	
	// update unique date (transaction id ?)
	
//	/** For now, only getUniqueDateIncrement is used. But other commented 
//	 * methods will may be use by transactions. */
//	
//	private void resetUniqueDate() {
//		uniqueDate = uniqueDateIncrement = uniqueDateIncrementCount = 0;
//	}
//	
//	/** Return unique date, reserve shardedInterval = SHARDED_INTERVAL. 
//	 * @see #getUniqueLongIncrement  */
//	public Date getUniqueDateIncrement() { 
//		return getUniqueDateIncrement( DLAtom.SHARDED_INTERVAL ); 
//	}
//	
//	/** Return unique date, reserve provided shardedInterval. 
//	 * @see #getUniqueLongIncrement  */
//	public Date getUniqueDateIncrement( long shardedInterval ) {
//		return new Date( getUniqueLongIncrement( shardedInterval ) );
//	}
//	
//	/** Return date (as long) that is unique across many JVM, and that is guaranteed unique, 
//	 * and greater than previous return. Use a sharded method to avoid a datastore get for each 
//	 * date request, based on uniqueDateIncrementCount (each date requested after resetUniqueDate 
//	 * get last datastore unique date, with shardedInterval milliseconds reserved in the future. 
//	 * Allow "guaranteed unique, and greater" without datastore request for shardedInterval date get). 
//	 * If the counter shardedInterval is reached, a new datastore request is done.  */
//	public long getUniqueLongIncrement( long shardedInterval ) {
//		if( uniqueDateIncrementCount == -1 ) {
//			uniqueDateIncrementCount = shardedInterval;
//		}
//		else if( uniqueDate == 0 ) {
//			uniqueDate = uniqueDateIncrement = DLAtom.getDateUnique( this, shardedInterval );
//			uniqueDateIncrementCount = shardedInterval;
//		}
//		else {
//			uniqueDateIncrement++;
//			uniqueDateIncrementCount--;
//			if( uniqueDateIncrementCount == 0 ) {
//				addLog( "ALERT, uniqueDateIncrement reach " + uniqueDateIncrement + " use." );
//				resetUniqueDate();
//				return getUniqueLongIncrement( shardedInterval );
//			}
//		}
//		return uniqueDateIncrement;
//	}
//	/** Hack to get atomic date once before increment it. 
//	 * TODO: has to be changed when old calls will be cleared. */
//	public long setUniqueLongIncrement( long shardedInterval ) {
//		if( uniqueDate != 0 ) throw new Error( "uniqueDate shoud equals 0" );
//		uniqueDateIncrementCount = -1;	// hack, ask next getUnique to initialize increment
//		return uniqueDate = uniqueDateIncrement = DLAtom.getDateUnique( this, shardedInterval );
//	}
	
	// convenient methods
	
	public void addLog( String s ) {
		addLog( new VOLog( s, 3 ) );
	}
	public void addLog( String s, long begTime ) {
		addLog( new VOLog( s + " (" + (System.currentTimeMillis()-begTime) + " ms)", 3 ) );
	}
	
	/** Convenient set RC and add a log string. */
	public void addRcLog( int v, String s ) {
		setRc( v );
		addLog( s );
	}
	/** Convenient set RC and add a log string, and elapse time. */
	public void addRcLog( int v, String s, long begTime ) {
		setRc( v );
		addLog( s, begTime );
	}
	
}

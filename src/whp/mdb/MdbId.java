package whp.mdb;

import whp.gae.DLAtom;
import whp.tls.Context;
import ads.cst.IDNAT;

import com.google.appengine.api.datastore.Entity;

/** [MdbId] General ID management. ID are used to identify a unique entities (VO, TX) across distributed
 * MDB bases at very hight speed with small memory (8 bytes). This class is very sensible, because 
 * all operations relate on unique id.
 * <p> MdbId is implemented in AS3 and JAVA.</p>
 * <li> [AS3] General ID management singleton exposed by Mdb. ID are used to identify a unique entity (VO, TX). </li>
 * <li> [JAVA] Same but is used as static, not a singleton. Do not use counters (do not generate id itself). </li>
 * <p>ID global principles</p>
 * <li> Counters are loaded by Mdb, they are splited by nature. </li>
 * <li> Each id contains a NATURE (user, mdb, tx, row, table, domain, dict), an IDB (mdb id), an IDL 
 * (local id, according nature). </li> 
 * <li> A full id (NATURE | IDB | IDL) is unique across any ADS bases. </li> 
 * <p> Natures differences </p>
 * <li> USER id natures are set by server only, has no IDB, and IDL is double. Use nature 0. </li> 
 * <li> MDB id natures are set by server only, has regular IDB, and IDL (is always 0). </li>
 * <li> TX, ROW, TABLE, DOMAIN natures set by clients or server, has regular IDB and IDL. </li> 
 * <p> Mdb Id appears as a 64 bits Number (java double), but in fact is a long (2 uint) transported in a Number. </p> 
 * <li> <b>NEVER</b> attempt to use an id as a Number, because it is <b>NOT a Number</b>. </li>
 * <li> An id is <b>transported inside a Number</b>. </li>
 * <li> Must never be changed: <b>is a definitive id</b>. </li>
 * <li> Can only be tested as Number for 2 values: 0 (null id) and <0 (other codage). </li>
 * <p> Contains these values for natures >0 (mdb, tx, domain, table, row, dict) </p>
 * 		<li> version (bit 63, 1 bit, value always 0, 1 reserved for other coding) </li>
 * 		<li> nature (bits 62-60, 3 bits, values 0-7) </li>
 * 		<li> idb (bits 59-41, 19 bits, values 0-524,287) </li>
 * 		<li> idl (bits 40-0, 41 bits, values 0-2,199,023,255,551) </li> 
 * <p> Contains these values for nature 0 (user) </p>
 * 		<li> version (bit 63, 1 bit, value always 0, 1 reserved for other coding) </li>
 * 		<li> nature (bits 62-60, 3 bits, values 0-7) </li>
 * 		<li> user session (bits 59-56, 4 bits, values 0-15) </li>
 * 		<li> security (bits 55-53, 3 bits) </li>
 * 		<li> user id (doube) (bits 52-0, 53 bits, values 0-9,007,199,254,740,990) </li> */

@SuppressWarnings("unused")
public final class MdbId {
	
	// ========================= static part of MdbId =========================
	
	// ----- counters
	
	private static long TX_COUNTER 			= 0;	// nat 2
	private static long DOMAIN_COUNTER 		= 0;	// nat 3
	private static long TABLE_COUNTER 		= 0;	// nat 4
	private static long ROW_COUNTER 		= 0;	// nat 5
	private static long DICT_COUNTER 		= 0;	// nat 6
	private static long COUNTER_MAX 		= 5;	
	
	// ----- version, nature, idb, idl
	
	// version
	
	private static long VER_MSK 			= 0x8000000000000000L;
	private static long VER_SHIFT 			= 31 + 32;
	
	// nature
	
	private static long NAT_MSK 			= 0x7000000000000000L;
	private static long NAT_SHIFT 			= 28 + 32;
	
	private static long NAT_USER 			= 0x0000000000000000L;
	private static long NAT_MDB 			= 0x1000000000000000L;		
	private static long NAT_TX 				= 0x2000000000000000L;
	private static long NAT_DOMAIN 			= 0x3000000000000000L;
	private static long NAT_TABLE 			= 0x4000000000000000L;
	private static long NAT_ROW 			= 0x5000000000000000L;
	private static long NAT_DICT 			= 0x6000000000000000L;
	
	// idb (except user)
	
	private static long IDB_MSK 			= 0x0ffffe0000000000L;
	private static long IDB_SHIFT 			= 9 + 32;
	
	// idl (large idl for user)
	
	private static long IDL_MSK 			= 0x000001ffFFFFFFFFL;
	private static long IDL_SHIFT 			= 0;
	
	private static long USER_IDL_MSK 		= 0x001fffffffffffffL;
	private static long USER_IDL_SHIFT 		= 0;
	
	private static long USER_SES_MSK 		= 0x0f00000000000000L;
	private static long USER_SES_SHIFT 		= 24 + 32;
		
	// ----- id counters
	
	// has no LOCAL_MDB VOMdb
	
	/** Local Mdb id GAE server is 1. */
	private static long LOCAL_IDB = NAT_MDB | 0x0000000100000000L;
	
	// ----- id caches
	
	private static long idExplainedKey;
	private static String idExplainedVal;
	
	private static long idTuplesKey;
	private static String idTuplesVal;
	
	// ===== is not a singleton
	
	// ===== services
	
	// ----- local mdb, idb
	
	// has no local VOMdb
	
	/** @see #LOCAL_IDB */
	static long getLocalIdb() { return LOCAL_IDB; }
	
	// ----- id factory
	
	/** Return a new userSessionId (= userId | userSession number). */
	static long getNewIdUserSession( long userId, long userSession ) {
		return userId | ( userSession << MdbId.USER_SES_SHIFT );
	}
	
	/** Return a new base id (use mdb atomic counter in datastore). */
	static long getNewIdMdb( Context context ) {
		long newBaseId = DLAtom.getUniqueBaseId( context );
		// set real idb (shift idb, then add nature (idl is 0 by shift))
		newBaseId <<= MdbId.IDB_SHIFT;
		newBaseId |= NAT_MDB;
		return newBaseId;
	}
	
	static long getNewIdTx() { throw new Error( "Not implemented" ); }
	static long getNewIdRow() { throw new Error( "Not implemented" ); }
	static long getNewIdTable() { throw new Error( "Not implemented" ); }
	static long getNewIdDomain() { throw new Error( "Not implemented" ); }
	static long getNewIdDict() { throw new Error( "Not implemented" ); }
	
	public static long getIdDomainRoot() { return getFixedId( NAT_DOMAIN, 0 ); }
	public static long getIdTableMdbs() { return getFixedId( NAT_TABLE, 0 ); }
	public static long getIdTableUsers() { return getFixedId( NAT_TABLE, 1 ); }
	public static long getIdDomainSys() { return getFixedId( NAT_DOMAIN, 1 ); }
	public static long getIdDomainApp() { return getFixedId( NAT_DOMAIN, 2 ); }
	public static long getIdDomainPub() { return getFixedId( NAT_DOMAIN, 3 ); }
	
	/** Return a shared fixed id (not use LOCAL_IDB, fixed id share the virtual idb (= 0) with
	 * all databases). Used only by fixed id (must have idl < 2^32-1). */
	static long getFixedId( long nat, long idl ) {
		return ( nat ) | ( 0 << IDB_SHIFT ) | ( idl );
	}
	
	// ----- helpers
	
	// bidRequest not implemented
	
	// helpers
	
	public static boolean isIdUser( long id ) { 
		int nat = (int) (( id & NAT_MSK ) >>> NAT_SHIFT);
		return( nat == NAT_USER ); 
	}
	public static boolean isIdMdb( long id ) { return( ( id & NAT_MDB ) != 0 ); }
	public static boolean isIdTx( long id ) { return( ( id & NAT_TX ) != 0 ); }
	public static boolean isIdDomain( long id ) { return( ( id & NAT_DOMAIN ) != 0 ); }
	public static boolean isIdTable( long id ) { return( ( id & NAT_TABLE ) != 0 ); }
	public static boolean isIdRow( long id ) { return( ( id & NAT_ROW ) != 0 ); }
	public static boolean isIdDict( long id ) { return( ( id & NAT_DICT ) != 0 ); }
	
	public static boolean isIdDomainRoot( long id ) { return( id == getFixedId( NAT_DOMAIN, 0 ) ); }
	public static boolean isIdTableMdbs( long id ) { return( id == getFixedId( NAT_TABLE, 0 ) ); }
	public static boolean isIdTableUsers( long id ) { return( id == getFixedId( NAT_TABLE, 1 ) ); }
	public static boolean isIdDomainSys( long id ) { return( id == getFixedId( NAT_DOMAIN, 1 ) ); }
	public static boolean isIdDomainApp( long id ) { return( id == getFixedId( NAT_DOMAIN, 2 ) ); }
	public static boolean isIdDomainPub( long id ) { return( id == getFixedId( NAT_DOMAIN, 3 ) ); }
	
	/** Return true if id is nature user and session 0. */
	public static boolean isIdUserReal( long id ) { 
		return( isIdUser( id ) && ( id & USER_SES_MSK ) == 0 ); 
	}
	
	/** Return true if id is nature user and session not 0. */
	public static boolean isIdUserSession( long id ) { 
		return( isIdUser( id ) && ( id & USER_SES_MSK ) != 0 ); 
	}
	
	/** Return true if id is nature mdb and idb is 0 and idl is 0. */
	public static boolean isIdMdbRequest( long id ) { 
		return( isIdMdb( id ) && ( id & IDB_MSK ) == 0 && ( id & IDL_MSK ) == 0 ); 
	}
	
	public static long getUserFromUserSession( long id ) {
		return ( isIdUser( id ) ? ( ( id & USER_IDL_MSK ) >>> USER_IDL_SHIFT ) : 0 ); 
	}
	
	public static long getSessionFromUserSession( long id ) {
		return ( isIdUserSession( id ) ? ( ( id & USER_SES_MSK ) >>> USER_SES_SHIFT ) : 0 ); 
	}
	
	// ----- id nature
	
	static String idNatLetter( long id ) {
		int nat = (int) (( id & NAT_MSK ) >>> NAT_SHIFT);
		return IDNAT.values()[ nat ].getKey();
	}
	
	// ----- id explained, tuples
	
	/** Return given id explained string. */
	static String idExplained( long id ) {
		
		if( id == idExplainedKey ) return idExplainedVal;
		idExplainedKey = id;
		
		String str, natKey;
		int nat = (int) (( id & NAT_MSK ) >>> NAT_SHIFT);
		natKey = IDNAT.values()[ nat ].getKey();
		if( nat == IDNAT.USER.ordinal() ) {
			// user id
			int userSession = (int) ( id & USER_SES_MSK >>> USER_SES_SHIFT );
			long user = ( id & USER_IDL_MSK >>> USER_IDL_SHIFT );
			str = natKey + user + "." + userSession;
		} else {
			// std id
			long idb = ( id & IDB_MSK ) >>> IDB_SHIFT;
			long idl = ( id & IDL_MSK ) >>> IDL_SHIFT;
			str = natKey + idb + "." + idl;
		}
		return idExplainedVal = str;
	}
	
	/** Return given id 4 tuples string. */
	static String idTuples( long id ) {
		
		if( id == idTuplesKey ) return idTuplesVal;
		idTuplesKey = id;
		
		int uLeft1, uLeft2, uRight1, uRight2;
		
		uLeft1 = (int) 	( id & 0xffff000000000000L >>> 48 );
		uLeft2 = (int) 	( id & 0x0000ffff00000000L >>> 32 );
		uRight1 = (int) ( id & 0x00000000ffff0000L >>> 16 );
		uRight2 = (int) ( id & 0x000000000000ffffL );
		return idTuplesVal = uLeft1 + "." + uLeft2 + "." + uRight1 + "." + uRight2;
	}
	
	// ===== interface IExternalizable (counters)
	
	// counter are not used nor stored
}

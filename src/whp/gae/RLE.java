package whp.gae;

import whp.util.Misc;

/** RLE: Run Time Last Error static. Lightweight error manager (no properties, only
 * static) used by VO and Tables to store the reason of an error.
 * Can be used as inherited class.
 * <p> Usage: if null (or false) is received, the caller can 
 * check rleClass, rleType, rleMsg or rleMsgFull to see if an msg is available.
 * These values are null if no message available. Warning: the rle values are static, 
 * and reflect only the last order (a new order erase previous rle). Use rleReset 
 * before set an error. </p> */
public class RLE 
{
	// ===== statics
	
	// these constants MUST be synchronized with AS client.
	private static String[] RLELIBS; 
	
	// base return code errors
	public static final int OK 				= 0;
	public static final int OK_ASYNC 		= 1;
	
	public static final int COMMENT 		= 10;
	public static final int TITLE 			= 11;
	
	public static final int INF_NOTLOGGED	= 20;
	public static final int INF_NETDOWN		= 21;
	public static final int INF_MUTEX		= 22;
	
	public static final int ERR_NULLVALUE	= 40;
	public static final int ERR_NOTFOUND	= 41;
	public static final int ERR_DUPNAME 	= 42;
	public static final int ERR_DUPID 		= 43;
	public static final int ERR_INCONSISTENT= 44;
	public static final int ERR_NOTINTABLE	= 45;
	public static final int ERR_HASCHILD	= 46;
	public static final int ERR_NOTDELREADY	= 47;
	public static final int ERR_BADTYPE		= 48;
	public static final int ERR_OUTPATH		= 49;
	public static final int ERR_LOGIN		= 50;
	public static final int ERR_PENDING		= 51;
	public static final int ERR_SESSION		= 52;
	public static final int ERR_REFUSED		= 53;
	public static final int ERR_EXIST		= 54;
	public static final int ERR_NOTEXIST	= 55;
	public static final int ERR_SHARDEDOVER	= 56;
	public static final int ERR_NOTVALID	= 57;
	
	public static final int ERR_DBCONFLICT	= 70;
	public static final int ERR_DBERROR		= 71;
	public static final int ERR_DBOVERLOAD	= 72;
	public static final int ERR_DBERRSTATE	= 73;
	public static final int ERR_DBERRFAIL	= 74;
	
	public static final int ERR_EXECERROR	= 90;
	public static final int ERR_TIMEOUT		= 91;
	public static final int ERR_RPCFAULT	= 92;
	public static final int ERR_ROLENA		= 93;
	public static final int ERR_NOTLOADED	= 94;
	public static final int ERR_PKEY		= 95;
	public static final int ERR_INTERNAL	= 96;
	public static final int ERR				= 97;
	
	private static final int RLEMax			= 100;	// max entries
	
	@SuppressWarnings("rawtypes")
	private static Class _class = null;
	private static int _type = 0;
	private static StringBuilder _msg = new StringBuilder();
	
	
	// init rlelibs once if null
	public RLE() {
		if( RLELIBS == null ) {
			RLELIBS 					= new String[ RLEMax ];
			
			RLELIBS[ OK ] 				= "ok";
			RLELIBS[ OK_ASYNC ]			= "okAsyncLaunched";
			RLELIBS[ COMMENT ]			= "comment";
			RLELIBS[ TITLE ]			= "title";
			
			RLELIBS[ INF_NOTLOGGED ] 	= "notLogged";
			RLELIBS[ INF_NETDOWN ] 		= "networkDown";
			RLELIBS[ INF_MUTEX ] 		= "mutexRefused";
			
			RLELIBS[ ERR_NULLVALUE ] 	= "nullValue";
			RLELIBS[ ERR_NOTFOUND ] 	= "notFound";
			RLELIBS[ ERR_DUPNAME ] 		= "dupName";
			RLELIBS[ ERR_DUPID ] 		= "dupId";
			RLELIBS[ ERR_INCONSISTENT ]	= "notConsistent";
			RLELIBS[ ERR_NOTINTABLE ] 	= "notInTable";
			RLELIBS[ ERR_HASCHILD ] 	= "hasChild";
			RLELIBS[ ERR_NOTDELREADY ] 	= "notReady";
			RLELIBS[ ERR_BADTYPE ] 		= "badType";
			RLELIBS[ ERR_OUTPATH ] 		= "outPath";
			RLELIBS[ ERR_LOGIN ] 		= "login";
			RLELIBS[ ERR_PENDING ] 		= "pending";
			RLELIBS[ ERR_SESSION ] 		= "session";
			RLELIBS[ ERR_REFUSED ] 		= "refused";
			RLELIBS[ ERR_EXIST ] 		= "exist";
			RLELIBS[ ERR_NOTEXIST ] 	= "notExist";
			RLELIBS[ ERR_SHARDEDOVER ] 	= "shardedOverflow";
			RLELIBS[ ERR_NOTVALID ] 	= "notValid";
			
			RLELIBS[ ERR_DBCONFLICT ] 	= "dbConflict";
			RLELIBS[ ERR_DBERROR ] 		= "dbError";
			RLELIBS[ ERR_DBOVERLOAD ] 	= "dbOverload";
			RLELIBS[ ERR_DBERRSTATE ] 	= "dbErrState";
			RLELIBS[ ERR_DBERRFAIL ] 	= "dbErrFail";
			
			RLELIBS[ ERR_EXECERROR ] 	= "execError";
			RLELIBS[ ERR_TIMEOUT ] 		= "timeout";
			RLELIBS[ ERR_RPCFAULT ] 	= "rpcReturnFault";
			RLELIBS[ ERR_RPCFAULT ] 	= "rpcReturnFault";
			RLELIBS[ ERR_ROLENA ] 		= "roleNotAllowed";
			RLELIBS[ ERR_NOTLOADED ] 	= "notLoaded";
			RLELIBS[ ERR_PKEY ] 		= "primaryKey";
			RLELIBS[ ERR_INTERNAL ]		= "internal";
			RLELIBS[ ERR ]				= "err";
		}
	}
	
	//TODO: add function name as parameter of rleReset and prefix message with it
	protected void rleReset() { _class = null; }
	
	protected Object rleSet( int type, String msg ) {
		
		if( _class == null ) _msg.setLength( 0 );			// if previous err, concat in msg
		_msg.append( "[OVERLAP ERROR: " + rleMsgFull() +  "]" );
		
		_class = this.getClass();	// get inherited class
		_type = type;				// typed message
		
		return null;
	}
	
	public String 	rleClass() 		{ return( _class != null ) ? Misc.getSimpleClassName( _class ) : null; }
	public int 		rleType() 		{ return( _class != null ) ? _type : 0; }
	public String 	rleTypeLib() 	{ return( _class != null ) ? RLELIBS[ _type ] : null; }
	public String 	rleMsg() 		{ return( _class != null ) ? _msg.toString() : null; }
	
	/** Return formated message: "Class: message (type)" or null if no message. */
	public String 	rleMsgFull()	{ return( _class != null ) ? Misc.getSimpleClassName( _class ) + ": " + _msg.toString() + " (" + rleTypeLib() + ")" : null; }
	
}

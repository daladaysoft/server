package whp.mdb;

import whp.tls.Context;

public class AppInitialize {
	
	// ===== constants & static variable
	// ===== serialized variable
	// ===== transient variables
	
	private Context context;
	
	// ===== instance initializer
	{
	}
	
	// ===== constructor
	
	public AppInitialize( Context context ) {
		this.context = context;
	}
	
	// ===== initialize first
	
	/** Initialize (domains root, sys, app,pub, eventTable, userTable, roleTbale, sysadmin user) in datastore. 
	 * @throws VOStoreException */
	public void initializeFirst() throws VOStoreException {
		
		long begTime = System.currentTimeMillis();
		
		// ===== SHARED BY CODE (GAE AND CLIENTS MUST CREATE THIS CONTEXT BY CODE) =====
		
		// ----- base domains
		
		VODomainRoot domainRoot;
		domainRoot 				= new VODomainRoot();
		domainRoot.setDomainId(	0 );					// a domain has no domain parent
		domainRoot.setKind(		VODomain.kind );		// root: no domain parent
		domainRoot.setId(		MdbId.getIdDomainRoot() );
		domainRoot.name 		= "domainRoot";
		domainRoot.storage		= VODomain.STORAGE_REMOTE;
		domainRoot.isUnique		= true;
		domainRoot.store();
		context.addLog( "domainRoot initialized, no parent ", begTime );
		
		VODomain domainSys;
		domainSys 				= new VODomain();
		domainSys.setDomainId(	0 );					// a domain has no domain parent
		domainSys.setTableId(	MdbId.getIdDomainRoot() );		
		domainSys.setId(		MdbId.getIdDomainSys() );
		domainSys.name 			= "sys";
		domainSys.storage		= domainRoot.storage;
		domainSys.isUnique		= true;
		domainSys.store();
		context.addLog( "domainSys initialized ", begTime );
		
		VODomain domainApp;
		domainApp 				= new VODomain();
		domainApp.setDomainId(	0 );					// a domain has no domain parent
		domainApp.setTableId(	MdbId.getIdDomainRoot() );		
		domainApp.setId(		MdbId.getIdDomainApp() );
		domainApp.name 			= "app";
		domainApp.storage		= domainRoot.storage;
		domainApp.isUnique		= true;
		domainApp.store();
		context.addLog( "domainApp initialized ", begTime );
		
		VODomain domainPub;
		domainPub 				= new VODomain();
		domainPub.setDomainId(	0 );					// a domain has no domain parent
		domainPub.setTableId(	MdbId.getIdDomainRoot() );		
		domainPub.setId(		MdbId.getIdDomainPub() );
		domainPub.name 			= "pub";
		domainPub.storage		= domainRoot.storage;
		domainPub.isUnique		= true;
		domainPub.store();
		context.addLog( "domainPub initialized ", begTime );
		
		// ----- base tables (not created for now)
		
//		VODUserTable userTable;
//		userTable 				= new VODUserTable();
//		userTable.domainId 		domainRoot.getDomainId() );	
//		userTable.parentId 		domainRoot.getDomainId() );	
//		userTable.id 			= "T0.0";			// fixed id for this table
//		userTable.name 			= "userTable";
//		userTable.store();
//		context.addLog( "userTable initialized, parent " + userTable.parentId + " ", begTime );
//		
//		VODMdbTable mdbTable;
//		mdbTable 				= new VODMdbTable();
//		mdbTable.domainId 		domainRoot.getDomainId() );	
//		mdbTable.parentId 		domainRoot.getDomainId() );	
//		mdbTable.id 			= "T0.1";			// fixed id for this table
//		mdbTable.name 			= "mdbTable";
//		mdbTable.store();
//		context.addLog( "mdbTable initialized, parent " + mdbTable.parentId + " ", begTime );
		
		// ----- base user entity
		
		VOUser user;
		
		// sysadmin
		user 					= new VOUser();
		user.setDomainId(		0 );	
		user.setKind(			VOUser.kind );
		// user.setId(); (auto id)
		user.name 				= "system";
		user.login 				= "sysadmin@whp.com";
		user.pswd 				= "sysadmin";
		user.userDomainId 		= MdbId.getIdDomainRoot();
		user.userRoleId 		= "a1";
		user.desc 				= "initialized by gae";
		user.store();
		context.addLog( "User (sysadmin) initialized, parent " + user.getTableId() + " ", begTime );
		
		// bidRequest
		user 					= new VOUser();
		user.setDomainId(		0 );	
		user.setKind(			VOUser.kind );
		// user.setId(); (auto id)
		user.name 				= "bidRequest";
		user.login 				= "bidRequest";
		user.pswd 				= "bidRequest";
		user.userDomainId 		= MdbId.getIdDomainRoot();;
		user.userRoleId 		= "0";
		user.desc 				= "initialized by gae";
		user.store();
		context.addLog( "User (bidrequest) initialized, parent " + user.getTableId() + " ", begTime );
		
		// ----- roles table (in root domain, by events)
		
//		VOGenericTablePersist roleTable = new VOGenericTablePersist();
//		roleTable.version 		= 1.0;
//		roleTable.setDomainId(	domainRoot.getDomainId() );	
//		roleTable.parentId 		= "D0.0";			// parent = domainRoot
//		roleTable.id 			= "T0.2";			// fixed id for this table
//		roleTable.name 			= "roles";
//		roleTable.store();
//		context.addLog( "roleTable initialized, parent " + roleTable.parentId + " ", begTime );
		
		// ===== UNSHARED BY CODE DATA (SHOULD BE PROPAGATED VIA EVENTS) =====

//		// ----- dictionary structure initialization (in app domain, by events)
//		
//		/** Dictionary tables and entities are created as dictionary entities AND events
//		 * in the /root/app domain. These events are intended to be loaded and deployed 
//		 * by clients when they start and update their /root/app domain, "as usual". */
//		
//		// VODictEntities
//		VODictEntities dictEntities 	= new VODictEntities();
//		dictEntities.version 			= 1.0;
//		dictEntities.setDomainId(		domainApp.getId() );					// no domain
//		dictEntities.setTableId(		domainApp.getId() );		
//		// TODO mettre en place getnew dans MdbId si l'on veut créer des entités coté serveur
//		// le bid réservé serveur est 1
//		dictEntities.setId(				"Dt0.0" );
//		dictEntities.name				= "dictEntities";
//		if( initializeFirst_CreateEvent( dictEntities, begTime ) == false ) return;
//		
//	}
//	
//	/** @see #initializeFirst_CreateEvent 
//	 * Conflit possible avec premiers events créés en local ? a vérifier. */ 
//	int initializeFirst_EventCounter = 1000;
//	
//	/** Create a new event from a VO, and serialize given VO in voEvent.voClone. 
//	 * If OK, store the voEvent, and return true. Else, do not store and return false. 
//	 * Use initializeFirst_EventCounter to create event id: vo.domainId + "_e" + counter + ".0" 
//	 * (this counter can be used as this as this sequence is launched only once and stay in one JVM). 
//	 * (Warning, do not use for VO that have a table isAutoGlobalId true). 
//	 * @throws VOStoreException */
//	private boolean initializeFirst_CreateEvent( VOBaseFull vo, long begTime ) throws VOStoreException {
//		
//		// set GRANITEDS context, necessary to do a serialization
//		setGraniteContext( begTime );
//		
//		// create an event for given vo
//		VOEvent voEvent 		= new VOEvent();
//		voEvent.version 		= 1.0;
//		
//		voEvent.setDomainId( vo.getDomainId() );
//		voEvent.setTableId(  vo.getTableId() );
//		voEvent.setEventId(  "eM0." + (initializeFirst_EventCounter++) );
//
//		voEvent.type			= VOEvent.INSERT;
//		voEvent.storage			= VODomain.STORAGE_REMOTE;
//		voEvent.userId			= "";
//		voEvent.voId			= vo.getIdd();
//		
//		// serialize the given VO to voEvent.voClone (AMF)
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		AMF3Serializer amf = new AMF3Serializer( out );
//		try { 
//			amf.writeObject( vo );
//			amf.flush();
//			voEvent.voClone = out.toByteArray();
//			voEvent.transport = VOEvent.TRANSPORT_VOFULL;
//		} catch ( IOException e ) {
//			context.addLog( "Grave error in gae init, Abort. AMF serialization " + e.toString() );
//			return false;		// Abort
//		}
//		
//		// store voEvent now
//		voEvent.store( context );
//		context.addLog( vo.name + " initialized as /root/app.event" , begTime );
//		
//		return true;
	}
	
//	/** @see #setGraniteContext */ 
//	boolean isGraniteContext = false;
//	
//	/** Set the granite context if not done. */
//	private boolean setGraniteContext( long begTime ) {
//		
//		if( isGraniteContext == true ) return isGraniteContext;
//		
//		// set GRANITEDS context, necessary to do a serialization
//		GraniteConfig graniteConfig = null;
//		try {
//			graniteConfig = new GraniteConfig( null, null, null, null );
//		} catch (IOException e) {
//			context.addLog( "Grave error in GraniteConfig init, Abort. AMF serialization " + e.toString() );
//			return false;		// Abort
//		} catch (SAXException e) {
//			context.addLog( "Grave error in GraniteConfig init, Abort. AMF serialization " + e.toString() );
//			return false;
//		} 
//		ServicesConfig servicesConfig = null;
//		try {
//			servicesConfig = new ServicesConfig( null, null, false );
//		} catch (IOException e) {
//			context.addLog( "Grave error in ServicesConfig init, Abort. AMF serialization " + e.toString() );
//			return false;		// Abort
//		} catch (SAXException e) {
//			context.addLog( "Grave error in ServicesConfig init, Abort. AMF serialization " + e.toString() );
//			return false;
//		} 
//		Map<String, Object> applicationMap = new HashMap<String, Object>(); 
//		SimpleGraniteContext.createThreadIntance( graniteConfig, servicesConfig, applicationMap ); 
//		
//		isGraniteContext = true;
//		return isGraniteContext;
//	}
}

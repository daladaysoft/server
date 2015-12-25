package whp.mdb;

import java.util.Date;

import whp.boot.VOLibBoot;
import whp.gae.DLAtom;
import whp.gae.RLE;
import whp.tls.Context;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

public class AppLib 
{	
	// ===== constants & static variable
	
	// ===== serialized variable
	// ===== transient variables
	
	private Context context;
	private AppFile appfile;
	
	// ===== instance initializer	
	// ===== constructor
	
	public AppLib( Context context ) {
		this.context = context;
		appfile = new AppFile( context );
	}
	
// ===== PUBLIC, UPLOAD LIB
	
	/** Upload an SWC/SWF library. Create or update VOLibBoot entity in datastore, 
	 * and create or update (delete then add) data under fileName in blobstore. 
	 * Return nothing if no error. */
	public void whpLibUpload( VOLibBoot voLib, byte[] data ) {
		
		// TODO: should put all of this in a transaction
		
		long begTime = System.currentTimeMillis();
		
		// search library entity by given name
		Entity lib = libraryGetByName( voLib.getNameFull() );
		
		// if found, delete old blobstore file, else create new
		if( lib != null ) libraryDeleteFile( lib, voLib );
		else lib = libraryCreate( voLib );
		
		// store uploaded file
		String filePath = appfile.fileStore( voLib.getFileName(), voLib.fileSize, data );
		if( filePath == null ) {
			context.setRc( RLE.ERR_REFUSED );
			context.addLog( "whpLibUpload " + voLib.getFileName() + " not stored", begTime );
			return;
		}
		
		// store entity with given values (and set last version library)
		libraryStore( voLib, lib, filePath );
		
		// log and return
		context.setRc( RLE.OK );
		context.addLog( "Upload and write lib " + voLib.getNameFull(), begTime );
	}
	
	// ===== PUBLIC, READ SYNC LIB
	
	/** Read sync an SWC/SWF library. The name MUST be: 
	 * <li> 'name_platform_signature_mode' (isVersion false, last version returned). </li>
	 * <li> 'name_platform_signature_mode_version' (isVersion true). </li> <p>
	 * <li> Return file data and new date if fromDate is &lt than current library date. </li>
	 * <li> Return OK without data if fromDate = current library date. </li>
	 * <li> Return NOTFOUND if library name not found. </li></p> */
	public void whpLibReadSync( String name, Date fromDate, boolean isVersion ) {
		
		long begTime = System.currentTimeMillis(); 
		
		// search library
		Entity lib;
		if( isVersion == true ) {
			
			// search GIVEN version library entity for the name, return if not found
			lib = libraryGetByName( name );
			if( lib == null ) {
				context.setRc( RLE.ERR_NOTFOUND );
				context.addLog( "whpLibReadSync " + name + " not found", begTime );
				return;
			}
			
		} else {
			
			// search LAST (newest) version library entity for the name, return if not found
			lib = libraryGetByLastVersionFast( name );
			if( lib == null ) {
				context.setRc( RLE.ERR_NOTFOUND );
				context.addLog( "whpLibReadSync " + name + " not found", begTime );
				return;
			}
		}
		
		// check date
		Date libUpd = (Date) lib.getProperty( "upd" );
		if( libUpd.equals( fromDate ) ) {
			context.addLog( "whpLibReadSync " + (String) lib.getProperty( "fileName" ) + " not downloaded (already sync).", begTime );
			return;
		}
		
		// construct VOLibBoot to return results
		VOLibBoot voLib = new VOLibBoot();
		entity2VoLib( lib, voLib );
		
		// read data from linked file
		String filePath = (String) lib.getProperty( "filePath" );
		byte[] libCode = appfile.fileRead( name, filePath, voLib.fileSize );
		if( libCode == null ) return;
		
		// set date and read data in context and log
		context.addData( voLib );
		context.addData( libCode );
		context.addLog( "whpLibReadSync " + name + " downloaded from " + filePath, begTime );
	}
	
	
// ===== PRIVATE, LIBRARY SERVICES
	
	/** Return library entity or null if not found. */
	private Entity libraryGetByName( String nameFull ) {
						
		Entity gaeEntity = null;
		Key rootKey = KeyFactory.createKey( "VOLibs", "lib" );
		Key key = KeyFactory.createKey( rootKey, "VOLibBoot", nameFull );
		try { gaeEntity = context.ds.get( key ); }
		catch( EntityNotFoundException e ) {}
		
		return gaeEntity;
	}
	
	/** Create a new Library entity (empty, not stored). */
	private Entity libraryCreate( VOLibBoot voLib ) {
		
		Key rootKey = KeyFactory.createKey( "VOLibs", "lib" );
		Key key = KeyFactory.createKey( rootKey, "VOLibBoot", voLib.getNameFull() );
		Entity lib = new Entity( key );
		return lib;
	}
	
	/** Delete linked blob file, set property entity.filePath to null. */
	private void libraryDeleteFile( Entity lib, VOLibBoot voLib ) {
		
		String filePath = (String) lib.getProperty( "filePath" );
		if( filePath != null ) {
			appfile.fileDelete( filePath, voLib.getNameFull() );
			lib.setUnindexedProperty( "filePath", null );
		}
	}
	
	/** Store library entity, with given properties. Set unique update date.
	 * Create or update the last library entity if new stored is the last. */
	private void libraryStore( VOLibBoot voLib, Entity lib, String filePath ) {
		
		voLib2Entity( voLib, lib, filePath );
		context.ds.put( lib );
		
		// get (or create) the newest library, store if needed with namePrefix, but last used fileName
		String namePrefix = voLib.name + "_" + voLib.platform + "_" + voLib.signature + "_" + voLib.mode + "_";
		Entity libLast = libraryGetByLastVersionFast( namePrefix );
		int vc = -1;
		if( libLast == null ) {
			// create if libLast not created
			Key rootKey = KeyFactory.createKey( "VOLibs", "lib" );
			Key key = KeyFactory.createKey( rootKey, "VOLibBoot", namePrefix );
			libLast = new Entity( key );
		} else {
			// else compare added lib.version to libLast.version
			String[] libVersionArray = ( (String) lib.getProperty( "version" ) ).split( "-" );
			String[] libLastVersionArray = ( (String) libLast.getProperty( "version" ) ).split( "-" );
			vc = versionCompare( libVersionArray, libLastVersionArray );
		}
		if( vc <= 0 ) {
			// then libLast becomes new added library (with lib.fileName)
			libLast.setPropertiesFrom( lib );
			context.ds.put( libLast );
			context.addLog( "LibLast updated for: " + namePrefix + " with version " + voLib.version );
		}
	}
	
	/** Get the newest last library (read library last record) or null if nothing found. */
	private Entity libraryGetByLastVersionFast( String namePrefix ) {
		
		Entity gaeEntity = null;
		Key rootKey = KeyFactory.createKey( "VOLibs", "lib" );
		Key key = KeyFactory.createKey( rootKey, "VOLibBoot", namePrefix );
		try { gaeEntity = context.ds.get( key ); }
		catch( EntityNotFoundException e ) {}
		
		return gaeEntity;
	}
	
	/** Copy voLib in entity format, voLib and entity must exist. */
	private void voLib2Entity( VOLibBoot voLib, Entity entity, String filePath ) {
		entity.setUnindexedProperty( "name", voLib.name );
		entity.setUnindexedProperty( "platform", voLib.platform );
		entity.setUnindexedProperty( "signature", voLib.signature );
		entity.setUnindexedProperty( "mode", voLib.mode );
		entity.setUnindexedProperty( "version", voLib.version );
		
		entity.setProperty( "upd", new Date( DLAtom.getUniqueDate( context, DLAtom.SHARDED_INTERVAL ) ) ); 
		entity.setUnindexedProperty( "desc", voLib.desc );
		
		entity.setUnindexedProperty( "fileName", voLib.getFileName() );
		entity.setUnindexedProperty( "fileSize", voLib.fileSize );
		
		entity.setUnindexedProperty( "filePath", filePath );	// from blobstore
	}
	
	/** Copy entity in voLib format, voLib and entity must exist. */
	private void entity2VoLib( Entity  entity, VOLibBoot voLib ) {
		
		voLib.name = (String) entity.getProperty( "name" );
		voLib.platform = (String) entity.getProperty( "platform" );
		voLib.signature = (String) entity.getProperty( "signature" );
		voLib.mode = (String) entity.getProperty( "mode" );
		voLib.version = (String) entity.getProperty( "version" );
		
		voLib.upd = (Date) entity.getProperty( "upd" );
		voLib.desc = (String) entity.getProperty( "desc" );
		
		voLib.setFileName( (String) entity.getProperty( "fileName" ) );
		voLib.fileSize = (int)(long)(Long) entity.getProperty( "fileSize" );
		// filePath never stored in voLib
	}
	
	/** Get the newest last library (read all in prefix) or null if nothing found. */
	@SuppressWarnings("unused")
	private Entity libraryGetByLastVersionFull( String namePrefix ) {
		
		// prepare name prefix to search
		int namePrefixLen = namePrefix.length();
		
		// prepare result
		Entity result = null;
		String[] resultVersionArray = null;
		
		// query kind (VoLibBoot) in root key
		Key rootKey = KeyFactory.createKey( "VOLibs", "lib" );
		Query query = new Query( "VOLibBoot", rootKey );
		
		// use simple filter (see https://developers.google.com/appengine/docs/java/datastore/queries)
		query.setFilter( new FilterPredicate( "name", FilterOperator.GREATER_THAN_OR_EQUAL, namePrefix ) );
		
		// loop on VoLibBoot entities
		Iterable<Entity> gaeEntities = context.ds.prepare( query ).asIterable();
		for( Entity gaeEntity : gaeEntities ) {
			
			// break if read name not match namePrefix
			String keyName = gaeEntity.getKey().getName();
			if( keyName.length() < namePrefixLen ) break;
			if( ! keyName.substring( 0, namePrefixLen ).equals( namePrefix ) ) break;
			
			// compare the versions, store the grater (= newest)
			int vc = 1;
			if( result != null ) {
				String[] readVersionArray = ( (String) gaeEntity.getProperty( "version" ) ).split( "-" );
				vc = versionCompare( resultVersionArray, readVersionArray );
			}
			if( vc > 0 ) {
				result = gaeEntity;
				resultVersionArray = ( (String) result.getProperty( "version" ) ).split( "-" );
			}
		}
		
		// return entity with last version or null if not found
		return result;
	}
	
	/** Compare versions arrays, return the greater (-1 if left(a), 0 if equals, 1 if (right)b). */
	private int versionCompare( String[] va, String[] vb ) {
		
		int vaLen = va.length;
		int vbLen = vb.length;
		int i = 0;
		while( true ) {
			if( i == vaLen && i == vbLen ) return 0;
			if( i == vaLen ) return 1;
			if( i == vbLen ) return -1;
			int v = compareStrInt( va[i], vb[i] );
			if( v != 0 ) return v;
			i++;
		}
	}
	
	/** Compare as integers if possible, else compare as strings 
	 * ("09" greater than "1", but "09a" less than "1" or "1a").
	 * Return the greater (-1 (left) if a, 0 (center) if equals, 1 (right) if b). */
	private int compareStrInt( String a, String b ) { 
		
		boolean isInt = false;
		int ia = 0;
		int ib = 0;
		try { 
			ia = Integer.parseInt( a ); 
			ib = Integer.parseInt( b ); 
			isInt = true;
		} 
		catch( NumberFormatException e ) {}
		
		if( isInt == true ) {
			if( ia > ib ) return -1;
			if( ia < ib ) return 1;
			return 0;
		}
		
		int v = a.compareTo( b );
		if( v > 0 ) return -1;
		if( v < 0 ) return 1;
		return v;
	} 

}

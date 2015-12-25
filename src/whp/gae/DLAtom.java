package whp.gae;

import java.util.Date;

import whp.tls.Context;
import whp.util.Misc;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;

/** Atomic services in GAE env. */
public class DLAtom {
	
	public static final long SHARDED_INTERVAL = 500;
	
	// unique date root entity
	static final String DATE_KIND = "AD";
	static final String DATE_NAME = "UPD";
	static final String DATE_VALUE = "v";
	
	// base id root entity
	static final String BASE_KIND = "AM";
	static final String BASE_NAME = "IDM";
	static final String BASE_VALUE = "v";
	
	static final int BASE_MIN = 2;
	static final int BASE_MAX = 524288; // 2^19-1
	
	/** Each getUniqueDate compute and return the last unique date that is ALWAYS
	 *  greater for "shardedInterval ms" than the previous returned date in the same kind, 
	 *  comprised any number of concurrent calls by different JVMs. This allows a "sharded"
	 *  add of new (max shardedInterval) events that can sort itself within the returned 
	 *  date, in order to maintain a strict sort and unique value. 
	 *  <p> getUniqueDate use a datastore transaction on this date. Thus, be careful, a
	 *  getUniqueDate take around 100ms. </p>
	 *  <p> Is synchronized to help the datastore transaction to avoid contention if more 
	 *  than one thread call getUniqueDate() in the same JVM. </p> 
	 *  <p> Use shared (by all) root entity kind "AD", name "UPD", property "v". </p> */
	public synchronized static long getUniqueDate( Context context, long shardedInterval ) {
		
		long jvmTime = System.currentTimeMillis();
		long begTime = jvmTime;
		
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Key key = KeyFactory.createKey( DATE_KIND, DATE_NAME );
		Transaction dst = ds.beginTransaction();
		
		try {
			// get entity "AD", "UPD" if exist
			Entity gaeEntity = null;
			try { gaeEntity = ds.get( dst, key ); }
			catch( EntityNotFoundException e ) {}
			
			// verify if last GAE date is < current JVM date
			if( gaeEntity == null ) gaeEntity = new Entity( key );
			else {
				long gaeLastTime = (Long) gaeEntity.getProperty( DATE_VALUE );
				if( gaeLastTime >= jvmTime )
					jvmTime = gaeLastTime + 1;
			}
			
			// add shardedInterval ms to next time use
			jvmTime += shardedInterval; 
						
			// store new last date that is ALWAYS greater than previous
			gaeEntity.setUnindexedProperty( DATE_VALUE, jvmTime );
			ds.put( dst, gaeEntity );
			dst.commit();
			context.addLog( "commit: " + Misc.getDateFormatHMSS().format( new Date( jvmTime ) ), begTime );
		} catch( Error e) { 
			dst.rollback();
			context.addLog( "FAILED and rollback: " + e, begTime );
		}
		
		return jvmTime;
	}
	
	/** Each getUniqueBaseId compute and return unique base id counter, from 2 to BASE_MAX.
	 *  Value returned as long, is ALWAYS previous returned + 1.
	 *  Throw error if mdb counter out of range or transaction error (contention).
	 *  <p><b> Do not use getUniqueBaseId, that returns a counter, not an id. See MdbId getNewIdMdb
	 *  to get a new Mdb id.</b></p>
	 *  <p> Is synchronized to help the datastore transaction to avoid contention if more 
	 *  than one thread call getUniqueBaseId() in the same JVM. </p> 
	 *  <p> Use shared (by all) root entity BASE_KIND, BASE_NAME, property BASE_VALUE. </p> */
	public synchronized static long getUniqueBaseId( Context context ) {
		
		long begTime = System.currentTimeMillis();
		
		// start 2 (0 idb not allowed, 1 reserved for gae server id)
		long baseId = 2;	
		long newBaseId = -1;
		
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Key key = KeyFactory.createKey( BASE_KIND, BASE_NAME );
		Transaction dst = ds.beginTransaction();
		
		try {
			// get entity "AM", "IDM" if exist
			Entity gaeEntity = null;
			try { gaeEntity = ds.get( dst, key ); }
			catch( EntityNotFoundException e ) {}
			
			// verify if last GAE date is < current JVM date
			if( gaeEntity == null ) gaeEntity = new Entity( key );
			else baseId = (Long) gaeEntity.getProperty( BASE_VALUE );
			
			// get and increment stored
			newBaseId = baseId;
			if( newBaseId < BASE_MIN || newBaseId > BASE_MAX )
				throw new Error( "invalid IDB value (range " + BASE_MIN + "-" + BASE_MAX +"): " + newBaseId );
			baseId++;
						
			// store new base id value
			gaeEntity.setUnindexedProperty( BASE_VALUE, baseId );
			ds.put( dst, gaeEntity );
			dst.commit();
			context.addLog( "IDB: " + newBaseId, begTime );
		} catch( Error e) { 
			dst.rollback();
			context.addLog( "FAILED and rollback: " + e, begTime );
			newBaseId = -1;
		}
		
		if( newBaseId < 0 ) throw new Error( "getUniqueBaseId failed" );
		
		return newBaseId;
	}
	
	// synchronized can help the datastore transaction to avoid contention
	// if more than one thread is working on the same kind in the same JVM
	// not used and probably a bad idea: see long id from gae datastore
//	public synchronized static long getUUID() {
//		
//		long startTime = System.nanoTime();
//		long uuid = 0;
//		
//		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
//		Key key = KeyFactory.createKey( "A" , "uuid" );
//		Transaction dst = ds.beginTransaction();
//		
//		try {
//			// get kind "A", key "uuid", prop "d" if exist
//			Entity gaeEntity = null;
//			try { gaeEntity = ds.get( key ); }
//			catch( EntityNotFoundException e ) {}
//			
//			// create if not found
//			if( gaeEntity == null )
//				gaeEntity = new Entity( key );
//			else
//				uuid = (Long) gaeEntity.getProperty("a");
//			
//			// increment uuid (= first is 1, GAE long key do not allows 0)
//			uuid++;
//			
//			// store new uuid
//			gaeEntity.setUnindexedProperty( "a", uuid );
//			ds.put( gaeEntity );
//			dst.commit();
//
//		} catch( Error e) { 
//			dst.rollback();
//			TLSAnswer.addLog( "FAILED and rollback: " + e 
//					+ " in " + ((System.nanoTime() - startTime) / 1000000) + "ms" );
//		}
//		
//		return uuid;
//	}
}

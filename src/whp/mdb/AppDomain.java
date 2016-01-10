package whp.mdb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.LinkedList;

import org.granite.messaging.amf.io.AMF3Deserializer;
import org.granite.messaging.amf.io.AMF3Serializer;
import org.granite.util.Base64;

import whp.WHPBroker;
import whp.gae.RLE;
import whp.tls.Context;
import whp.util.Misc;
import ads.cst.TXRISO;
import ads.net.pusher.Pusher;
import ads.type.BytesOut;
import ads.type.LongAds;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreFailureException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

/** Centralize domain methods (VODomain,VODomainRoot) */
public class AppDomain 
{
	// ===== constants & static variable
	
	/** Max GAE entity size (1024000 minus reserved). */
	private static final int TRANS_CHUNK = 1000000;
	
	/** Max size of returned transaction data (4 MB) */
	private static final int TRANS_SIZEMAX = 4096000;
	
	/** DEF bytes definitions. */
	public static final int END_DATA 	= 0;
	public static final int OPEN_TX 	= 1;
	public static final int OPEN_NEST 	= 2;
	public static final int OPEN_EVENT 	= 3;
	public static final int CLOSE_TX 	= 4;

	// ===== serialized variable
	// ===== transient variables
	
	private Context context;
	
	// ===== instance initializer	
	// ===== constructor
	
	public AppDomain( Context context ) {
		this.context = context;
	}
	
	// ===== PUBLIC, WHP DOMAIN SYNCHRONIZATION
	
	/** Synchronize events (return events fromDate, receive journal bytes to store).
	 * Event received are not stored if return code is not OK. */
	public void adsDomainSync( long domainId, Date fromDate, byte[] txBytes ) {
		domainSyncRead( domainId, fromDate, txBytes );
	}
	
	// ===== PUBLIC, WHP DOMAIN GET PATH
	
	/** Get domains by Id as fast as possible, without instantiate them. Return list of 
	 * domains id in reverse order (from:child, until:parent) directly in context.answer.
	 * If domain from is found, "from" is returned, but "until" is never returned. If "until" 
	 * never reached, return RLE.ERR_INCONSISTENT and domains list until last parent before root.
	 * If "from" equals "until" and found, return only "from".
	 * Return RLE.ERR_NOTFOUND if from domain not found. */
	public void whpDomainGetPath( long fromDomainId, long untilDomainId ) {
		
		long begTime = System.currentTimeMillis();
		int count = 0;
		Entity gaeEntity;
		
		while( true ) {
			count++;
			Key key = VODomain.getKeyFromFactory( fromDomainId );
			try { gaeEntity = context.ds.get( key ); }
			catch( EntityNotFoundException e ) {
				context.setRc( RLE.ERR_NOTFOUND );
				break;
			}
			context.addData( fromDomainId );
			if( MdbId.isIdDomainRoot( fromDomainId ) ) break;
			fromDomainId = (Long) gaeEntity.getProperty( "dpId" );	// dpId: domain parent id
			if( fromDomainId == untilDomainId ) break;
			if( MdbId.isIdDomainRoot( fromDomainId ) ) {
				// until not found
				context.setRc( RLE.ERR_INCONSISTENT );
				context.addLog( "untilDomainId not found" );
				break;
			}
		}
		
		context.addLog( "read " + count, begTime );
	}
	
	// ===== PUBLIC, WHP DOMAIN GET UNIQUE DOMAIN
	
	/** Get domain in parent, by its name. Create domain if not found, and insert it in domain journal. 
	 *  After whpDomainGetUnique, the client has to sync (in standard way) to get existing or created domain.
	 * Checks always parent domain before get or create unique domain. The client has to provide a new
	 * domain id, that will be used if unique domain is created (but if domain is found, the id will be different). */
	public void whpDomainGetUnique( long domainId, String domainName, long domainParentId )  {
		
		// used to avoid too long session
		long begTime = System.currentTimeMillis();
		
		// check parameters
		if( ! MdbId.isIdDomain( domainId ) ) context.addRcLog( RLE.ERR_EXECERROR, "domainId must be a domain" );
		if( domainName == null ) context.addRcLog( RLE.ERR_EXECERROR, "domainName cannot be null" );
		if( ! MdbId.isIdDomain( domainParentId ) ) context.addRcLog( RLE.ERR_EXECERROR, "domainParentId must be a domain" );
		if( context.getRc() != RLE.OK ) return;	
		
		// initialize datastore context
		Key parentKey = VODomain.getKeyFromFactory( domainParentId );
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction dst = ds.beginTransaction();
		
		// TODO revoir le mécanisme fromdate pour création de domaine, il est imprécis
		// devrait arriver du client ? mais pas certain.
		
		// get atomic date
		int shardedInterval = 0;
		LongAds txo = new LongAds();
		long fromDate = getAtomicDate( parentKey, ds, dst, shardedInterval, txo );
		if( fromDate == 0 ) {
			dst.rollback();
			return;
		}
		
		// --- search the domain by name
			
		VODomain domain = domainGetByNameInParent( ds, dst, domainName, domainParentId );
		if( domain != null ) {
			dst.commit();
			context.addLog( "domain unique found (sync to get it)", begTime );
			context.setRc( RLE.OK );
			return;
		}
		
		// if error
		if( context.getRc() != RLE.OK ) {
			return;
		}
		
		// --- not found, have to create the domain and insert it (as MDB transaction event)
		
		byte[] transBytes = null;
		
		while( true ) {
			
			// create new unique domain
			domain = new VODomain();
			domain.setDomainId( 0 );					// a domain has no domain parent
			domain.setTableId( domainParentId );		
			domain.setId( domainId );
			domain.name = domainName;
			domain.storage = VODomain.STORAGE_REMOTE;
			domain.isUnique = true;
			
			try {
				if( transBytes == null ) transBytes = getDomainTransaction( domain );
				break;
			} catch (IOException e) {
				// if time limit reached
				if( System.currentTimeMillis() - begTime > WHPBroker.ELAPSE_MAX ) {
					dst.rollback();
					context.addLog( "Time limit reached during whpDomainGetUnique in " + domainParentId );
					context.setRc( RLE.ERR_TIMEOUT );
					return;
				}
			}
		}
		
		// check commit, can have too much contention
		try { dst.commit(); }
		catch ( Error e ) {
			context.addLog( "commit received transaction error " + e.getMessage() );
			context.setRc( RLE.ERR_DBERROR );
		}
		catch ( ConcurrentModificationException e ) {
			context.addLog( "whpDomainGetUnique commit ConcurrentModificationException " + e.getMessage() );
			context.setRc( RLE.ERR_DBOVERLOAD );
			throw e;
		}
		catch ( IllegalStateException e ) {
			context.addLog( "whpDomainGetUnique commit IllegalStateException " + e.getMessage() );
			context.setRc( RLE.ERR_DBERRSTATE );
		}
		catch ( DatastoreFailureException e ) {
			context.addLog( "whpDomainGetUnique commit DatastoreFailureException " + e.getMessage() );
			context.setRc( RLE.ERR_DBERRFAIL );
		}
		
		// now have to call domainSyncRead (will store transaction and create the domain)
		domainSyncRead( domainParentId, new Date( fromDate ), transBytes );
		
		// end
		context.addLog( "domain unique inserted in server journal (sync to get it) ", begTime );
		context.setRc( RLE.OK );
	}
	
	/** Return domain by name in parent, null if not found. */
	public VODomain domainGetByNameInParent( DatastoreService ds, Transaction dst, String domainName, long domainParentId ) {
		
		// used to avoid too long session
		long begTime = System.currentTimeMillis();
		
		// query domain by dpId
		Query query = new Query( "D" );
		Filter filter = new FilterPredicate( "dpId", Query.FilterOperator.EQUAL, domainParentId );
		query.setFilter( filter );
		
		// iterate on query
		Iterable<Entity> gaeEntities = ds.prepare( query ).asIterable();
		for( Entity gaeEntity : gaeEntities ) {
			
			String eName = (String) gaeEntity.getProperty( "name" );
			if( eName.equals( domainName ) ) { 
				VODomain domain = VODomain.readEntity( gaeEntity );
				return domain;
			}
			
			// if time limit reached message "have to index parent id + name"
			if( System.currentTimeMillis() - begTime > WHPBroker.ELAPSE_MAX ) {
				dst.rollback();
				context.addLog( "Time limit reached during search loop in domain " + domainParentId );
				context.addLog( "(should add index on parentid + name, and change domainGetByNameInParent() method)" );
				context.setRc( RLE.ERR_TIMEOUT );
				return null;
			}
		}
		
		return null;
	}
	
	// ===== PACKAGE, DOMAINS SERVICES
	
	// get domain by id
	
	/** Search VODomain by id, return domain or null if not found. 
	 * TODO: add a JVM fast cache (simple hash on id) on domains (as a domain can't be removed,
	 * and should never be modified). */
	VODomain domainGetById( long domainId ) {
						
		Entity gaeEntity;
		Key key = VODomain.getKeyFromFactory( domainId );
		try { gaeEntity = context.ds.get( key ); }
		catch( EntityNotFoundException e ) {
			context.addLog( "ERROR, domain '" + domainId + "' not found " );
			return null;
		}
		
		VODomain domain = ( gaeEntity != null ) ? VODomain.readEntity( gaeEntity ) : null;
		return domain;
	}
	
	// check domain path
	// TODO: set domains in cache
	
	/** Return the list of domains, from startDomain to stopDomain (lookup).
	 *  List.size = 0 if startDomain not in stopDomain, else = 1 if startDomain is a direct 
	 * child of stopDomain, else > 1 following depth of startDomain in stopDomain. 
	 * <p> List is ordered from parent to children. Example:
	 * d1(d11, d12), d11(d111, d112, d113), d12(d121), d112(d112a, d112b), and 
	 * startDomain=d112b, stopDomain=d11, list from 0 to end = d112, d112b if d11 reached.
	 * Else, list is empty. </p> */
	LinkedList<VODomain> domainGetPathList( VODomain startDomain, VODomain stopDomain ) {
		long begTime = System.currentTimeMillis();
		
		LinkedList<VODomain> domainList = new LinkedList<VODomain>();
		if( startDomain != null && stopDomain != null ) {
			while( startDomain != null && startDomain.getTableId() != 0 ) {
				
				domainList.push( startDomain );
				if( startDomain.getTableId() == stopDomain.getId() ) return domainList;
				startDomain = domainGetById( startDomain.getTableId() );
				if( startDomain == null ) context.addLog( " ERROR (domain.parent of domain " + domainList.getLast().getId() + " not found)", begTime );
				else context.addLog( startDomain.name + " " + domainList.size() + " " + startDomain.getId(), begTime );
			}
		}
		
		// return empty list
		return new LinkedList<VODomain>();
	}
	
	// ===== PRIVATE, DOMAINS SERVICES
	
	/** Synchronize domain. Domain is not checked (can read/store into not created parentKey domain).
	 * This allows to create the domain later (some seconds, or more). Manage (0,1) 'new-transaction' 
	 * received to valid, and (0,N) 'old-transactions' found in datastore to return.
	 * <p><b> Main principles: </b></p>
	 * <li> Always search datastore for old-transactions after fromDate to return to the client. </li>
	 * <li> If found old-transactions and there is a new-transaction, return old-transactions and reject 
	 * new-transaction. If there is no new-transaction, just return old-transactions as sync. In any case,
	 * the client has to increment its fromDate with received transaction date (lastUpd not set). </li>
	 * <li> If no found old-transactions and there is a new-transaction, accept new-transaction, and set 
	 * lastUpd (new-transaction date). If there is no new-transaction, do nothing: just return empty sync. </li>
	 * <p><b> Notes: </b></p>
	 * <li> If a new-transaction is already stored and found in old-transactions, it is accepted (but not stored): 
	 * twice send can arise if the client was not able to store last returned lastUpd (example: network failure). </li> 
	 * <li> Can return more than one old-transactions in a return sequence, but always bound to a transaction. 
	 * Stop to return old-transactions if reach limits (4 MB and 10 seconds), set moreData true in this case 
	 * (the client has to ask for more sync before attempt to send new-transaction if needed). </li> 
	 * <li> If new-transaction accepted, and hasDomain true, then the content of new-transaction is read, and
	 * events related to a domain are deployed (create or update domains). Elsewhere, the server do not spend
	 * time to read contained events. </li> 
	 * <li> If new-transaction accepted, it's contained date is updated with unique lastUpd date. Thus, stored 
	 * transaction has correct unique date inside data, and other clients will receive correct data. But the
	 * initiator that sent new-transaction has to do the same job with lastUpd received on accept. </li> */
	@SuppressWarnings({ "resource", "unused" })
	private int domainSyncRead( long domainId, Date fromDate, byte[] txBytes ) {
		
		// used to avoid too long session
		long begTime = System.currentTimeMillis();
		int shardedInterval = 0;
		
		// initialize datastore context
		Key parentKey = KeyFactory.createKey( "D", domainId );
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction dst = ds.beginTransaction();
		
		// === check received transaction header, and get some values:
		// === transId, bytesAvailable, shardedInterval, transHasDomain
		
		// transaction head data (unused properties must be read in stream)
		int bytesAvailable = 0;
		int def = 0;
		int dlen = 0;
		int clen = 0;
		
		int txType = 0;
		int txHasDomain = 0;
		LongAds txo = new LongAds();
		long txUpd = 0;
		int txRiso = 0;
		long txId = 0;
		long txUid = 0;
		
		AMF3Deserializer tx = null;
		
		// check received tx head and get head values, will be evntually stored later
		if( txBytes != null ) {
		
			tx = new AMF3Deserializer( new ByteArrayInputStream( txBytes ) );
			try {
				
				// compute shardedInterval according in.available size
				tx.mark( 0 );
				bytesAvailable = tx.available();
				shardedInterval = bytesAvailable / TRANS_CHUNK; // integer truncate
				
				// get DEF, check transaction is DEF1
				def = tx.readByte();
				if( def != OPEN_TX ) {
					dst.rollback();
					context.addLog( "received, DEF not DEF_TRANS_OPEN (" + def + ")" );
					context.setRc( RLE.ERR_INCONSISTENT );
					streamClose( tx );
					return context.getRc();
				}
				
				// get dlen (tx data len) & clen (tx content len)
				dlen = tx.readInt();
				clen = tx.readInt();
				
				// get txType and txHasDomain
				txType = tx.readUnsignedByte();
				txHasDomain = ( txType & 2 ) == 2 ? 1 : 0;
				txType = ( txType & 1 ) == 1 ? 1 : 0;
				
				// get other transaction values
				txo.value = tx.readLong();
				txUpd = (long) tx.readDouble();
				txRiso = tx.readUnsignedByte();
				txId = tx.readLong();
				txUid = tx.readLong();
				
				// check clen
				if( clen == 0 ) {
					dst.rollback();
					context.addLog( "transaction received: clen 0" );
					context.setRc( RLE.ERR_INCONSISTENT );
					streamClose( tx );
					return context.getRc();
				}
				// (OPEN_TX:def+dlen+clen=1+4+4) + dlen + (END_DATA:def+dlen=1+4) + clen + (CLOSE_TX:def+clen=1+4) = 19
				if( bytesAvailable != ( dlen + clen + 19 ) ) {
					dst.rollback();
					context.addLog( "transaction received incoorect length (available != dlen + clen + 19)" );
					context.setRc( RLE.ERR_INCONSISTENT );
					streamClose( tx );
					return context.getRc();
				}				
			}
			catch ( IOException e ) {
				dst.rollback();
				context.addLog( "transaction received: decoding error " + e.getMessage() );
				context.setRc( RLE.ERR_INCONSISTENT );
				streamClose( tx );
				return context.getRc();
			}
			
			streamClose( tx );
		}
		
		// === check if there are previous transaction(s) (in 'conflict' if received bytes)
		// === (transactions greater than received fromDate with != transId)
		
		// prepare out buffer
		BytesOut outBytes = new BytesOut();
		AMF3Serializer out = new AMF3Serializer( outBytes );
		
		// query greater than fromDate in domain id, if count > 0, there is a conflict
		long entityUpdKey = 0;
		long outTxo = 0;			// last txo wrote in out buffer
		long outUpdDate = 0;		// last transaction date wrote in out buffer
		long fromLong = fromDate != null ? fromDate.getTime() : 1;
		// query on Key (empty parameter), must have filter Key 
		Query query = new Query( "t", parentKey );
		Filter filter = new FilterPredicate( 
				Entity.KEY_RESERVED_PROPERTY, 
				Query.FilterOperator.GREATER_THAN, 
				KeyFactory.createKey( parentKey, "t", fromLong ) );
		query.setFilter( filter );
		query.addSort( Entity.KEY_RESERVED_PROPERTY, SortDirection.ASCENDING );
		
		int count = 0;
		boolean moreData = false;
		long entityIndex;
		long entityIndexMax;
		long entitytxo = 0;
		long entityId;
		Iterable<Entity> gaeEntities = ds.prepare( dst, query ).asIterable();
		for( Entity gaeEntity : gaeEntities ) {
			
			// count > 0 if something read
			count++;
			
			// get (last) key long date, set as setLastUpd in context
			entityUpdKey = gaeEntity.getKey().getId();
			
			// get index and id
			entityIndex 	= ( Long ) gaeEntity.getProperty( "tindex" );
			entityIndexMax 	= ( Long ) gaeEntity.getProperty( "tindexmax" );
			entitytxo 		= ( Long ) gaeEntity.getProperty( "txo" );
			entityId 		= ( Long ) gaeEntity.getProperty( "tid" );
			
			context.addLog( "read transaction tid " + entityId );
			
			// double client request ? return OK if last chunk, else loop until
			if( entityId == txId ) {
				if( entityIndex != entityIndexMax ) continue; // skip to next chunk
				dst.rollback();
				context.addLog( "read, already received transation (ok)" );
				context.setTxo( entitytxo );
				context.setLastUpd( entityUpdKey );
				context.setRc( RLE.OK );
				streamClose( out );
				return context.getRc();
			}
			
			// get bytes
			Blob entityBlob			= ( Blob ) gaeEntity.getProperty( "tblob" );
			byte[] entityBlobBytes	= entityBlob == null ? null : entityBlob.getBytes();
			
			// check bytes
			if( entityBlobBytes == null ) {
				dst.rollback();
				context.addLog( "read, transaction chunk bytes null" );
				context.setRc( RLE.ERR_INCONSISTENT );
				streamClose( out );
				return context.getRc();
			}
			
			// add read entity bytes to out stream
			try { 
				out.write( entityBlobBytes, 0, entityBlobBytes.length ); 
				outUpdDate = entityUpdKey;
				outTxo = entitytxo;
			} 
			catch (IOException e) {
				dst.rollback();
				context.addLog( "read, out write error: " + e.getMessage() );
				context.setRc( RLE.ERR_INCONSISTENT );
				streamClose( out );
				return context.getRc();
			}
			
			// check bid should be done here ?
			
			// never cut returned transaction in middle
			if( entityIndex != entityIndexMax ) continue;
			
			// check out size limit or elapse time. Note that returned transaction chunks can be 
			// truncated (the client has to aggregate chunks until moreData false). 
			if( out.size() > TRANS_SIZEMAX || System.currentTimeMillis() - begTime > WHPBroker.ELAPSE_MAX ) {
				moreData = true;
				break;
			}	
		}
		
		// === if count > 0: return read transactions. Returned transactions are never truncated 
		// === (they can be immediately stored by client, even if moreData true).
		// === At this step, dst didn't write anything in datastore (dst.commit or rollback do nothing) 
		
		if( count > 0 ) {
			// data older than received data: conflict 
			if( txBytes != null ) {
				dst.rollback();
				context.addLog( "transaction received: conflict, not stored.", begTime );
				context.setBytesOut( outBytes ); 
				context.setTxo( entitytxo );
				context.setLastUpd( outUpdDate );
				context.setMoreData( moreData );
				streamClose( out );
				return context.setRc( RLE.ERR_DBCONFLICT );
			}
			// data older, no received data: just sync
			else {
				dst.commit();
				context.addLog( "domainSyncRead: sync data", begTime );
				context.setBytesOut( outBytes ); 
				context.setTxo( entitytxo );
				context.setLastUpd( outUpdDate );
				context.setMoreData( moreData );
				streamClose( out );
				return context.setRc( RLE.OK );
			}
		}
		
		// stream out no longer used, close it
		streamClose( out );
		
		// === there was no older data (count 0)
		
		// if nothing received (to store), exit now as 'empty sync'
		if( txBytes == null ) {
			dst.commit();
			context.addLog( "domainSyncRead: empty sync", begTime );
			context.setMoreData( false );
			return context.setRc( RLE.OK );
		}
		
		// last check of txo on received tx (txo can be set on double receive (trap before), else not allowed)
		if( txo.value != 0 ) {
			dst.rollback();
			context.addLog( "transaction received: not double and txo not 0" );
			context.setRc( RLE.ERR_INCONSISTENT );
			streamClose( tx );
			return context.getRc();
		}
		
		// === received tx is not in conflict nor doubled, we have to set unique date and store
		
		try { 
			
			// get atomic date in current domain parentKey
			entityUpdKey = getAtomicDate( parentKey, ds, dst, shardedInterval, txo );
			if( entityUpdKey == 0 ) throw new Error( "Atomic date error" );
			//System.out.println( "getAtomicDate new txo " + txo.value ); 
			
			// === split received transaction into GAE entities of 1 MB max
			// === can only receive one transaction by call
			
			// update txo (= atomic order) inside bytes as long
			int at = MdbTxReal.TXO_P + 9;	// TXO offset + 9 (1+4+4)
			BytesOut.writeLong( txBytes, at, txo.value );	
			
			// update transUpdKey (= atomic date) inside bytes as double
			long dl = Double.doubleToLongBits( entityUpdKey );
			at = MdbTxReal.UPDDATE_P + 9;	// UPDDATE offset + 9 (1+4+4)
			BytesOut.writeLong( txBytes, at, dl );	
			
			// reset before split received bytes
			tx.reset();
			long transIndex = 0;
			
			// split received bytes into chunks
			txUpd = entityUpdKey;	// transaction date
			while( tx.available() > 0 ) {
				
				// remain to write
				int remain = tx.available() > TRANS_CHUNK ? TRANS_CHUNK : tx.available();
				
				// set GAE entity unique key
				Entity gaeEntity = new Entity( "t", entityUpdKey, parentKey );
				
				// increment key milliseconds for next chunk
				entityUpdKey++;
				
				// bytes to store in entity
				byte[] chunkBytes = new byte[ remain ];
				tx.read( chunkBytes, 0, remain );
				
				// set transaction entity properties (last date is in key)
				gaeEntity.setUnindexedProperty( "tindex", transIndex++ );
				gaeEntity.setUnindexedProperty( "tindexmax", shardedInterval );
				gaeEntity.setUnindexedProperty( "txo", txo.value );
				gaeEntity.setUnindexedProperty( "tid", txId );
				gaeEntity.setUnindexedProperty( "tblob", new Blob( chunkBytes ) );
				
				// store GAE entity
				ds.put( dst, gaeEntity );
			}
			
			// end loop, set last used date (will be returned)
			entityUpdKey--;
		} 
		catch ( IOException e ) {
			dst.rollback();
			context.addLog( "decoding transaction error " + e.getMessage() );
			context.setRc( RLE.ERR_INCONSISTENT );
			return context.getRc();
		}
		catch( Error e ) {
			dst.rollback();
			context.addLog( e.getMessage() );
			context.setRc( RLE.ERR_INCONSISTENT );
			return context.getRc();
		}
		
		// transaction global report
		context.addLog( "def " + def + " length " + dlen + " bytesAvailable " + bytesAvailable );
		context.addLog( "trans ttype " + txType + " clen " + clen + " uid " + txUid );
		
		// check datastore commit, can have too much contention
		try { dst.commit(); }
		catch ( Error e ) {
			context.addLog( "commit received transaction error " + e.getMessage() );
			context.setRc( RLE.ERR_DBERROR );
			return context.getRc();
		}
		catch ( ConcurrentModificationException e ) {
			context.addLog( "commit received transaction ConcurrentModificationException " + e.getMessage() );
			context.setRc( RLE.ERR_DBOVERLOAD );
			return context.getRc();
		}
		catch ( IllegalStateException e ) {
			context.addLog( "commit received transaction IllegalStateException " + e.getMessage() );
			context.setRc( RLE.ERR_DBERRSTATE );
			return context.getRc();
		}
		catch ( DatastoreFailureException e ) {
			context.addLog( "commit received transaction DatastoreFailureException " + e.getMessage() );
			context.setRc( RLE.ERR_DBERRFAIL );
			return context.getRc();
		}
		
		// === transaction return code OK
		context.setRc( RLE.OK );
		
		// === if transaction concerns a domain, deploy events, reset RC if error
		if( txHasDomain == 1 && readTransactionEvents( txBytes ) > 0 ) 
			context.setRc( RLE.ERR_EXECERROR );
		
		// synchronization well finished
		context.setTxo( txo.value );
		context.setLastUpd( entityUpdKey );
		
		// if received tx, push to pubsub broker (pusher)
		if( txBytes != null ) {
			txPublish( txBytes, txo, domainId );
		}
		
		return context.getRc();
	}
	
	/** Publish tx (used for received tx validated). Tx is encoded as Pusher "tx" event.
	 * PusherEvent has 3 properties (channel, event, data), data has 3 properties (event, key, data), 
	 * and 2 optional if fragmented (fcur, fmax).
	 * <li> event.channel: "D" + domainId explained. Type is String. </li>
	 * <li> event.event: "tx". Type is String. </li>
	 * <li> event.data: object event in pusher event, allow fragmentation. Type is json object. </li>
	 * <li> event.data.key: unique event key, "tx" use txo (long). Key type depends on event.event. </li>
	 * <li> event.data.fcur: current fragment index. Type is integer. </li>
	 * <li> event.data.fmax: number of fragments, present only on first fragment (fcur 0). Type is integer.</li>
	 * <li> event.data.data: event.event content, for "tx" = transaction. Type is Base64.</li>
	 * 
	 * TODO il faudrait peut être faire attention au temps de la transaction gae (dans l'espace autorisé ?) */
	private boolean txPublish( byte[] txBytes, LongAds txo, long domainId ) {
		
		long dt = System.currentTimeMillis();
		
		int chunkSzMax = 9000;										// 10 KB minus pusher and http overhead		
		String tx64 = Base64.encodeToString( txBytes, false );		// get tx coded as base64 string 
		int tx64len = tx64.length();								// tx64 length
		int fcur = 0;												// current fragment index
		int fmax = tx64len / chunkSzMax;							// number of fragments
		if( ( tx64len % chunkSzMax ) != 0 ) fmax++;					// adjust fmax if modulo not 0
		
		JSONObject json = new JSONObject();
		int chunkPos = 0;
		int chunkSz = 0;
		while( fcur < fmax ) {
			
			try {
				
				// compute size to write, set position
				chunkPos += chunkSz;
				chunkSz = tx64len - chunkPos;
				if( chunkSz > chunkSzMax ) chunkSz = chunkSzMax;
				
				// populate json fragment
				json.put( "key", txo.value );
				json.put( "fcur", fcur );
				if( fcur == 0 ) json.put( "fmax", fmax );
				else json.remove( "fmax" );
				json.put( "data", tx64.substring( chunkPos, ( chunkPos + chunkSz ) ) );
				
				// write json to pusher
				HTTPResponse hr = Pusher.triggerPush( "D" + MdbId.idExplained( domainId ), "tx", json.toString() );
				String msg = "channel: " + "D" + MdbId.idExplained( domainId ) + ", event:" + "tx"
						+ ", key: " + txo.value + ", fcur: " + fcur + ( fcur == 0 ? ", fmax: " + fmax : "" ) 
						+ ", tx64len: " + tx64len + ", chunkSz: " + chunkSz + ", httpcode: " + hr.getResponseCode();
				context.addLog( msg, ( System.currentTimeMillis() - dt ) );
				if( context.isGaeSdk() ) System.out.println( msg + ", data: " + json.toString() );
				
				// stop if rc != 202
				if( hr.getResponseCode() != 202 )
					return false;
				
			} catch (JSONException e) {
				context.addRcLog(RLE.ERR_INTERNAL, e.toString() );
				return false;
			}
			
			// increment fcur
			fcur++;
		}
		
		dt = System.currentTimeMillis() - dt;
		
		return true;
	}
	
	/** Create a transaction domain for new created domain (TRANS-OPEN, EVENT(INSERT(new domain)), TRANS-CLOSE).
	 * @throws IOException */
	private byte[] getDomainTransaction( VODomain voDomain ) throws IOException {
		
		// out stream
		
		ByteArrayOutputStream outBytes = new ByteArrayOutputStream( 256 );
		AMF3Serializer out = new AMF3Serializer( outBytes );
		
		// buffers
		
		MdbTxRealBuf txBuf = new MdbTxRealBuf();
		MdbTxEventBuf txEventBuf = new MdbTxEventBuf();
		
		ByteArrayOutputStream txBytes = new ByteArrayOutputStream( 256 );
		AMF3Serializer tx = new AMF3Serializer( txBytes );
		
		ByteArrayOutputStream txEventBytes = new ByteArrayOutputStream( 256 );
		AMF3Serializer txEvent = new AMF3Serializer( txEventBytes );
		
		// === write DEF_TOPEN to out (transaction open)
		
		// prepare new transaction in _transBytes (transaction buffer)
		txBuf.initialize( 1, TXRISO.REP, context.userId );
		int dlen = txBuf.writeBytes( tx );
		int clen = 0;
		
		// --- header, append EOS
		out.writeByte( OPEN_TX );	
		out.writeInt( dlen );
		out.writeInt( clen );
		
		// --- values
		out.write( txBytes.toByteArray() );
		
		// --- trailer
		out.writeByte( END_DATA );
		out.writeInt( dlen );
		
		// set cur transaction length
		clen = dlen + 14; // 9 OPEN_TX + 5 END_DATA 
		
		// === write DEF_EVENT to out (event)
		
		// prepare new event _eventBytes (event buffer), with vo last event
		txEventBuf.initialize( MdbTxEvent.INSERT, voDomain, 0 );
		int eventLength = txEventBuf.writeBytes( txEvent );
			
		// update the vo with current position (= DEF_EVENT), append EOS
		// ... vo._lastEvent not managed by server
		
		// --- header 
		out.writeByte( OPEN_EVENT );
		out.writeInt( eventLength );	// real length
		
		// --- values
		out.write( txEventBytes.toByteArray() );
		
		// --- trailer
		out.writeByte( END_DATA );
		out.writeInt( eventLength );
		
		// update transaction length
		clen += eventLength + 10; // 5 OPEN_EVENT + 5 END_DATA
		
		// === write DEF_TCLOS to out (transaction close)
		
		// set cur transaction.ttype in journal (store hasDomain)
		// ... not mandatory, because transBuf.initialize set always hasDomain true
		
		// set cur transaction.closed in journal
		// ... done after get out bytes
		
		// write transaction close
		out.writeByte( CLOSE_TX );
		out.writeInt( clen );
		
		// bytes to return
		byte[] bytes = outBytes.toByteArray();
		
		// set transaction.closed on byte[] (after get bytes, because not know how do this otherwise)
		int at = 5;	// OPEN_TX + dlen = clen position
		BytesOut.writeInteger( bytes, at, clen );
		
		// close out
		// TODO verifier que bytes reste ok
		streamClose( out );
		
		return bytes;
	}
	
	/** Read transaction bytes for events, handle domain events. Return 0 if succeed, 
	 * else number of errors (errors are logged into context). */
	@SuppressWarnings("unused")
	private int readTransactionEvents( byte[] bytes ) {
		
		AMF3Deserializer in = new AMF3Deserializer( new ByteArrayInputStream( bytes ) );
		int errCount = 0;
		int skip = 0;
		
		MdbTxEvent event = new MdbTxEvent();
		
		try {
			while( true ) {
				
				// skip to next DEF if asked
				if( skip > 0 ) {
					in.skip( skip );
					skip = 0;
				}
				
				// exit at end
				if( in.available() == 0 ) return errCount;
				
				// read DEF and length
				int dlen;
				int clen;
				int def = in.readByte();
				
				switch( def ) {
					
					case END_DATA:
						dlen = in.readInt();
						continue;
						
					case OPEN_EVENT:
						dlen = in.readInt();
						event.readBytes( in );
						if( event.vo instanceof VODomain ) errCount += domainDeploy( event );
						continue;
					
					case OPEN_NEST:
						continue;
						
					case OPEN_TX:
						dlen = in.readInt();
						clen = in.readInt();
						skip = dlen;
//						trans.readBytes( in );	no need to read transaction, speed up
						continue;
						
					case CLOSE_TX:
						dlen = in.readInt();
						continue;
						
					default:
						context.addLog( "unknow DEF: " + def + ". Stop parsing." );
						errCount += 1;
						return errCount;
				}
			}
		} 
		catch (IOException e) {
			context.addLog( "decoding error: " + e.getMessage() );
			errCount += 1;
		}
		return errCount;
	}
	
	/** Apply an event to deployed entities (domains): create or update domain entity. Return 0 if succeed, 
	 * else 1 (errors are logged into context). */
	private int domainDeploy( MdbTxEvent event ) {
		
		int errCount = 0;
		
		if( ! ( event.vo instanceof VODomain ) ) {
			context.addLog( "event.vo is not VODomain." );
			return errCount += 1;
		}
			 
		VODomain domain = ( VODomain ) event.vo;
		
		switch( event.dbStmnt ) {
		
			case MdbTxEvent.INSERT:
				try { domainStore( domain ); }
				catch( VOStoreException e ) { 
					context.addLog( "INSERT domain error: " + e.getMessage() ); 
					errCount += 1;
				}
				break;
				
			case MdbTxEvent.UPDATE:
				try { domainStore( domain ); }
				catch( VOStoreException e ) { 
					context.addLog( "UPDATE domain error: " + e.getMessage() ); 
					errCount += 1;
				}
				break;
				
			case MdbTxEvent.REMOVE:
				context.addLog( "cannot REMOVE a domain (see revoke ?)." );
				errCount += 1;
				break;
				
			case MdbTxEvent.MOVEIN:
				context.addLog( "cannot MOVEIN a domain." );
				errCount += 1;
				break;
				
			default:
				context.addLog( "unknow event.etype: " + event.dbStmnt + "." );
				errCount += 1;
				break;
		}
		
		return errCount;
	}
	
	// store domain (can only be handled by an event)
	
	/** Store domain (insert or update, use domain store). Usable only for received domain from sync
	 * (do not confuse with getDomainTransaction, that create a transaction inserted in sync process, 
	 * dedicated to server initiatives).
	 * @throws VOStoreException */
	private VODomain domainStore( VODomain domain ) throws VOStoreException {
		long begTime = System.currentTimeMillis();
		
		if( domain == null ) return null;

		// Store the new domain, without domain.parent control. This can create a domain
		// that references a non existing parent - until the parent will be created.
		
		domain.store();
		context.addLog( domain.name, begTime );
		return domain;
	}
	
	// get last atomic date
	
	/** Return new atomic date in current domain parentKey. If not possible, return 0 (a valid value cannot be 0),
	 * but do NOT rollback dst. The given value of LongAds txo (last tx sequence order), is not used, but returns 
	 * always the new sequence value. */
	private long getAtomicDate( Key parentKey, DatastoreService ds, Transaction dst, int shardedInterval, LongAds txo ) {
		
		// get atomic date in current domain parentKey
		long begTime = System.currentTimeMillis();
		long jvmTime = begTime;
		long gaeLastTime;
		long gaeTxo;
		
		try {
			Key key = KeyFactory.createKey( parentKey, "A", "ud" );
			
			// get (domain parentKey, kind "A", key "ud", prop "d") if exist
			Entity gaeEntity = null;
			try { gaeEntity = ds.get( dst, key ); }
			catch( EntityNotFoundException e ) {}
			
			// if gaeLastTime >= JVM, increment JVM time
			if( gaeEntity == null ) {
				gaeEntity = new Entity( key );
				txo.value = 0L;
			}
			else {
				gaeLastTime = (Long) gaeEntity.getProperty( "a" );
				if( gaeLastTime >= jvmTime ) jvmTime = gaeLastTime + 1;
				txo.value = (Long) gaeEntity.getProperty( "b" );
			}
			
			// add shardedInterval to gaeLastTime (for next use)
			gaeLastTime = jvmTime + shardedInterval; 
			gaeTxo = ++txo.value;	// first value is 1
						
			// store new last date that is ALWAYS greater than previous, and txo
			gaeEntity.setUnindexedProperty( "a", gaeLastTime );
			gaeEntity.setUnindexedProperty( "b", gaeTxo );
			ds.put( dst, gaeEntity );	
		} 
		catch( Error e) { 
			context.addLog( "domainSyncRead: get atomic date failed: " + e, begTime );				
			context.setRc( RLE.ERR_INCONSISTENT );
			return 0;
		}
		
		context.addLog( "Unique Date updated (" + 
			"jvmTime " + Misc.getDateFormatHMSS().format( new Date( jvmTime ) ) + 
			", gaeLastTime " + Misc.getDateFormatHMSS().format( new Date( gaeLastTime ) ) +
			", shardedInterval " + shardedInterval + 
			", txo " + txo.value + ")", begTime );
		
		return jvmTime;
	}
	
	// ===== services
	
	// close AMF serializer
	
	/** Close a stream (AMF3Deserializer, AMF3Serializer). Return without error even if close error. */
	private void streamClose( Object o ) {
		try { 
			if( o instanceof AMF3Deserializer ) ( (AMF3Deserializer) o ).close(); 
			else if( o instanceof AMF3Serializer ) ( (AMF3Serializer) o ).close(); 
			else throw new Error( "Given object not recognized" );
		} catch ( IOException txe ) {}
	}
	
}
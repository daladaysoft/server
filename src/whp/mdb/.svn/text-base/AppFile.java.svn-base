package whp.mdb;

import java.io.IOException;
import java.nio.ByteBuffer;

import whp.gae.RLE;
import whp.tls.Context;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileReadChannel;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;

public class AppFile 
{	
	// ===== constants & static variable
	
	// ===== serialized variable
	// ===== transient variables
	
	private Context context;
	
	// ===== instance initializer	
	// ===== constructor
	
	public AppFile( Context context ) {
		this.context = context;
	}
	
	// ===== PACKAGE, FILES SERVICES
	
	// ===== PUBLIC, FILES SERVICES
	
	/** Store the given data as file. The given name is stored as meta data with the file.
	 * Return the blobstore id (filePath), or null if the file is not stored. 
	 * Log error if any in context. */
	public String fileStore( String name, int size, byte[] data ) {
		
		long begTime = System.currentTimeMillis();
		
		// Get a file service
		FileService fileService = FileServiceFactory.getFileService();
		
		// Create a new Blob file
		AppEngineFile file;
		try {
			file = fileService.createNewBlobFile( "application/x-shockwave-flash", name );
		} catch (IOException e) {
			context.setRc( RLE.ERR_REFUSED );
			context.addLog( "fileStore " + name + " not stored: createNewBlobFile: " + e.getMessage() );
			return null;
		}
		
		// Open a channel to write to it
		FileWriteChannel writeChannel;
		boolean lock = true;
		try {
			writeChannel = fileService.openWriteChannel( file, lock );
		} catch ( Exception e) {	// FileNotFoundException, FinalizationException, LockException, IOException
			context.setRc( RLE.ERR_REFUSED );
			context.addLog( "fileStore " + name + " not stored: openWriteChannel: " + e.getMessage() );
			return null;
		}
		
		// Write on file channel
		try {
			writeChannel.write( ByteBuffer.wrap( data, 0, size ) );
		} catch (IOException e) {
			context.setRc( RLE.ERR_REFUSED );
			context.addLog( "fileStore " + name + " not stored: write: " + e.getMessage() );
			return null;
		}
		
		// Now finalize
		try {
			writeChannel.closeFinally();
		} catch ( Exception e) {	// IllegalStateException, IOException
			context.setRc( RLE.ERR_REFUSED );
			context.addLog( "fileStore " + name + " not stored: closeFinally: " + e.getMessage() );
			return null;
		}
		
		// get file path
		String filePath = file.getFullPath(); 
		
		// log
		context.addLog( "fileStore " + name + " done in ", begTime );
		
		return filePath;
	}
	
	/** Read the given blob filePath. Return data byte array if successful, else return null. */
	public byte[] fileRead( String name, String filePath, int size ) {
		
		long begTime = System.currentTimeMillis(); 
		
		// Get a file service
		FileService fileService = FileServiceFactory.getFileService();
		
		// get app engine file from filePath
		AppEngineFile file = new AppEngineFile( filePath ); 
		if( ! file.isReadable() ) {
			context.setRc( RLE.ERR_REFUSED );
			context.addLog( "fileRead, " + name + " not readable." );
			return null;
		}
		
		// set data buffer
		ByteBuffer data = ByteBuffer.allocate( size );
		
		// open channel and read data
		FileReadChannel readChannel;
		try {
			readChannel = fileService.openReadChannel( file, false );
			readChannel.read( data );
		} catch ( Exception e) {
			context.setRc( RLE.ERR_NOTFOUND );
			context.addLog( "fileRead " + name + " not read: " + e.getMessage() );
			return null;
		} 
		
		context.addLog( "fileRead " + name + ", size " + size + ", read "+ data.position() + " in ", begTime );
		
		return data.array();
	}
	
	/** Delete blob file 
	 * Some problems discovered with delete blob (__BlobFileIndex__ not deleted on SDK 1.7.1, see
	 * http://code.google.com/p/googleappengine/issues/detail?id=6849, not tested in production) */
	public void fileDelete( String filePath ) { fileDelete( filePath, "unknown" ); }
	public void fileDelete( String filePath, String name ) {
		
		FileService fileService = FileServiceFactory.getFileService();
		
		// get app engine file from filePath, then blobKey from filePath
		AppEngineFile file = new AppEngineFile( filePath );
		BlobKey blobKey = fileService.getBlobKey( file );
		if( blobKey == null ) {
			context.addLog( "fileDelete " + name + " not deleted (blobKey null)" );
			return;
		}
		
		// delete the blob file
		BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
	    blobstoreService.delete( blobKey );
	}

}

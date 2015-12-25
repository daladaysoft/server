package ads.type;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/** BytesOut, is byte[] class used to return bytes from java to as3, helping as3 to embed 
 * byte[] in a class that can use register class to map BytesOut.java to Bytes.as. 
 * Expose some static services on byte[] */
public class BytesOut extends ByteArrayOutputStream implements Externalizable 
{
	
	// ===== interface Externalizable
	
	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
		throw new IOException( "Cannot be used to read." );
	}
	
	public void writeExternal( ObjectOutput out ) throws IOException {
		out.writeInt( count );
		out.write( buf, 0, count );
	}
	
	// ===== static services
	
	/** Write int value (4 bytes) at position in bytes. */
	public static void writeInteger( byte[] bytes, int at, int value ) {
		bytes[ at ] 	= (byte) ( value >>> 24 );
		bytes[ at + 1 ] = (byte) ( value >>> 16 );
		bytes[ at + 2 ] = (byte) ( value >>> 8 );
		bytes[ at + 3 ] = (byte) ( value >>> 0 );
	}
	
	/** Write long value (8 bytes) at position in bytes. */
	public static void writeLong( byte[] bytes, int at, long value ) {
		bytes[ at ] 	= (byte) ( value >>> 56 );
		bytes[ at + 1 ] = (byte) ( value >>> 48 );
		bytes[ at + 2 ] = (byte) ( value >>> 40 );
		bytes[ at + 3 ] = (byte) ( value >>> 32 );
		bytes[ at + 4 ] = (byte) ( value >>> 24 );
		bytes[ at + 5 ] = (byte) ( value >>> 16 );
		bytes[ at + 6 ] = (byte) ( value >>>  8 );
		bytes[ at + 7 ] = (byte) ( value >>>  0 );
	}
}

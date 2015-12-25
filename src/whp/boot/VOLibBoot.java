package whp.boot;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;

/** An SWF or SWC initial library. Does not contains the code (stored in blobstore under fileName). 
 * Does not extends VOBase (managed by AppLib). */
public class VOLibBoot implements Externalizable 
{
	// ===== constants & static variable
	
	private static final long serialVersionUID = 1L;
	
	// ===== serialized variable
	
	public	double	 		cversion;
	
	public	String			name;			// simple project name (does not contains .swc or .swf)
	public	String			platform;		// AIR, BWR, CSX (signature swf) or LIB (signature swc)
	public	String			signature;		// swc, swf
	public	String			mode;			// rel, dbg
	public	String			version;		// version-release-build suggested

	public 	Date 			upd;			// received (unique) date
	public	String			desc;			// short description
	
	private String 			fileName;		// file name (name_platform_signature_mode_version)
	public 	int				fileSize;		// file data size
	
	// ===== transient variables	
	// ===== constructor
	
	/** Default constructor. */
	public VOLibBoot() {}	
	
	// ===== specific to this class
	
	/** Return the prefix name (without version). */
	public String getNamePrefix() { return name + "_" + platform + "_" + signature + "_" + mode + "_"; }
	
	/** Return the full name (that is also the file name): name, platform, signature, mode, version.
	 * <li> WhpInddAutocatExt_CSX_swf_rel_1-a-656 for "WhpInddAutocatExt", "CSX", "swf", "release", "1-a-656". </li> */
	public String getNameFull() { return getNamePrefix() + version; }
	
	public String getFileName() {
		if( fileName == null || fileName.equals( "" ) ) fileName = getNameFull();
		return fileName;
	}
	public void setFileName( String v ) { fileName = v; }
	
	
	// ===== interface Externalizable

	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
		
		cversion = in.readDouble();
		if( Double.isNaN( cversion ) ) cversion = 0;
		
		name 						= in.readUTF();
		platform 					= in.readUTF();
		signature 					= in.readUTF();
		mode 						= in.readUTF();
		version 					= in.readUTF();
		
		upd 						= (Date) in.readObject();
		desc 						= in.readUTF();
		
		fileName 					= in.readUTF();
		fileSize 					= in.readInt();
	}
	
	public void writeExternal( ObjectOutput out ) throws IOException {
		
		out.writeDouble( cversion );
		
		out.writeUTF( name );
		out.writeUTF( platform );
		out.writeUTF( signature );
		out.writeUTF( mode );
		out.writeUTF( version );
		
		out.writeObject( upd );
		out.writeUTF( desc );
		
		out.writeUTF( fileName );
		out.writeInt( fileSize );
	}
}

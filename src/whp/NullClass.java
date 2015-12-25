package whp;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/** Used as null object in externalize operations. */
public class NullClass implements Externalizable 
{
	public static final NullClass nullObject = new NullClass();
	public NullClass() {}	
	public void readExternal( ObjectInput in ) {}
	public void writeExternal( ObjectOutput out ) {}
}

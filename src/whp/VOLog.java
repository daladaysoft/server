package whp;

import java.io.Serializable;

import whp.util.Misc;


public class VOLog implements Serializable //Externalizable
{
	private static final long serialVersionUID = 1L;
	
	public	double	 	version;
	public	Long	 	id;
	
	public	String		className;
	public	String		desc;
	public	long		level;

	public VOLog() {}
	
	public VOLog( String desc ) {
		
		this.version = 1;
		this.id = System.currentTimeMillis();
		
		this.className = Misc.getCallerInfoAll( 2 );	// 2 return VOLog caller
		this.desc = desc;
		this.level = 0;
	}
	public VOLog( String desc, int depth ) {
		
		this.version = 1;
		this.id = System.currentTimeMillis();
		
		this.className = Misc.getCallerInfoAll( depth );
		this.desc = desc;
		this.level = 0;
		
	}
	
	// NOT STORED FOR NOW
	
//	public static VOLog gaeTojava( Entity gaeEntity ) {
//		
//		VOLog voLog 	= new VOLog();
//		
//		voLog.version 	= (Double) gaeEntity.getProperty("version");
//		voLog.id 		= gaeEntity.getKey().getId();
//		
//		voLog.className = (String) gaeEntity.getProperty("className");
//		voLog.desc 		= (String) gaeEntity.getProperty("desc");
//		voLog.level 	= (Long) gaeEntity.getProperty("level");
//		
//		return voLog;
//	}
//	
//	public Entity javaToGae() {
//		
//		Key key = KeyFactory.createKey( gaeKeyKind(), id );
//		Entity gaeEntity = new Entity( key );
//		
//		gaeEntity.setProperty( "version", version );
//		
//		gaeEntity.setUnindexedProperty( "className", className );
//		gaeEntity.setUnindexedProperty( "desc", desc );
//		gaeEntity.setUnindexedProperty( "level", level );
//		
//		return gaeEntity;
//	}
	
	// ===== store / unstore
	

	/**
	 * Pas réussi a utiliser externalizable !!
	 * donc j'ai laissé la sérialisation automatique prendre le relais.
	 * Elle est certainement un peu plus couteuse, mais fonctionne bien.
	 */
	// ===== interface Externalizable
	
//	@Override
//	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {}
//	
//	@Override
//	public void writeExternal( ObjectOutput out ) throws IOException {
//		
//		out.writeDouble( version );
//		out.writeUTF( id );	// attention, chang� en long (date.getTime)
//		out.writeUTF( className );
//		out.writeUTF( desc );
//		out.writeInt( (int) level );
//		
//	}

}

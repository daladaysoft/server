package whp.mdb;

import java.text.ParseException;

import whp.util.Hex;

/** A structure that store the key of any WHP VO. */
public class VOKey {
	
	public long			domainId;	// datastore ancestor (0 no ancestor, or domain id)
	public String		tableKind;	// datastore kind (tableId hex, or string kind)
	public long			id;			// datastore id (only ads id)
	
	/** Return true if tableKind is an id, else false (null or string kind). */
	public boolean isTableKindAnId() { return( tableKind != null && tableKind.length() == 16 ); }
	
	/** Return tableId as long if tableKind is not a kind string, else 0. */
	public long getTableId() { 
		try { return( isTableKindAnId() ? Hex.hexToLong( tableKind ) : 0 ); } 
		catch (ParseException e) { return 0; }
	}
	
	/** Return kind as string if tableKind is a kind string, else null. */
	public String getKind() { 
		return( isTableKindAnId() ? null : tableKind );
	}
	
	public String getInfo() {
		String kind = isTableKindAnId() ?  MdbId.idExplained( getTableId() ) : tableKind;
		return "VOKey (domainId " + MdbId.idExplained( domainId ) + ", tableId/kind " + kind + ", id " + MdbId.idExplained( id ) + ")";
	}
	
}

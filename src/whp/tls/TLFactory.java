package whp.tls;

/** Create Thread Local objects. For future.*/
public class TLFactory {

	// ===== Context 
	
	private static class TLContext extends ThreadLocal< Context > {
		public TLContext() {}
		public Context initialValue() { 
			System.out.println( "new TLS Context" ); 
			return new Context( "TLFactory" ); 
		}
	}
	
	private static TLContext tlContext = new TLContext();
	
	/** Return context data for a request, is thread local (warning, can
	 * be a new instanced, or can reuse old context, depending on pool 
	 * thread management provided by J2EE, see context.state).  */
	public static final Context getContext() { return tlContext.get(); }
	
	/** Remove from thread local variable pool */
	public static final void removeContext() { tlContext.remove(); }
	
	// ===== another in any ...

}

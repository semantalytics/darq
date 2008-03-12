package com.hp.hpl.jena.query.darq.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.query.expr.Expr;

public class MapMultipleServiceGroup extends MapServiceGroup {
	
	 Set<RemoteService> services = new HashSet<RemoteService>();  

	public MapMultipleServiceGroup() {
		super(null);
	}
	 
	public MapMultipleServiceGroup(RemoteService s) {
	        super(null);
	        services.add(s);
	    }
	    
	    public void addService(RemoteService s) {
	        services.add(s);
	    }

	    /* (non-Javadoc)
	     * @see com.hp.hpl.jena.query.federated.core.ServiceGroup#getService()
	     */
	    @Override
	    @Deprecated
	    public RemoteService getService() {
	        throw new UnsupportedOperationException("this is a MapMultipleServiceGroup - use getServices");
	    }

	    public Set<RemoteService> getServices() {
	        return services;
	    }
	    
	    /*
	     * erstellt ne neue SG mit Triplen und Filtern aus der aktuellen
	     * MSG  
	     */
	    public MapServiceGroup getServiceGroup(RemoteService s) {
	        
	    	if (!services.contains(s)) return null;
	       
	        MapServiceGroup sg = new MapServiceGroup(s);
	        sg.setTriple(this.getTriple());
	        sg.setFilters(this.getFilters());
	        return sg;
	    }

	    /* (non-Javadoc)
	     * @see com.hp.hpl.jena.query.federated.core.ServiceGroup#clone()
	     */
	    @Override
	    public MapMultipleServiceGroup clone() {
	        MapMultipleServiceGroup sg = new MapMultipleServiceGroup();
	        sg.setTriple(this.getTriple());
	        sg.setFilters(new ArrayList<Expr>(this.getFilters()));
	        sg.services= new HashSet<RemoteService>(this.services);
	        return sg;
	    }

	    /* (non-Javadoc)
	     * @see com.hp.hpl.jena.query.federated.core.ServiceGroup#equals(java.lang.Object)
	     */
	    @Override
	    public boolean equals(Object obj) {
	        if (obj instanceof MapMultipleServiceGroup) {
	            MapMultipleServiceGroup otherGroup = (MapMultipleServiceGroup) obj;
	            if (getServices().equals(otherGroup.getServices()) && getTriple().equals(otherGroup.getTriple()) && getFilters().equals(otherGroup.getFilters()) ) return true;
	        } 
	        return false;
	    }
	    
	    /* (non-Javadoc)
	     * @see java.lang.Object#hashCode()
	     */
	    @Override
	    public int hashCode() {
	        return services.hashCode() ^ getTriple().hashCode() ^ getFilters().hashCode();
	    }
	    

}

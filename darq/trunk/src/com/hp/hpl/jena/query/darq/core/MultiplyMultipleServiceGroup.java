package com.hp.hpl.jena.query.darq.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.expr.Expr;

import de.hu_berlin.informatik.wbi.darq.mapping.Rule;

public class MultiplyMultipleServiceGroup extends MultiplyServiceGroup {

	Set<RemoteService> services = new HashSet<RemoteService>();
	
	public MultiplyMultipleServiceGroup(RemoteService s, Set<Rule> subjectRules, Set<Rule> predicateRules, Set<Rule> objectRules) {
		super(null, subjectRules, predicateRules, objectRules);
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
	        throw new UnsupportedOperationException("this is a MultiplyMultipleServiceGroup - use getServices");
	    }

	    public Set<RemoteService> getServices() {
	        return services;
	    }
	    
	    public MultiplyServiceGroup getServiceGroup(RemoteService s) {
	        if (!services.contains(s)) return null;
	        
	        MultiplyServiceGroup muSG = new MultiplyServiceGroup(s,subjectRules,predicateRules,objectRules);
	        muSG.setTriples(this.getTriples());
	        muSG.setFilters(this.getFilters());
	        muSG.muTriples = new HashMap<Triple, Triple>(muTriples);

	        return muSG;
	    }

	    /* (non-Javadoc)
	     * @see com.hp.hpl.jena.query.federated.core.ServiceGroup#clone()
	     */
	    @Override
	    public MultiplyMultipleServiceGroup clone() {
	        MultiplyMultipleServiceGroup multiplyMSG = new MultiplyMultipleServiceGroup(null,subjectRules,predicateRules,objectRules);
	        multiplyMSG.setTriples(this.getTriples());
	        multiplyMSG.setFilters(new ArrayList<Expr>(this.getFilters()));
	        multiplyMSG.services= new HashSet<RemoteService>(this.services);
	        multiplyMSG.muTriples = new HashMap<Triple, Triple>(muTriples);
	        return multiplyMSG;
	    }

	    /* (non-Javadoc)
	     * @see com.hp.hpl.jena.query.federated.core.ServiceGroup#equals(java.lang.Object)
	     */
	    @Override
	    public boolean equals(Object obj) {
	        if (obj instanceof MultiplyMultipleServiceGroup) {
	            MultiplyMultipleServiceGroup otherGroup = (MultiplyMultipleServiceGroup) obj;
	            if (getServices().equals(otherGroup.getServices()) && 
	            	getTriples().equals(otherGroup.getTriples()) && 
	            	getFilters().equals(otherGroup.getFilters()) &&
	            	subjectRules.equals(otherGroup.subjectRules)&&
	            	predicateRules.equals(otherGroup.predicateRules)&&
	            	objectRules.equals(otherGroup.objectRules)&&
	            	muTriples.equals(otherGroup.muTriples))
	            	return true;
	        } 
	        return false;
	    }
	    
	    /* (non-Javadoc)
	     * @see java.lang.Object#hashCode()
	     */
	    @Override
	    public int hashCode() {
	        return services.hashCode() ^ getTriples().hashCode() ^ getFilters().hashCode() ^  muTriples.hashCode()^subjectRules.hashCode()
                     ^ predicateRules.hashCode()^objectRules.hashCode();
	   
	    }
}

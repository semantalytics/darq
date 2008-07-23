package com.hp.hpl.jena.query.darq.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.expr.Expr;

import de.hu_berlin.informatik.wbi.darq.mapping.Rule;

public class StringConcatMultipleServiceGroup extends StringConcatServiceGroup{

	Set<RemoteService> services = new HashSet<RemoteService>();
	
	public StringConcatMultipleServiceGroup(RemoteService s, Set<Rule> subjectRules, Set<Rule> predicateRules, Set<Rule> objectRules) {
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
	        throw new UnsupportedOperationException("this is a StringConcatMultipleServiceGroup - use getServices");
	    }

	    public Set<RemoteService> getServices() {
	        return services;
	    }
	    
	    public StringConcatServiceGroup getServiceGroup(RemoteService s) {
	        if (!services.contains(s)) return null;
	        
	        StringConcatServiceGroup scSG = new StringConcatServiceGroup(s,subjectRules,predicateRules,objectRules);
	        scSG.setTriples(this.getTriples());
	        scSG.setFilters(this.getFilters());
	        scSG.scTriples = new HashMap<Triple, Triple>(scTriples);
	        scSG.concat = this.concat;
	        scSG.tripleInHead =this.tripleInHead;
	        scSG.concat = this.concat;
	        scSG.usedVariables = this.usedVariables;
	        scSG.variablesOrderedByRule = this.variablesOrderedByRule;
	        return scSG;
	    }

	    /* (non-Javadoc)
	     * @see com.hp.hpl.jena.query.federated.core.ServiceGroup#clone()
	     */
	    @Override
	    public StringConcatMultipleServiceGroup clone() {
	        StringConcatMultipleServiceGroup scMSG = new StringConcatMultipleServiceGroup(null,subjectRules,predicateRules,objectRules);
	        scMSG.setTriples(this.getTriples());
	        scMSG.setFilters(new ArrayList<Expr>(this.getFilters()));
	        scMSG.services= new HashSet<RemoteService>(this.services);
	        scMSG.scTriples = new HashMap<Triple, Triple>(scTriples);
	        scMSG.concat = this.concat;
	        scMSG.variablesOrderedByRule = new HashMap<Integer, String>(variablesOrderedByRule);
	        scMSG.tripleInHead = this.tripleInHead;
	        return scMSG;
	    }

	    /* (non-Javadoc)
	     * @see com.hp.hpl.jena.query.federated.core.ServiceGroup#equals(java.lang.Object)
	     */
	    @Override
	    public boolean equals(Object obj) {
	        if (obj instanceof StringConcatMultipleServiceGroup) {
	            StringConcatMultipleServiceGroup otherGroup = (StringConcatMultipleServiceGroup) obj;
	            if (getServices().equals(otherGroup.getServices()) && 
	            	getTriples().equals(otherGroup.getTriples()) && 
	            	getFilters().equals(otherGroup.getFilters()) &&
	            	subjectRules.equals(otherGroup.subjectRules)&&
	            	predicateRules.equals(otherGroup.predicateRules)&&
	            	objectRules.equals(otherGroup.objectRules)&&
	            	scTriples.equals(otherGroup.scTriples)&&
	            	concat.equals(otherGroup.concat)&&
	            	tripleInHead.equals(otherGroup.tripleInHead) &&
	            	variablesOrderedByRule.equals(otherGroup.variablesOrderedByRule))
	            	return true;
	        } 
	        return false;
	    }
	    
	    /* (non-Javadoc)
	     * @see java.lang.Object#hashCode()
	     */
	    @Override
	    public int hashCode() {
	        return services.hashCode() ^ getTriples().hashCode() ^ getFilters().hashCode() ^  scTriples.hashCode()^subjectRules.hashCode()
                     ^ predicateRules.hashCode()^objectRules.hashCode() ^concat.hashCode()^concat.hashCode() ^ tripleInHead.hashCode() ^variablesOrderedByRule.hashCode();
	   
	    }
}

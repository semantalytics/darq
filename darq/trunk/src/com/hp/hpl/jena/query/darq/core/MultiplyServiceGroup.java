package com.hp.hpl.jena.query.darq.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.expr.Expr;

import de.hu_berlin.informatik.wbi.darq.mapping.Rule;

public class MultiplyServiceGroup extends ServiceGroup {
	Set<Rule> subjectRules = new HashSet<Rule>();
	Set<Rule> predicateRules = new HashSet<Rule>();
	Set<Rule> objectRules = new HashSet<Rule>();
	HashMap<Triple,Triple> muTriples = new HashMap<Triple,Triple>();
	
	public MultiplyServiceGroup(RemoteService s, Set<Rule> subjectRules, Set<Rule> predicateRules, Set<Rule> objectRules) {
		super(s);
		this.subjectRules = subjectRules;
		this.predicateRules = predicateRules;
		this.objectRules = objectRules;
	}

	public Set<Rule> getSubjectRules() {
		return subjectRules;
	}

	public Set<Rule> getPredicateRules() {
		return predicateRules;
	}

	public Set<Rule> getObjectRules() {
		return objectRules;
	}

	public void addSubjectRules(Set<Rule> subjectRules) {
		subjectRules.addAll(subjectRules);
	}
	
	public void addPredicateRules(Set<Rule> predicateRules) {
		predicateRules.addAll(predicateRules);
	}

	public void addObjectRules(Set<Rule> objectRules) {
		objectRules.addAll(objectRules);
	}
	
	public void addallRules(Set<Rule> subjectRules, Set<Rule> predicateRules, Set<Rule> objectRules) {
		subjectRules.addAll(subjectRules);
		predicateRules.addAll(predicateRules);
		objectRules.addAll(objectRules);
	}
	
	 public void addOrignalTriple(Triple triple, Triple originalTriple ){
		 muTriples.put(triple, originalTriple);
	 }
	 
	public Triple getOriginalTriple(Triple triple) {
		return muTriples.get(triple);
	}
	
	public HashMap<Triple,Triple> getMuTriples() {
		return muTriples;
	}
	
	public void setMuTriples(HashMap<Triple, Triple> muTriples) {
		this.muTriples = muTriples;
	}
	
    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public ServiceGroup clone()  {
        MultiplyServiceGroup sg = new MultiplyServiceGroup(service, subjectRules, predicateRules,objectRules);
        sg.triples=new ArrayList<Triple>(this.triples);
        sg.filters=new ArrayList<Expr>(this.filters);
        sg.usedVariables = new HashSet<String>(usedVariables);
        sg.muTriples = new HashMap<Triple, Triple>(muTriples);
        sg.subjectRules = new HashSet<Rule>(subjectRules);
        sg.predicateRules =  new HashSet<Rule>(predicateRules);
        sg.objectRules =  new HashSet<Rule>(objectRules);
        return sg;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MultiplyServiceGroup) {
            MultiplyServiceGroup otherGroup = (MultiplyServiceGroup) obj;
            if (service.equals(otherGroup.service) && 
            	triples.equals(otherGroup.triples) && 
            	filters.equals(otherGroup.filters) &&
            	subjectRules.equals(otherGroup.subjectRules)&&
            	predicateRules.equals(otherGroup.predicateRules) &&
            	objectRules.equals(otherGroup.objectRules)&&
            	muTriples.equals(otherGroup.muTriples)) return true;
        } 
        return false;
      
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
    	int hc = service.getUrl().hashCode() ^ triples.hashCode() ^ filters.hashCode() ^ muTriples.hashCode() ^subjectRules.hashCode() 
    	        ^predicateRules.hashCode() ^objectRules.hashCode();
    	if (service.getGraph()!=null){
    		hc= hc ^ service.getGraph().hashCode() ;
    	}
        return hc;
    }

	
}

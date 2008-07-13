/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.core;

import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import com.hp.hpl.jena.query.core.Var;
import com.hp.hpl.jena.query.expr.Expr;

public class ServiceGroup {

    
    protected List<Triple> triples = new ArrayList<Triple>();
    
    protected List<Expr> filters = new ArrayList<Expr>();

    protected RemoteService service;

    protected Set<String> usedVariables = new HashSet<String>();
    
    //private Set<String> predicatesWithBoundObjects = new HashSet<String>();

    public ServiceGroup(RemoteService s) {
        service = s;
    }
    
    public RemoteService getService() {
        return service;
    }

    public void addB(Triple t) {
        triples.add(0,t); // TODO: THIS IS BECAUSE THE EVALUATION ORDER OF THE COMPILER! SHOULD DO THIS SOMEHOW DIFFERENT! ?
        buildUsedVariabels(t);
        
            
    }
    
    private void addE(Triple t) {
            
            triples.add(t); // TODO: THIS IS BECAUSE THE EVALUATION ORDER OF THE COMPILER! SHOULD DO THIS SOMEHOW DIFFERENT! ?
            buildUsedVariabels(t);

    }
    
    private void buildUsedVariabels(Triple t) {
        if (t.getObject().isConcrete()) {
            // object is bound
            //   predicatesWithBoundObjects.add(t.getPredicate().getURI());
            
        } else {
            // it's a variable
            usedVariables.add(t.getObject().getName());
        }
        
        if(t.getSubject().isVariable()) {
            usedVariables.add(t.getSubject().getName());
        }
    }   
    
    public List<Triple> getTriples() {
        return triples;
    }
    
    public boolean containsTriple(Triple t){
    	if (triples.contains(t)) return true;
    	return false;
    }
    
    public void setTriples(List<Triple> list) {
        triples.clear();
        for (Triple t:list) {
            this.addE(t);
        }
        
    }
    
    
    
    public Set<Set<RequiredBinding>> requiredBindings() {
        return service.getRequiredBindings();
    }

    
    
    
    /**
     * Checks the required bindings 
     * @param boundVariables
     * @return  
     */
    public boolean checkInput(Set<String> boundVariables) {
     
        if (service.getRequiredBindings().size()==0) return true;
        
        Set<String> predicatesWithBoundObjects = new HashSet<String>();
        Set<String> predicatesWithBoundSubjects = new HashSet<String>();
        
        for (Triple t:triples) {
            Node o = t.getObject(); 
            Node s = t.getSubject();
            
            if (o.isConcrete() || (o.isVariable() && boundVariables.contains(o.getName()) )) predicatesWithBoundObjects.add(t.getPredicate().getURI());
            if (s.isConcrete() || (s.isVariable() && boundVariables.contains(s.getName()) )) predicatesWithBoundSubjects.add(t.getPredicate().getURI());
                
                    
        }
        
        for (Set<RequiredBinding> bs:service.getRequiredBindings()) {
            
            boolean tmpresult = true;
            
            for (RequiredBinding rb: bs) {
                
                switch (rb.getType()) {
                case RequiredBinding.OBJECT_BINDING:
                    
                    if (!predicatesWithBoundObjects.contains(rb.getPredicateString())) tmpresult = false;
                    
                    break;

                case RequiredBinding.SUBJECT_BINDING:
                    
                    if (!predicatesWithBoundSubjects.contains(rb.getPredicateString())) tmpresult = false;
                    
                    break;
                    
                default:
                    break;
                }
                
            
            if (tmpresult) return true;
        
            }
           
           }
       return false;
    }



    public Set<String> getUsedVariables() {
        return usedVariables;
    }




    /**
     * 
     * @param c  Filter to add
     * @return false if the variables in the filter are not a subset of the variables in the statements in this group. 
     */
    public boolean addFilter(Expr c) {
        
        if (filters.contains(c)) return true;
        
        Set<String> filtervars = new HashSet<String>();
        
        Set<Var> vars = new HashSet<Var>();
        c.varsMentioned(vars);
       
        for (Var v:vars) {
        	filtervars.add(v.toString().substring(1));
        }
       
        // is filtervars a subset of usedvars ?
        filtervars.removeAll(usedVariables); 
        if (filtervars.size()==0) {
            filters.add(c);
            return true;
        }
        
        return false;
        
    }
    
    public boolean addFilters(List<Expr> lc) {
    	boolean result=true;
    	for (Expr c:lc) result = result && addFilter(c);
    	return result;
    }
    
    
    public List<Expr> getFilters() {
        return filters;
    }
    
    /**
     * Sets list of filters
     * !! DOES NOT check if the variables in the filter are not a subset of the variables in the statements in this group. !! 
     * @param c list of filters
     */
    public void setFilters(List<Expr> c) {
        this.filters=c;
    }



    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public ServiceGroup clone()  {
        ServiceGroup sg = new ServiceGroup(service);
        sg.triples=new ArrayList<Triple>(this.triples);
        sg.filters=new ArrayList<Expr>(this.filters);
        sg.usedVariables = new HashSet<String>(usedVariables);
        return sg;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServiceGroup) {
            ServiceGroup otherGroup = (ServiceGroup) obj;
            if (service.equals(otherGroup.service) && triples.equals(otherGroup.triples) && filters.equals(otherGroup.filters) ) return true;

        } 
        return false;
      
    }



    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
    	int hc = service.getUrl().hashCode() ^ triples.hashCode() ^ filters.hashCode();
    	if (service.getGraph()!=null) hc= hc ^ service.getGraph().hashCode() ;
        return hc;
    }
    
   
  
}

/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
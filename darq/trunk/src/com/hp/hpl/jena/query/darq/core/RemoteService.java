/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.core;



import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.core.Var;
import com.hp.hpl.jena.query.darq.engine.optimizer.CostBasedFunction;
import com.hp.hpl.jena.query.darq.engine.optimizer.SelectivityFunction;
import com.hp.hpl.jena.query.darq.mapping.rewriting.TripleRewriter;
import com.hp.hpl.jena.query.engine.BindingMap;
import com.hp.hpl.jena.query.expr.Expr;
import com.hp.hpl.jena.query.lang.arq.ARQParser;
import com.hp.hpl.jena.query.lang.arq.ParseException;

public class RemoteService {

    private String url;

    private String label;

    private String description;

    private Set<Capability> capabilities = new HashSet<Capability>();
    
    private static int INVALID_TRIPLECOUNT = -184;  // an impossible number of triples
    
    private int tripleCount=INVALID_TRIPLECOUNT;

    private boolean isDefinitive;

    private Set<String> preferedFilters;
        
    private Set<Set<RequiredBinding>> requiredBindings = new HashSet<Set<RequiredBinding>>();

    private SelectivityFunction selectivityFunction = new CostBasedFunction();
    

    public RemoteService(String url, String label, String description, boolean isDefinitive) {
        this.url = url;
        this.label = label;
        this.description = description;
        this.isDefinitive = isDefinitive;
        
   //     capabilities = new HashMap<String,String>();
     //   preferedFilters = new HashSet<String>();
        
    }

        
    public boolean hasCapability(Triple t) {
        
        
        //! String objectFilter = capabilities.get(t.getPredicate().getURI());
        if (!t.getPredicate().isURI()) return false;
        Capability c = findCapability(t.getPredicate().getURI());
      
        
        if (c==null) return false; 
        
        String objectFilter = null;  
        objectFilter = c.getObjectFilter();
        
        
        // There is no constraint for the object
        if (objectFilter==null) return true;
        if (objectFilter.equals("")) return true;
        
        
        // XXX Object is a Variable. We can't check the filter.  TODO handle in a better way?  
        if (t.getObject().isVariable()) return true;
 
        // There is a object and we have to check whether the Service knows about it.
        
        // add FILTER keyword for parsing
        objectFilter = "FILTER("+objectFilter+")";
        
        StringReader sr = new StringReader(objectFilter);
        ARQParser parser = new ARQParser(sr);
        
        Query query = new Query();
        parser.setQuery(query);

        Expr constraint = null;  
        
        try
        {
            constraint = parser.Constraint();
            
        } catch (ParseException e)
        {
            System.err.println(e.getStackTrace());
            return false;
        }

        BindingMap bindingMap = new BindingMap();
        bindingMap.add(Var.alloc("object"), t.getObject());
        
        return constraint.isSatisfied( bindingMap, null);

    }
    
    
    public String getDescription() {
        return description;
    }

    public boolean isDefinitive() {
        return isDefinitive;
    }
    
    

    public boolean hasObjectFilter(Triple t) {
        //! String objectFilter = capabilities.get(t.getPredicate().getURI());
        String objectFilter = findCapability(t.getPredicate().getURI()).getObjectFilter();

        // Service does not know about the predicate of the triple
        if (objectFilter==null) return false;
        
        // There is no constraint for the object
        if (objectFilter.equals("")) return false;
        
        
        return true;
    }

    public Double getObjectSelectivity(String predicate) {
        return findCapability(predicate).getObjectSelectivity();
    }
    
    public Double getSubjectSelectivity(String predicate) {
        return findCapability(predicate).getSubjectSelecticity();
    }
    
    /**
     * @return Returns the SelectivityFunction.
     */
    public SelectivityFunction getSelectivityFunction() {
        return selectivityFunction;
    }


    /**
     * @param sel The SelectivityFunction to set.
     */
    public void setSelectivityFunction(SelectivityFunction sel) {
        if (sel != null) selectivityFunction = sel;
    }


    /**
     * @return Returns the total number of triples .
     */
    public int getTripleCount() {
        return tripleCount;
    }


    /**
     * @param tripleCount the total number of triples to set.
     */
    public void setTripleCount(int tripleCount) {
        this.tripleCount = tripleCount;
    }


    public Set<Capability> getCapability() {
        return capabilities;
    }
   
    public void addCapability(Capability c) {
        capabilities.add(c);
    }
    
    
    public TripleRewriter getTripleRewriter(Triple t) {
        return findCapability(t.getPredicate().getURI()).getTripleRewriter();
    }
    

    public void addTripleRewriter(String predicate, TripleRewriter rewriter) {
        findCapability(predicate).setTripleRewriter(rewriter);
    }
    
    /**
     * 
     * @param t
     * @return the number of triples with the same predicate as in t. 
     */
    public int getTriples(Triple t) {
        return findCapability(t.getPredicate().getURI()).getTriples();
    }
    
    
    public String getLabel() {
        return label;
    }

    public Set getPreferedFilters() {
        return preferedFilters;
    }

      
    public void addPreferedFilter(String predicate) {
        preferedFilters.add(predicate);
    }

    

    public Set<Set<RequiredBinding>> getRequiredBindings() {
        return requiredBindings;
    }

    public void addRequiredBinding(Set<RequiredBinding> r) {
        requiredBindings.add(r);
    }

    
    public String getUrl() {
        return url;
    }
    
    private Capability findCapability(String predicate) {
        for (Capability c:capabilities) {
            if (c.getPredicate().equals(predicate)) return c;
        }
        return null;
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
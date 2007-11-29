/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.engine.compiler.iterators;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.core.ElementFilter;
import com.hp.hpl.jena.query.core.ElementGroup;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.compiler.RewrittenTripleIterator;
import com.hp.hpl.jena.query.darq.mapping.rewriting.TripleRewriter;
import com.hp.hpl.jena.query.darq.util.QueryUtils;
import com.hp.hpl.jena.query.engine.Binding;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.expr.Expr;


public class RemoteQuery {
    
    ServiceGroup serviceGroup;
    ExecutionContext context;
    Binding binding;
    
    RewrittenTripleIterator tripleIterator=null;
    long minResultsToStop = 1;
    
    public RemoteQuery(ServiceGroup sg, ExecutionContext c, Binding b) {
        serviceGroup=sg;
        context=c;
        binding=b;
        buildTripleIterator();
    }
    
    
    
    public boolean hasNextQuery(long previousResults){
        if (previousResults>=minResultsToStop) return false;
        return tripleIterator.hasNext();
    }
    
    public Query getNextQuery() {
        if (!tripleIterator.hasNext()) throw new IndexOutOfBoundsException("No more rewritings.");
        Query remoteQuery = buildQuery();
        return  remoteQuery;
    }


    private void buildTripleIterator() {
        List<Triple> tripleList =serviceGroup.getTriples();
        
        RewrittenTripleIterator prevIterator= null;
        for (int i=tripleList.size()-1; i>=0; i--) {
            Triple t= tripleList.get(i);
            
            Node subject = QueryUtils.replacewithBinding(t.getSubject(),binding);
            Node predicate = QueryUtils.replacewithBinding(t.getPredicate(),binding);
            Node object = QueryUtils.replacewithBinding(t.getObject(),binding);

            Triple newtriple = new Triple(subject,predicate,object);
            
            TripleRewriter tripleRewriter = serviceGroup.getService().getTripleRewriter(newtriple);
            
            if (tripleRewriter!=null) {
                    prevIterator = new RewrittenTripleIterator(tripleRewriter.getRewritings(newtriple),prevIterator);
                    if (tripleRewriter.getMinimumResultsToStop()>minResultsToStop) minResultsToStop=tripleRewriter.getMinimumResultsToStop(); 
            } else {
                ArrayList<Triple> tmpal = new ArrayList<Triple>();
                tmpal.add(t);
                prevIterator = new RewrittenTripleIterator(tmpal,prevIterator);
            }
            
            tripleIterator=prevIterator;
            
            
   }
        
    }
    
    private Query buildQuery() {
        
        Query remoteQuery = new Query();
        remoteQuery.setPrefixMapping(context.getQuery()
                .getPrefixMapping());
        remoteQuery.setBaseURI(context.getQuery().getBaseURI());
        remoteQuery.setSyntax(context.getQuery().getSyntax());
        remoteQuery.setQueryType(Query.QueryTypeSelect);
        remoteQuery.setQueryResultStar(true);

        ElementGroup eg = new ElementGroup();
        
        for (Triple t: tripleIterator.next()) {
                
                 Node subject = QueryUtils.replacewithBinding(t.getSubject(),binding);
                 Node predicate = QueryUtils.replacewithBinding(t.getPredicate(),binding);
                 Node object = QueryUtils.replacewithBinding(t.getObject(),binding);

                 Triple newtriple = new Triple(subject,predicate,object);

                 eg.addTriplePattern(newtriple);
        }
        
        
        for (Expr c:serviceGroup.getFilters()) {
            eg.addElementFilter(new ElementFilter(QueryUtils.replacewithBinding(c,binding,context.getQuery())));
        }
        
        remoteQuery.setQueryPattern(eg);

        
        return remoteQuery;
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
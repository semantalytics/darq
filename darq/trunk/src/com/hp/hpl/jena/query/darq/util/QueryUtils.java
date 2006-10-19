/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.util;

import java.io.StringReader;
import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.query.core.Binding;
import com.hp.hpl.jena.query.core.Constraint;
import com.hp.hpl.jena.query.core.ElementFilter;
import com.hp.hpl.jena.query.expr.Expr;
import com.hp.hpl.jena.query.lang.arq.ARQParser;
import com.hp.hpl.jena.query.lang.arq.ParseException;
import com.hp.hpl.jena.query.util.FmtUtils;

public class QueryUtils {

    public static Node replacewithBinding(Node node, Binding b) {
        if (node.isVariable() && b.contains(node.getName())) {
            Node n = b.get(node.getName());
            if (n== null) return node; // should not happen!
            if (n.isBlank()) {
                  // we do not support blank nodes
                throw new QueryExecException("Cannot handle Blank Nodes over different graphs.");
            } else {
                return n;
            }
        }
        return node;
    }
    
    public static Expr replacewithBinding(Constraint c, Binding b,Query orgQuery) {
        String filter = "FILTER"+c.toString();
        
        for (Iterator it = b.names(); it.hasNext() ;) {
            String varName = (String)it.next();
            Node n=b.get(varName);
            if (n==null) continue;
            
            if (n.isBlank()) { 
                throw new QueryExecException("Cannot handle Blank Nodes over different graphs.");
            } else {
                String value = FmtUtils.stringForNode(n);
                filter=filter.replaceAll("\\?"+varName,value);
            }
        }
          
        StringReader sr = new StringReader(filter);
        ARQParser parser = new ARQParser(sr);
        Query q= new Query();
        q.setPrefixMapping(orgQuery.getPrefixMapping());
        parser.setQuery(q);
        
        Expr expr= null;

        try
        {
            expr = (Expr)parser.Constraint();

        } catch (ParseException e)
        {
            throw new QueryExecException(e); // should not happen !!
        }
        
      
        
        
        return expr;
        
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
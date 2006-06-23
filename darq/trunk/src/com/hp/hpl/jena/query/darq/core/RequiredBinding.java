/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.core;

import com.hp.hpl.jena.graph.Node;

public class RequiredBinding {
    
    public final static int OBJECT_BINDING = 10;
    public final static int SUBJECT_BINDING = 20;

    private Node predicate ;
    private int type;

    public RequiredBinding(Node p, int t) {
        predicate=p;
        type=t;
    }
    
    
    public RequiredBinding(String s, int t) {
        predicate=Node.createURI(s);
        type=t;
    }
    
    /**
     * @return Returns the predicate.
     */
    public Node getPredicate() {
        return predicate;
    }
    
    
    public String getPredicateString() {
        return predicate.getURI();
    }
    
    
    public int getType() {
        return type;
    }
    


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String s;
       if (type==OBJECT_BINDING) s= "ObjectBinding: ";
       else s= "Binding unknown type: ";
       return s+predicate.getURI();
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof RequiredBinding)) return false;
        
        RequiredBinding tmp = (RequiredBinding)obj;
        
        if (this.predicate.equals(tmp.predicate) && this.type == tmp.type) return true;
        
        return false;
        
        
    }


    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return predicate.hashCode() ^ new Integer(type).hashCode();
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
/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


import com.hp.hpl.jena.query.expr.Expr;

public class MultipleServiceGroup extends ServiceGroup {
    
    Set<RemoteService> services = new HashSet<RemoteService>();  

    public MultipleServiceGroup() {
        super(null);
    }
    
    public MultipleServiceGroup(RemoteService s) {
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
        throw new UnsupportedOperationException("this is a MultipleServiceGroup - use getServices");
    }

    public Set<RemoteService> getServices() {
        return services;
    }
    
    public ServiceGroup getServiceGroup(RemoteService s) {
        if (!services.contains(s)) return null;
        
        ServiceGroup sg = new ServiceGroup(s);
        sg.setTriples(this.getTriples());
        sg.setFilters(this.getFilters());

        return sg;
    }

    /* (non-Javadoc)
     * @see com.hp.hpl.jena.query.federated.core.ServiceGroup#clone()
     */
    @Override
    public MultipleServiceGroup clone() {
        MultipleServiceGroup sg = new MultipleServiceGroup();
        sg.setTriples(this.getTriples());
        sg.setFilters(new ArrayList<Expr>(this.getFilters()));
        sg.services= new HashSet<RemoteService>(this.services);
        return sg;
    }

    /* (non-Javadoc)
     * @see com.hp.hpl.jena.query.federated.core.ServiceGroup#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MultipleServiceGroup) {
            MultipleServiceGroup otherGroup = (MultipleServiceGroup) obj;
            if (getServices().equals(otherGroup.getServices()) && getTriples().equals(otherGroup.getTriples()) && getFilters().equals(otherGroup.getFilters()) ) return true;
        } 
        return false;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return services.hashCode() ^ getTriples().hashCode() ^ getFilters().hashCode();
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
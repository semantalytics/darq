/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.query.darq.engine.compiler;


import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngineFactory;
import com.hp.hpl.jena.query.darq.util.FedPlanVisitor;
import com.hp.hpl.jena.query.darq.util.OutputUtils;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.PlanVisitor;
import com.hp.hpl.jena.query.engine1.plan.PlanElement1;
import com.hp.hpl.jena.query.engine1.plan.Transform;
import com.hp.hpl.jena.query.util.Context;

import de.hu_berlin.informatik.wbi.darq.cache.Caching;


/**
 * 
 * @author Bastian Quilitz
 * @version $ID$
 *
 */
public class FedPlanService extends PlanElement1
{
	private ServiceGroup serviceGroup ;
	private Caching cache;
	private Boolean cacheEnabled;
	
    
    public ServiceGroup getServiceGroup() {
        return serviceGroup;
    }
    
    public static PlanElement make(Context c,ServiceGroup sg, PlanElement subElt, Caching cache, Boolean cacheEnabled)
    {
        return new FedPlanService(c, sg, subElt, cache, cacheEnabled) ;
    }
    
    
    
    private FedPlanService(Context c,ServiceGroup sg, PlanElement cElt, Caching cache, Boolean cacheEnabled)
    {
        super(c, cElt) ;
        serviceGroup = sg ;
        this.cache = cache;
        this.cacheEnabled = cacheEnabled;
    }

    
    
    public QueryIterator build(QueryIterator input, ExecutionContext execCxt)
    {  
//    	FedQueryEngineFactory factory =  FedQueryEngineFactory.getInstance(); //TEST
//    	Configuration conf = factory.getConfig();
//    	IDarqQueryIteratorFactory iter =  conf.getDarqQueryIteratorFactory();
//    	QueryIterator queryiter =  iter.getNewInstance(input, serviceGroup, execCxt, getSubElement());
        return FedQueryEngineFactory.getInstance().getConfig().getDarqQueryIteratorFactory().getNewInstance(input, serviceGroup, execCxt, getSubElement(), cache, cacheEnabled);    }
    
    public void visit(PlanVisitor visitor) { 
        
        if (visitor instanceof FedPlanVisitor) { 
            ((FedPlanVisitor)visitor).visit(this);
        } else {
        //    visitor.visit((PlanBasicPattern) PlanBasicPattern.make(new Plan())) ; 
        }
    }

    /* (non-Javadoc)
     * @see com.hp.hpl.jena.query.engine1.compiler.PlanElementBase#toString()
     */
    @Override
    public String toString() {
        // TODO Auto-generated method stub
        
        return OutputUtils.serviceGroupToString(this.serviceGroup);
    }

    @Override
    public PlanElement apply(Transform transform, PlanElement x) {
        return x; //TODO FIXME ?
    }

    @Override
    public PlanElement copy(PlanElement newSubElement) {
        return make(getContext(),serviceGroup,newSubElement,cache,cacheEnabled);
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
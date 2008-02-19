/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.query.darq.engine.compiler;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.query.core.Var;
import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.engine.compiler.iterators.QueryIterUnionParallel;
import com.hp.hpl.jena.query.darq.util.FedPlanVisitor;
import com.hp.hpl.jena.query.darq.util.OutputUtils;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.PlanVisitor;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterDistinct;
import com.hp.hpl.jena.query.engine1.plan.PlanElement1;
import com.hp.hpl.jena.query.engine1.plan.Transform;
import com.hp.hpl.jena.query.util.Context;


/**
 * 
 * @author Bastian Quilitz
 * @version $ID$
 *
 */
public class FedPlanMultipleService extends PlanElement1
{
	private MultipleServiceGroup serviceGroup ;
	
    
    public MultipleServiceGroup getServiceGroup() {
        return serviceGroup;
    }
    
    public static PlanElement make(Context c,  MultipleServiceGroup sg, PlanElement subElt)
    {
        return new FedPlanMultipleService(c,  sg, subElt) ;
    }
    
    
    
    private FedPlanMultipleService(Context c, MultipleServiceGroup sg, PlanElement cElt)
    {
        super(c, cElt) ;
        serviceGroup = sg ;
    }

    
    
    public QueryIterator build(QueryIterator input, ExecutionContext execCxt)
    {
       /* QueryIterConcat concat = new QueryIterConcat(execCxt);
        
        for (RemoteService s: serviceGroup.getServices()) {
            concat.add(new FedQueryIterService(input,serviceGroup.getServiceGroup(s),execCxt,null));
        }
        return new QueryIterDistinct(concat,execCxt);*/
        
        List<PlanElement> list = new ArrayList<PlanElement>();
        for (RemoteService s: serviceGroup.getServices()) {
            list.add(FedPlanService.make(this.getContext(),serviceGroup.getServiceGroup(s),this.getSubElement()) );
            
        }
        
        //PlanDistinct planDistinct = PlanDistinct.make(execCxt.getContext(), this.getSubElement(), (Collection) serviceGroup.getUsedVariables());
        
        
        Set<String> usedVariables=serviceGroup.getUsedVariables();
        
        List<Var> vars= new ArrayList<Var>();
        for (String s:usedVariables) vars.add(Var.alloc(s));
        
        for (String s: (List<String>)execCxt.getQuery().getResultVars()) vars.add(Var.alloc(s));
        
        
        
        QueryIterator qIter = BindingImmutableDistinctUnion.create(vars, (QueryIterator) new QueryIterUnionParallel(input,list,execCxt), execCxt) ;
        
    //    QueryIterator qIter = new QueryIterUnionParallel(input,list,execCxt) ;
        
        return  new QueryIterDistinct(qIter, execCxt) ;
        
        
        
       
    	
    }
    
    public void visit(PlanVisitor visitor) { 
        
        if (visitor instanceof FedPlanVisitor) { 
            ((FedPlanVisitor)visitor).visit(this);
        } else {
        //    visitor.visit((PlanBasicGraphPattern) PlanBasicGraphPattern.make(new Plan())) ; 
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
        return make(getContext(),serviceGroup,newSubElement);
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
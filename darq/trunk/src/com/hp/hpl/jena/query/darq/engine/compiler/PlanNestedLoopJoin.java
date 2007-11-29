/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.query.darq.engine.compiler;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.engine.compiler.iterators.NestedLoopJoinIterator;
import com.hp.hpl.jena.query.darq.engine.compiler.iterators.QueryIterUnionParallel;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.PlanOperatorBase;
import com.hp.hpl.jena.query.darq.util.FedPlanVisitor;
import com.hp.hpl.jena.query.darq.util.OutputUtils;
import com.hp.hpl.jena.query.engine.BindingImmutable;
import com.hp.hpl.jena.query.engine.BindingRoot;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.PlanVisitor;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterDistinct;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterSingleton;
import com.hp.hpl.jena.query.engine1.plan.PlanElement0;
import com.hp.hpl.jena.query.engine1.plan.PlanElement1;
import com.hp.hpl.jena.query.engine1.plan.Transform;
import com.hp.hpl.jena.query.util.Context;


/**
 * 
 * @author Bastian Quilitz
 * @version $ID$
 *
 */
public class PlanNestedLoopJoin extends PlanElement0
{
	private PlanElement left;
	private PlanElement right;
	
	
    
    
    
    public static PlanElement make(Context c,  PlanElement left, PlanElement right)
    {
        return new PlanNestedLoopJoin(c, left, right) ;
    }
    
    
    
    private PlanNestedLoopJoin(Context c, PlanElement left, PlanElement right)
    {
        super(c);
        this.left=left;
        this.right=right;
    }

    
    
    public QueryIterator build(QueryIterator input, ExecutionContext execCxt)
    {
    	// build query iterators for left and right
    	QueryIterator qi = new QueryIterSingleton(BindingRoot.create(), execCxt) ; 
    	QueryIterator leftit = left.build(qi, execCxt);
    	qi=new QueryIterSingleton(BindingRoot.create(), execCxt) ; 
    	QueryIterator rightit = right.build(qi, execCxt);
        
    	// do the nested loop twice -> the regular nested loop and a nested loop with the input iterator
        return  new NestedLoopJoinIterator(execCxt, input, new NestedLoopJoinIterator(execCxt, leftit , rightit));
	
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
        return "NestedLoopJoin: " +left.toString() + " |><| " + right.toString();
    }




	@Override
	public PlanElement apply(Transform transform) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public PlanElement copy() {
		// TODO Auto-generated method stub
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
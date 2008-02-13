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
import com.hp.hpl.jena.query.darq.util.MapFedPlanVisitor;

/**
 * 
 * @author Bastian Quilitz
 * @version $ID$
 *
 */
public class MapFedPlanMultipleService extends PlanElement1 {
	/*
	 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
	 * All rights reserved.
	 * [See end of file]
	 */

	



		private MultipleServiceGroup serviceGroup ;
		
	    
	    public MultipleServiceGroup getServiceGroup() {
	        return serviceGroup;
	    }
	    
	    public static PlanElement make(Context c,  MultipleServiceGroup sg, PlanElement subElt)
	    {
	        return new MapFedPlanMultipleService(c, sg, subElt) ;
	    }
	    
	    
	    
	    private MapFedPlanMultipleService(Context c, MultipleServiceGroup sg, PlanElement cElt)
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
	            list.add(MapFedPlanService.make(this.getContext(),serviceGroup.getServiceGroup(s),this.getSubElement()) );
	            
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
	        
	        if (visitor instanceof MapFedPlanVisitor) { 
	            ((MapFedPlanVisitor)visitor).visit(this);
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


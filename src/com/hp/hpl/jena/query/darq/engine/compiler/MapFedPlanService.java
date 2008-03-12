package com.hp.hpl.jena.query.darq.engine.compiler;

import com.hp.hpl.jena.query.darq.core.MapServiceGroup;
import com.hp.hpl.jena.query.darq.engine.MapFedQueryEngineFactory;
import com.hp.hpl.jena.query.darq.util.MapFedPlanVisitor;
import com.hp.hpl.jena.query.darq.util.MapOutputUtils;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.PlanVisitor;
import com.hp.hpl.jena.query.engine1.plan.PlanElement1;
import com.hp.hpl.jena.query.engine1.plan.Transform;
import com.hp.hpl.jena.query.util.Context;


/**
 * @author Alexander Musidlowski
 * @version $ID$
 *
 */
public class MapFedPlanService extends PlanElement1{
	
		private MapServiceGroup serviceGroup ;
		
	    
	    public MapServiceGroup getServiceGroup() {
	        return serviceGroup;
	    }
	    
	    public static PlanElement make(Context c,MapServiceGroup sg, PlanElement subElt)
	    {
	        return new MapFedPlanService(c, sg, subElt) ;
	    }
	    
	    
	    
	    private MapFedPlanService(Context c,MapServiceGroup sg, PlanElement cElt)
	    {
	        super(c,cElt);
	        serviceGroup = sg ;
	    }

	    
	    
	    public QueryIterator build(QueryIterator input, ExecutionContext execCxt)
	    {
	        return MapFedQueryEngineFactory.getInstance().getConfig().getDarqQueryIteratorFactory().getNewInstance(input, serviceGroup, execCxt, getSubElement());
	    }
	    
	    public void visit(PlanVisitor visitor) { 
	        
	        if (visitor instanceof MapFedPlanVisitor) { 
	            ((MapFedPlanVisitor)visitor).visit(this);
	        } else {
	        //    visitor.visit((PlanBasicPattern) PlanBasicPattern.make(new Plan())) ; 
	        }
	    }

	    /* (non-Javadoc)
	     * @see com.hp.hpl.jena.query.engine1.compiler.PlanElementBase#toString()
	     */
	    @Override
	    public String toString() {     
	        return MapOutputUtils.serviceGroupToString(this.serviceGroup);
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


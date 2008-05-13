package com.hp.hpl.jena.query.darq.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanMultipleService;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanService;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanUnionService;
import com.hp.hpl.jena.query.darq.engine.compiler.PlanNestedLoopJoin;
import com.hp.hpl.jena.query.darq.util.FedPlanVisitor;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.plan.PlanBasicGraphPattern;
import com.hp.hpl.jena.query.engine1.plan.PlanBlockTriples;
import com.hp.hpl.jena.query.engine1.plan.PlanDataset;
import com.hp.hpl.jena.query.engine1.plan.PlanDistinct;
import com.hp.hpl.jena.query.engine1.plan.PlanElement0;
import com.hp.hpl.jena.query.engine1.plan.PlanElement1;
import com.hp.hpl.jena.query.engine1.plan.PlanElementExternal;
import com.hp.hpl.jena.query.engine1.plan.PlanElementN;
import com.hp.hpl.jena.query.engine1.plan.PlanExtension;
import com.hp.hpl.jena.query.engine1.plan.PlanFilter;
import com.hp.hpl.jena.query.engine1.plan.PlanGroup;
import com.hp.hpl.jena.query.engine1.plan.PlanLimitOffset;
import com.hp.hpl.jena.query.engine1.plan.PlanNamedGraph;
import com.hp.hpl.jena.query.engine1.plan.PlanOptional;
import com.hp.hpl.jena.query.engine1.plan.PlanOrderBy;
import com.hp.hpl.jena.query.engine1.plan.PlanOuterJoin;
import com.hp.hpl.jena.query.engine1.plan.PlanProject;
import com.hp.hpl.jena.query.engine1.plan.PlanPropertyFunction;
import com.hp.hpl.jena.query.engine1.plan.PlanUnion;
import com.hp.hpl.jena.query.engine1.plan.PlanUnsaid;
import com.hp.hpl.jena.query.engine1.plan.Transformer;

public class MapDarqTransformer {
/* Vererbung geht wieder nicht wegen privatem Konstruktor*/ 	
	   static boolean noDupIfSame = true ;
	    
	    public static PlanElement transform(MapDarqTransform tranform, PlanElement elt)
	    {
	        if ( elt == null )
	        {
	            LogFactory.getLog(Transformer.class).warn("Attempt to transform a null PlanElement - ignored") ;
	            return elt ;
	        }
	        return new MapDarqTransformer().transformation(tranform, elt) ;
	    }
	    
	    
	    private MapDarqTransformer() { }
	    
	    public PlanElement transformation(MapDarqTransform tranform, PlanElement elt)
	    {
	        MapDARQTransformApplyBase v = new MapDARQTransformApplyBase(tranform) ;
	        elt.visit(v) ;
	        return v.result() ;
	    }
	   
}

class MapDARQTransformApplyBase  implements FedPlanVisitor{
	 Stack stack = new Stack() ;
	 Stack<PlanElement> parent = new Stack<PlanElement>();
	 
	    private PlanElement pop() { return (PlanElement)stack.pop(); }
	    
	    private void push(PlanElement newElt)
	    { 
	        // Including nulls
	        stack.push(newElt) ;
	    }
	        

	    public PlanElement result()
	    { 
	        if ( stack.size() != 1 )
	            LogFactory.getLog(MapDARQTransformApplyBase.class).warn("Stack is not aligned") ;
	        return pop() ; 
	    }

	    private MapDarqTransform mapTransform;
	    MapDARQTransformApplyBase(MapDarqTransform transform) { this.mapTransform = transform; }
	    
	    public void visit(PlanElement0 planElt)
	    {
	        push(planElt.apply(mapTransform)) ;
	    }

	    public void visit(PlanElement1 planElt)
	    {
	    	parent.push(planElt);
	        planElt.getSubElement().visit(this) ;
	        PlanElement x = pop() ;
	        push(planElt.apply(mapTransform, x)) ;
	        parent.pop();
	    }

	    public void visit(PlanElementN planElt)
	    { 
	    	parent.push(planElt);
	        List x = new ArrayList() ;
	        int N = planElt.numSubElements() ;
	        for ( int i = 0 ; i < N ; i++ )
	        {
	            planElt.getSubElement(i).visit(this) ;
	            x.add(pop()) ;
	        }
	        push(planElt.apply(mapTransform, x)) ;
	        parent.pop();
	    }

	    public void visit(PlanElementExternal planElt)
	    {
	        push(planElt) ;
	    }

		public void visit(FedPlanService planElt) {
			 push(planElt) ;
		}

		public void visit(FedPlanMultipleService planElt) {
			 push(planElt) ;
			
		}
		
		public void visit(FedPlanUnionService planElt){
			push(planElt);
		}

		public void visit(PlanNestedLoopJoin planNestedLoopJoin) {
			 push(planNestedLoopJoin) ;
			
		}

		public void visit(PlanBlockTriples planElt) {
			visit((PlanElement0)planElt);
			
		}

		public void visit(PlanBasicGraphPattern planElt) {
			push(mapTransform.transform(planElt,null,parent.lastElement()));
		}

		public void visit(PlanGroup planElt) {
			visit((PlanElementN)planElt);
		}

		public void visit(PlanUnion planElt) {
			visit((PlanElementN)planElt);
		}

		public void visit(PlanOptional planElt) {
			visit((PlanElement1)planElt);
		}

		public void visit(PlanUnsaid planElt) {
			visit((PlanElement1)planElt);
		}

		public void visit(PlanFilter planElt) {
			visit((PlanElement0)planElt);
		}

		public void visit(PlanNamedGraph planElt) {
			visit((PlanElement1)planElt);
		}

		public void visit(PlanOuterJoin planElt) {
			visit((PlanElement0)planElt);
		}

		public void visit(PlanPropertyFunction planPF) {
			visit((PlanElement0)planPF);
		}

		public void visit(PlanExtension planElt) {
			visit((PlanElement0)planElt);
		}

		public void visit(PlanDataset planElt) {
			visit((PlanElement1)planElt);
			
		}

		public void visit(PlanProject planElt) {
			visit((PlanElement1)planElt);
		}

		public void visit(PlanDistinct planElt) {
			visit((PlanElement1)planElt);
		}

		public void visit(PlanOrderBy planElt) {
			visit((PlanElement1)planElt);
		}

		public void visit(PlanLimitOffset planElt) {
			visit((PlanElement1)planElt);
		}
	
}
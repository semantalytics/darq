package com.hp.hpl.jena.query.darq.engine.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.query.core.Var;
import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.MultiplyMultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.core.StringConcatMultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.UnionServiceGroup;
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

import de.hu_berlin.informatik.wbi.darq.cache.Caching;

public class FedPlanUnionService extends PlanElement1 {
	
private UnionServiceGroup serviceGroup ;
private Caching cache;	
private Boolean cacheEnabled;

    public UnionServiceGroup getServiceGroup() {
        return serviceGroup;
    }
    
    public static PlanElement make(Context c,  UnionServiceGroup sg, PlanElement subElt, Caching cache, Boolean cacheEnabled)
    {
        return new FedPlanUnionService(c,  sg, subElt, cache, cacheEnabled) ;
    }
    
    private FedPlanUnionService(Context c, UnionServiceGroup sg, PlanElement cElt, Caching cache, Boolean cacheEnabled)
    {
        super(c, cElt) ;
        serviceGroup = sg ;
        this.cache = cache;
        this.cacheEnabled = cacheEnabled;
    }
    
    
    public QueryIterator build(QueryIterator input, ExecutionContext execCxt)
    {
       /* QueryIterConcat concat = new QueryIterConcat(execCxt);
        
        for (RemoteService s: serviceGroup.getServices()) {
            concat.add(new FedQueryIterService(input,serviceGroup.getServiceGroup(s),execCxt,null));
        }
        return new QueryIterDistinct(concat,execCxt);*/
    	System.out.println(" [FedPlanUnionService.build]");
        List<PlanElement> list = new ArrayList<PlanElement>();
        
        /* geht die einzelnen Services der MSG durch  */
        /* selber Aufruf von FedPlanService wie bei SG nur mit SubElement */
        /* FRAGE Was ist das SubElement? */
        for(ServiceGroup sg : serviceGroup.getServiceGroups().values()){
        	if (sg instanceof MultipleServiceGroup){
        		MultipleServiceGroup msg = (MultipleServiceGroup) sg;
                for (RemoteService s: msg.getServices()) {
                    list.add(FedPlanService.make(this.getContext(),msg.getServiceGroup(s),this.getSubElement(),cache,cacheEnabled) );                    
                }        		
        	}
        	else if (sg instanceof MultiplyMultipleServiceGroup) {
				MultiplyMultipleServiceGroup muMSG = (MultiplyMultipleServiceGroup) sg;
				for (RemoteService s: muMSG.getServices()) {
                    list.add(FedPlanService.make(this.getContext(),muMSG.getServiceGroup(s),this.getSubElement(),cache,cacheEnabled) );                    
                }
			}
        	else if  (sg instanceof ServiceGroup){ /* and MultiplyServiceGroup,StrnigConcatServiceGroup */
        		 list.add(FedPlanService.make(this.getContext(),sg,this.getSubElement(),cache,cacheEnabled) );		 
        	}
        	else if (sg instanceof StringConcatMultipleServiceGroup) {
        		StringConcatMultipleServiceGroup scMSG = (StringConcatMultipleServiceGroup) sg;
				for (RemoteService s: scMSG.getServices()) {
                    list.add(FedPlanService.make(this.getContext(),scMSG.getServiceGroup(s),this.getSubElement(),cache,cacheEnabled) );                    
                }
			}
        	else if  (sg instanceof ServiceGroup){ /* and MultiplyServiceGroup,StrnigConcatServiceGroup */
        		 list.add(FedPlanService.make(this.getContext(),sg,this.getSubElement(),cache,cacheEnabled) );		 
        	}
        }
        
        //PlanDistinct planDistinct = PlanDistinct.make(execCxt.getContext(), this.getSubElement(), (Collection) serviceGroup.getUsedVariables());
        
        
        Set<String> usedVariables=serviceGroup.getUsedVariables();
        
        List<Var> vars= new ArrayList<Var>();
        for (String s:usedVariables) vars.add(Var.alloc(s));
        
        for (String s: (List<String>)execCxt.getQuery().getResultVars()) vars.add(Var.alloc(s));
        
        
        /* Ist das der Union? 
         * QueryIterator ist ein Zähler über das Ergebnis */
        /* TODO  Eventuell muss ich das Ergebnis hier nochmal unterteilen um ggf. Transformationen vorzunehmen */
        /* Problem, welches Triple hat, benötigt welche Transformation? */ 
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
        return OutputUtils.serviceGroupToString(this.serviceGroup);
    }

    @Override
    public PlanElement apply(Transform transform, PlanElement x) {
        return x; //TODO FIXME ?
    }

    @Override
    public PlanElement copy(PlanElement newSubElement) {
        return make(getContext(),serviceGroup,newSubElement,cache, cacheEnabled);
    }
}

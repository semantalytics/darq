package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.MultiplyMultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.core.StringConcatMultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.UnionServiceGroup;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanMultipleService;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanService;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanUnionService;
import com.hp.hpl.jena.query.darq.engine.optimizer.CostBasedBasicOptimizer;
import com.hp.hpl.jena.query.darq.engine.optimizer.OptimizerElement;
import com.hp.hpl.jena.query.darq.engine.optimizer.PlanUnfeasibleException;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.util.Context;

import de.hu_berlin.informatik.wbi.darq.cache.Caching;

public class OperatorServiceGroup extends PlanOperatorBase {

	ServiceGroup sg;
	Caching cache;
	Boolean cacheEnabled;
	public OperatorServiceGroup(ServiceGroup sg, Caching cache, Boolean cacheEnabled) {
		this.sg = sg;
		this.cache = cache;
		this.cacheEnabled = cacheEnabled;
	}

	
	@Override
	public Set<String> getBoundVariables_() {
		if (sg instanceof UnionServiceGroup) {
			UnionServiceGroup usg = (UnionServiceGroup) sg;
			return usg.getUsedVariables();
		}
		return sg.getUsedVariables();
	}

	@Override
	public double getCosts_(Set<String> bound) throws PlanUnfeasibleException {
		return getResultsize(bound) * CT;
	}

	@Override
	public double getResultsize(Set<String> boundVariables)
			throws PlanUnfeasibleException {
		OptimizerElement<ServiceGroup> rsg = null;

		Set<String> bound;
		if (boundVariables == null)
			bound = new HashSet<String>();
		else
			bound = boundVariables;
		/* sg can be instance of USG, MSG, SG 
		 * an instance of USG can also be a MSG, SG, muSG or muMSG */
		if (sg instanceof UnionServiceGroup){

        	UnionServiceGroup usg = (UnionServiceGroup)sg;
        	boolean b = true;
        	boolean requiredBindingMSG=true;
        	boolean requiredBindingMuMSG=true;
        	boolean requiredBindingScMSG=true;
        	boolean requiredBindingSG=true;
        	for (ServiceGroup serviceGroup: usg.getServiceGroups().values() ){

        		if (serviceGroup instanceof MultipleServiceGroup) {

        			requiredBindingMSG=true;
        			for (RemoteService s:((MultipleServiceGroup)serviceGroup).getServices()){
        				if (!CostBasedBasicOptimizer.checkInput(serviceGroup.getTriples(), bound, s)) requiredBindingMSG=false; 
        				//looks if  RequiredBindings from service with Triple fits, if not next service
        			}                				
        			if (!requiredBindingMSG) throw new PlanUnfeasibleException(); //if requiredBinding does not fit, get next MSG 
        		} 

        		else if(serviceGroup instanceof MultiplyMultipleServiceGroup){
        			requiredBindingMuMSG=true;
        			for (RemoteService s:((MultiplyMultipleServiceGroup)serviceGroup).getServices()){
        				if (!CostBasedBasicOptimizer.checkInput(serviceGroup.getTriples(), bound, s)) requiredBindingMuMSG=false; 
        			}                				
        			if (!requiredBindingMuMSG) throw new PlanUnfeasibleException(); 
        		}
        		
        		else if(serviceGroup instanceof StringConcatMultipleServiceGroup){
        			requiredBindingScMSG=true;
        			for (RemoteService s:((StringConcatMultipleServiceGroup)serviceGroup).getServices()){
        				if (!CostBasedBasicOptimizer.checkInput(serviceGroup.getTriples(), bound, s)) requiredBindingScMSG=false; 
        			}                				
        			if (!requiredBindingScMSG) throw new PlanUnfeasibleException(); 
        		}
        		/* instance of ServiceGroup or MultiplyServiceGroup, StringConcatServiceGroup */
        		else {
        			requiredBindingSG=true;
        			if (!CostBasedBasicOptimizer.checkInput(serviceGroup.getTriples(), bound, serviceGroup.getService())) requiredBindingSG = false;
        			if (!requiredBindingSG) throw new PlanUnfeasibleException();;
        		}
        	}
        	b = requiredBindingMSG && requiredBindingSG && requiredBindingMuMSG && requiredBindingScMSG;
        	// if all RB fit build plan
        	if (!b) throw new PlanUnfeasibleException();
        	rsg = CostBasedBasicOptimizer.getCheapestPlanForUnionServiceGroup((UnionServiceGroup)sg, bound);
		}
		else if (sg instanceof MultipleServiceGroup) {

			boolean b = true;
			for (RemoteService s : ((MultipleServiceGroup) sg).getServices())
				if (!CostBasedBasicOptimizer.checkInput(sg.getTriples(), bound,
						s))
					b = false;
			if (!b)
				throw new PlanUnfeasibleException();

			rsg = CostBasedBasicOptimizer.getCheapestPlanForMultipleServiceGroup((MultipleServiceGroup) sg, bound);

		} else {

			if (!CostBasedBasicOptimizer.checkInput(sg.getTriples(), bound, sg
					.getService()))
				throw new PlanUnfeasibleException();
			;

			rsg = CostBasedBasicOptimizer.getCheapestPlanForServiceGroup(sg,
					bound); // get the best order for the triples in the group.
		}

		return rsg.getRankvalue();
	}

	@Override
	public Set<ServiceGroup> getServiceGroups_() {
		Set<ServiceGroup> result = new HashSet<ServiceGroup>();
		result.add(sg);
		return result;
	}
	
	public ServiceGroup getServiceGroup() {
		return sg;
	}

	@Override
	public boolean isCompatible(PlanOperatorBase pob) {
		if (pob.getServiceGroups().size()==1 && pob.getServiceGroups().contains(sg))  return true;
		return false;
	}

	@Override
	public void visit(PlanOperatorVisitor visitor) {
		visitor.visit(this);
		
	}



	@Override
	public boolean canBeRight() {
		return true;
	}

	@Override
	public PlanElement toARQPlanElement(Context context) {
		if (sg instanceof UnionServiceGroup) {
			return FedPlanUnionService.make(context, (UnionServiceGroup) sg, null,cache, cacheEnabled);
		}
		else if (sg instanceof MultipleServiceGroup) {
            return FedPlanMultipleService.make(context, (MultipleServiceGroup) sg, null,cache, cacheEnabled);
        } 
		else {
            return FedPlanService.make(context, sg, null, cache, cacheEnabled);
        }
	
	}

	@Override
	public PlanOperatorBase clone() {
		return this;
	}

	@Override
	public int getHight() {
		return 1;
	}

}

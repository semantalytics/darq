package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanMultipleService;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanService;
import com.hp.hpl.jena.query.darq.engine.optimizer.CostBasedBasicOptimizer;
import com.hp.hpl.jena.query.darq.engine.optimizer.OptimizerElement;
import com.hp.hpl.jena.query.darq.engine.optimizer.PlanUnfeasibleException;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.util.Context;

public class OperatorServiceGroup extends PlanOperatorBase {

	ServiceGroup sg;

	public OperatorServiceGroup(ServiceGroup sg) {
		this.sg = sg;
	}

	@Override
	public Set<String> getBoundVariables_() {

		return sg.getUsedVariables();
	}

	@Override
	public double getCosts_() throws PlanUnfeasibleException {
		return getResultsize(null) * CT;
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

		if (sg instanceof MultipleServiceGroup) {

			boolean b = true;
			for (RemoteService s : ((MultipleServiceGroup) sg).getServices())
				if (!CostBasedBasicOptimizer.checkInput(sg.getTriples(), bound,
						s))
					b = false;
			if (!b)
				throw new PlanUnfeasibleException();

			rsg = CostBasedBasicOptimizer
					.getCheapestPlanForMultipleServiceGroup(
							(MultipleServiceGroup) sg, bound);

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
		if (sg instanceof MultipleServiceGroup) {
            return FedPlanMultipleService.make(context, (MultipleServiceGroup) sg, null);
        } else {
            return FedPlanService.make(context, sg, null);
        }
	
	}

}

package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.darq.core.MapMultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.MapServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.compiler.MapFedPlanMultipleService;
import com.hp.hpl.jena.query.darq.engine.compiler.MapFedPlanService;
import com.hp.hpl.jena.query.darq.engine.optimizer.MapCostBasedBasicOptimizer;
import com.hp.hpl.jena.query.darq.engine.optimizer.OptimizerElement;
import com.hp.hpl.jena.query.darq.engine.optimizer.PlanUnfeasibleException;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.util.Context;

public class MapOperatorServiceGroup extends MapPlanOperatorBase {

	MapServiceGroup sg;

	public MapOperatorServiceGroup(MapServiceGroup sg) {
		this.sg = sg;
		this.setServiceGroup(sg); //kein POB, klar ist abstrakte Klasse
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
	public double getResultsize(Set<String> boundVariables)	throws PlanUnfeasibleException {
		
		OptimizerElement<MapServiceGroup> rsg = null;
		Set<String> bound;
		List<Triple> triples = new ArrayList<Triple>();
		
		if (boundVariables == null)
			bound = new HashSet<String>();
		else
			bound = boundVariables;

		if (sg instanceof MapMultipleServiceGroup) {

			boolean b = true;
			for (RemoteService s : ((MapMultipleServiceGroup) sg).getServices()){
				triples.clear();
				triples.add(sg.getTriple());
				if (!MapCostBasedBasicOptimizer.checkInput(triples, bound,s))
					b = false;
			}
			if (!b)
				throw new PlanUnfeasibleException();

			rsg = MapCostBasedBasicOptimizer.getCheapestPlanForMultipleServiceGroup((MapMultipleServiceGroup) sg, bound);

		} 
		else {
			triples.clear();
			triples.add(sg.getTriple());
			if (!MapCostBasedBasicOptimizer.checkInput(triples, bound, sg.getService()))
				throw new PlanUnfeasibleException();
			
			rsg = MapCostBasedBasicOptimizer.getCheapestPlanForServiceGroup(sg,bound); // get the best order for the triples in the group.
		}
		return rsg.getRankvalue();
	}

	@Override
	public Set<MapServiceGroup> getServiceGroups_() {
		Set<MapServiceGroup> result = new HashSet<MapServiceGroup>();
		result.add(sg);
		return result;
	}
	
	public MapServiceGroup getServiceGroup() {
		return sg;
	}

	@Override
	public boolean isCompatible(MapPlanOperatorBase pob) {
		if (pob.getServiceGroups().size()==1 && pob.getServiceGroups().contains(sg))  return true;
		return false;
	}


	public void visit(MapPlanOperatorVisitor visitor) {
		visitor.visit(this);
		
	}

	@Override
	public boolean canBeRight() {
		return true;
	}
	
	@Override
	public PlanElement toARQPlanElement(Context context) {
		if (sg instanceof MapMultipleServiceGroup) {
            return MapFedPlanMultipleService.make(context, (MapMultipleServiceGroup) sg, null);
        } else {
            return MapFedPlanService.make(context, sg, null);
        }
	
	}
}
package com.hp.hpl.jena.query.darq.engine.optimizer;

import java.util.HashMap;
import java.util.List;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.darq.core.MapServiceGroup;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.MapPlanOperatorBase;

public interface MapOptimizedPlanGenerator {

	public final static double PLAN_UNFEASIBLE_SELECTIVITY = -154;

	public final static double PLAN_UNFEASIBLE_RESULTS = Double.MAX_VALUE;

	public MapPlanOperatorBase getCheapestPlan(List<MapServiceGroup> sgs, HashMap<Triple, Integer> similarTripleMap) throws PlanUnfeasibleException;

	public double getCosts();
}

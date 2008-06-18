package com.hp.hpl.jena.query.darq.engine.optimizer;

import java.util.List;

import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.PlanOperatorBase;

import de.hu_berlin.informatik.wbi.darq.cache.Caching;

public interface OptimizedPlanGenerator {

	public final static double PLAN_UNFEASIBLE_SELECTIVITY = -154;

	public final static double PLAN_UNFEASIBLE_RESULTS = Double.MAX_VALUE;

	public PlanOperatorBase getCheapestPlan(List<ServiceGroup> sgs, Caching cache, Boolean cacheEnabled)
			throws PlanUnfeasibleException;

	public double getCosts();

}

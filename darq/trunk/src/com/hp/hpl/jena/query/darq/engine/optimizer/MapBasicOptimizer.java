package com.hp.hpl.jena.query.darq.engine.optimizer;

import java.util.List;

import com.hp.hpl.jena.query.darq.core.MapServiceGroup;

public interface MapBasicOptimizer {
	public final static double PLAN_UNFEASIBLE_SELECTIVITY = -154;
	public final static double PLAN_UNFEASIBLE_RESULTS = Double.MAX_VALUE;

	public List<MapServiceGroup> getCheapestPlan(List<MapServiceGroup> serviceGroups) throws PlanUnfeasibleException;

	public double getCosts();
}

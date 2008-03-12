package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.query.darq.core.MapServiceGroup;
import com.hp.hpl.jena.query.darq.engine.optimizer.PlanUnfeasibleException;

public abstract class MapJoin extends MapPlanOperatorBase {

	private MapPlanOperatorBase left;

	private MapPlanOperatorBase right;

	private Set<String> joinedVariables;

	public MapJoin(MapPlanOperatorBase left, MapPlanOperatorBase right) {
		this.left = left;
		this.right = right;

		// joinedVariables - intersection of left and right variables...
		this.joinedVariables = new HashSet<String>(left.getBoundVariables());
		this.joinedVariables.retainAll(right.getBoundVariables()); 

	}

	public Set<String> getJoinedVariables() {
		return joinedVariables;
	}

	public MapPlanOperatorBase getLeft() {
		return left;
	}

	public void setLeft(MapPlanOperatorBase left) {
		this.left = left;
	}

	public MapPlanOperatorBase getRight() {
		return right;
	}

	public void setRight(MapPlanOperatorBase right) {
		this.right = right;
	}

	@Override
	public Set<String> getBoundVariables_() {
		Set<String> bound = new HashSet<String>(left.getBoundVariables());
		bound.addAll(right.getBoundVariables());
		return bound;
	}

	/*
	 * (non-Javadoc)
	 * @see com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.MapPlanOperatorBase#getResultsize(java.util.Set)
	 * FRAGE Wie funktioniert die Funktion?
	 * Funktion ruft sich immer wieder selber auf (Rekursion), klar!
	 * Funktion soll double zurückliefern, Es gibt jedoch kein Funktion/Parameter/Methode
	 * die aufgerufen wird, die einen double zurückgibt. 
	 * Das letzte return müßte den initialen Wert liefern, ruft aber wiederum nur sich selbst auf
	 * UNKLAR
	 * Was ist 2d???
	 */
	@Override
	public double getResultsize(Set<String> boundVariables) throws PlanUnfeasibleException {
		for (String s : left.getBoundVariables()) {
			if (right.getBoundVariables().contains(s))
				return (left.getResultsize(boundVariables) + right.getResultsize(boundVariables)) / 2d;
		}
		return left.getResultsize(boundVariables) * right.getResultsize(boundVariables);
	}

	@Override
	public double getCosts_() throws PlanUnfeasibleException {
		double result = getCosts__();
		if (!(left instanceof MapOperatorServiceGroup)) {
			result += left.getCosts();
		}
		if (!(right instanceof MapOperatorServiceGroup)) {
			result += right.getCosts();
		}
		return result;
	}

	public abstract double getCosts__() throws PlanUnfeasibleException;

	@Override
	public boolean isCompatible(MapPlanOperatorBase pob) {
		if (pob.getServiceGroups().containsAll(this.getServiceGroups()) && this.getServiceGroups().containsAll(pob.getServiceGroups()))
			return true;
		else
			return false;
	}

	@Override
	public Set<MapServiceGroup> getServiceGroups_() {
		Set<MapServiceGroup> sgs = new HashSet<MapServiceGroup>(left.getServiceGroups());
		sgs.addAll(right.getServiceGroups());
		return sgs;
	}
}

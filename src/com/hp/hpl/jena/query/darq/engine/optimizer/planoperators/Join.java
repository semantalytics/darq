package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.optimizer.PlanUnfeasibleException;
import com.hp.hpl.jena.query.engine1.PlanElement;

public abstract class Join extends PlanOperatorBase{

	private PlanOperatorBase left;

	private PlanOperatorBase right;

	private Set<String> joinedVariables ;


	public Join(PlanOperatorBase left, PlanOperatorBase right)
	{
		this.left = left;
		this.right = right;
		

		// joinedVariables - intersection of left and right variables...
		this.joinedVariables= new HashSet<String>(left.getBoundVariables());
		this.joinedVariables.retainAll(right.getBoundVariables());

	}

	
	public Set<String> getJoinedVariables() {
		return joinedVariables;
	}



	public PlanOperatorBase getLeft() {
		return left;
	}

	public void setLeft(PlanOperatorBase left) {
		this.left = left;
	}

	

	public PlanOperatorBase getRight() {
		return right;
	}

	public void setRight(PlanOperatorBase right) {
		this.right = right;
	}

	@Override
	public Set<String> getBoundVariables_() {
		Set<String> bound = new HashSet<String>(left.getBoundVariables());
		bound.addAll(right.getBoundVariables());
		return bound;
	}



	@Override
	public double getResultsize(Set<String> boundVariables) throws PlanUnfeasibleException {
		for (String s:left.getBoundVariables()) {
			if (right.getBoundVariables().contains(s)) return (left.getResultsize(boundVariables)+right.getResultsize(boundVariables))/2d;
		}
		return left.getResultsize(boundVariables)*right.getResultsize(boundVariables);
	}
	
	

	@Override
	public double getCosts_() throws PlanUnfeasibleException {
		double result = getCosts__();
		if (! (left instanceof OperatorServiceGroup)) {
			result += left.getCosts();
		}
		if (! (right instanceof OperatorServiceGroup)) {
			result += right.getCosts();
		}
		return result;
	}
	
	public abstract double getCosts__() throws PlanUnfeasibleException ;

	@Override
	public boolean isCompatible(PlanOperatorBase pob) {
		// TODO Auto-generated method stub
		if (pob.getServiceGroups().containsAll(this.getServiceGroups()) && this.getServiceGroups().containsAll(pob.getServiceGroups())) return true;
		else return false;
	}

	@Override
	public Set<ServiceGroup> getServiceGroups_() {
		Set<ServiceGroup> sgs = new HashSet<ServiceGroup>(left.getServiceGroups());
		sgs.addAll(right.getServiceGroups());
		return sgs;
	}
	
	
	
	

}

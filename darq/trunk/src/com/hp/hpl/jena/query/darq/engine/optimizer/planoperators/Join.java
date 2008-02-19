package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.engine1.PlanElement;

public abstract class Join extends PlanOperatorBase{

	protected PlanOperatorBase left;

	protected PlanOperatorBase right;

	private Set<String> joinedVariables ;
	
	private int cachedHight =  -99;


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
	
	
	@Override
	public int getHight() {
		if (cachedHight==-99) cachedHight=Math.max(getLeft().getHight(), getRight().getHight())+1; 
		return cachedHight;
	}
	

}

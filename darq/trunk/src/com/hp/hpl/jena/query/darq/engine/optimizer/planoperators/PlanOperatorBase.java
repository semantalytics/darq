package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;

import java.util.Set;

import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.optimizer.PlanUnfeasibleException;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.util.Context;

public abstract class PlanOperatorBase implements Comparable<PlanOperatorBase>{

	/**
	 * costs per result
	 */
	public final static double CT = 5;

	/**
	 * costs per request
	 */
	public final static double CR = 20;

	private double cachedResultSize=-1;

	private double cachedCosts = -1;
	
	private Set<ServiceGroup> serviceGroups = null;
	
	private Set<String> boundVariables = null;
//	public final static double SEL = 1 / 100; // TODO FIXME : Replace with
												// better estimation!!

	public abstract double getCosts_() throws PlanUnfeasibleException;
	
	public double getCosts() throws PlanUnfeasibleException {
		if (cachedCosts ==-1) cachedCosts = getCosts_();
		return cachedCosts;
	}

	public abstract double getResultsize(Set<String> boundVariables)
			throws PlanUnfeasibleException;
	
	public  double getResultsize() 	throws PlanUnfeasibleException {
			if (cachedResultSize==-1) cachedResultSize = getResultsize(null);  
			return cachedResultSize;
		
	}
	
	public  Set<String> getBoundVariables() {
		if (boundVariables==null) boundVariables= getBoundVariables_();
		return boundVariables; 
	}
	public abstract Set<String> getBoundVariables_();

	public abstract boolean isCompatible(PlanOperatorBase pob);

	public  Set<ServiceGroup> getServiceGroups() {
		if (serviceGroups==null) serviceGroups= getServiceGroups_();
		return serviceGroups; 
	}
	
	public abstract Set<ServiceGroup> getServiceGroups_();

	public abstract void visit(PlanOperatorVisitor visitor);

	public int compareTo(PlanOperatorBase pob) {
		try {
			return new Double(this.getCosts()-pob.getCosts()).intValue();
		} catch (PlanUnfeasibleException e) {
		
		}
		return 0;
	}
	public boolean overlaps(PlanOperatorBase p2) {
		for (ServiceGroup sg:getServiceGroups()) if (p2.getServiceGroups().contains(sg)) return true;
		return false;
	}
	
	
	public boolean joins(PlanOperatorBase p2) {
		for (String v:getBoundVariables()) if (p2.getBoundVariables().contains(v)) return true;
		return false;
	}
	
	@Override
	public String toString() {
		
		try {
			return "(Costs: "+getCosts()+")"+super.toString();
		} catch (PlanUnfeasibleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return super.toString();
	}
	
	public abstract PlanElement toARQPlanElement(Context context);
	
	/**
	 * Can this Operator be the right element of an bind join operator?
	 * i.e. do the estimates change when variables are bound?
	 * @return false - only left side...
	 */
	public abstract boolean canBeRight();
	
	public int size() {
		return getServiceGroups().size();
	}
	
	
}

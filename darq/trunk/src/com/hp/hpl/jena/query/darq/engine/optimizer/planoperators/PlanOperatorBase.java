package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;

import java.util.Set;

import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.core.UnionServiceGroup;
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
	public final static double CR = 10;

	 double cachedResultSize=-1;

	 double cachedCosts = -1;
	
	 Set<ServiceGroup> serviceGroups = null;
	
	 Set<String> boundVariables = null;
//	public final static double SEL = 1 / 100; // TODO FIXME : Replace with
												// better estimation!!

	public abstract double getCosts_(Set<String> bound) throws PlanUnfeasibleException;
	
	public double getCosts() throws PlanUnfeasibleException {
		return getCosts(null);
	}
	
	public double getCosts(Set<String> bound) throws PlanUnfeasibleException {
		if (cachedCosts ==-1 || bound!=null) cachedCosts = getCosts_(bound);
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
			return new Double(this.getCosts(null)-pob.getCosts(null)).intValue();
		} catch (PlanUnfeasibleException e) {
		
		}
		return 0;
	}
	public boolean overlaps(PlanOperatorBase p2) {
		for (ServiceGroup sg:getServiceGroups()) if (p2.getServiceGroups().contains(sg)) return true;
		return false;
	}
	
	/* FRAGE ist es möglich, dass mehrere SGs in einer POB sind? 
	 * Habe jetzt nichts gegenteiliges gefunden! 
	 * Bastian!
	 * 
	 * Logik: Es darf alles gejoint werden ausser es handelt sich um 
	 *        zwei USGs mit dem selben similar. 
	 * */
	public boolean joins(PlanOperatorBase p2) {
		Set<ServiceGroup> serviceGroupsP1 = this.getServiceGroups();
		Set<ServiceGroup> serviceGroupsP2 = p2.getServiceGroups();
		int similarGroupP1 = -1;
		int similarGroupP2 = -2;
		
		if (serviceGroupsP1.size() > 1 || serviceGroupsP2.size()>1 )
			System.out.println("Error [PlanOperatorBase] More than one (Union-/Multiple-)Servicegroup in a OperatorServiceGroup.");
		
		if (serviceGroupsP1.iterator().next() instanceof UnionServiceGroup && 
				serviceGroupsP2.iterator().next() instanceof UnionServiceGroup) {
			UnionServiceGroup usg1 = (UnionServiceGroup) serviceGroupsP1.iterator().next();
			similarGroupP1 = usg1.getSimilar();
			UnionServiceGroup usg2 = (UnionServiceGroup) serviceGroupsP2.iterator().next();
			similarGroupP2 = usg2.getSimilar();
		}
		
		if (similarGroupP1 != similarGroupP2) {
			for (String v : getBoundVariables()) {
				if (p2.getBoundVariables().contains(v)) {
					return true;
				}
			}
		}
		return false;
	}	
	
	@Override
	public String toString() {
		
		try {
			return "(Costs: "+getCosts(null)+")"+super.toString();
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
	
	public abstract PlanOperatorBase clone();
	
	public abstract int getHight();
	
}

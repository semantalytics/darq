package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.optimizer.PlanUnfeasibleException;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.plan.PlanGroup;
import com.hp.hpl.jena.query.util.Context;

public class BindJoin extends Join {

	public BindJoin(PlanOperatorBase left, PlanOperatorBase right) {
		super(left, right);
		
	}

	

	
/*	public double getCosts__() throws PlanUnfeasibleException {
		//R(q_1) c_t + R(q_1) c_r + R(q_2') c_t$,
		return getLeft().getResultsize()*CT+getLeft().getResultsize()*CR+getRight().getResultsize(getLeft().getBoundVariables())*CT;
	}*/
	
	public double getCosts__(Set <String> bound) throws PlanUnfeasibleException {
		//R(q_1) c_t + R(q_1) c_r + R(q_2') c_t$,
		Set<String> b= new HashSet<String>(getLeft().getBoundVariables());
		if (bound != null) b.addAll(bound);
		return getLeft().getResultsize(bound)*CT+getLeft().getResultsize(bound)*CR+getRight().getResultsize(b)*CT;
	}

	@Override
	public void visit(PlanOperatorVisitor visitor) {
		visitor.visit(this);
		
	}

	

	@Override
	public boolean canBeRight() {
		return false;
	}



	@Override
	public PlanElement toARQPlanElement(Context context) {
		List<PlanElement> l= new ArrayList<PlanElement>();
		
		PlanElement le = getLeft().toARQPlanElement(context);
		PlanElement re = getRight().toARQPlanElement(context);
		/*if (le instanceof PlanGroup ) {
			l.addAll((List<PlanElement>)((PlanGroup)le).getSubElements());
		} else l.add(le);
		if (re instanceof PlanGroup ) {
			l.addAll((List<PlanElement>)((PlanGroup)re).getSubElements());
		} else l.add(re);*/
		l.add(le);
		l.add(re);
		return PlanGroup.make(context, l, false);
	}



	@Override
	public PlanOperatorBase clone() {
		return new BindJoin(getLeft().clone(),getRight().clone());
	}



	@Override
	/*public double getCosts_() throws PlanUnfeasibleException {
		double result = getCosts__();
		if (! (left instanceof OperatorServiceGroup)) {
			result += left.getCosts();
		}
		if (! (right instanceof OperatorServiceGroup)) {
			if (this instanceof BindJoin) ((BindJoin)right).getCosts__(left.getBoundVariables());
			else result += right.getCosts();
		}
		return result;
	}*/

	public double getCosts_(Set<String> bound) throws PlanUnfeasibleException {
		double result = getCosts__(bound);
		if (! (left instanceof OperatorServiceGroup)) {
			 result += left.getCosts(bound);
		}
		if (! (right instanceof OperatorServiceGroup)) {
			Set<String> b = new HashSet<String>(getLeft().getBoundVariables());
			if (bound!=null) b.addAll(bound);
			result += getRight().getCosts(b);
		}
		return result;
	}




	@Override
	public double getResultsize(Set<String> boundVariables) throws PlanUnfeasibleException {
		for (String s:left.getBoundVariables()) {
			if (right.getBoundVariables().contains(s)) return (left.getResultsize(boundVariables)+right.getResultsize(boundVariables))/2d;
		}
		return left.getResultsize(boundVariables)*right.getResultsize(boundVariables);
	}


	




	

}

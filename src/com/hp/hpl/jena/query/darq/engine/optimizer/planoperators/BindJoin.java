package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;

import java.util.ArrayList;
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

	

	@Override
	public double getCosts__() throws PlanUnfeasibleException {
		//R(q_1) c_t + R(q_1) c_r + R(q_2') c_t$,
		return getLeft().getResultsize()*CT+getLeft().getResultsize()*CR+getRight().getResultsize(getLeft().getBoundVariables())*CT;
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
		l.add(getLeft().toARQPlanElement(context));
		l.add(getRight().toARQPlanElement(context));
		return PlanGroup.make(context, l, false);
	}	
	
	

}

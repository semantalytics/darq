package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;

import com.hp.hpl.jena.query.darq.engine.compiler.PlanNestedLoopJoin;
import com.hp.hpl.jena.query.darq.engine.optimizer.PlanUnfeasibleException;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.util.Context;

public class MapNestedLoopJoin extends MapJoin {

	public MapNestedLoopJoin(MapPlanOperatorBase left, MapPlanOperatorBase right) {
		super(left, right);
	}

	@Override
	public double getCosts__() throws PlanUnfeasibleException {
		// $C(q_1 \bowtie q_2) = R(q_1) c_t + R(q_2) c_t + 2 c_r$
		return getLeft().getResultsize() * CT + getRight().getResultsize() * CT + 2 * CR;
	}

	@Override
	public void visit(MapPlanOperatorVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public PlanElement toARQPlanElement(Context context) {
		return PlanNestedLoopJoin.make(context, getLeft().toARQPlanElement(context), getRight().toARQPlanElement(context));
	}

	@Override
	public boolean canBeRight() {
		return getLeft().canBeRight() && getRight().canBeRight();
	}

}

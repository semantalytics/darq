package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;

public class ConvertToARQPlanVisitor implements PlanOperatorVisitor {

	public static void toARQPlan(PlanOperatorBase pob) {
		pob.visit(new PrintVisitor());
	}
	
	public void visit(NestedLoopJoin op) {
		// TODO Auto-generated method stub

	}

	public void visit(BindJoin op) {
		// TODO Auto-generated method stub

	}

	public void visit(OperatorServiceGroup op) {
		// TODO Auto-generated method stub

	}

}

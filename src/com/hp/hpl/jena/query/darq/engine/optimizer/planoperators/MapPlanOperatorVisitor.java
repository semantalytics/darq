package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;

public interface MapPlanOperatorVisitor extends PlanOperatorVisitor {

	public void visit(MapOperatorServiceGroup op) ;
	public void visit(MapNestedLoopJoin op) ;
	public void visit(MapBindJoin op) ;
	public void visit(Union op) ;
}

package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;


public interface PlanOperatorVisitor {

	public void visit(NestedLoopJoin op) ;
	public void visit(BindJoin op) ;
	public void visit(OperatorServiceGroup op) ;
}

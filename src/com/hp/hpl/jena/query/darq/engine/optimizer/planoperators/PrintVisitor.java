package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;

import com.hp.hpl.jena.query.darq.engine.optimizer.PlanUnfeasibleException;
import com.hp.hpl.jena.query.util.IndentedWriter;


public class PrintVisitor implements PlanOperatorVisitor {

	IndentedWriter out = null;
    
	public static void printPlan(PlanOperatorBase pob) {
		pob.visit(new PrintVisitor());
	}
	
	public static void printPlan(PlanOperatorBase pob, IndentedWriter writer) {
		pob.visit(new PrintVisitor(writer));
	}
	
	public PrintVisitor() {
		out = new IndentedWriter(System.out);
	}
	
	
	public PrintVisitor(IndentedWriter writer) {
		out = writer;
	}

	public void visit(NestedLoopJoin op) {
		try {
			out.println("NestedLoppJoin (" +op.getCosts_()+")");
		} catch (PlanUnfeasibleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		out.incIndent();
		op.getLeft().visit(this);
		op.getRight().visit(this);
		out.decIndent();
		out.flush();
	}

	public void visit(BindJoin op) {
		try {
			out.println("BindJoin (" +op.getCosts_()+")");
		} catch (PlanUnfeasibleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		out.incIndent();
		op.getLeft().visit(this);
		op.getRight().visit(this);
		out.decIndent();
		out.flush();
	}

	public void visit(OperatorServiceGroup op) {
		out.println(op.getServiceGroup().toString());
		out.incIndent();
		out.println(op.getServiceGroup().getTriples().toString());
		out.decIndent();
		out.flush();
	}

}

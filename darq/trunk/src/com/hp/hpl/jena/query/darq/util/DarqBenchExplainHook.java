package com.hp.hpl.jena.query.darq.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.PlanOperatorBase;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.PrintVisitor;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.util.IndentedWriter;

public class DarqBenchExplainHook implements DARQLogHook {

	public void logOptimizerExplain(PlanOperatorBase pob) {
		PrintVisitor.printPlan(pob);

	}

	public void logPlan(Query query, PlanElement planElement) {
	//	System.out.println(planElement.toString());

	}

	public void logPlanOptimized(Query query, PlanElement planElement) {
		FedPlanFormatter.out(new IndentedWriter(System.out), planElement);
	}

	public void logSubquery(Query query) {
		// TODO Auto-generated method stub

	}

}

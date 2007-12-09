package com.hp.hpl.jena.query.darq.util;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.PlanOperatorBase;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.PrintVisitor;
import com.hp.hpl.jena.query.engine1.PlanElement;

public class DarqBenchExplainHook implements DARQLogHook {

	public void logOptimizerExplain(PlanOperatorBase pob) {
		PrintVisitor.printPlan(pob);

	}

	public void logPlan(Query query, PlanElement planElement) {
		// TODO Auto-generated method stub

	}

	public void logPlanOptimized(Query query, PlanElement planElement) {
		

	}

	public void logSubquery(Query query) {
		// TODO Auto-generated method stub

	}

}

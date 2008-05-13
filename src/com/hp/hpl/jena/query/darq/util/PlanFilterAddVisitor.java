package com.hp.hpl.jena.query.darq.util;

import java.util.List;

import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanMultipleService;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanService;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanUnionService;
import com.hp.hpl.jena.query.darq.engine.compiler.PlanNestedLoopJoin;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.PlanVisitorBase;
import com.hp.hpl.jena.query.engine1.plan.PlanGroup;
import com.hp.hpl.jena.query.expr.Expr;

public class PlanFilterAddVisitor extends PlanVisitorBase implements
		FedPlanVisitor {

	List<Expr> filters = null;
	
	
	
	public PlanFilterAddVisitor(List<Expr> filters) {
		super();
		this.filters = filters;
	}

	public void visit(FedPlanService planElt) {
		planElt.getServiceGroup().addFilters(filters);

	}

	public void visit(FedPlanMultipleService planElt) {
		planElt.getServiceGroup().addFilters(filters);

	}
	
	public void visit(FedPlanUnionService planElt){
		planElt.getServiceGroup().addFilters(filters);
	}//FRAGE Eventuell nicht korrekt, da Filter aus (M)SG genommen werden müssen
	//Filter in USG wenig sinnvoll wegen der Transformation

	public void visit(PlanNestedLoopJoin planNestedLoopJoin) {
		planNestedLoopJoin.getLeft().visit(this);
		planNestedLoopJoin.getRight().visit(this);

	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.query.engine1.PlanVisitorBase#visit(com.hp.hpl.jena.query.engine1.plan.PlanGroup)
	 */
	@Override
	public void visit(PlanGroup planElt) {
		for(PlanElement p: (List<PlanElement>)planElt.getSubElements()){
			p.visit(this);
		}
	}
	
	

}

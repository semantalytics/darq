package com.hp.hpl.jena.query.darq.util;

import com.hp.hpl.jena.query.darq.engine.compiler.MapFedPlanMultipleService;
import com.hp.hpl.jena.query.darq.engine.compiler.MapFedPlanService;


public interface MapFedPlanVisitor extends FedPlanVisitor {
	/**
	 * see FedPlanVisitor
	 * @param planElt
	 */
	public void visit(MapFedPlanService planElt) ;
	public void visit(MapFedPlanMultipleService planElt) ;
}

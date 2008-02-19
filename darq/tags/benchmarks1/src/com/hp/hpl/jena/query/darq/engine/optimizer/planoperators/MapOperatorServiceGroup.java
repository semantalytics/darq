package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;


import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.compiler.MapFedPlanMultipleService;
import com.hp.hpl.jena.query.darq.engine.compiler.MapFedPlanService;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.util.Context;

public class MapOperatorServiceGroup extends OperatorServiceGroup {
	ServiceGroup sg;

	public MapOperatorServiceGroup(ServiceGroup sg) {
		super(sg);
		this.sg = sg;
	}

	
	@Override
	public PlanElement toARQPlanElement(Context context) {
		if (sg instanceof MultipleServiceGroup) {
            return MapFedPlanMultipleService.make(context, (MultipleServiceGroup) sg, null);
        } else {
            return MapFedPlanService.make(context, sg, null);
        }
	
	}
}
package com.hp.hpl.jena.query.darq.util;

import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.MapPlanOperatorBase;

public interface MapDARQLogHook extends DARQLogHook {
	public void logOptimizerExplain(MapPlanOperatorBase pob);
}

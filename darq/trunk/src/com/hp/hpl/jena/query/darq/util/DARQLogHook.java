package com.hp.hpl.jena.query.darq.util;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.engine1.PlanElement;

public interface DARQLogHook {
    
    public void logPlan(Query query, PlanElement planElement);
    public void logPlanOptimized(Query query, PlanElement planElement);
    public void logSubquery(Query query);
    
    
    
    
}

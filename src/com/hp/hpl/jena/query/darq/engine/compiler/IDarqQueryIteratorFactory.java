package com.hp.hpl.jena.query.darq.engine.compiler;

import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.PlanElement;

public interface IDarqQueryIteratorFactory {
    
    public DarqQueryIterator getNewInstance(QueryIterator input, ServiceGroup sg,
            ExecutionContext context, PlanElement subComp);

}
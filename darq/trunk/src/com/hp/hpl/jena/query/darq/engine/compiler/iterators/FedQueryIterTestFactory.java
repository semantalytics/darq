package com.hp.hpl.jena.query.darq.engine.compiler.iterators;

import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.PlanElement;

import de.hu_berlin.informatik.wbi.darq.cache.Caching;

public class FedQueryIterTestFactory implements IDarqQueryIteratorFactory {

    public DarqQueryIterator getNewInstance(QueryIterator input, ServiceGroup sg, ExecutionContext context, PlanElement subComp, Caching cache, Boolean cacheEnabled) {
        return new FedQueryIterTest(input,sg,context,subComp,cache, cacheEnabled);
    }

}

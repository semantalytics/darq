package com.hp.hpl.jena.query.darq.engine.compiler.iterators;

import com.hp.hpl.jena.query.darq.core.MapServiceGroup;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.PlanElement;

public interface MapIDarqQueryIteratorFactory extends IDarqQueryIteratorFactory {
	 
	public MapDarqQueryIterator getNewInstance(QueryIterator input, MapServiceGroup sg,
	            ExecutionContext context, PlanElement subComp);
}

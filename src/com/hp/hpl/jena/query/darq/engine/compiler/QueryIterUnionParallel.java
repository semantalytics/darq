package com.hp.hpl.jena.query.darq.engine.compiler;

import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.query.core.Binding;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterConcat;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterSingleton;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterUnion;

public class QueryIterUnionParallel extends QueryIterUnion{
    List compElements ;
    
    public QueryIterUnionParallel(QueryIterator input, List compElements, ExecutionContext context) {
        
        
            super(input, compElements,  context) ;
            this.compElements = compElements ;
     

    }

    protected QueryIterator nextStage(Binding binding)
    {
        QueryIterConcatParallel unionQIter = new QueryIterConcatParallel(getExecContext()) ;
        for ( Iterator iter = compElements.listIterator() ; iter.hasNext() ; )
        {
            PlanElement el = (PlanElement)iter.next() ;
            
            QueryIterator b = null ;
            if ( binding != null )
                b = new QueryIterSingleton(binding, getExecContext()) ;
            
            QueryIterator cIter = el.build(b, getExecContext()) ;
            unionQIter.add(cIter) ;
        }
        
        return unionQIter ;
    }
    
}

package com.hp.hpl.jena.query.darq.engine.compiler.iterators;

import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.query.engine.Binding;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterRepeatApply;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterSingleton;

public class QueryIterUnionParallel extends QueryIterRepeatApply{
    List compElements ;
    
    public QueryIterUnionParallel(QueryIterator input, List compElements, ExecutionContext context) {
    	   super(input, context) ;
        
        //    super(input, compElements,  context) ;
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

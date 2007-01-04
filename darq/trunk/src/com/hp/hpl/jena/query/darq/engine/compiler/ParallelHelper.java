package com.hp.hpl.jena.query.darq.engine.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

import com.hp.hpl.jena.query.core.Binding;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterPlainWrapper;

public class ParallelHelper implements Runnable {

    QueryIterator queryIterator = null;
    boolean finished = false;
    
    QueryIterator resultIterator = null;
    Exception exception = null;
    
    boolean error = false;
    
    public ParallelHelper(QueryIterator iterator) {
        queryIterator=iterator;
  
    }
    
    public void run() {
        
        List<Binding> newBindings = new ArrayList<Binding>();
        // TODO Auto-generated method stub
        try {
        while (queryIterator.hasNext()) {
            newBindings.add(queryIterator.nextBinding());
        }
        } catch (Exception e) {
            exception=e;
            error=true;
            
        }
        
        resultIterator = new QueryIterPlainWrapper(newBindings.iterator(), null);
        finished=true;
       
    }

    /**
     * @return the resultIterator
     * @throws Exception 
     */
    public QueryIterator getResultIterator() throws Exception {
        if (!finished) throw new Exception("");
        return resultIterator;
    }

    /**
     * @return the error
     */
    public boolean isError() {
        return error;
    }

    /**
     * @return the exception
     */
    public Exception getException() {
        return exception;
    }
    
    
    
    
    
   
    
    
    

}

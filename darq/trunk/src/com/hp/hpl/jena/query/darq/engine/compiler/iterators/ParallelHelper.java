package com.hp.hpl.jena.query.darq.engine.compiler.iterators;

import java.util.LinkedList;
import java.util.List;

import com.hp.hpl.jena.query.engine.Binding;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterPlainWrapper;

public class ParallelHelper implements Runnable {

	private QueryIterator queryIterator = null;
    private boolean finished = false;
    
    private QueryIterator resultIterator = null;
    private long resultSize =-1;
    private Exception exception = null;
    private List<Binding> results = null;
    
    private boolean error = false;
    
    public ParallelHelper(QueryIterator iterator) {
        queryIterator=iterator;
  
    }
    
    public void run() {
    	
    //	System.err.println("START " +this.toString() +" "+ queryIterator.toString());
        
        List<Binding> newBindings = new LinkedList<Binding>();
       
        try {
        while (queryIterator.hasNext()) {
            newBindings.add(queryIterator.nextBinding());
     //       System.out.println(this + " Binding added");
        }
        } catch (Exception e) {
        	//System.err.println("ERROR "+ this);
        	//e.printStackTrace();
            exception=e;
            error=true;
            
        }
        
        resultSize=newBindings.size();
        results=newBindings;
        resultIterator = new QueryIterPlainWrapper(newBindings.iterator(), null);
        finished=true;
     //   System.err.println("END " +this.toString() +" "+ queryIterator.toString());
    }

    /**
     * @return the resultIterator
     * @throws Exception 
     */
    public QueryIterator getResultIterator() throws Exception {
        if (!finished) throw new Exception("Thread still running: "+ this);
        return resultIterator;
    }
    public long getResultSize() throws Exception {
    	if (!finished) throw new Exception("Thread still running: "+ this);
        return resultSize;
    }
    
    

    public List<Binding> getResults() {
		return results;
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

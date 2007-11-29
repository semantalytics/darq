package darq;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.hyades.logging.commons.Logger;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.query.darq.core.DarqDataset;
import com.hp.hpl.jena.query.darq.engine.DarqTransform;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngine;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngineFactory;
import com.hp.hpl.jena.query.darq.engine.compiler.iterators.FedQueryIterService;
import com.hp.hpl.jena.query.darq.engine.optimizer.CostBasedBasicOptimizer;
import com.hp.hpl.jena.query.resultset.ResultSetMem;

public class DarqBench {

    final static int SLEEPTIME=1000;
    
    private static class Measures {
    	
    	public int results;
    	public double optimizerTime;
    	public double exectime;
		public Measures(int results, double optimizerTime, double exectime) {
			this.results = results;
			this.optimizerTime = optimizerTime;
			this.exectime = exectime;
		}
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
    
    	
    	
    	long transformtime = 0;
    	long exectime = 0;
    	int resultsize = 0;
    	
    	double transformtime_nop = 0;
    	double exectime_nop = 0;
    	int resultsize_nop = 0;
    	/*Logger.getLogger(DarqTransform.class).setLevel(Level.OFF);
        Logger.getLogger(FedQueryEngine.class).setLevel(Level.OFF);
        //Logger.getLogger(BasicPlanOptimizer.class).setLevel(Level.DEBUG);
        Logger.getLogger(CostBasedBasicOptimizer.class).setLevel(Level.OFF);
        Logger.getLogger(FedQueryIterService.class).setLevel(Level.OFF);*/

     
    	String configfile = args[0];
    	String queryfile = args[1];
    	double runs =5; // default = 5
    	try {
    		runs = new Double(args[2]);
    	}
    	catch (Exception e) {
    		System.err.println("Invalid number of runs - using default ("+runs+") - "+e.getMessage());
    	}
    	
    	
    	List<Measures> unoptimized = new ArrayList<Measures>((int)runs);
    	List<Measures> optimized = new ArrayList<Measures>((int)runs);
    	
    	
        FedQueryEngineFactory.register(configfile);

     
        
        
        // create model - it is used for the local parts of the query -
        Dataset ds = new DarqDataset();
        
        // get FedQueryEngineFactory
      

        long t1;
        long t2;
        long t3;
        
        ResultSet rs;
        ResultSetMem rsm;
        Query query = null;
        FedQueryEngine qe = null;
        
        // one warmup
        System.out.println("UNOptimized WARMUP");
        
        query = QueryFactory.read(queryfile, null, Syntax.syntaxSPARQL) ;
        qe = (FedQueryEngine)QueryExecutionFactory.create(query, ds);
       qe.setOptimize(false);
        t1 = System.nanoTime();
        rs = qe.execSelect();
        t2 = System.nanoTime();
        rsm = new ResultSetMem(rs);
        t3 = System.nanoTime();
        
        ResultSetFormatter.out(System.out, rsm, query);
        
        System.out.println("\n\nnot optimizing");
        for (int i=0; i < runs; i++) {
        
    
        sleep();
        // execute
         query = QueryFactory.read(queryfile, null, Syntax.syntaxSPARQL) ;
         qe = (FedQueryEngine)QueryExecutionFactory.create(query, ds);
        qe.setOptimize(false);
         t1 = System.nanoTime();
         rs = qe.execSelect();
         t2 = System.nanoTime();
         rsm = new ResultSetMem(rs);
         t3 = System.nanoTime();
        
        
         transformtime_nop += qe.getTransformTime();
         exectime_nop+= (t3-t1);
         resultsize_nop=rsm.size();
        
        unoptimized.add(new Measures(rsm.size(),qe.getTransformTime(),(t3-t1)));
        

        
        }
        
        
        transformtime = 0;
        exectime = 0;
        resultsize = 0;
        
        
        // one warmup
        
        System.out.println("Optimized WARMUP");
        
        query = QueryFactory.read(queryfile, null, Syntax.syntaxSPARQL) ;
        qe = (FedQueryEngine)QueryExecutionFactory.create(query, ds);
       qe.setOptimize(true);
        t1 = System.nanoTime();
        rs = qe.execSelect();
        t2 = System.nanoTime();
        rsm = new ResultSetMem(rs);
        t3 = System.nanoTime();
        
        ResultSetFormatter.out(System.out, rsm, query);
        System.out.println("\n\noptimizing");
        for (int i=0; i < runs; i++) {
        
    
        sleep();
        
        
        query = QueryFactory.read(queryfile, null, Syntax.syntaxSPARQL) ;
         qe = (FedQueryEngine)QueryExecutionFactory.create(query, ds);
         qe.setOptimize(true);
        // execute
         t1 = System.nanoTime();
         rs = qe.execSelect();
         t2 = System.nanoTime();
         rsm = new ResultSetMem(rs);
         t3 = System.nanoTime();
         
         transformtime += qe.getTransformTime();
         exectime+= (t3-t1);
         resultsize=rsm.size();
         
        optimized.add(new Measures(rsm.size(),qe.getTransformTime(),(t3-t1))); 
         
    
         
        }
        
        System.out.println("----------------------");
        System.out.println("---- No Optimization ----");
        System.out.println("Runs: "+ runs);
        System.out.println("Avg. Transform: " + (transformtime_nop/runs) /1000000.0);
        System.out.println("Avg. Execution: " + (exectime_nop/runs) / 1000000.0);
        System.out.println("Resultsize: "+ resultsize_nop);
        System.out.println("---- Optimization ----");
        System.out.println("Runs: "+ runs);
        System.out.println("Avg. Transform: " + (transformtime/runs)/1000000.0);
        System.out.println("Avg. Execution: " + (exectime/runs) / 1000000.0);
        System.out.println("Resultsize: "+ resultsize);
        System.out.println("----------------------");
         
    }
    
    static void sleep() {
        try
        {
           Thread.sleep(SLEEPTIME); // this number is the sleep time in milliseconds   
        }
        catch(InterruptedException e)
        {
          System.out.println("sleep error: " +e);
        }
    }

}

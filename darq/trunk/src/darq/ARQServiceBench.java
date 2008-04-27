package darq;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.query.darq.core.DarqDataset;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngine;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngineFactory;
import com.hp.hpl.jena.query.darq.util.DarqBenchExplainHook;
import com.hp.hpl.jena.query.engineHTTP.QueryEngineHTTP;

public class ARQServiceBench {

	final static int SLEEPTIME = 5000;

	private static class Measures {

		public long results;
		public double optimizerTime;
		public double exectime;

		public Measures(long results, double optimizerTime, double exectime) {
			this.results = results;
			this.optimizerTime = optimizerTime;
			this.exectime = exectime;
		}
	}

	private static List<Measures> unoptimized = null;
	private static List<Measures> optimized = null;

	private static long transformtime = 0;
	private static long exectime = 0;
	private static long resultsize = 0;

	private static long transformtime_nop = 0;
	private static long exectime_nop = 0;
	private static long resultsize_nop = 0;

	private static void run(String queryfile, String url) {
	

		long t1;
		long t2;
		long t3;

		ResultSet rs;

		Query query = null;
		long results = 0;
		QuerySolution rb = null;

		query = QueryFactory.read(queryfile, null, Syntax.syntaxSPARQL);
		QueryEngineHTTP qe = new QueryEngineHTTP(url,query);
		
	
		t1 = System.nanoTime();
		rs = qe.execSelect();
		t2 = System.nanoTime();
		while (rs.hasNext()) {
			rb = rs.nextSolution();
			results++;
		}
		t3 = System.nanoTime();

	
			unoptimized.add(new Measures(results, 0,
					(t3 - t1) ));
		
		rb = null;
		rs = null;
		qe = null;
		query = null;
		System.gc();
	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {

		/*
		 * Logger.getLogger(DarqTransform.class).setLevel(Level.OFF);
		 * Logger.getLogger(FedQueryEngine.class).setLevel(Level.OFF);
		 * //Logger.getLogger(BasicPlanOptimizer.class).setLevel(Level.DEBUG);
		 * Logger.getLogger(CostBasedBasicOptimizer.class).setLevel(Level.OFF);
		 * Logger.getLogger(FedQueryIterService.class).setLevel(Level.OFF);
		 */
	

		
		String queryfile = args[1];
		double runs = 5; // default = 5
		try {
			runs = new Double(args[2]);
		} catch (Exception e) {
			System.err.println("Invalid number of runs - using default ("
					+ runs + ")");
		}

		String url = "";
		try {
			url =args[0];
		} catch (Exception e) {
			System.err.println("wrong/missing url");
		}

	

		unoptimized = new ArrayList<Measures>((int) ( runs));
		

		
		// go
		for (int i = 0; i < runs; i++) {
			
				System.err.println("..." + i);
				run(queryfile, url);
				sleep();
		}

		System.err.println("----------------------");
		System.err.println("---- No Optimization ----");
		System.err.println("Runs: " + (2.0 * runs));
		System.err.println("Avg. Transform: "
				+ (transformtime_nop / (2.0 * runs)) / 1000000.0);
		System.err.println("Avg. Execution: " + (exectime_nop / (2.0 * runs))
				/ 1000000.0);
		System.err.println("Resultsize: " + resultsize_nop);
		System.err.println("---- Optimization ----");
		System.err.println("Runs: " + (2.0 * runs));
	

		String opt_transform = "Optimized Transform;";
		String opt_exec = "Optimized Exec;";
		String opt_total = "Optimized Total;";

		String nopt_transform = "UnOptimized Transform;";
		String nopt_exec = "UnOptimized Exec;";
		String nopt_total = "UnOptimized Total;";

		for (int i = 0; i < runs; i++) {
			
			nopt_transform += unoptimized.get(i).optimizerTime / 1000000.0
					+ ";";
		
			 nopt_exec += unoptimized.get(i).exectime / 1000000.0 + ";";

		
			 nopt_total += (unoptimized.get(i).exectime + unoptimized.get(i).optimizerTime)
					/ 1000000.0 + ";";
		}
		System.out.println(opt_transform);
		System.out.println(opt_exec);
		System.out.println(opt_total);
		System.out.println(nopt_transform);
		System.out.println(nopt_exec);
		System.out.println(nopt_total);
		System.out.println(resultsize);
	}

	static void sleep() {
		try {
			Thread.sleep(SLEEPTIME); // this number is the sleep time in
			// milliseconds
		} catch (InterruptedException e) {
			System.err.println("sleep error: " + e);
		}
	}

}

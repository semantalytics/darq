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

import de.hu_berlin.informatik.wbi.darq.cache.Caching;

public class DarqBench {

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

	private static void run(String queryfile, boolean optimize) {
		Dataset ds = new DarqDataset();

		long t1;
		long t2;
		long t3;

		ResultSet rs;

		Query query = null;
		FedQueryEngine qe = null;
		long results = 0;
		QuerySolution rb = null;

		query = QueryFactory.read(queryfile, null, Syntax.syntaxSPARQL);
		qe = (FedQueryEngine) QueryExecutionFactory.create(query, ds);
		qe.setOptimize(optimize);
		t1 = System.nanoTime();
		rs = qe.execSelect();
		t2 = System.nanoTime();
		while (rs.hasNext()) {
			rb = rs.nextSolution();
			results++;
		}
		t3 = System.nanoTime();

	

		if (optimize) {
			transformtime += qe.getTransformTime();
			exectime += (t3 - t1) - qe.getTransformTime();
			resultsize = results;
			optimized.add(new Measures(results, qe.getTransformTime(),
					(t3 - t1) - qe.getTransformTime()));
		} else {
			transformtime_nop += qe.getTransformTime();
			exectime_nop += (t3 - t1) - qe.getTransformTime();
			resultsize_nop = results;
			unoptimized.add(new Measures(results, qe.getTransformTime(),
					(t3 - t1) - qe.getTransformTime()));
		}
		rb = null;
		rs = null;
		qe = null;
		query = null;
		System.gc();
	}

	private static void explain(String queryfile) {
		Dataset ds = new DarqDataset();

		ResultSet rs;

		Query query = null;
		FedQueryEngine qe = null;

		FedQueryEngineFactory.setLoghook(new DarqBenchExplainHook());

		query = QueryFactory.read(queryfile, null, Syntax.syntaxSPARQL);
		qe = (FedQueryEngine) QueryExecutionFactory.create(query, ds);
		qe.setOptimize(false);

		rs = qe.execSelect();

		System.out.println();
		System.out.println(qe.getTransformTime() / 1000000.0);

		query = QueryFactory.read(queryfile, null, Syntax.syntaxSPARQL);
		qe = (FedQueryEngine) QueryExecutionFactory.create(query, ds);
		qe.setOptimize(true);

		rs = qe.execSelect();

		System.out.println();
		System.out.println(qe.getTransformTime() / 1000000.0);

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
		String action = args[0];
		Caching cache = Caching.getInstance("N:\\Studium\\Diplomarbeit\\Darq\\DarqCacheConfiguration.xml");
		String configfile = args[1];
		String queryfile = args[2];
		double runs = 5; // default = 5
		try {
			runs = new Double(args[3]);
		} catch (Exception e) {
			System.err.println("Invalid number of runs - using default ("
					+ runs + ")");
		}

		int type = 1; // default = 5
		try {
			type = new Integer(args[4]);
		} catch (Exception e) {
			System.err.println("doing only optimized query....");
		}

		FedQueryEngineFactory.register(configfile,null,0,cache,true);

		if (action.equals("explain")) {
			explain(queryfile);
			return;
		}

		unoptimized = new ArrayList<Measures>((int) (2 * runs));
		optimized = new ArrayList<Measures>((int) (2 * runs));

		// create model - it is used for the local parts of the query -
		Dataset ds = new DarqDataset();

		// get FedQueryEngineFactory

		ResultSet rs = null;

		Query query = null;
		FedQueryEngine qe = null;

		// one warmup
		if ((type & 1) == 1) {
			System.err.println("Optimized WARMUP");

			query = QueryFactory.read(queryfile, null, Syntax.syntaxSPARQL);
			qe = (FedQueryEngine) QueryExecutionFactory.create(query, ds);
			qe.setOptimize(true);

			rs = qe.execSelect();

			ResultSetFormatter.out(System.err, rs, query);

			sleep();
		}

		if ((type & 2) == 2) {
			System.err.println("UNOptimized WARMUP");

			query = QueryFactory.read(queryfile, null, Syntax.syntaxSPARQL);
			qe = (FedQueryEngine) QueryExecutionFactory.create(query, ds);
			qe.setOptimize(false);

			rs = qe.execSelect();

			ResultSetFormatter.out(System.err, rs, query);

			query = null;
			qe = null;
			rs = null;
			System.gc();
			sleep();
		}

		// go
		for (int i = 0; i < runs; i++) {
			if ((type & 2) == 2) {
				System.err.println("not optimizing");
				run(queryfile, false);
				sleep();
			}
			if ((type & 1) == 1) {
				System.err.println("optimizing");
				run(queryfile, true);
				sleep();
			}

		}

		for (int i = (int) runs; i < 2 * runs; i++) {

			if ((type & 1) == 1) {
				System.err.println("optimizing");
				run(queryfile, true);
				sleep();
			}

			if ((type & 2) == 2) {
				System.err.println("not optimizing");
				run(queryfile, false);
				sleep();
			}

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
		System.err.println("Avg. Transform: " + (transformtime / (2.0 * runs))
				/ 1000000.0);
		System.err.println("Avg. Execution: " + (exectime / (2.0 * runs))
				/ 1000000.0);
		System.err.println("Resultsize: " + resultsize);
		System.err.println("----------------------");

		String opt_transform = "Optimized Transform;";
		String opt_exec = "Optimized Exec;";
		String opt_total = "Optimized Total;";

		String nopt_transform = "UnOptimized Transform;";
		String nopt_exec = "UnOptimized Exec;";
		String nopt_total = "UnOptimized Total;";

		for (int i = 0; i < 2 * runs; i++) {
			if ((type&1) == 1)opt_transform += optimized.get(i).optimizerTime / 1000000.0 + ";";
			if ((type&2) == 2) nopt_transform += unoptimized.get(i).optimizerTime / 1000000.0
					+ ";";
			if ((type&1) == 1)opt_exec += optimized.get(i).exectime / 1000000.0 + ";";
			if ((type&2) == 2) nopt_exec += unoptimized.get(i).exectime / 1000000.0 + ";";

			if ((type&1) == 1)opt_total += (optimized.get(i).exectime + optimized.get(i).optimizerTime)
					/ 1000000.0 + ";";
			if ((type&2) == 2) nopt_total += (unoptimized.get(i).exectime + unoptimized.get(i).optimizerTime)
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

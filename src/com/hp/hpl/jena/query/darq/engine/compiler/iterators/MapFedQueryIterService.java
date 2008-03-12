package com.hp.hpl.jena.query.darq.engine.compiler.iterators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.darq.core.MapServiceGroup;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngineFactory;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engineHTTP.QueryExceptionHTTP;

public class MapFedQueryIterService extends MapDarqQueryIterator {
	Log log = LogFactory.getLog(FedQueryIterService.class);

	private static String TESTING_STRING = "_testing_";

	public MapFedQueryIterService(QueryIterator input, MapServiceGroup sg,
			ExecutionContext context, PlanElement subComp) {
		super(input, sg, context, subComp);
	}

	/**
	 * Query the remote Service
	 */
	protected ResultSet ExecRemoteQuery(Query q) {

		q.setBaseURI(""); // FIXME

		// System.out.println("Executing "+q);

		String url = serviceGroup.getService().getUrl();

		ResultSet remoteResults = null;
		log.trace(url + "?q=" + q);
		String defGraph = serviceGroup.getService().getGraph();
		if (defGraph != null) {
			qexec = QueryExecutionFactory.sparqlService(url, q, defGraph);
		} else {
			qexec = QueryExecutionFactory.sparqlService(url, q);
		}

		try {

			FedQueryEngineFactory.logSubquery(q);
			remoteResults = qexec.execSelect();

		} catch (QueryExceptionHTTP e) {
			throw new QueryExecException("Failed to connect to Endpoint: "
					+ url);
		} finally {

		}
		return remoteResults;
	}
}

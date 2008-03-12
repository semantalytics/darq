package com.hp.hpl.jena.query.darq.engine.compiler.iterators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.core.Var;
import com.hp.hpl.jena.query.darq.core.MapServiceGroup;
import com.hp.hpl.jena.query.engine.Binding;
import com.hp.hpl.jena.query.engine.BindingMap;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterPlainWrapper;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterRepeatApply;
import com.hp.hpl.jena.rdf.model.RDFNode;

public abstract class MapDarqQueryIterator extends QueryIterRepeatApply {

	Log log = LogFactory.getLog(DarqQueryIterator.class);

	// Node sourceNode ;
	protected PlanElement subPattern; // should be null ?

	protected MapServiceGroup serviceGroup = null;

	protected QueryExecution qexec = null;

	public MapDarqQueryIterator(QueryIterator input, MapServiceGroup sg, ExecutionContext context, PlanElement subComp) {
		super(input, context);
		// sourceNode = _sourceNode ;
		this.serviceGroup = sg;
		subPattern = subComp;
	}

	/**
	 * Query the remote Service Query q - the query to be sent
	 */
	protected abstract ResultSet ExecRemoteQuery(Query q);

	@Override
	protected QueryIterator nextStage(Binding binding) {

		MapRemoteQuery remoteQuery = new MapRemoteQuery(serviceGroup, getExecContext(), binding);

		/*
		 * long noResults = 0;
		 * 
		 * QueryIterConcat concatIterator = new
		 * QueryIterConcat(getExecContext());
		 * 
		 * while (remoteQuery.hasNextQuery(noResults)) {
		 */

		Query query = remoteQuery.getNextQuery();

		List<Binding> newBindings = new ArrayList<Binding>();
		try {
			ResultSet remoteResults = ExecRemoteQuery(query);

			while (remoteResults.hasNext()) {

				// noResults++;

				BindingMap bm = new BindingMap(binding);
				QuerySolution sol = remoteResults.nextSolution();

				for (Iterator solVars = sol.varNames(); solVars.hasNext();) {
					String varName = (String) solVars.next();

					// XXX CHECK if VARIABLE EXISTS IN BINDING !??
					RDFNode obj = sol.get(varName);
					if (obj != null)
						bm.add(Var.alloc(varName), obj.asNode());
				}

				newBindings.add(bm);

			}

			/*
			 * if (newBindings.size()>0) concatIterator.add(new
			 * QueryIterPlainWrapper(newBindings.iterator(), null));
			 *  }
			 */
		}/*
			 * catch (Exception e) { throw new ARQInternalErrorException(e); }
			 */finally {
			if (qexec != null)
				qexec.close();
		}

		return new QueryIterPlainWrapper(newBindings.iterator(), null); // new
																		// QueryIterDistinct(concatIterator,getExecContext());
	}

}

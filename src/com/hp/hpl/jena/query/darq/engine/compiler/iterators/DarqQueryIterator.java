package com.hp.hpl.jena.query.darq.engine.compiler.iterators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.core.Var;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.engine.Binding;
import com.hp.hpl.jena.query.engine.BindingMap;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterPlainWrapper;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterRepeatApply;
import com.hp.hpl.jena.rdf.model.RDFNode;

import de.hu_berlin.informatik.wbi.darq.cache.CacheKey;
import de.hu_berlin.informatik.wbi.darq.cache.Caching;

/**
 * Abstract Class DarqQueryIterator
 * 
 * @author Bastian Quilitz
 * @version $ID$
 * 
 */
public abstract class DarqQueryIterator extends QueryIterRepeatApply {

	Log log = LogFactory.getLog(DarqQueryIterator.class);

	// Node sourceNode ;
	protected PlanElement subPattern; // should be null ?

	protected ServiceGroup serviceGroup = null;

	protected Caching cache;
	protected Boolean cacheEnabled;

	// protected QueryExecution qexec=null;

	public DarqQueryIterator(QueryIterator input, ServiceGroup sg, ExecutionContext context, PlanElement subComp, Caching cache, Boolean cacheEnabled) {
		super(input, context);
		// sourceNode = _sourceNode ;
		this.serviceGroup = sg;
		this.subPattern = subComp;
		this.cache = cache;
		this.cacheEnabled = cacheEnabled;
	}

	/**
	 * Query the remote Service Query q - the query to be sent
	 */
	protected abstract ResultSet ExecRemoteQuery(Query q);

	@Override
	protected QueryIterator nextStage(Binding binding) {

		RemoteQuery remoteQuery = new RemoteQuery(serviceGroup, getExecContext(), binding);

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
		List<Binding> cacheResult = new ArrayList<Binding>();

		try {
			ResultSet remoteResults = null;
			if (cache != null) {

				 cache.output(); // TESTAUSGABE
				/* ask cache */
				cacheResult = cache.getElement(serviceGroup);

				if (!cacheResult.isEmpty()) {
					System.out.println("Cache Hit"); // TESTAUSGABE
					/* found in Cache */
					newBindings.addAll(cacheResult);
				} else {
					/* element not found in cache */
					remoteResults = ExecRemoteQuery(query);
				}
			} else {
				/* without cache aks remoteservice immediately */
				remoteResults = ExecRemoteQuery(query);
			}

			/* ask remoteservice */
			if (cacheResult.isEmpty()) {
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
			}
			
			/* adding bindings to cache */
			if (cache != null && cacheResult.isEmpty())
				cache.addElement(serviceGroup, newBindings);
			// cache.output(); //TESTAUSGABE

			/*
			 * if (newBindings.size()>0) concatIterator.add(new
			 * QueryIterPlainWrapper(newBindings.iterator(), null)); }
			 */
		}
		/*
		 * catch (Exception e) { throw new ARQInternalErrorException(e); }
		 */
		finally {
			// if (qexec!=null) qexec.close();
		}

		return new QueryIterPlainWrapper(newBindings.iterator(), null);
		// new QueryIterDistinct(concatIterator,getExecContext());
	}
}

package com.hp.hpl.jena.query.darq.engine.compiler.iterators;

import static de.hu_berlin.informatik.wbi.darq.mapping.MapSearch.SWRL_MULTIPLY;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.core.Var;
import com.hp.hpl.jena.query.darq.core.MultiplyServiceGroup;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.engine.Binding;
import com.hp.hpl.jena.query.engine.BindingMap;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterPlainWrapper;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterRepeatApply;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;

import de.hu_berlin.informatik.wbi.darq.cache.Caching;
import de.hu_berlin.informatik.wbi.darq.mapping.Rule;

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
		
		HashMap<String ,Double> multiplier = new HashMap<String ,Double>();
		
	

		try {
			ResultSet remoteResults = null;
			if (cache != null) {

				// cache.output(); // TESTAUSGABE

				/* ask cache */
				cacheResult = cache.getElement(serviceGroup);
				if (!cacheResult.isEmpty()) {
					/* found in Cache */
					
					/* transformation multiply */
					/* Logik: das Ergebnis aus dem Cache wird durchlaufen, wenn
					 * Multiply notwendig, wird das Binding ausgetauscht. */
					multiplier = getMultiplier();
					int index = 0;
					
					/* go through bindings */
					for (Iterator<Binding> bindingIter = cacheResult.iterator(); bindingIter.hasNext();) {
						Binding bindingTemp = bindingIter.next();
						/* get index of current binding, required for delete */
						index = cacheResult.indexOf(bindingTemp);
						BindingMap bm = new BindingMap(binding);

						/* go through variables of binding */
						for (Iterator variableIter = bindingTemp.vars(); variableIter.hasNext();) {
							Var var = (Var) variableIter.next();
							String varName = var.getName();
							Node obj = bindingTemp.get(Var.alloc(varName));

							if (multiplier.containsKey(varName)) {
								Literal objLiteral = (Literal) obj;
								double objvalue = objLiteral.getFloat();
								// System.out.println(objLiteral.getValue());//TESTAUSGABE
								// System.out.println(objLiteral.getDatatype());//TESTAUSGABE
								objvalue = objvalue * multiplier.get(varName);
								// BigDecimal objValueDecimal = BigDecimal.valueOf(objvalue);
								Model model = ModelFactory.createDefaultModel();
								Literal objNode = model.createTypedLiteral(objvalue);
								bm.add(Var.alloc(varName), objNode.asNode());
								cacheResult.remove(index);
								cacheResult.add(bm);
							}
						}
						
					}
//					System.out.println("Cache Hit"); // TESTAUSGABE					
					newBindings.addAll(cacheResult);
				} else {
					/* element not found in cache */
					remoteResults = ExecRemoteQuery(query);
				}
			} else {
				/* without cache, ask remoteservice immediately */
				remoteResults = ExecRemoteQuery(query);
			}

			/* ask remoteservice */
			if (cacheResult.isEmpty()) {
				while (remoteResults.hasNext()) {
						
					multiplier = getMultiplier();
					
					// noResults++;
					BindingMap bm = new BindingMap(binding);
					QuerySolution sol = remoteResults.nextSolution();

					for (Iterator solVars = sol.varNames(); solVars.hasNext();) {
						String varName = (String) solVars.next();

						// XXX CHECK if VARIABLE EXISTS IN BINDING !??
						
						RDFNode obj = sol.get(varName);
						
						/* transform multipy */
						if(multiplier.containsKey(varName)){
							Literal objLiteral = (Literal) obj;
							double objvalue = objLiteral.getFloat();
//							System.out.println(objLiteral.getValue()); //TESTAUSGABE
//							System.out.println(objLiteral.getDatatype());//TESTAUSGABE
							objvalue = objvalue *  multiplier.get(varName);
//							BigDecimal objValueDecimal = BigDecimal.valueOf(objvalue);
							Model model = ModelFactory.createDefaultModel();
							Literal objNode = model.createTypedLiteral(objvalue);						
							bm.add(Var.alloc(varName), objNode.asNode());
						}
						else if (obj != null){
							bm.add(Var.alloc(varName), obj.asNode());
						}
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
	
	private HashMap<String,Double> getMultiplier() {
		HashMap<String,Double> multiplier = new HashMap<String,Double>();
		/* preparation for multiply transform */
		
		if (serviceGroup instanceof MultiplyServiceGroup) {
			MultiplyServiceGroup muSG = (MultiplyServiceGroup) serviceGroup;

			for(Triple tripleMuSG : muSG.getTriples()){
				Node predicate = tripleMuSG.getPredicate();
				URI predicateURI = URI.create(predicate.getURI());
				Node oriPredicate = muSG.getOriginalTriple(tripleMuSG).getPredicate();
				URI  oriPredicateURI = URI.create(oriPredicate.getURI());
				double  value = 1;
				String variable = null; 

				/* get connection between variable from query and rule, get multiplier */
				for(Rule rule : muSG.getPredicateRules()){
					/* find the rule */
					if (rule.isMultiply() && rule.containsPart(predicateURI) && rule.containsPart(oriPredicateURI)) {
						Node object = tripleMuSG.getObject();
						value = rule.getPart( URI.create(SWRL_MULTIPLY)).getMultiplier();
						if(object.isVariable()){
							variable = object.toString(); 
							variable = variable.substring(1, variable.length()); 
						}
						if(rule.getPart(predicateURI).isBody()){
							multiplier.put(variable, value);
						}
						else{
							multiplier.put(variable, 1/value);
						}
					}
				}
			}
		}
		
		return multiplier; 
	}
	
}
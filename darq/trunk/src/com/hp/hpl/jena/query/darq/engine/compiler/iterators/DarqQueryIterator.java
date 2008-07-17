package com.hp.hpl.jena.query.darq.engine.compiler.iterators;

import static de.hu_berlin.informatik.wbi.darq.mapping.MapSearch.SWRL_MULTIPLY;
import static de.hu_berlin.informatik.wbi.darq.mapping.MapSearch.SWRL_STRINGCONCAT;

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
		List<Binding> BindingsOriginal = new ArrayList<Binding>();
		List<Binding> cacheResult = new ArrayList<Binding>();
		List<Binding> cacheResultTransformed  = new ArrayList<Binding>();
		HashMap<String ,Double> multiplier = new HashMap<String ,Double>();
		Boolean transform = false;
		
		try {
			ResultSet remoteResults = null;
			if (cache != null) {

				// cache.output(); // TESTAUSGABE

				/* ask cache */
				cacheResult = cache.getElement(serviceGroup);
				if (!cacheResult.isEmpty()) {
					/* found in Cache */

					/* transformation multiply */
					if (serviceGroup instanceof MultiplyServiceGroup) {
						multiplier = getMultiplier();
						int index = 0;

//						if (!multiplier.isEmpty()){
						cacheResultTransformed.addAll(cacheResult);//notwendig?


						/* go through bindings */
						for (Binding cacheBinding : cacheResult) {
							/* get index of current binding, required for delete */
							index = cacheResult.indexOf(cacheBinding);
							BindingMap bm = new BindingMap(binding);

							/* go through variables of binding */
							for (Iterator variableIter = cacheBinding.vars(); variableIter.hasNext();) {
								Var var = (Var) variableIter.next();
								String varName = var.getName();
								Node obj = cacheBinding.get(Var.alloc(varName));

								if (multiplier.containsKey(varName)) {
									/* this should work but does not */
//									Literal objLiteral = (Literal) obj;
//									double objvalue = objLiteral.getFloat();

									Node objLiteral = (Node) obj;
//									System.out.println("Wert Cache: " + objLiteral.getLiteralValue()); TESTAUSGABE
									String objValueStr = objLiteral.getLiteralValue().toString();
//									System.out.println("String: "+ objValueStr); //TESTAUSGABE
									Double objValueDouble =  Double.parseDouble(objValueStr);
									System.out.println("Double: "+ objValueDouble); //TESTAUSGABE
									objValueDouble = objValueDouble * multiplier.get(varName);
									// BigDecimal objValueDecimal = BigDecimal.valueOf(objvalue);
									Model model = ModelFactory.createDefaultModel();
									Literal objNode = model.createTypedLiteral(objValueDouble);
									bm.add(Var.alloc(varName), objNode.asNode()); /* change binding */
								}	
								else{ /* no multiplier, so keep this binding */
									bm.add(var, obj); 
								}
							}// end Vars 
							cacheResultTransformed.remove(index);
							cacheResultTransformed.add(index, bm);
						}//End Bindings 

						//TESTAUSGABE
//						for(Iterator<Binding> bnditer = cacheResultTransformed.iterator();bnditer.hasNext();){
//							Binding bnd =  bnditer.next();
//							System.out.print("Transformed Binding from Cache: ");
//							for(Iterator variableIter = bnd.vars();variableIter.hasNext();){
//								Var variable = (Var) variableIter.next();
//								System.out.print(variable+": " + bnd.get(variable));
//							}
//							System.out.println();
//						}

						newBindings.addAll(cacheResultTransformed);
					}
					else{
						/* no transformation */
						newBindings.addAll(cacheResult);
					}
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
					BindingMap bmOriginal = new BindingMap(binding);
					QuerySolution sol = remoteResults.nextSolution();

					for (Iterator solVars = sol.varNames(); solVars.hasNext();) {
						String varName = (String) solVars.next();

						// XXX CHECK if VARIABLE EXISTS IN BINDING !??
						
						RDFNode obj = sol.get(varName);
						
						/* transform multipy */
						if(multiplier.containsKey(varName)){
							bmOriginal.add(Var.alloc(varName), obj.asNode());
							transform = true;
							Literal objLiteral = (Literal) obj;
							double objvalue = objLiteral.getFloat();
							objvalue = objvalue *  multiplier.get(varName);
//							BigDecimal objValueDecimal = BigDecimal.valueOf(objvalue);
							Model model = ModelFactory.createDefaultModel();
							Literal objNode = model.createTypedLiteral(objvalue);						
							bm.add(Var.alloc(varName), objNode.asNode());
						}
						else if (obj != null){
							bm.add(Var.alloc(varName), obj.asNode());
							bmOriginal.add(Var.alloc(varName), obj.asNode());
						}
					}
					newBindings.add(bm);
					BindingsOriginal.add(bmOriginal);
				}
			}

			/* adding bindings to cache, without any transformation */
			if (cache != null && cacheResult.isEmpty()){
				if (!transform){
					cache.addElement(serviceGroup, newBindings);	
				}
				else{
					cache.addElement(serviceGroup, BindingsOriginal);
				}
			}
				
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
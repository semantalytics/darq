package com.hp.hpl.jena.query.darq.engine.compiler.iterators;

import static de.hu_berlin.informatik.wbi.darq.mapping.MapSearch.SWRL_MULTIPLY;
import static de.hu_berlin.informatik.wbi.darq.mapping.MapSearch.SWRL_STRINGCONCAT;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

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
import com.hp.hpl.jena.query.darq.core.StringConcatServiceGroup;
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
				
				/* found in Cache */
				if (!cacheResult.isEmpty()) {
					System.out.println("[DarqQueryIterator] Cache Hit");
					int index = 0;
					cacheResultTransformed.addAll(cacheResult);
					
					/* transformation multiply */
					if (serviceGroup instanceof MultiplyServiceGroup) {
						multiplier = getMultiplier();
						
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
					
					/* transform StringConcat */
					else if (serviceGroup instanceof StringConcatServiceGroup) {
						StringConcatServiceGroup scSG = (StringConcatServiceGroup) serviceGroup;
						HashMap<Integer, String> variableValues= new HashMap<Integer, String>();
						/* go through bindings */
						for (Binding cacheBinding : cacheResult) {
							
							/* get index of current binding, required for delete */
							index = cacheResult.indexOf(cacheBinding);
							BindingMap bm = new BindingMap(binding);
							
							String scObjValue="";
							String originalVariable;
							Integer countVariables=0;
							
							/* go through variables of binding */
							for (Iterator variableIter = cacheBinding.vars(); variableIter.hasNext();) {
								Var var = (Var) variableIter.next();
								String varName = var.getName();
								Node obj = cacheBinding.get(Var.alloc(varName));
								
								/* concat */
								if(scSG.isConcat()&& !scSG.getTripleInHead()){
									HashMap<String, List<String>> concatVariables = getStringConcatVariables();
//									List<String> conVariables = concatVariables.get("concat"); 
									HashMap<Integer, String> conVariables = scSG.getVariablesOrderedByRule(); 
									
									Integer realIndex =0;
									if (!conVariables.isEmpty()) {
										if (conVariables.values().contains(varName)) {
											transform = true;
//											Literal objLiteral = (Literal) obj; //Node_Literal
											String objLiteral = (String) obj.getLiteralValue();
											for(Integer variableIndex : conVariables.keySet()){
												if(conVariables.get(variableIndex).equals(varName)){
													realIndex = variableIndex;
												}
											}
											variableValues.put(realIndex, objLiteral);								
											countVariables++;
										}
										
										if (countVariables == conVariables.size()) {
											for(Integer variableIndex = 0 ;  variableIndex < variableValues.size();variableIndex++ ){
												if (scObjValue.equals("")){
													scObjValue = variableValues.get(variableIndex);
												}
												else{
													scObjValue = scObjValue + " "+ variableValues.get(variableIndex);	
												}
											}
											originalVariable = concatVariables.get("original").iterator().next();
											System.out.println("[DarqQueryIterator] new Binding: " + originalVariable + " = " + scObjValue);
											Model model = ModelFactory.createDefaultModel();
											Literal objNode = model.createTypedLiteral(scObjValue);
											bm.add(Var.alloc(originalVariable), objNode.asNode());
										}

									}	
								}
								else if(scSG.isConcat() && scSG.getTripleInHead()){
									/* no transformation, keep this binding*/
									/*ist der fall, wenn head bei StringConcat abgefragt wird*/
									newBindings.addAll(cacheResult);
								}
								
								else if(!scSG.isConcat() && scSG.getTripleInHead()){
									HashMap<String, List<String>> splitVariables = getStringSplitVariables();
									List<String> spVariables = splitVariables.get("split");
									Integer variableIndex=0;
									
									if (!spVariables.isEmpty()) {
										if (spVariables.contains(varName)) { 
											transform = true;
//											Literal objLiteral = (Literal) obj; /*hier steigt er aus */ 
											scObjValue = (String) obj.getLiteralValue();
											Pattern p = Pattern.compile(" ");
											String[] scObjValues = p.split(scObjValue);
											Model model = ModelFactory.createDefaultModel();
											Literal objNode;
											variableIndex= 0;
											HashMap<Integer, String>originalVariables = scSG.getVariablesOrderedByRule();											
											for (String newValue : scObjValues) {
												objNode = model.createTypedLiteral(newValue);
												originalVariable = originalVariables.get(variableIndex);
												System.out.println("[DarqQueryIterator] new Binding: "+ originalVariable + " = "+ objNode.toString()+", index "+index);//TESTAUSGABE
												bm.add(Var.alloc(originalVariable), objNode.asNode());
												variableIndex++;
											}
										}
										else if (obj != null) { /* scSG only with head triple (concat) and scSG with body triples (split) */
											bm.add(Var.alloc(varName), obj); //wahrscheinlich nicht notwendig, da ja vorher an cacheResulTransformed übergeben
										}
									}
									else if (obj != null) { /* scSG only with head triple (concat) and scSG with body triples (split) */
										bm.add(Var.alloc(varName), obj);
									}
								}
								else{
									newBindings.addAll(cacheResult);	
								}
								
							}
							cacheResultTransformed.remove(index);
							cacheResultTransformed.add(index, bm);
						}
						if (transform){
							newBindings.addAll(cacheResultTransformed);
						}
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

					/* StringConcat*/
					Integer countVariables=0;
					String scObjValue="";

					// noResults++;
					BindingMap bm = new BindingMap(binding);
					BindingMap bmOriginal = new BindingMap(binding);
					QuerySolution sol = remoteResults.nextSolution();
					HashMap<Integer, String> variableValues= new HashMap<Integer, String>();
					
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

						/* transform StringConcat */
						else if (serviceGroup instanceof StringConcatServiceGroup) {
							StringConcatServiceGroup scSG = (StringConcatServiceGroup) serviceGroup;
							String originalVariable;

							if (scSG.isConcat() && !scSG.getTripleInHead()) {
								/* concat */
								HashMap<String, List<String>> concatVariables = getStringConcatVariables();
								HashMap<Integer, String> conVariables = scSG.getVariablesOrderedByRule(); 
								
								//concatVariables.get("concat");
								Integer realIndex =0;
								if (!conVariables.isEmpty()) {
									if (conVariables.values().contains(varName)) {
										bmOriginal.add(Var.alloc(varName), obj.asNode());
										transform = true;
										Literal objLiteral = (Literal) obj;
										for(Integer index : conVariables.keySet()){
											if(conVariables.get(index).equals(varName)){
												realIndex = index;
											}
										}
										variableValues.put(realIndex, objLiteral.getString());								
										countVariables++;
									}
									if (countVariables == conVariables.size()) {
										for(Integer index =0 ;  index < variableValues.size();index++ ){
											if (scObjValue.equals("")){
												scObjValue = variableValues.get(index);
											}
											else{
												scObjValue = scObjValue + " "+ variableValues.get(index);	
											}
											
										}
										originalVariable = concatVariables.get("original").iterator().next();
										System.out.println("[DarqQueryIterator] new Binding: " + originalVariable + " = " + scObjValue);
										Model model = ModelFactory.createDefaultModel();
										Literal objNode = model.createTypedLiteral(scObjValue);
										bm.add(Var.alloc(originalVariable), objNode.asNode());
									}
								}
							}
							/* split */
							else if (!scSG.isConcat() && scSG.getTripleInHead()){
								
								HashMap<String, List<String>> splitVariables = getStringSplitVariables();
								List<String> spVariables = splitVariables.get("split");
								Integer index=0;
								
								if (!spVariables.isEmpty()) {
									if (spVariables.contains(varName)) { 
										bmOriginal.add(Var.alloc(varName), obj.asNode());
										transform = true;
										Literal objLiteral = (Literal) obj;
										scObjValue = objLiteral.getString();
										Pattern p = Pattern.compile(" ");
										
										String[] scObjValues = p.split(scObjValue);
										Model model = ModelFactory.createDefaultModel();
										Literal objNode;
										index= 0;
										HashMap<Integer, String>originalVariables = scSG.getVariablesOrderedByRule();											
										for (String newValue : scObjValues) {
											objNode = model.createTypedLiteral(newValue);
											originalVariable = originalVariables.get(index);
											System.out.println("[DarqQueryIterator] new Binding: "+ originalVariable + " = "+ objNode.toString()+", index "+index);//TESTAUSGABE
											bm.add(Var.alloc(originalVariable), objNode.asNode());
											index++;
										}
									}
									else if (obj != null) { /* scSG only with head triple (concat) and scSG with body triples (split) */
										bm.add(Var.alloc(varName), obj.asNode());
										bmOriginal.add(Var.alloc(varName), obj.asNode());
									}
								}
							}
							else if (obj != null) { /* scSG only with head triple (concat) */
								bm.add(Var.alloc(varName), obj.asNode());
								bmOriginal.add(Var.alloc(varName), obj.asNode());
							}
						} 
						else if (obj != null) {
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
	
	private HashMap<String,List<String>> getStringConcatVariables() {
//		List<String> scVariables = new ArrayList<String>();
		List<String> originalVariables = new ArrayList<String>();
		HashMap<String, List<String>> concatVariables = new HashMap<String, List<String>>();
//		String concat = null;
		if (serviceGroup instanceof StringConcatServiceGroup) {
			StringConcatServiceGroup scSG = (StringConcatServiceGroup) serviceGroup;			
			Triple originalTriple = scSG.getOriginalTriple(scSG.getTriples().iterator().next());
			Node object = originalTriple.getObject();
			if (object.isVariable()){
				String variable = object.toString(); 
				variable = variable.substring(1, variable.length());
				originalVariables.add( variable);
				concatVariables.put("original", originalVariables);
			}
//			for (Rule rule : scSG.getPredicateRules()) {
//				if (rule.isStrincConcat()) {
//					if (scSG.isConcat()) {
//						/* concat */
//						concat = "concat";
//						for(Triple triple : scSG.getTriples()){
//							Node tripleObject = triple.getObject();
//							if (tripleObject.isVariable()){
//								String variable = tripleObject.toString(); 
//								variable = variable.substring(1, variable.length()); 
//								scVariables.add(variable);	
//							}							
//						}	
//					} 
//				} 
//			}
		}
//		concatVariables.put(concat, scVariables);
		return concatVariables;
	}
	
	private HashMap<String, List<String>> getStringSplitVariables(){
		List<String> spVariables = new ArrayList<String>();
		HashMap<String, List<String>> splitVariables = new HashMap<String, List<String>>();
		if (serviceGroup instanceof StringConcatServiceGroup) {
			StringConcatServiceGroup scSG = (StringConcatServiceGroup) serviceGroup;
			/* es kann nur eins sein */
			Triple triple  = scSG.getTriples().iterator().next();
			if(triple.getObject().isVariable()){
				String variable = triple.getObject().getName();
				spVariables.add(variable);
			}
			
		}
		splitVariables.put("split", spVariables);
		return splitVariables;
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
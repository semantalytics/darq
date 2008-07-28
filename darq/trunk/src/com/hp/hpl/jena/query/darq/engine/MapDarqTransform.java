/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.engine;

import static de.hu_berlin.informatik.wbi.darq.mapping.MapSearch.SWRL_MULTIPLY;
import static de.hu_berlin.informatik.wbi.darq.mapping.MapSearch.SWRL_STRINGCONCAT;

import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owl.model.OWLOntology;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryBuildException;
import com.hp.hpl.jena.query.core.ElementBasicGraphPattern;
import com.hp.hpl.jena.query.core.Var;
import com.hp.hpl.jena.query.darq.config.Configuration;
import com.hp.hpl.jena.query.darq.config.ServiceRegistry;
import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.MultiplyMultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.MultiplyServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.core.StringConcatMultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.StringConcatServiceGroup;
import com.hp.hpl.jena.query.darq.core.UnionServiceGroup;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanMultipleService;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanService;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanUnionService;
import com.hp.hpl.jena.query.darq.engine.compiler.PlanGroupDarq;
import com.hp.hpl.jena.query.darq.engine.optimizer.PlanUnfeasibleException;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.PlanOperatorBase;
import com.hp.hpl.jena.query.engine.Plan;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.plan.PlanBasicGraphPattern;
import com.hp.hpl.jena.query.engine1.plan.PlanBlockTriples;
import com.hp.hpl.jena.query.engine1.plan.PlanFilter;
import com.hp.hpl.jena.query.engine1.plan.PlanGroup;
import com.hp.hpl.jena.query.engine1.plan.TransformCopy;
import com.hp.hpl.jena.query.util.Context;

import de.hu_berlin.informatik.wbi.darq.cache.Caching;
import de.hu_berlin.informatik.wbi.darq.mapping.MapSearch;
import de.hu_berlin.informatik.wbi.darq.mapping.Rule;
import de.hu_berlin.informatik.wbi.darq.mapping.RulePart;


public class MapDarqTransform extends TransformCopy {

	Log log = LogFactory.getLog(DarqTransform.class);

	protected Plan plan = null;
	private Context context = null;
	private ServiceRegistry registry = null;
	private Configuration config = null;
	private OWLOntology mapping = null;
	private Caching cache;
	private Boolean cacheEnabled;
	private Set<Triple> triples = new HashSet<Triple>();
	//collect new generated (similar) triples
	
	DefaultMutableTreeNode root;
	DefaultTreeModel tree;
	
	private HashMap<Triple, Integer> similarTripleMap = new HashMap<Triple, Integer>();
	//collects similar triples accessed by a triple, same Integer = similar Triple 
	
	private int transitivityDepth = 1; 

	// Subject
	private Set<URI> subjectSubClass = new HashSet<URI>();
	private Set<URI> subjectEquClass = new HashSet<URI>();
	private Set<URI> subjectSubProp = new HashSet<URI>();
	private Set<URI> subjectEquProp = new HashSet<URI>();
	private Set<URI> subjectSameIndi = new HashSet<URI>();
	private boolean subjectVariable = false;
	private HashMap<URI, HashSet<Rule>> foundRulesForSubject = new HashMap<URI, HashSet<Rule>>();
	private HashMap<URI, HashSet<Rule>> allFoundRulesForSubject = new HashMap<URI, HashSet<Rule>>();
	
	// Predicate
	private Set<URI> predicateSubClass = new HashSet<URI>();
	private Set<URI> predicateEquClass = new HashSet<URI>();
	private Set<URI> predicateSubProp = new HashSet<URI>();
	private Set<URI> predicateEquProp = new HashSet<URI>();
	private Set<URI> predicateSameIndi = new HashSet<URI>();
	private boolean predicateVariable = false;
	private HashMap<URI, HashSet<Rule>> foundRulesForPredicate = new HashMap<URI, HashSet<Rule>>();
	private HashMap<URI, HashSet<Rule>> allFoundRulesForPredicate = new HashMap<URI, HashSet<Rule>>();
	
	// Object
	private Set<URI> objectSubClass = new HashSet<URI>();
	private Set<URI> objectEquClass = new HashSet<URI>();
	private Set<URI> objectSubProp = new HashSet<URI>();
	private Set<URI> objectEquProp = new HashSet<URI>();
	private Set<URI> objectSameIndi = new HashSet<URI>();
	private boolean objectVariable = false;
	private HashMap<URI, HashSet<Rule>> foundRulesForObject = new HashMap<URI, HashSet<Rule>>();
	private HashMap<URI, HashSet<Rule>> allFoundRulesForObject = new HashMap<URI, HashSet<Rule>>();
	//	            URI from Object

	
	private HashMap<URI, Rule> multiply = new HashMap<URI, Rule>();

	// The return stack
	private Stack<PlanElement> retStack = new Stack<PlanElement>();

	HashMap<RemoteService, ServiceGroup> groupedTriples = new HashMap<RemoteService, ServiceGroup>();

	HashMap<Triple, MultipleServiceGroup> queryIndividuallyTriples = new HashMap<Triple, MultipleServiceGroup>();

	HashMap<Integer, UnionServiceGroup> unionService = new HashMap<Integer, UnionServiceGroup>();
	
	List<ServiceGroup> sgsPos = new LinkedList<ServiceGroup>();

	HashMap<Rule, ServiceGroup> scServiceGroup = new HashMap<Rule, ServiceGroup>();
	Triple scTripleInHead=null;
	
	HashMap<Integer, String> splitVariables = new HashMap<Integer,String>();
	Integer numberOfRuleParts;
	
	Set varsMentioned = new HashSet();

	boolean optimize = true;

	/**
	 * @return the optimize
	 */
	public boolean isOptimize() {
		return optimize;
	}

	/**
	 * @param optimize
	 *            the optimize to set
	 */
	public void setOptimize(boolean optimize) {
		this.optimize = optimize;
	}

	public MapDarqTransform(Context cntxt, Configuration conf, OWLOntology ontology, Integer transitivity, Caching cache, Boolean cacheEnabled) {
		super();
		context = cntxt;
		config = conf;
		registry = conf.getServiceRegistry();
		mapping = ontology;
		transitivityDepth = transitivity;
		this.cache = cache;
		this.cacheEnabled = cacheEnabled;
		log.debug("Konstruktor: MapDarqTransform");//TESTAUSGABE
	}

	
	public PlanElement transform(PlanBasicGraphPattern planElt, List newElts,PlanElement parent) {
		/*
		 * We do this only if we have a basic graph pattern with one triple:
		 * PlanGroups with one Triple will be converted to PlanBasicGraphPattern
		 * by ARQ :( We need to handle that!
		 * There are also some other cases where PlanBasicGraphPattern is not in a group... :((
		 */
		
		if (parent instanceof PlanGroup) return planElt;
	/*	 if (!( (newElts.size()==1) && (newElts.get(0) instanceof
		 PlanBlockTriples) &&
		 (((PlanBlockTriples)newElts.get(0)).getSubElements().size()==1) ) )
		 return planElt;*/
		 
		List<PlanElement> acc = new ArrayList<PlanElement>();
		acc.add(planElt);
		return make(acc, planElt.getContext());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.hpl.jena.query.engine1.plan.TransformBase#transform(com.hp.hpl.jena.query.engine1.plan.PlanBasicGraphPattern,
	 *      java.util.List)
	 */
	
	
	public PlanElement transform(PlanGroup planElt, List newElts) {
		return make(newElts, planElt.getContext());
	}

	
	/*
	 *  -- Searching for alternative Triple elements --
	 *  Idea: First run looks for similar triples, triples contains only original triple
	 *   adds similar triples to triples, runs again with original and similar triples and
	 *   looks for alternatives. As triples is a hashset no triple can be twice. 
	 *   Runs until TransitivityDepth is reached or no more similar triples added to triples
	 */
		private PlanElement make(List newElts, Context context) {
		
		log.debug("MapDarqTransform.make");//TESTAUSGABE
		groupedTriples.clear(); // new for each PlanBasicGraphPattern !
		queryIndividuallyTriples.clear(); // "
		unionService.clear();
		Set<Triple> tempTriples = new HashSet<Triple>();
		
		List<Triple> unmatchedTriples = new LinkedList<Triple>();
		List<PlanFilter> filters = new ArrayList<PlanFilter>();
		List<PlanElement> acc = new ArrayList<PlanElement>();
		
		int runs, triplesSizeBefore, similar;
		similar=0;
		URI tempURI = null;
		
		
		// planning
		for (PlanElement elm : (List<PlanElement>) newElts) {

			if (elm instanceof PlanBasicGraphPattern) {

				for (PlanElement el : (List<PlanElement>) elm.getSubElements()) {

					if (el instanceof PlanBlockTriples) {
						splitVariables.clear();
						for (Triple originalTriple : (List<Triple>) ((PlanBlockTriples) el)
								.getPattern()) {
							log.debug("[MapDarqTransform] original triple: " + originalTriple);//TESTAUSGABE							
							
							/* Subject */
							subjectSubClass.clear();
							subjectEquClass.clear(); 
							subjectSubProp.clear();
							subjectEquProp.clear();
							subjectSameIndi.clear();
							subjectVariable = false;
							foundRulesForSubject.clear();
								
							/* Predicate */
							predicateSubClass.clear();
							predicateEquClass.clear();
							predicateSubProp.clear();
							predicateEquProp.clear();
							predicateSameIndi.clear();
							predicateVariable = false;
							foundRulesForPredicate.clear();
							
							/*  Object */
							objectSubClass.clear();
							objectEquClass.clear();
							objectSubProp.clear();
							objectEquProp.clear();
							objectSameIndi.clear();
							objectVariable = false;
							foundRulesForObject.clear();
							
							//delete all triples from the run before
							triples.clear(); 
							triples.add(originalTriple);
							root = new DefaultMutableTreeNode(originalTriple);
							tree = new DefaultTreeModel(root);
							runs = 1;
							do{//transitivity
								log.debug("Durchlauf: " + runs);//TESTAUSGABE
								tempTriples.clear(); 
								tempTriples.addAll(triples); 
								triplesSizeBefore = triples.size();
								
								for(Triple similarTriple : tempTriples){
									log.debug(similarTriple);//TESTAUSGABE
									if (!similarTriple.getSubject().isVariable()){
										tempURI = URI.create(similarTriple.getSubject().getURI());
										log.debug("Subject");//TESTAUSGABE
										if (mapping.containsClassReference(tempURI)) {
											subjectSubClass = MapSearch.SearchSubclass(tempURI, mapping );
											subjectEquClass = MapSearch.searchEquivalentClass(tempURI, mapping );
											searchRulesForURI(tempURI, "subject");
											
											//call within the if makes sure the URI exists in the ontology 
										}
										if (mapping.containsObjectPropertyReference(tempURI)){
											subjectSubProp = MapSearch.searchSubObjectProperty(tempURI, mapping );
											subjectEquProp = MapSearch.searchEquivalentObjectProperty(tempURI, mapping );
											searchRulesForURI(tempURI, "subject");
										}
										if (mapping.containsDataPropertyReference(tempURI)){
											subjectSubProp = MapSearch.searchSubDataProperty(tempURI, mapping );
											subjectEquProp = MapSearch.searchEquivalentDataProperty(tempURI, mapping );
											searchRulesForURI(tempURI, "subject");
										}
										if (mapping.containsIndividualReference(tempURI)){
											subjectSameIndi = MapSearch.searchSameIndividual(tempURI, mapping);
											searchRulesForURI(tempURI, "subject");
										}
										
									}else{ subjectVariable = true;}

									if (!similarTriple.getPredicate().isVariable()){
										log.debug("Predicate");//TESTAUSGABE
										tempURI = URI.create(similarTriple.getPredicate().getURI());
//										MapSearch.AllAxiomsfromClass(URI.create(similarTriple.getPredicate().getURI()), mapping);//TESTAUSGABE
										if (mapping.containsClassReference(tempURI)){
											predicateSubClass = MapSearch.SearchSubclass(tempURI, mapping );
											predicateEquClass = MapSearch.searchEquivalentClass(tempURI, mapping );
											searchRulesForURI(tempURI, "predicate");
										}
										if (mapping.containsObjectPropertyReference(tempURI)){
											predicateSubProp = MapSearch.searchSubObjectProperty(tempURI, mapping );
											predicateEquProp = MapSearch.searchEquivalentObjectProperty(tempURI, mapping );
											searchRulesForURI(tempURI, "predicate");
										}
										if (mapping.containsDataPropertyReference(tempURI)){
											predicateSubProp = MapSearch.searchSubObjectProperty(tempURI, mapping );
											predicateEquProp = MapSearch.searchEquivalentObjectProperty(tempURI, mapping );
											searchRulesForURI(tempURI, "predicate");
										}
										if (mapping.containsIndividualReference(tempURI)){
											predicateSameIndi = MapSearch.searchSameIndividual(tempURI, mapping);
											searchRulesForURI(tempURI, "predicate");
										}
									}else{ predicateVariable = true;}

									if (!similarTriple.getObject().isVariable()){
										if(!similarTriple.getObject().isLiteral()){
											log.debug("Object");//TESTAUSGABE
											tempURI = URI.create(similarTriple.getObject().getURI());
//											MapSearch.AllAxiomsfromClass(URI.create(t.getObject().getURI()), mapping);//TESTAUSGABE						
											if (mapping.containsClassReference(tempURI)){
												objectSubClass = MapSearch.SearchSubclass(tempURI, mapping );
												objectEquClass = MapSearch.searchEquivalentClass(tempURI, mapping );
												searchRulesForURI(tempURI, "object");
											}
											if (mapping.containsObjectPropertyReference(tempURI)){
												objectSubProp = MapSearch.searchSubObjectProperty(tempURI, mapping );
												objectEquProp = MapSearch.searchEquivalentObjectProperty(tempURI, mapping );
												searchRulesForURI(tempURI, "object");
											}
											if (mapping.containsDataPropertyReference(tempURI)){
												objectSubProp = MapSearch.searchSubObjectProperty(tempURI, mapping );
												objectEquProp = MapSearch.searchEquivalentObjectProperty(tempURI, mapping );
												searchRulesForURI(tempURI, "object");
											}
											if (mapping.containsIndividualReference(tempURI)){
												objectSameIndi = MapSearch.searchSameIndividual(tempURI, mapping);
												searchRulesForURI(tempURI, "object");
											}
										}
									}else{ objectVariable = true;}
			
									addConcatVariablesToScSGI(originalTriple);

									// Creating triples with the alternative elements
									createSimilarTriples(similarTriple);
									
								}//END FOR searching similar triples
								runs++;
							}while(transitivityDepth > runs && triples.size() != triplesSizeBefore); 

							//save rules for all triple parts
							allFoundRulesForSubject.putAll(foundRulesForSubject);
							allFoundRulesForPredicate.putAll(foundRulesForPredicate);
							allFoundRulesForObject.putAll(foundRulesForObject);		
							
							
							if (triples.size() == 1){ 
								/* just the original triple */
								Triple triple = triples.iterator().next();
								List<RemoteService> services = selectServices(registry.getMatchingServices(triple));
								
								if (services.size() == 1) {
									putIntoGroupedTriples(services.get(0), triple);
								} 
								else if (services.size() > 1) {
									/*
									 * if there are more than one service, the triple has to
									 * be passed to the services individually. This is
									 * because ... TODO
									 */
									for (int j = 0; j < services.size(); j++) {
										putIntoQueryIndividuallyTriples(triple, services.get(j)); 
									}
								} 
								else {
									unmatchedTriples.add(triple);
									log.warn("No service found for statement: " + triple
											+ " - it will be queried locally.");
								}								
							} 
							else{
								/* more than one triple --> original triple + similar triple */
								for(Triple similarTriple:triples){
									log.debug("[MapDarqTransform] similar triples: "+similarTriple);//TESTAUSGABE
									List<RemoteService> services = selectServices(registry
											.getMatchingServices(similarTriple));
									/* looks into SD if resources exist (capabilities)*/
									log.debug("[MapDarqTransform] Number of services: "+services.size()); //TESTAUSGABE
									similarTripleMap.put(cloneTriple(similarTriple), similar);

									if (services.size() >=1){
										putIntoUnionServiceGroup(similarTriple,services,similar);
									}
									else {
										/* if no service found for similar triple skip this */
										log.warn("No service found for generated statement: " + similarTriple
												+ " - it will be ignored.");
									}
								} //End For similarTriple:triples
								similar++;
							}//End Else (zu if (triples.size == 1))
						}//End For originalTriple:PlanBlockTriple 	
//						outputTree(); //TESTAUSGABE			
					} else if (el instanceof PlanFilter) {
						filters.add((PlanFilter) el);
					} else {
						acc.add(0, el);
					}
				}
			} else if (elm instanceof PlanFilter) {
				filters.add((PlanFilter) elm);
			} else {
				acc.add(0, elm);
			}
		}		
		
		addConcatVariablesToScSGII(); 
		
		// add filters to service groups and to plan (filters are also applied
		// locally because we don't trust the remote services)
		for (PlanFilter f : filters) {
			acc.add(f); //local filter
			if (optimize) { // do we optimize?
				for (ServiceGroup sg : groupedTriples.values()) { 
					sg.addFilter(f.getExpr());
				}
				for (ServiceGroup sg : queryIndividuallyTriples.values()) {
					sg.addFilter(f.getExpr());
				}
				for (UnionServiceGroup usg : unionService.values()) {
					for (ServiceGroup serviceGroup : usg.getServiceGroups().values()) {
						if (serviceGroup instanceof MultipleServiceGroup) {
							MultipleServiceGroup msg = (MultipleServiceGroup) serviceGroup;
							msg.addFilter(f.getExpr()); //remote filter MSG
						} 
						else {
							serviceGroup.addFilter(f.getExpr()); // remote filter SG
						}
					}
				}
			}
		}

		// TESTAUSGABE
//		for(UnionServiceGroup usg : unionService.values()){
//			usg.output();
//		}
		
		
		// build new subplan
		if (groupedTriples.size() > 0 || queryIndividuallyTriples.size() > 0 || unionService.size() > 0) {

			/*
			 * ArrayList<ServiceGroup> al = new ArrayList<ServiceGroup>(
			 * groupedTriples.values());
			 * al.addAll(queryIndividuallyTriples.values());
			 */

			ArrayList<ServiceGroup> al = new ArrayList<ServiceGroup>(sgsPos);

			if (optimize) { // run optimizer
				PlanElement optimizedPlan = null;
				try {
					PlanOperatorBase planOperatorBase = config
							.getPlanOptimizer().getCheapestPlan(al,cache,cacheEnabled);
					FedQueryEngineFactory.logExplain(planOperatorBase);
					optimizedPlan = planOperatorBase.toARQPlanElement(context);

				} catch (PlanUnfeasibleException e) {
					throw new QueryBuildException("No feasible plan: " + e);
				}
				acc.add(0, optimizedPlan);
				// log.debug("selected: \n"
				// + optimizedPlan.toString());

			} else { // no optimization -> just add elements
				int pos = 0;
				for (ServiceGroup sg : al) {
					if (sg instanceof UnionServiceGroup){
						acc.add(pos, FedPlanUnionService.make(context, (UnionServiceGroup) sg, null,cache,cacheEnabled));
					}
					else if (sg instanceof MultipleServiceGroup) {
						acc.add(pos, FedPlanMultipleService.make(context,
								(MultipleServiceGroup) sg, null,cache,cacheEnabled));
					} else
						acc.add(pos, FedPlanService.make(context, sg, null,cache,cacheEnabled));
					pos++;

				}
			}
		}

		// unmatched patterns are executed locally
		if (unmatchedTriples.size() > 0) {
			ElementBasicGraphPattern elementBasicGraphPattern = new ElementBasicGraphPattern();
			for (Triple t : unmatchedTriples)
				elementBasicGraphPattern.addTriple(t);
			acc.add(0, PlanBasicGraphPattern.make(context,
					elementBasicGraphPattern));
		}

		PlanElement ex = PlanGroupDarq.make(context, (List) acc, false);

		// FedPlanFormatter.out(new IndentedWriter(System.out), ex);
		// System.out.println("-----");

		return ex;
	}

	private List<RemoteService> selectServices(List<RemoteService> services) {
		ArrayList<RemoteService> result = new ArrayList<RemoteService>();
		for (Iterator<RemoteService> it = services.iterator(); it.hasNext();) {
			RemoteService rs = it.next();

			// if there is a Definitive Service only use this one
			if (rs.isDefinitive()) {
				result.clear();
				result.add(rs);
				break;
			}
			result.add(rs);
		}
		return result;
	}
	
	/**
	 *  Puts triple into a ServiceGroup with its 
	 *  remote service, one or more triple, just one 
	 *  remote service possible
	 * @param s RemoteService
	 * @param t triple
	 */
	private void putIntoGroupedTriples(RemoteService s, Triple t) {
		ServiceGroup tg = groupedTriples.get(s);
		if (tg == null) {
			tg = new ServiceGroup(s);
			groupedTriples.put(s, tg);
			sgsPos.add(tg);
		}
		tg.addB(t);
	}
	
	/**
	 * Puts triple into a MultipleServiceGroup with its 
	 *  remote service, just one  but one or more 
	 *  remote services possible
	 * 
	 * @param t triple
	 * @param s remote service
	 */
	private void putIntoQueryIndividuallyTriples(Triple t, RemoteService s) {
		MultipleServiceGroup msg = queryIndividuallyTriples.get(t);
		if (msg == null) {
			msg = new MultipleServiceGroup();
			queryIndividuallyTriples.put(t, msg);
			sgsPos.add(msg); //FRAGE Kam, hinzu, wozu?
			msg.addB(t);
		}
		msg.addService(s);
	}
	
	
	/**
	 * A USG make a union of the SG within the USG.
	 * Can contain any possible ServiceGroup. It is
	 * used to answer queries with mappings. 
	 * 
	 * @param triple Triple
	 * @param services List of remote services 
	 * @param similar just an integer value
	 */
	private void putIntoUnionServiceGroup(Triple triple, List<RemoteService> services, int similar){
		
		
		UnionServiceGroup newUSG = null;
		
		ServiceGroup sg = null;
		MultipleServiceGroup msg = null;
		
		MultiplyServiceGroup muSG = null;
		MultiplyMultipleServiceGroup muMSG = null;

		StringConcatServiceGroup scSG = null;
		StringConcatMultipleServiceGroup scMSG = null;
		
		int serviceSize = services.size();
		
		Node subject, predicate, object;
		Set<Rule> subjectRules = new HashSet<Rule>();
		Set<Rule> predicateRules = new HashSet<Rule>();
		Set<Rule> objectRules = new HashSet<Rule>();
		
		subject = triple.getSubject();
		predicate = triple.getPredicate();
		object = triple.getObject();
		
		/* search for rules for every part of the triple */
		if (subject.isURI()){  
			if(!foundRulesForSubject.isEmpty()){
				URI subjectURI = URI.create(triple.getSubject().getURI());
				subjectRules =  foundRulesForSubject.get(subjectURI);	
			}
				
		}
		
		if(predicate.isURI()){
			if(!foundRulesForPredicate.isEmpty()){
				URI predicateURI = URI.create(triple.getPredicate().getURI());
				predicateRules = foundRulesForPredicate.get(predicateURI);	
			}
						
		}
		 	 	
		if(object.isURI()){
			if(!foundRulesForObject.isEmpty()){
				URI objectURI = URI.create(triple.getObject().getURI());
				objectRules = foundRulesForObject.get(objectURI);	
			}
				
		}
		/* search for an existing USG 
		 * if it is a scSG search for similarTriple within (could be a generated triple
		 * which will be replaced by the original) */
		UnionServiceGroup usg = unionService.get(similar);		
		if (usg == null) {
			for (UnionServiceGroup tempUSG : unionService.values()) {
				for (ServiceGroup tempSG : tempUSG.getServiceGroups().values()) {
					if (tempSG instanceof StringConcatServiceGroup) {
						for (Triple tempTriple : tempSG.getTriples()) {
							if (tempTriple.getSubject().equals(triple.getSubject()) && tempTriple.getPredicate().equals(triple.getPredicate())) {
								usg = tempUSG;
							}
						}
					}
				}
			}
		}
		
		/* get type of the rule*/
		String ruleType = getRuleType(subjectRules, predicateRules, objectRules);
		Triple originalTriple = (Triple) ((DefaultMutableTreeNode) tree.getRoot()).getUserObject();
		
		/* 1. USG existiert */
		if(usg != null){ 
			if (serviceSize == 1){ /* 1.1 SG */
				RemoteService service=services.get(0);
				sg = usg.getServiceGroup(service);
				/* get sc ServiceGroup, only if triple not in head */
				if (sg == null && scTripleInHead == null){
					if (!foundRulesForPredicate.isEmpty()){
						for(Rule rule : foundRulesForPredicate.get(URI.create(triple.getPredicate().getURI()))){
							sg = scServiceGroup.get(rule);	
						}
					}
				}
				/* ServiceGroup does not exist*/
				if (sg == null){
					/* if rules exists create specialized servicegroups */
					
					/* multiply */
					if (ruleType.equals(SWRL_MULTIPLY) && !triple.equals(originalTriple)){
						muSG = new MultiplyServiceGroup(service, subjectRules,predicateRules,objectRules);
						muSG.addB(triple);	
						muSG.addOrignalTriple(triple, originalTriple);
					}
					
					/* string concat */
					else if (ruleType.equals(SWRL_STRINGCONCAT) ){ 
						scSG = new StringConcatServiceGroup(service, subjectRules, predicateRules,objectRules);
						scSG.addB(triple);
						scSG.addOrignalTriple(triple, originalTriple);
						

							scSG.setVariablesOrderedByRule(splitVariables);
						
						/* concat tells if it is a split or a concat for the ServiceGroup
						 * scServiceGroup is necessary to find the USG and add triple to 
						 * the scSG (in case there are 2 original triple for scSG
						/* original Triple (from query) in Head --> concat results */
						URI originalPredicateURI=URI.create(originalTriple.getPredicate().getURI());
						for(Rule rule : foundRulesForPredicate.get(originalPredicateURI)){
							if (rule.getPart(originalPredicateURI).isHead()) {
								scSG.setConcat(true);
							}
							scServiceGroup.put(rule, scSG);
						}
						/* current triple in head ? */
						URI PredicateURI=URI.create(triple.getPredicate().getURI());
						for(Rule rule : foundRulesForPredicate.get(PredicateURI)){
							if (rule.getPart(PredicateURI).isHead()) {
								scSG.setTripleInHead(true);
							}
						}
					}
					
					/* no rule */
					else if (ruleType.equals("") || triple.equals(originalTriple)){
						sg = new ServiceGroup(service);
						sg.addB(triple);
					}
					
					/* add muSG,scSG or SG to USG */
					if(muSG != null){
						usg.addServiceGroup(triple, muSG);	
					}
					else if(scSG != null){ 
						usg.addServiceGroup(triple, scSG);
					}
					else{
						usg.addServiceGroup(triple, sg); 
					}
				}
				
				/* ServiceGroup exists */
				else if (!sg.containsTriple(triple)){  
					
					if (sg instanceof MultiplyServiceGroup) {
						muSG = (MultiplyServiceGroup) sg;
						muSG.addallRules(subjectRules, predicateRules, objectRules);
						muSG.addB(triple);  
					}
					else if (sg instanceof StringConcatServiceGroup){
						scSG = (StringConcatServiceGroup) sg;
						scSG.addallRules(subjectRules, predicateRules, objectRules);
						scSG.addOrignalTriple(triple, originalTriple);
						
						/* replace generated triple in scSG with the original triple from query */
						Triple replaceTriple=null;
						Boolean tripleAlreadyAdded = false;
						for(Triple tempTriple : scSG.getTriples()){
							if(tempTriple.getSubject().equals(triple.getSubject()) && tempTriple.getPredicate().equals(triple.getPredicate()) ){
								if(tempTriple.getObject().toString().equals("?StringConcatPart")){
									replaceTriple = tempTriple;
								}
								else if(triple.getObject().toString().equals("?StringConcatPart")){
									tripleAlreadyAdded = true;
								}
							}
						}
						if (replaceTriple !=null){
							scSG.getTriples().remove(replaceTriple);	
						}
						if (!tripleAlreadyAdded){
							scSG.addB(triple);	
						}
					}
				} 		
			}
			else if (serviceSize > 1){/* 1.2 MSG */
				for (RemoteService service:services){
					sg = usg.getServiceGroup(triple);
//					if (sg == null && scTripleInHead != null) {
//						if (!foundRulesForPredicate.isEmpty()){
//							for(Rule rule : foundRulesForPredicate.get(URI.create(triple.getPredicate().getURI()))){
//								sg = scServiceGroup.get(rule);	
//								if (!(sg instanceof StringConcatMultipleServiceGroup) && !(sg instanceof MultiplyMultipleServiceGroup) && !(sg instanceof MultipleServiceGroup)) {
//									sg = null; /*sg has to be a (sc/mu)multipleServiceGroup*/
//								}
//							}
//						}
//					}
					/* MSG does not exist */
					if (sg == null){
						/* multiply */	
						if (ruleType.equals(SWRL_MULTIPLY)&& !triple.equals(originalTriple)){
							muMSG = new MultiplyMultipleServiceGroup(service, subjectRules,predicateRules,objectRules);
							muMSG.addB(triple);
							muMSG.addOrignalTriple(triple, originalTriple);
						}
						/* string concat */ 
						if (ruleType.equals(SWRL_STRINGCONCAT)  ){
							scMSG = new StringConcatMultipleServiceGroup(service,subjectRules,predicateRules,objectRules);
							scMSG.addB(triple);
							scMSG.addOrignalTriple(triple, originalTriple);
							scMSG.setVariablesOrderedByRule(splitVariables);
							
							URI predicateURI=URI.create(originalTriple.getPredicate().getURI());
							for(Rule rule : foundRulesForPredicate.get(predicateURI)){
								if (rule.getPart(predicateURI).isHead()) {
									scMSG.setConcat(true);
								}
								scServiceGroup.put(rule, scMSG);
							}
							/* current triple in head ? */
							URI PredicateURI=URI.create(triple.getPredicate().getURI());
							for(Rule rule : foundRulesForPredicate.get(PredicateURI)){
								if (rule.getPart(PredicateURI).isHead()) {
									scMSG.setTripleInHead(true);
								}
							}
						}
						
						/* no rules */
						if (ruleType.equals("")|| triple.equals(originalTriple)){
							msg = new MultipleServiceGroup();
							msg.addB(triple);
							msg.addService(service);
						}
						
						/* add muMSG,scMSG, MSG to USG */
						if(muMSG != null){
							usg.addServiceGroup(triple, muMSG);	
						}
						else if(scMSG != null ){
							usg.addServiceGroup(triple, scMSG);
						}
						else{
							usg.addServiceGroup(triple, msg);
						}		
					}
					/* MSG does exist */
					else if (sg instanceof MultipleServiceGroup){
						msg = (MultipleServiceGroup) sg;
						msg.addService(service);
					}					
					else if (sg instanceof MultiplyMultipleServiceGroup){
						muMSG = (MultiplyMultipleServiceGroup) sg;
						muMSG.addService(service);
						/* MSG contains just one triple, so no more rules to add */
					}
					else if (sg instanceof StringConcatMultipleServiceGroup) {
						scMSG = (StringConcatMultipleServiceGroup) sg;
						scMSG.addService(service);
					}
					else {
						log.warn("This error should not occur.(ServiceSize > 1 and not (Multiply-/StringConcat-)MultipleServiceGroup)");
					}
				}
			}			
		}
		
		/* 2. USG existiert nicht */
		else if (usg == null ){ 
			if (serviceSize == 1){ /* 2.1 SG */
				RemoteService service = services.get(0);
				
				/* if rules exists create specialized ServiceGroup */
				/* multiply */
				if (ruleType.equals(SWRL_MULTIPLY) && !triple.equals(originalTriple)) {
					muSG = new MultiplyServiceGroup(service, subjectRules,predicateRules,objectRules);
					muSG.addB(triple);	
					muSG.addOrignalTriple(triple, originalTriple);
				}
				
				/* string concat */ 
				else if (ruleType.equals(SWRL_STRINGCONCAT)	 ){
					scSG = new StringConcatServiceGroup(service,subjectRules,predicateRules,objectRules);
					scSG.addB(triple);
					scSG.addOrignalTriple(triple, originalTriple);
					scSG.setVariablesOrderedByRule(splitVariables);
					URI predicateURI=URI.create(originalTriple.getPredicate().getURI());
					for(Rule rule : foundRulesForPredicate.get(predicateURI)){
						if (rule.getPart(predicateURI).isHead()) {
							scSG.setConcat(true);
						}
						scServiceGroup.put(rule, scSG);
					}
					/* current triple in head ? */
					URI PredicateURI=URI.create(triple.getPredicate().getURI());
					for(Rule rule : foundRulesForPredicate.get(PredicateURI)){
						if (rule.getPart(PredicateURI).isHead()) {
							scSG.setTripleInHead(true);
						}
					}
				}
				
				/* no rule */
				else if (ruleType.equals("")|| triple.equals(originalTriple)){
					sg = new ServiceGroup(service);
					sg.addB(triple);
				}
				
				if(muSG != null){
					newUSG = new UnionServiceGroup(triple,muSG, similar);	
				}
				else if (scSG != null){
					newUSG = new UnionServiceGroup(triple,scSG, similar);
				}
				else{
					newUSG = new UnionServiceGroup(triple,sg, similar);
				}
				unionService.put(similar, newUSG);
				sgsPos.add(newUSG);
			}
			else if (serviceSize > 1 ){ /* 2.2 MSG */
				/* at the first run USG does not exist, but in the first run a USG is created
				 * therefore a USG has to exist in the second run */
				for (RemoteService service:services){
					if (newUSG != null){
						sg = newUSG.getServiceGroup(triple);	
						
						if (sg == null && scTripleInHead == null) {
							for(Rule rule : foundRulesForPredicate.get(URI.create(triple.getPredicate().getURI()))){
								sg = scServiceGroup.get(rule);	
							}
						}
					}
					/* MSG does not exist */
					if (sg == null){
						
						/* multiply */
						if (ruleType.equals(SWRL_MULTIPLY)&& !triple.equals(originalTriple)){
							muMSG = new MultiplyMultipleServiceGroup(service, subjectRules,predicateRules,objectRules);
							muMSG.addB(triple);
							muMSG.addOrignalTriple(triple, originalTriple);
						}
						
						/* string concat */
						else if (ruleType.equals(SWRL_STRINGCONCAT) ){
							scMSG = new StringConcatMultipleServiceGroup(service,subjectRules,predicateRules,objectRules);
							scMSG.addB(triple);
							scMSG.addOrignalTriple(triple, originalTriple);							
							scMSG.setVariablesOrderedByRule(splitVariables);						
							URI predicateURI=URI.create(originalTriple.getPredicate().getURI());
							for(Rule rule : foundRulesForPredicate.get(predicateURI)){
								if (rule.getPart(predicateURI).isHead()) {
									scMSG.setConcat(true);
								}
								scServiceGroup.put(rule, scMSG);
							}
							/* current triple in head ? */
							URI PredicateURI=URI.create(triple.getPredicate().getURI());
							for(Rule rule : foundRulesForPredicate.get(PredicateURI)){
								if (rule.getPart(PredicateURI).isHead()) {
									scSG.setTripleInHead(true);
								}
							}
						}
						
						/* no rules */
						else if (ruleType.equals("")|| triple.equals(originalTriple)){
							msg = new MultipleServiceGroup();
							msg.addB(triple);
							msg.addService(service);
						}
						
						if(muMSG != null){
							newUSG = new UnionServiceGroup(triple,muMSG, similar);	
						}
						else if (scMSG != null){
							newUSG = new UnionServiceGroup(triple,scMSG, similar);
						}
						else{
							newUSG = new UnionServiceGroup(triple,msg, similar);
						}
						unionService.put(similar, newUSG);
						sgsPos.add(newUSG);
					}	
					
					/* MSG exist */
					else if (sg instanceof MultipleServiceGroup){
						msg = (MultipleServiceGroup) sg;
						msg.addService(service);
					}					
					else if (sg instanceof MultiplyMultipleServiceGroup){
						muMSG = (MultiplyMultipleServiceGroup) sg;
						muMSG.addService(service);
					}
					else if (sg instanceof StringConcatMultipleServiceGroup) {
						scMSG = (StringConcatMultipleServiceGroup) sg;
						scMSG.addService(service);
					}
					else{
						log.warn("This error should not occur.(ServiceSize > 1 and not (Multiply)MultipleServiceGroup)");				
					}
			 
				}
			}
		}			
//		usg.output(); // TESTAUSGABE
	}
	
	
	/**
	 * creates new triples with the given Sets of classes, properties and rules
	 * adds triples to a private set of the class
	 * @param Triple
	 */
	
	private void createSimilarTriples(Triple triple) {

		Set<URI> subjects = new HashSet<URI>();
		Set<URI> predicates = new HashSet<URI>();
		Set<URI> objects = new HashSet<URI>(); 
		
		scTripleInHead = null;
		Node sub = Node.NULL;
		Node pred = Node.NULL;
		Node obj = Node.NULL;
		Triple newTriple = new Triple(sub, pred, obj);
		
		DefaultMutableTreeNode treeNode = getTreeNode(triple);
		
		HashSet<Rule> rules = new HashSet<Rule>();
		HashSet<Rule> existingRules = new HashSet<Rule>();
		
		URI counterPartURI = null;
		
		Triple originalTriple = (Triple) ((DefaultMutableTreeNode) tree.getRoot()).getUserObject();
		
		if (!subjectVariable) {
			subjects.add(URI.create(triple.getSubject().getURI())); // original subject
			subjects.addAll(subjectEquClass);
			subjects.addAll(subjectSubClass);
			subjects.addAll(subjectEquProp);
			subjects.addAll(subjectSubProp);
			subjects.addAll(subjectSameIndi);		
		}

		if (!predicateVariable) {
			predicates.add(URI.create(triple.getPredicate().getURI())); // original predicate
			predicates.addAll(predicateEquClass);
			predicates.addAll(predicateSubClass);
			predicates.addAll(predicateEquProp);
			predicates.addAll(predicateSubProp);
			predicates.addAll(predicateSameIndi);
			for (URI predicateURI : foundRulesForPredicate.keySet()){
				rules = foundRulesForPredicate.get(predicateURI);
				
				for (Rule rule : rules){
					existingRules = new HashSet<Rule>();
					
					/*  multiply */
					if (rule.isMultiply()){
						log.debug("[MapDarqTransform] Counterpart multiply: " + getMultiplyCounterpart(predicateURI, rule)); //TESTAUSGABE
						counterPartURI = getMultiplyCounterpart(predicateURI, rule);
						predicates.add(counterPartURI);
						/* if there are rules for counterPartURI, get them, add current rule and 
						 * put them into found rules*/
						if (foundRulesForPredicate.get(counterPartURI) != null){
							existingRules = foundRulesForPredicate.get(counterPartURI);
						}
						existingRules.add(rule);
						foundRulesForPredicate.put(counterPartURI, existingRules);
					}
					
					/* String Concat */
					else if (rule.isStrincConcat()) {

						/* found triple in head */
						if (rule.getPart(predicateURI).isHead()) {
							scTripleInHead = triple;
							int i = 1;
							for (RulePart rulepart : rule.getBodyParts()) {
								URI scPredicateURI = rulepart.getUri();						
								if (!(scPredicateURI.toString()).equals(SWRL_STRINGCONCAT)) {
									
									newTriple = Triple.create(triple.getSubject(), Node.create(scPredicateURI.toString()), Var.alloc("StringConcat"+i));
									i++;
									log.debug("[StringConcat] " + newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());// TESTAUSGABE
									triples.add(newTriple);
									
									/* add new predicate and current rule to foundRules */
									if (foundRulesForPredicate.get(scPredicateURI) != null){
										existingRules = foundRulesForPredicate.get(scPredicateURI);
									}
									existingRules.add(rule);
									foundRulesForPredicate.put(scPredicateURI, existingRules);
									
									/* update tree */
									if (getTreeNode(newTriple) == null) {
										DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
										treeNode.add(child);
									}
								}
							}
						}

						/* found triple in body */
						else {
							for (RulePart rulepart : rule.getBodyParts()) {
								URI scPredicateURI = rulepart.getUri();
								
								if (!(scPredicateURI.toString()).equals(SWRL_STRINGCONCAT) && !scPredicateURI.equals(predicateURI)) {
									newTriple = Triple.create(triple.getSubject(), Node.create(scPredicateURI.toString()), Var.alloc("StringConcatPart"));
									log.debug("[StringConcat] " + newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());// TESTAUSGABE
									triples.add(newTriple);
									
									/* add new predicate and current rule to foundRules */
									if (foundRulesForPredicate.get(scPredicateURI) != null){
										existingRules = foundRulesForPredicate.get(scPredicateURI);
									}
									existingRules.add(rule);
									foundRulesForPredicate.put(scPredicateURI, existingRules);
									
									if (getTreeNode(newTriple) == null) {
										DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
										treeNode.add(child);
									}
								}
							}
							
							for (RulePart rulepart : rule.getHeadParts()) {
								URI scPredicateURI = rulepart.getUri();
								newTriple = Triple.create(triple.getSubject(), Node.create(scPredicateURI.toString()),Var.alloc("StringConcat"));
								log.debug("[StringConcat] " + newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());// TESTAUSGABE
								triples.add(newTriple);
								scTripleInHead = newTriple;
								
								/* add new predicate and current rule to foundRules */
								if (foundRulesForPredicate.get(scPredicateURI) != null){
									existingRules = foundRulesForPredicate.get(scPredicateURI);
								}
								existingRules.add(rule);
								foundRulesForPredicate.put(scPredicateURI, existingRules);
								
								if (getTreeNode(newTriple) == null) {
									DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
									treeNode.add(child);
								}
							}
						}
					}						
				}				
			}			
		}

		if (!objectVariable) {
			objects.add(URI.create(triple.getObject().getURI())); // original object
			objects.addAll(objectEquClass);
			objects.addAll(objectSubClass);
			objects.addAll(objectEquProp);
			objects.addAll(objectSubProp);
			objects.addAll(objectSameIndi);
		}

		if(subjects.size() == 1 && predicates.size()==1 && objects.size() == 1){}
		else{

			if (!subjectVariable) {
				if (!predicateVariable) {
					if (!objectVariable) {//Subject is bound, Predicate is bound, Object is bound

						//  ---> 1.Case SPO <---
						for (URI subject : subjects) {
							for (URI predicate : predicates) {
								for (URI object : objects) {
									newTriple = Triple.create(Node.create(subject.toString()), Node.create(predicate.toString()), Node.create(object.toString()));
									log.debug("[SPO] "+newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());// TESTAUSGABE
									triples.add(newTriple);
									if(getTreeNode(newTriple)==null){
										DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
										treeNode.add(child);	
									}								 
								}
							}
						}
					} 
					else {// Subject is bound, Predicate is bound, Object is Variable

						//  ---> 2.Case SP <---			
						for (URI subject : subjects) {
							for (URI predicate : predicates) {
								newTriple = Triple.create(Node.create(subject.toString()), Node.create(predicate.toString()), triple.getObject());
								log.debug("[SP] "+newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());//TESTAUSGABE
								triples.add(newTriple);
								if(getTreeNode(newTriple)==null){
									DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
									treeNode.add(child);	
								}
							}
						}
					}
				} 
				else {
					if (!objectVariable) {//Subject is bound, Predicate is Variable, Object is bound

						// ---> 3.Case SO <---
						log.warn("Predicate not bound in query");
						for (URI subject : subjects) {

							for (URI object : objects) {
								newTriple = Triple.create(Node.create(subject.toString()), triple.getPredicate(), Node.create(object.toString()));
								log.debug("[SO] "+newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());//TESTAUSGABE
								triples.add(newTriple);
								if(getTreeNode(newTriple)==null){
									DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
									treeNode.add(child);	
								}
							}
						}
					} 
					else {//Subject is bound, Predicate is Variable, Object is Variable

						//  ---> 4.Case S <---
						log.warn("Predicate not bound in query");
						for (URI subject : subjects) {
							newTriple = Triple.create(Node.create(subject.toString()), triple.getPredicate(), triple.getObject());
							log.debug("[S] "+newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());//TESTAUSGABE
							triples.add(newTriple);
							if(getTreeNode(newTriple)==null){
								DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
								treeNode.add(child);	
							}
						}
					}
				}
			} 
			else {
				if (!predicateVariable) {
					if (!objectVariable) {//Subject is Variable, Predicate is bound, Object is bound

						//  ---> 5.Case PO  <---
						for (URI predicate : predicates) {
							for (URI object : objects) {
								newTriple = Triple.create(triple.getSubject(), Node.create(predicate.toString()), Node.create(object.toString()));
								log.debug("[PO] "+newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());//TESTAUSGABE
								triples.add(newTriple);
								if(getTreeNode(newTriple)==null){
									DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
									treeNode.add(child);	
								}
							}
						}
					} 
					else {//Subject is Variable, Predicate is bound, Object is Variable

						//  ---> 6.Case P <---
						for (URI predicate : predicates) {
							newTriple = Triple.create(triple.getSubject(), Node.create(predicate.toString()), triple.getObject());
							log.debug("[P] "+newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());//TESTAUSGABE
							triples.add(newTriple);
							if(getTreeNode(newTriple)==null){
								DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
								treeNode.add(child);	
							}
						}
					}
				} 
				else {
					if (!objectVariable) {//Subject is Variable, Predicate is Variable, Object is bound

						//  ---> 7.Case O <---
						log.warn("Predicate not bound in query");
						for (URI object : objects) {
							newTriple = Triple.create(triple.getSubject(), triple.getPredicate(), Node.create(object.toString()));
							log.debug("[O] "+newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());//TESTAUSGABE
							triples.add(newTriple);
							if(getTreeNode(newTriple)==null){
								DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
								treeNode.add(child);	
							}
						}
					} 
					else {//Subject is Variable, Predicate is Variable, Object is Variable

						//  ---> 8.Case Nothing	 <---
						log.warn("Predicate not bound in query");
						triples.add(triple);
						if(getTreeNode(newTriple)==null){
							DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
							treeNode.add(child);	
						}
					}
				}
			}
		}
	}
	
	/**
	 *  find rules for a part of a triple given by URI
	 *  @param URI Part of triple, given by URI 
	 *  @param TriplePart defines the part of the triple (subject, predicate or object) */
	private void searchRulesForURI(URI tempURI, String triplePart){
		HashSet<Rule> rulesForURI = new HashSet<Rule>();
		rulesForURI = MapSearch.searchRules(tempURI, mapping);
		if (rulesForURI!=null){
			if (triplePart.equals("subject") && !rulesForURI.isEmpty()){
				foundRulesForSubject.put(tempURI, rulesForURI);
			}
			if (triplePart.equals("predicate")&& !rulesForURI.isEmpty()){
				foundRulesForPredicate.put(tempURI, rulesForURI);
			}
			if (triplePart.equals("object")&& !rulesForURI.isEmpty()){
				foundRulesForObject.put(tempURI, rulesForURI);
			}	
		}
	}
	 
	/**
	 * Assuming only one rule per triplePart !!!
	 * 
	 * @param subjectRules
	 * @param predicateRules
	 * @param objectRules
	 * @return Type of the rule, StringConcat or Multiply (at the moment)
	 */
	private String getRuleType(Set<Rule> subjectRules, Set<Rule> predicateRules, Set<Rule> objectRules) {
		boolean subjectMultiply = false;
		boolean predicateMultiply = false;
		boolean objectMultiply = false;
		boolean subjectStringConcat = false;
		boolean predicateStringConcat = false;
		boolean objectStringConcat = false;

		if (subjectRules !=null){ 
			for (Rule rule : subjectRules) {
				if (rule.isMultiply()) {
					subjectMultiply = true;
				}
				if (rule.isStrincConcat()) {
					subjectStringConcat = true;
				}
			}
		}
		if ( predicateRules != null ){
			for (Rule rule : predicateRules) {
				if (rule.isMultiply()) {
					predicateMultiply = true;
				}
				if (rule.isStrincConcat()) {
					predicateStringConcat = true;
				}
			}
		}
		if (objectRules!= null) {
			for (Rule rule : objectRules) {
				if (rule.isMultiply()) {
					objectMultiply = true;
				}
				if (rule.isStrincConcat()) {
					objectStringConcat = true;
				}
			}
		}
		if (subjectMultiply || predicateMultiply || objectMultiply) {
				return SWRL_MULTIPLY;
		}
		if (subjectStringConcat || predicateStringConcat || objectStringConcat) {
				return SWRL_STRINGCONCAT;
		}		
		return "";
	}
	
/**
 * 
 * @param uri URI of the triple part
 * @param rule Multiply rule
 * @return Counterpart of the triple part in head or body of the rule
 */
	private URI getMultiplyCounterpart(URI uri , Rule rule){
		RulePart original = rule.getPart(uri);
		if (original.isBody()){
			return rule.getHeadParts().iterator().next().getUri();
		}
		else{
			for(RulePart rulepart : rule.getBodyParts()){
				if(!rulepart.getType().equals(SWRL_MULTIPLY) && !rulepart.isHead()){
					return rulepart.getUri();
				}
			}
		}
		return null;
	}
	
	
	/**
	 * collects variables for concat or split to know what to split or to concat
	 * Part I
	 */
	private void addConcatVariablesToScSGI(Triple originalTriple){
		Integer tripleIndex = 0;
		numberOfRuleParts =0;
		if (!foundRulesForPredicate.isEmpty()){
			HashSet<Rule> temp  = foundRulesForPredicate.get(URI.create (originalTriple.asTriple().getPredicate().getURI()));
			if(temp != null){
			for(Rule rule : temp){
				if (rule.isStrincConcat()){
					int index =0;
					tripleIndex =0;
					for(RulePart rp: rule.getRulePartList()){
						if(rp.getUri().equals(URI.create(originalTriple.getPredicate().getURI()))){
							if(rp.isBody()){
								tripleIndex =index;
							}
							else{
								tripleIndex = null;
							}
						}
						numberOfRuleParts = index++;
					}
				}
			}
			if( originalTriple.getObject().isVariable()&& tripleIndex != null){
				splitVariables.put(tripleIndex, originalTriple.getObject().getName());
			}
		}
		}
	}
	
	/**
	 * collects variables for concat or split to know what to split or to concat
	 * Part II 
	 */
	private void addConcatVariablesToScSGII() {
		int tripleIndex =0;
		int numberOfRuleParts = 0;
		for (UnionServiceGroup usg : unionService.values()) {
			for (ServiceGroup serviceGroup : usg.getServiceGroups().values()) {
				if (serviceGroup instanceof StringConcatMultipleServiceGroup || serviceGroup instanceof StringConcatServiceGroup ) {
					StringConcatServiceGroup scSG = (StringConcatServiceGroup) serviceGroup;
					if ( !scSG.getTripleInHead()){ 
						for (Triple triple : scSG.getTriples()){
							for(Rule rule : scSG.getPredicateRules()){
								if (rule.isStrincConcat()){
									int index =0;
									tripleIndex =0;
									for(RulePart rp: rule.getRulePartList()){
										if(rp.getUri().equals(URI.create(triple.getPredicate().getURI()))){
											tripleIndex =index;
										}
										numberOfRuleParts = index++;
									}
								}
							}
							if( triple.getObject().isVariable()){
								splitVariables.put(tripleIndex, triple.getObject().getName());
							}
						}
					}

				} 
			} 
			for (ServiceGroup serviceGroup : usg.getServiceGroups().values()) {
				if (serviceGroup instanceof StringConcatMultipleServiceGroup || serviceGroup instanceof StringConcatServiceGroup ) {
					StringConcatServiceGroup scSG = (StringConcatServiceGroup) serviceGroup;
					/* minus one because of swrlb:stringConcat Part */
					if((numberOfRuleParts-1) == splitVariables.size()){
						scSG.setVariablesOrderedByRule(splitVariables);
					}
					else{
						log.warn("Stringconcatination is not possible because of missing parts, see other warning");
						scSG.setVariablesOrderedByRule(splitVariables); 
					}
						
				}
			}
		}
	}
	
	
	
	/* Deep First Search */	
	private DefaultMutableTreeNode getTreeNode(Triple triple){
		DefaultMutableTreeNode node;
		node = (DefaultMutableTreeNode) tree.getRoot();
		Triple nodeTriple = (Triple) node.getUserObject();
		if (nodeTriple.equals(triple)) return node;
		else{return getTreeNode(node,triple);}
	}

	private DefaultMutableTreeNode getTreeNode(DefaultMutableTreeNode node, Triple triple){
		Enumeration children = node.children();
		while(children.hasMoreElements()){
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
			if(child.getUserObject().equals(triple)){return child;}
			else{if (getTreeNode(child, triple) == null) {} else{return getTreeNode(child,triple);}
			}
		}
		return null;
	}
	
	private Triple cloneTriple(Triple triple){
		Triple clonedTriple = new Triple(triple.getSubject(),triple.getPredicate(),triple.getObject());
		return clonedTriple;
	}
	public int getTransitivityDepth() {
		return transitivityDepth;
	}

	public void setTransitivityDepth(int transitivityDepth) {
		this.transitivityDepth = transitivityDepth;
	}


	public HashMap<Triple, Integer> getSimilarTripleMap() {
		return similarTripleMap;
	}

	public void setSimilarTripleMap(HashMap<Triple, Integer> similarTripleMap) {
		this.similarTripleMap = similarTripleMap;
	}

	
	//TESTAUSGABE
	private void outputTree(){
		DefaultMutableTreeNode node;
		Enumeration children = root.children();
		System.out.println("Root: " + root.getUserObject().toString()); //TESTAUSGABE
		while (children.hasMoreElements()){
			outputTree((DefaultMutableTreeNode) children.nextElement(),0);
		}
	}

	private void outputTree(DefaultMutableTreeNode parents, int depth){    		
		Enumeration children = parents.children();
		depth++;
		System.out.println("[Depth " +depth+"] Child of Node: " + parents.getUserObject().toString()); //TESTAUSGABE
		while (children.hasMoreElements()){
			outputTree((DefaultMutableTreeNode) children.nextElement(), depth);
		}
		
	}
}


/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP All rights
 * reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. The name of the author may not
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
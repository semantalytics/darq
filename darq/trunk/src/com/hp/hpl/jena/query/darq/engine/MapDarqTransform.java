/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.engine;

import static de.hu_berlin.informatik.wbi.darq.mapping.MapSearch.SWRL_MULTIPLY;

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
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owl.model.OWLOntology;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryBuildException;
import com.hp.hpl.jena.query.core.ElementBasicGraphPattern;
import com.hp.hpl.jena.query.darq.config.Configuration;
import com.hp.hpl.jena.query.darq.config.ServiceRegistry;
import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
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
	
	private Set<Triple> triples = new HashSet<Triple>();
	//collect new generated (similar) triples
	
	DefaultMutableTreeNode root;
	DefaultTreeModel tree;
	
	private HashMap<Triple, Integer> similarTripleMap = new HashMap<Triple, Integer>();
	//collects similar triples accessed by a triple, same Integer = similar Triple 
	
	private int transitivityDepth = 0; 

	// Subject
	private Set<URI> subjectSubClass = new HashSet<URI>();
	private Set<URI> subjectEquClass = new HashSet<URI>();
	private Set<URI> subjectSubProp = new HashSet<URI>();
	private Set<URI> subjectEquProp = new HashSet<URI>();
	private Set<URI> subjectSameIndi = new HashSet<URI>();
	private boolean subjectVariable = false;
	
	// Predicate
	private Set<URI> predicateSubClass = new HashSet<URI>();
	private Set<URI> predicateEquClass = new HashSet<URI>();
	private Set<URI> predicateSubProp = new HashSet<URI>();
	private Set<URI> predicateEquProp = new HashSet<URI>();
	private Set<URI> predicateSameIndi = new HashSet<URI>();
	private boolean predicateVariable = false;
	
	// Object
	private Set<URI> objectSubClass = new HashSet<URI>();
	private Set<URI> objectEquClass = new HashSet<URI>();
	private Set<URI> objectSubProp = new HashSet<URI>();
	private Set<URI> objectEquProp = new HashSet<URI>();
	private Set<URI> objectSameIndi = new HashSet<URI>();
	private boolean objectVariable = false;
	
	//rules (URI from Triplepart, Rules)
	private HashMap<URI, HashSet<Rule>> foundRules = new HashMap<URI, HashSet<Rule>>();
	
	private HashMap<URI, Rule> multiply = new HashMap<URI, Rule>();

	// The return stack
	private Stack<PlanElement> retStack = new Stack<PlanElement>();

	HashMap<RemoteService, ServiceGroup> groupedTriples = new HashMap<RemoteService, ServiceGroup>();

	HashMap<Triple, MultipleServiceGroup> queryIndividuallyTriples = new HashMap<Triple, MultipleServiceGroup>();

	HashMap<Integer, UnionServiceGroup> unionService = new HashMap<Integer, UnionServiceGroup>();
	
	List<ServiceGroup> sgsPos = new LinkedList<ServiceGroup>();
	//FRAGE Bastian Was ist das, wozu?

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

	public MapDarqTransform(Context cntxt, Configuration conf, OWLOntology ontology, Integer transitivity) {
		super();
		context = cntxt;
		config = conf;
		registry = conf.getServiceRegistry();
		mapping = ontology;
		transitivityDepth = transitivity;
		System.out.println("Konstruktor: MapDarqTransform");
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
	
	
	/*FRAGE: Ist es sinnvoll stur alle Unterklassen in der Query zu ersetzen?
	 Was ist mit Unterklassen in der selben Ontologie, diese sollten keinen
	 Mehrwert bieten?! (Was ist, wenn eine andere Quelle die selbe Ontologie nutzt und
	 nur diese Unterklassen bietet?)
	 */
	
	private PlanElement make(List newElts, Context context) {
		
		System.out.println("MapDarqTransform.make");
		groupedTriples.clear(); // new for each PlanBasicGraphPattern !
		queryIndividuallyTriples.clear(); // "
		unionService.clear();
		Set<Triple> tempTriples = new HashSet<Triple>();
		
		List<Triple> unmatchedTriples = new LinkedList<Triple>();
		//PlanBlockTriples sind Tripels, die per Klammern eingeschlossen sind
		List<PlanFilter> filters = new ArrayList<PlanFilter>();
		//Filter aus der Query
		List<PlanElement> acc = new ArrayList<PlanElement>();
		//Operatoren des Plans
		
		int runs, triplesSizeBefore, similar;
		similar=0;
		URI tempURI = null;
		
		
		// planning
		for (PlanElement elm : (List<PlanElement>) newElts) {

			if (elm instanceof PlanBasicGraphPattern) {

				for (PlanElement el : (List<PlanElement>) elm.getSubElements()) {

					if (el instanceof PlanBlockTriples) {
						for (Triple originalTriple : (List<Triple>) ((PlanBlockTriples) el)
								.getPattern()) {
							System.out.println("[MapDarqTransform] original triple: " + originalTriple);//TESTAUSGABE
							
							foundRules.clear();//delete all rules from the run before
							triples.clear(); //delete all triples from the run before
							triples.add(originalTriple);
							root = new DefaultMutableTreeNode(originalTriple);
							tree = new DefaultTreeModel(root);
							runs = 1;
							do{//transitivity
								System.out.println("Durchlauf: " + runs);//TESTAUSGABE
								tempTriples.clear(); //sollte unnötig sein, schadet aber nicht
								tempTriples.addAll(triples); // brauche tempTriples, da triples in der Schleife erweitert wird und das ist schlecht, wenn
								//gleichzeitig darüber iteriert wird
								triplesSizeBefore = triples.size();
								for(Triple similarTriple : tempTriples){
									System.out.println(similarTriple);
									if (!similarTriple.getSubject().isVariable()){
										tempURI = URI.create(similarTriple.getSubject().getURI());
										System.out.println("Subject");//TESTAUSGABE
										if (mapping.containsClassReference(tempURI)) {
											subjectSubClass = MapSearch.SearchSubclass(tempURI, mapping );
											subjectEquClass = MapSearch.searchEquivalentClass(tempURI, mapping );
											searchRulesForURI(tempURI);
											
											//Aufruf stellt sicher, dass die URI in der Ontologie existiert, daher nicht ausserhalb der ifs
										}
										if (mapping.containsObjectPropertyReference(tempURI)){
											subjectSubProp = MapSearch.searchSubObjectProperty(tempURI, mapping );
											subjectEquProp = MapSearch.searchEquivalentObjectProperty(tempURI, mapping );
											searchRulesForURI(tempURI);
										}
										if (mapping.containsDataPropertyReference(tempURI)){
											subjectSubProp = MapSearch.searchSubDataProperty(tempURI, mapping );
											subjectEquProp = MapSearch.searchEquivalentDataProperty(tempURI, mapping );
											searchRulesForURI(tempURI);
										}
										if (mapping.containsIndividualReference(tempURI)){
											subjectSameIndi = MapSearch.searchSameIndividual(tempURI, mapping);
											searchRulesForURI(tempURI);
										}
										
									}else{ subjectVariable = true;}

									if (!similarTriple.getPredicate().isVariable()){
										System.out.println("Predicate");//TESTAUSGABE
										tempURI = URI.create(similarTriple.getPredicate().getURI());
//										MapSearch.AllAxiomsfromClass(URI.create(similarTriple.getPredicate().getURI()), mapping);//TESTAUSGABE
										if (mapping.containsClassReference(tempURI)){
											predicateSubClass = MapSearch.SearchSubclass(tempURI, mapping );
											predicateEquClass = MapSearch.searchEquivalentClass(tempURI, mapping );
											searchRulesForURI(tempURI);
										}
										if (mapping.containsObjectPropertyReference(tempURI)){
											predicateSubProp = MapSearch.searchSubObjectProperty(tempURI, mapping );
											predicateEquProp = MapSearch.searchEquivalentObjectProperty(tempURI, mapping );
											searchRulesForURI(tempURI);
										}
										if (mapping.containsDataPropertyReference(tempURI)){
											predicateSubProp = MapSearch.searchSubObjectProperty(tempURI, mapping );
											predicateEquProp = MapSearch.searchEquivalentObjectProperty(tempURI, mapping );
											searchRulesForURI(tempURI);
										}
										if (mapping.containsIndividualReference(tempURI)){
											predicateSameIndi = MapSearch.searchSameIndividual(tempURI, mapping);
											searchRulesForURI(tempURI);
										}
									}else{ predicateVariable = true;}

									if (!similarTriple.getObject().isVariable()){
										if(!similarTriple.getObject().isLiteral()){
											System.out.println("Object");//TESTAUSGABE
											tempURI = URI.create(similarTriple.getObject().getURI());
//											MapSearch.AllAxiomsfromClass(URI.create(t.getObject().getURI()), mapping);//TESTAUSGABE						
											if (mapping.containsClassReference(tempURI)){
												objectSubClass = MapSearch.SearchSubclass(tempURI, mapping );
												objectEquClass = MapSearch.searchEquivalentClass(tempURI, mapping );
												searchRulesForURI(tempURI);
											}
											if (mapping.containsObjectPropertyReference(tempURI)){
												objectSubProp = MapSearch.searchSubObjectProperty(tempURI, mapping );
												objectEquProp = MapSearch.searchEquivalentObjectProperty(tempURI, mapping );
												searchRulesForURI(tempURI);
											}
											if (mapping.containsDataPropertyReference(tempURI)){
												objectSubProp = MapSearch.searchSubObjectProperty(tempURI, mapping );
												objectEquProp = MapSearch.searchEquivalentObjectProperty(tempURI, mapping );
												searchRulesForURI(tempURI);
											}
											if (mapping.containsIndividualReference(tempURI)){
												objectSameIndi = MapSearch.searchSameIndividual(tempURI, mapping);
												searchRulesForURI(tempURI);
											}//FRAGE Kann ein Object das alles sein, Was ist mit DataProperty?
										}
									}else{ objectVariable = true;}

									
									// Creating triples with the alternative elements
									//brauche Node zum Tripel
									
									
									createSimilarTriples(similarTriple);
									
								}//END FOR searching similar triples
								runs++;
							}while(transitivityDepth >= runs && triples.size() != triplesSizeBefore); 
							
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
									System.out.println("[MapDarqTransform] similar triples: "+similarTriple);//TESTAUSGABE
									List<RemoteService> services = selectServices(registry
											.getMatchingServices(similarTriple));
									/* hier wird in den SD nachgeschaut, ob die Ressourcen (capabilities) existieren*/
									System.out.println("Anzahl Services: "+services.size()); //TESTAUSGABE
									similarTripleMap.put(cloneTriple(similarTriple), similar);

									if (services.size() >=1){
										putIntoUnionServiceGroup(similarTriple,services,similar,tree);
									}
									else {
//										unmatchedTriples.add(similarTriple); 
										/*
										 *  similarTriple einfach zu unmatchedTriples hinzufügen funktioniert nicht, 
										 *  da diese eine weitere Verarbeitung (Union, Multiply, StringConcat) erfordern. 
										 *  Daher werden die Tripel erstmal verworfen. Ideal wäre es, den lokalen Service
										 *  als RemoteService zu behandeln, so dass ganz normal auf (M/U)SG zurückgegriffen 
										 *  werden kann.
										 */ 
										log.warn("No service found for generated statement: " + similarTriple
												+ " - it will be ignored.");
									}
								} //End For similarTriple:triples
								similar++;
							}//End Else (zu if (triples.size == 1))
						}//End For originalTriple:PlanBlockTriple 	
						outputTree();			
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
		
		
		// add filters to service groups and to plan (filters are also applied
		// locally because we don't trust the remote services)
		for (PlanFilter f : filters) {
			acc.add(f); //local filter
			if (optimize) { // do we optimize?
				for (ServiceGroup sg : groupedTriples.values()) { //durchsucht alle SG und fügt Filter hinzu
					sg.addFilter(f.getExpr());
				}
				for (ServiceGroup sg : queryIndividuallyTriples.values()) {
					sg.addFilter(f.getExpr());
				}
				for (UnionServiceGroup usg : unionService.values()) {
					//TODO in der USG kann Multiply vorkommen, wenn Filter einen 
					//entsprechenden Wert aufweist, muss dieser umgerechnet werden
					//also muss erkannt werden, dass Filter zur MultiplyRegel gehört
					//und ggf. umgerechnet werden muss. 
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
		for(UnionServiceGroup usg : unionService.values()){
			usg.output();
		}
		
		
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
							.getPlanOptimizer().getCheapestPlan(al);
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
						acc.add(pos, FedPlanUnionService.make(context, (UnionServiceGroup) sg, null));
					}
					else if (sg instanceof MultipleServiceGroup) {
						acc.add(pos, FedPlanMultipleService.make(context,
								(MultipleServiceGroup) sg, null));
					} else
						acc.add(pos, FedPlanService.make(context, sg, null));
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
	
	/*
	 * groupedTriples ist eine Hashmap(Key, Wert):
	 * Key = RemoteService (RS)
	 * Wert = ServiceGroups (SG)
	 * (hier werden Triples zum RS gespeichert)
	 * 
	 * ServiceGroup:
	 * -Triples
	 * -Filters
	 * -RS
	 * -usedVariables
	 * 
	 * Fazit: erzeugt SG mit RS und Triple, trägt diesen
	 * mit dem Schlüssel RS in groupedTriples ein
	 * so kommt man über den RS an die Triple
	 * 
	 * ein RS --> mehrere Triple
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
	
	/*
	 * queryIndividuallyTriples ist eine HashMap
	 * Key = Triple
	 * Wert = MSG 
	 * (hier werden SG(==RS) zum Triple gespeichert)
	 * 
	 * MultipleServiceGroup (Erweiterung von SG für mehrere RS)
	 * -Triples
	 * -RSs
	 * -Filters
	 * -RS
	 * -usedVariables
	 * 
	 * Fazit: stellt nicht wirklich sicher, dass nur zu einem Triple
	 * mehrere RS hinzugefügt werden können. 
	 * Hashmap stellt nur sicher, dass ein Triple nicht mehrere MSG hat
	 * (diese könnten theoretisch mehrere Triple enthalten)
	 * ein Triple --> mehrere RSs
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
	
	/*
	 * 
	 * Logik: bekomme Triple, RS, similar
	 * existiert USG?
	 * 1.Fall: (similar suche)
	 *  -suche USG mit similar (Annahme: es existiert bereits eine USG
	 *   wegen anderer similar Tripel) 
	 * 2.Fall (triple suche)
	 *  -es findet sich keine USG mit similar, trotzdem ist es möglich
	 *   dass das Tripel in einer USG ist (anderes similar), weil es 
	 *   in einer anderen BGP auftaucht, daher suche nach Triple
	 * 3.Fall (keine USG) 
	 *  -es gibt keine USG (weder mit similar, noch mit triple suche)

	 * 1.+2. Fall USG existiert
	 * 1.1 ServiceSize == 1 (SG)
	 *     -hole SG aus USG mit RS (RS ist eindeutig)
	 *     -wurde die USG über ein Triple gefunden heißt das, dass die SG (inkl. Triple+RS) bereits existiert
	 *     -dann muss verhindert werden, dass das Triple der Liste in der SG hinzugefügt wird, ansonsten
	 *     -füge Triple der SG hinzu (USG wurde über similar gefunden, d.h. die SG existiert noch nicht oder die SG 
	 *      beinhaltet das Triple nicht) (Anm: HashSet wäre besser als LinkList)
	 *     -existiert keine solche SG, erstelle eine und füge Triple hinzu (innerhalb der USG)
	 * 1.2 ServiceSize > 1 (MSG)
	 * 	   -hole MSG aus USG mit Triple (Triple ist eindeutig)
	 *     -füge RS der MSG hinzu
	 *     -existiert keine MSG, erstelle eine, füge Triple und Service hinzu (innerhalb der USG)
	 
	 * 3. Fall USG existiert nicht
	 * 3.1 ServiceSize == 1 (SG)
	 *     -erstelle SG und USG, füge SG der USG hinzu
	 * 3.2. ServiceSize > 1 (MSG)
	 *	   -erstelle MSG und USG, füge MSG der USG hinzu

	 * Anmerkung: Im Fall 1 muss keine (M)SG in der USG existieren, da die USG durch similarTriple angelegt wurde
	 *            Im Fall 2 muss das Triple aber existieren, da die USG darüber gefunden wird. 
	 *            D.h. im Falle der SG, dass das Triple+RS schon eingetragen ist. 
	 *            Bei der MSG werden die RS in einem HashSet gespeichert, daher ist Prüfung nicht notwendig. 
	 * 
	 * 
	 *  FRAGE Was passiert, wenn ein Triple mehrmals vorkommt, aber
	 *  unterschiedliche Variablen hat? ?x name ?y ?u name ?v 
	 *  --> müßte eigentlich durch die SG abgedeckt werden bzw. Triple
	 *  (Anm.: Logik sollte sicherstellen, dass jedes Triple nur einmal 
	 *  vorkommt in den USGs (als auch in der Menge von SG,MSG, USG))
	 *  Was passiert beim Join über ?x bzw. ?u ??? 
	 *  Bastian FRAGE !!! Wie passiert es bei SG? 
	 *  
	 *  Da die Tripel suche auf dem Tripel inklusive Variablen passiert, sollten Tripel mit 
	 *  unterschiedlichen Variablen auch unterschiedliche (U/M)SGs ergeben.
	 *  (Optimierung? --> Cache!) 
	 */
	
	
	private void putIntoUnionServiceGroup(Triple triple, List<RemoteService> services, int similar, DefaultTreeModel tree){
		
		UnionServiceGroup usg = unionService.get(similar);
		ServiceGroup sg = null;
		MultipleServiceGroup msg = null;
		int serviceSize = services.size();
		
		if (usg == null){
			for (UnionServiceGroup helpUSG : unionService.values()){
				if (helpUSG.containsTriple(triple)){
					usg = helpUSG;
				}
			}
		}
		
		/*1. USG existiert */
		if(usg != null){ 
			if (serviceSize == 1){ /* 1.1 SG */
				RemoteService service=services.get(0);
				sg = usg.getServiceGroup(service); 
				if (sg == null){
					sg = new ServiceGroup(service);
					sg.addB(triple);
				}
				else if (!sg.containsTriple(triple)){ 
					/* wird geprüft für den Fall, dass die SG (Triple + RS) schon existiert (TripleSuche der USG) */ 
					sg.addB(triple);
				} 		
				usg.addServiceGroup(triple, sg);
			}
			else if (serviceSize > 1){/* 1.2 MSG */
				for (RemoteService service:services){
					sg = usg.getServiceGroup(triple);
					//MSG suchen bzw. neu erstellen
					if (sg == null){
						msg = new MultipleServiceGroup();
						msg.addB(triple);
						msg.addService(service); 
					}
					else if (sg instanceof MultipleServiceGroup){
						msg = (MultipleServiceGroup) sg;
						msg.addService(service);
					}					
					else System.err.println("Error [MAPDARQTRANSFORM]: This error should not occur.(ServiceSize > 1 and not MultipleServiceGroup)");
					usg.addServiceGroup(triple, msg);	
				}
			}			
		}
		
		/* 2. USG existiert nicht */
		else if (usg == null ){ 
			if (serviceSize == 1){ /* 2.1 SG */
				RemoteService service = services.get(0);
				/* diese SG kann noch nicht existieren */
				sg = new ServiceGroup(service);
				sg.addB(triple);//füge Triple in SG ein
				usg = new UnionServiceGroup(triple,sg, similar);
				unionService.put(similar, usg);
				sgsPos.add(usg);
			}
			else if (serviceSize >1 ){ /* 2.2 MSG */
				/* diese MSG kann noch nicht existieren, erst im 2. Durchlauf der Schleife */
				for (RemoteService service:services){
					if (usg != null){
						sg = usg.getServiceGroup(triple);	
					}
					if (sg == null){
						msg = new MultipleServiceGroup();
						msg.addB(triple);
						msg.addService(service);
						usg = new UnionServiceGroup(triple,msg, similar);
						unionService.put(similar, usg);
						sgsPos.add(usg);
					}
					else if (sg instanceof MultipleServiceGroup){
						msg = (MultipleServiceGroup) sg;
						msg.addService(service);
					}					
					else System.err.println("Error [MAPDARQTRANSFORM]: This error should not occur.(ServiceSize > 1 and not MultipleServiceGroup)");	 
				}
//				usg.addServiceGroup(triple, msg); //Sollte eigentlich nicht notwendig sein, da neue USG erzeugt und dadurch Tripel,MSG eingetragen
//				wird. Ansonsten hole ich mir ja das MSG Objekt und ändere das, somit sollte addServiceGroup nicht notwendig sein.  
			}
		}			
//		usg.output(); // TESTAUSGABE
	}
	
	
	/**
	 * creates new triples with the given Sets of classes and properties
	 * adds triples to a private set of the class
	 */
	/*
	 * Die Zusammensetzung der Triples auch aus den Regeln muss hier geschehen. 
	 * Was passiert bei StringConcat?
	 * Anfrage: 
	 * ?x name ?y
	 * ?y partof ?z
	 * 
	 * ?x vorname ?u
	 * ?x nachname ?v
	 * y stringconcat u,v
	 * 
	 * FRAGE
	 * 
	 * 1.Austausch bei StringConcat nicht möglich
	 * 2.muss neue Variable einführen
	 * 3.muss sicherstellen, dass beide Tripel zusammengehören 
	 * --> muss eigentlich neues PlanblockTripel erzeugen. 
	 * reicht es aus, wenn ich die Tripel ?x vorname ?u und ?x nachname ?v
	 * erzeuge?
	 * Ich weiß vorher nicht, ob ?y noch gebraucht wird. 
	 * Wie schaffe ich die Zuweisung? Also das y = u+v in der 
	 * nächsten Anfrage mitgenommen wird. 
	 * 
	 */
	private void createSimilarTriples(Triple triple) {

		Set<URI> subjects = new HashSet<URI>();
		Set<URI> predicates = new HashSet<URI>();
		Set<URI> objects = new HashSet<URI>();

		Node sub = Node.NULL;
		Node pred = Node.NULL;
		Node obj = Node.NULL;
		Triple newTriple = new Triple(sub, pred, obj);
		
		DefaultMutableTreeNode treeNode = getTreeNode(triple);
		
		HashSet<Rule> rules = new HashSet<Rule>();
		
		if (!subjectVariable) {
			subjects.add(URI.create(triple.getSubject().getURI())); // original subject
			subjects.addAll(subjectEquClass);
			subjects.addAll(subjectSubClass);
			subjects.addAll(subjectEquProp);
			subjects.addAll(subjectSubProp);
			subjects.addAll(subjectSameIndi);

			/* TODO analog für predicate / object */
			for (URI subjectURI : foundRules.keySet()){
				rules = foundRules.get(subjectURI);
				for (Rule rule : rules){
					if (rule.isMultiply()){
						
						subjects.add(getMultiplyCounterpart(subjectURI, rule));
						multiply.put(subjectURI, rule); 
						//Irgendwie muss ich mir merken, dass hier noch eine Multiplikation fehlt. 
						//Die kann jedoch erst im Resultset gemacht werden. bzw. division
						//Damit ist die HashMap subjectRules erstmal unnötig, HashSet reicht auch. TODO
						//für StringConcat HashMap wieder sinnvoll
					}
					else if (rule.isStrincConcat()){
						if (rule.getPart(subjectURI).isHead()) {
							// 2 neue Tripel erzeugen mit den URIs 
							//der Teile aus dem Body und neuen Variablen
							// t1(?x vorname ?y)
							// t2(?x nachname ?z)
							//im ResultSet Stringconcat (?y,?z)
						}
							
						//weiter mit ResultSet suchen und Multiply umsetzen bzw.
						//muss sich ja um eine USG handeln, d.h. dort das RS suchen beispiel MSG Union
						
						//Methode StringConcat
						
						/* Problem: 
						 * Es müssen neue Tripel erzeugt werden, 
						 * die nur als Concat Similar sind! Demzufolge
						 * müßten sie eigentlich als BGP (PlanBlockTripel) 
						 * erstellt werden. //FRAGE TODO 
						 */
					}	
				}				
			}			
		}

		if (!predicateVariable) {
			predicates.add(URI.create(triple.getPredicate().getURI())); // original predicate
			predicates.addAll(predicateEquClass);
			predicates.addAll(predicateSubClass);
			predicates.addAll(predicateEquProp);
			predicates.addAll(predicateSubProp);
			predicates.addAll(predicateSameIndi);
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
									System.out.println("[SPO] "+newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());// TEST
									triples.add(newTriple);
									if(getTreeNode(newTriple)==null){
										DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
										treeNode.add(child);	
									}								 
								}
							}
						}
					} else {// Subject is bound, Predicate is bound, Object is Variable

						//  ---> 2.Case SP <---			
						for (URI subject : subjects) {
							for (URI predicate : predicates) {
								newTriple = Triple.create(Node.create(subject.toString()), Node.create(predicate.toString()), triple.getObject());
								System.out.println("[SP] "+newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());//TEST
								triples.add(newTriple);
								if(getTreeNode(newTriple)==null){
									DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
									treeNode.add(child);	
								}
							}
						}
					}
				} else {
					if (!objectVariable) {//Subject is bound, Predicate is Variable, Object is bound

						// ---> 3.Case SO <---
						System.err.println("Warning [MAPPING]: Predicate not bound in query");
						for (URI subject : subjects) {

							for (URI object : objects) {
								newTriple = Triple.create(Node.create(subject.toString()), triple.getPredicate(), Node.create(object.toString()));
								System.out.println("[SO] "+newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());//TEST
								triples.add(newTriple);
								if(getTreeNode(newTriple)==null){
									DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
									treeNode.add(child);	
								}
							}
						}
					} else {//Subject is bound, Predicate is Variable, Object is Variable

						//  ---> 4.Case S <---
						System.err.println("Warning [MAPPING]: Predicate not bound in query");
						for (URI subject : subjects) {
							newTriple = Triple.create(Node.create(subject.toString()), triple.getPredicate(), triple.getObject());
							System.out.println("[S] "+newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());//TEST
							triples.add(newTriple);
							if(getTreeNode(newTriple)==null){
								DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
								treeNode.add(child);	
							}
						}
					}
				}
			} else {
				if (!predicateVariable) {
					if (!objectVariable) {//Subject is Variable, Predicate is bound, Object is bound

						//  ---> 5.Case PO  <---
						for (URI predicate : predicates) {
							for (URI object : objects) {
								newTriple = Triple.create(triple.getSubject(), Node.create(predicate.toString()), Node.create(object.toString()));
								System.out.println("[PO] "+newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());//TEST
								triples.add(newTriple);
								if(getTreeNode(newTriple)==null){
									DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
									treeNode.add(child);	
								}
							}
						}
					} else {//Subject is Variable, Predicate is bound, Object is Variable

						//  ---> 6.Case P <---
						for (URI predicate : predicates) {
							newTriple = Triple.create(triple.getSubject(), Node.create(predicate.toString()), triple.getObject());
							System.out.println("[P] "+newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());//TEST
							triples.add(newTriple);
							if(getTreeNode(newTriple)==null){
								DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
								treeNode.add(child);	
							}
						}
					}
				} else {
					if (!objectVariable) {//Subject is Variable, Predicate is Variable, Object is bound

						//  ---> 7.Case O <---
						System.err.println("Warning [MAPPING]: Predicate not bound in query");
						for (URI object : objects) {
							newTriple = Triple.create(triple.getSubject(), triple.getPredicate(), Node.create(object.toString()));
							System.out.println("[O] "+newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());//TEST
							triples.add(newTriple);
							if(getTreeNode(newTriple)==null){
								DefaultMutableTreeNode child = new DefaultMutableTreeNode(newTriple);
								treeNode.add(child);	
							}
						}
					} else {//Subject is Variable, Predicate is Variable, Object is Variable

						//  ---> 8.Case Nothing	 <---
						System.err.println("Warning: [MAPPING] Predicate not bound in query");
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
	
	/*
	 * just for the case that there exists
	 * no rules for this URI
	 */
	
	private void searchRulesForURI(URI tempURI){
		HashSet<Rule> rulesForURI = new HashSet<Rule>();
		rulesForURI = MapSearch.searchRules(tempURI, mapping);
		if (rulesForURI!=null){
			foundRules.put(tempURI, rulesForURI);	
		}
	}
	
	/* Idee: bekomme URI vom Original Triple Part, bekomme
	 * ebenfalls die multiply Regel, suche die Alternative aus
	 * der Regel und gebe sie zurück*/
	private URI getMultiplyCounterpart(URI uri , Rule rule){
		RulePart original = rule.getPart(uri);
		if (original.isBody()){
			return rule.getHeadParts().iterator().next().getUri();
		}
		else{
			for(RulePart rulepart : rule.getBodyParts()){
				if(!rulepart.getType().equals(SWRL_MULTIPLY)){
					return rulepart.getUri();
				}
			}
		}
		return null;
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
		System.out.println("Root: " + root.getUserObject().toString());
		while (children.hasMoreElements()){
			outputTree((DefaultMutableTreeNode) children.nextElement(),0);
		}
	}

	private void outputTree(DefaultMutableTreeNode parents, int depth){    		
		Enumeration children = parents.children();
		depth++;
		System.out.println("[Depth " +depth+"] Child of Node: " + parents.getUserObject().toString());
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
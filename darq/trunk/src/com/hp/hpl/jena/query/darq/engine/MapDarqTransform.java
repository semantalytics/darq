package com.hp.hpl.jena.query.darq.engine;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLOntology;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryBuildException;
import com.hp.hpl.jena.query.darq.config.MapConfiguration;
import com.hp.hpl.jena.query.darq.config.ServiceRegistry;
import com.hp.hpl.jena.query.darq.core.MapMultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.MapServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.engine.compiler.MapFedPlanMultipleService;
import com.hp.hpl.jena.query.darq.engine.compiler.MapFedPlanService;
import com.hp.hpl.jena.query.darq.engine.optimizer.PlanUnfeasibleException;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.MapPlanOperatorBase;
import com.hp.hpl.jena.query.engine.Plan;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.plan.PlanBasicGraphPattern;
import com.hp.hpl.jena.query.engine1.plan.PlanBlockTriples;
import com.hp.hpl.jena.query.engine1.plan.PlanFilter;
import com.hp.hpl.jena.query.engine1.plan.PlanGroup;
import com.hp.hpl.jena.query.util.Context;

import de.hu_berlin.informatik.wbi.darq.mapping.MapSearch;
import de.hu_berlin.informatik.wbi.darq.mapping.Rule;
import de.hu_berlin.informatik.wbi.darq.mapping.RulePart;
/**
 * 
 * @author Alexander Musidlowski
 * @version $ID$
 *
 */
public class MapDarqTransform extends DarqTransform {
	
	protected Plan plan = null;
	private Context context = null;
	private ServiceRegistry registry = null;
	private MapConfiguration config = null;
	private OWLOntology mapping = null;
	Log log = LogFactory.getLog(MapDarqTransform.class);
	boolean optimize = true;

	private Set<Triple> triples = new HashSet<Triple>();
	//collect new generated (similar) triples
	
	private HashMap<Triple, Integer> similarTripleMap = new HashMap<Triple, Integer>();
	//collects similar triples accessed by a triple, same Integer = similar Triple 
	
	private int transitivityDepth = 0; 
	
	// Subject
	private Set<URI> subjectSubClass = new HashSet<URI>();
	private Set<URI> subjectEquClass = new HashSet<URI>();
	private Set<URI> subjectSubProp = new HashSet<URI>();
	private Set<URI> subjectEquProp = new HashSet<URI>();
	private Set<URI> subjectSameIndi = new HashSet<URI>();
	private HashMap<URI, HashSet<Rule>> subjectRules = new HashMap<URI, HashSet<Rule>>();
	private boolean subjectVariable = false;
	
	// Predicate
	private Set<URI> predicateSubClass = new HashSet<URI>();
	private Set<URI> predicateEquClass = new HashSet<URI>();
	private Set<URI> predicateSubProp = new HashSet<URI>();
	private Set<URI> predicateEquProp = new HashSet<URI>();
	private Set<URI> predicateSameIndi = new HashSet<URI>();
	private HashMap<URI, HashSet<Rule>> predicateRules = new HashMap<URI, HashSet<Rule>>();
	private boolean predicateVariable = false;
	
	// Object
	private Set<URI> objectSubClass = new HashSet<URI>();
	private Set<URI> objectEquClass = new HashSet<URI>();
	private Set<URI> objectSubProp = new HashSet<URI>();
	private Set<URI> objectEquProp = new HashSet<URI>();
	private Set<URI> objectSameIndi = new HashSet<URI>();
	private HashMap<URI, HashSet<Rule>> objectRules = new HashMap<URI, HashSet<Rule>>();
	private boolean objectVariable = false;

	// The return stack
	private Stack<PlanElement> retStack = new Stack<PlanElement>();

	HashMap<Triple, MapServiceGroup> oneService = new HashMap<Triple, MapServiceGroup>();
	//Speichert ein Triple pro Service
	HashMap<Triple, MapMultipleServiceGroup> multipleServices = new HashMap<Triple, MapMultipleServiceGroup>();
	//Speichert ein Tripel, das an mehrere Services geschickt wird 
	
	public MapDarqTransform(Context cntxt, MapConfiguration conf, OWLOntology ontology, Integer transitivity) {
		super(cntxt, conf);
		context = cntxt;
		config = conf;
		registry = conf.getServiceRegistry();
		mapping = ontology;
		transitivityDepth = transitivity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.hpl.jena.query.engine1.plan.TransformBase#transform(com.hp.hpl.jena.query.engine1.plan.PlanBasicGraphPattern,
	 *      java.util.List)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public  PlanElement transform(PlanBasicGraphPattern planElt, List newElts) {

		oneService.clear(); // new for each PlanBasicGraphPattern !
		multipleServices.clear(); // 
		Set<Triple> tempTriples = new HashSet<Triple>();
		

		PlanBlockTriples unmatchedTriples = new PlanBlockTriples(context);
		//PlanBlockTriples sind Tripels, die per Klammern eingeschlossen sind

		List<PlanFilter> filters = new ArrayList<PlanFilter>();
		//Filter aus der Query

		List<PlanElement> acc = new ArrayList<PlanElement>();
		//Operatoren des Plans
		
		int runs, triplesSizeBefore, similar;
		similar=0;
		URI tempURI = null;
		
		//Idee: die Tripelliste mit den neu generierten Tripels vorher erzeugen
		//Problem dabei wird sein, dass es kein Planelement gibt
		
		//FRAGE: Ist es sinnvoll stur alle Unterklassen in der Query zu ersetzen?
		// Was ist mit Unterklassen in der selben Ontologie, diese sollten keinen
		// Mehrwert bieten?! (Was ist, wenn eine andere Quelle die selbe Ontologie nutzt und
		// nur diese Unterklassen bietet?)
		
		

		// planning
		for (PlanElement el : (List<PlanElement>) newElts) {
			if (el instanceof PlanBlockTriples) {
				for (Triple originalTriple : (List<Triple>) ((PlanBlockTriples) el).getPattern()) {
					System.out.println("[MapDarqTransform] original triple: " + originalTriple);//TESTAUSGABE
					/*
					 *  -- Searching for alternative Triple elements --
					 *  Idea: First run looks for similar triples, triples contains only original triple
					 *   adds similar triples to triples, runs again with original and similar triples and
					 *   looks for alternatives. As triples is a hashset no triple can be twice. 
					 *   Runs until TransitivityDepth is reached or no more similar triples added to triples
					 */
					/* Logik
					 * triples erhält originaltriple
					 * simtriples = triples 
					 * über simtriples iterieren
					 * triples erweitern
					 */
					triples.clear(); //delete all triples from the run before
					triples.add(originalTriple); 
					runs = 0;
//					triplesSizeBefore = simTriples.size();
					do{//transitivity
						System.out.print(runs);//TESTAUSGABE
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
									subjectEquClass = MapSearch.SearchEquivalentClass(tempURI, mapping );
									subjectRules.put(tempURI, MapSearch.searchRules(tempURI, mapping));
									//Aufruf stellt sicher, dass die URI in der Ontologie existiert, daher nicht ausserhalb der ifs
								}
								if (mapping.containsObjectPropertyReference(tempURI)){
									subjectSubProp = MapSearch.SearchSubProperty(tempURI, mapping );
									subjectEquProp = MapSearch.SearchEquivalentProperty(tempURI, mapping );
									subjectRules.put(tempURI, MapSearch.searchRules(tempURI, mapping));
								}//FRAGE Eventuell muss DataProp noch angepasst werden, drüber nachdenken
								if (mapping.containsDataPropertyReference(tempURI)){
									subjectSubProp = MapSearch.SearchSubProperty(tempURI, mapping );
									subjectEquProp = MapSearch.SearchEquivalentProperty(tempURI, mapping );
									subjectRules.put(tempURI, MapSearch.searchRules(tempURI, mapping));
								}
								if (mapping.containsIndividualReference(tempURI)){
									subjectSameIndi = MapSearch.SearchSameIndividual(tempURI, mapping);
									subjectRules.put(tempURI, MapSearch.searchRules(tempURI, mapping));
								}
								
							}else{ subjectVariable = true;}

							if (!similarTriple.getPredicate().isVariable()){
								System.out.println("Predicate");//TESTAUSGABE
								tempURI = URI.create(similarTriple.getPredicate().getURI());
//								MapSearch.AllAxiomsfromClass(URI.create(similarTriple.getPredicate().getURI()), mapping);//TEST
								if (mapping.containsClassReference(tempURI)){
									predicateSubClass = MapSearch.SearchSubclass(tempURI, mapping );
									predicateEquClass = MapSearch.SearchEquivalentClass(tempURI, mapping );
									predicateRules.put(tempURI, MapSearch.searchRules(tempURI, mapping));
								}
								if (mapping.containsObjectPropertyReference(tempURI)){
									predicateSubProp = MapSearch.SearchSubProperty(tempURI, mapping );
									predicateEquProp = MapSearch.SearchEquivalentProperty(tempURI, mapping );
									predicateRules.put(tempURI, MapSearch.searchRules(tempURI, mapping));
								}
								if (mapping.containsDataPropertyReference(tempURI)){
									predicateSubProp = MapSearch.SearchSubProperty(tempURI, mapping );
									predicateEquProp = MapSearch.SearchEquivalentProperty(tempURI, mapping );
									predicateRules.put(tempURI, MapSearch.searchRules(tempURI, mapping));
								}
								if (mapping.containsIndividualReference(tempURI)){
									predicateSameIndi = MapSearch.SearchSameIndividual(tempURI, mapping);
									predicateRules.put(tempURI, MapSearch.searchRules(tempURI, mapping));
								}
							}else{ predicateVariable = true;}

							if (!similarTriple.getObject().isVariable()){
								if(!similarTriple.getObject().isLiteral()){
									System.out.println("Object");//TESTAUSGABE
									tempURI = URI.create(similarTriple.getObject().getURI());
//									MapSearch.AllAxiomsfromClass(URI.create(t.getObject().getURI()), mapping);//TEST							
									if (mapping.containsClassReference(tempURI)){
										objectSubClass = MapSearch.SearchSubclass(tempURI, mapping );
										objectEquClass = MapSearch.SearchEquivalentClass(tempURI, mapping );
										objectRules.put(tempURI, MapSearch.searchRules(tempURI, mapping));
									}
									if (mapping.containsObjectPropertyReference(tempURI)){
										objectSubProp = MapSearch.SearchSubProperty(tempURI, mapping );
										objectEquProp = MapSearch.SearchEquivalentProperty(tempURI, mapping );
										objectRules.put(tempURI, MapSearch.searchRules(tempURI, mapping));
									}
									if (mapping.containsDataPropertyReference(tempURI)){
										objectSubProp = MapSearch.SearchSubProperty(tempURI, mapping );
										objectEquProp = MapSearch.SearchEquivalentProperty(tempURI, mapping );
										objectRules.put(tempURI, MapSearch.searchRules(tempURI, mapping));
									}
									if (mapping.containsIndividualReference(tempURI)){
										objectSameIndi = MapSearch.SearchSameIndividual(tempURI, mapping);
										objectRules.put(tempURI, MapSearch.searchRules(tempURI, mapping));
									}//FRAGE Kann ein Object das alles sein, Was ist mit DataProperty?
								}
							}else{ objectVariable = true;}

							/*
							 *  -- Creating triples with the alternative elements --
							 */
							triplesSizeBefore = triples.size();
							createSimilarTriples(similarTriple);
							//TODO Hier müssen die Regeln ausgwertet werden und 
							//entsprechende Anfragen generiert werden
							//if (rule.isMultiply){//do something}
							//eventuell createSimilarTriple(Rule rule) erzeugen
							//(SWRL.java --> SearchRules())
							/*
							 * für die USG (Union SG) muss geprüft werden, ob
							 * es similarTriples gibt. 
							 */
							
						}//END FOR searching similar triples
						runs++;
					}while(transitivityDepth >= runs && triples.size() != triplesSizeBefore); 
						
					
					// TODO Create Triple für Regeln! 
					// TODO UNION dieser Triples
					 				
					/*
					 * 
					 * -- looks for fitting services for each triple and puts it into -- 
					 *    a SG or MSG 
					 */
					if (triples.size() == 1){ 
						/* nur Originaltriple vorhanden */
						Triple triple = triples.iterator().next();
						List<RemoteService> services = selectServices(registry.getMatchingServices(triple));
						//normale SG bzw. MSG
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
							unmatchedTriples.addTriple(triple);
							log.warn("No service found for statement: " + triple
									+ " - it will be queried locally.");
						}//END ELSE

						
					}//End Triple.size == 1 
					else{
						/* similar triple vorhanden */
						/* in USG packen */
						for(Triple similarTriple:triples){
							System.out.println("[MapDarqTransform] similar triples: "+similarTriple);//TEST
							List<RemoteService> services = selectServices(registry
									.getMatchingServices(similarTriple));
							/* hier wird in den SD nachgeschaut, ob die Ressourcen (capabilities) existieren
							 */
							System.out.println("Anzahl Services: "+services.size());
							similarTripleMap.put(cloneTriple(similarTriple), similar);

							if (services.size() == 1) {
								putIntoGroupedTriples(services.get(0), similarTriple);
							} 
							else if (services.size() > 1) {
								/*
								 * if there are more than one service, the triple has to
								 * be passed to the services individually. This is
								 * because ... TODO
								 */
								for (int j = 0; j < services.size(); j++) {
									putIntoQueryIndividuallyTriples(similarTriple, services.get(j)); 
								}
							} 
							else {
								unmatchedTriples.addTriple(similarTriple);
								log.warn("No service found for statement: " + similarTriple
										+ " - it will be queried locally.");
							}//END ELSE
						} //END FOR searching services for similar triples
						similar++;
					}//END FOR t (original triples)
				} 
			}
			else if (el instanceof PlanFilter) {
				filters.add((PlanFilter) el);
			} 
			else {
				acc.add(0, el);
			}
		}//END FOR Plan Elements
		
		/*
		 * Was ist bis hierhin passiert? Aus den Triples wurden ähnliche Triples erzeugt. Zu allen triples wurden die Services gesucht
		 * und in die 2 Töpfe groupedTriples und queryIndividuallyTriples gesteckt. 
		 * Das ist an dieser Stelle wahrscheinlich nicht richtig, da sich in den Töpfen äquivalente Triples befinden können. Vorher waren es 
		 * nur Triples aus dem Plan! 
		 * 
		 * Weiteres Vorgehen: Besteht die Notwendigkeit die Töpfe zu trennen? Wahrscheinlich schon. 
		 * Töpfe so trennen, dass nur äquivalente Triples in den Töpfen sind (in der Hoffnung, dass man die Töpfe später wieder verdichten kann)
		 * Das geht wahrscheinlich nicht, weil man höchstens das Ergebnis verdichten kann. (DISTINCT?) 
		 */
		
		/*
		 * kommt hier theoretisch nur ein Triple an? 
		 * in den SG sind vorher nur Tripel angekommen, die ein (F)BGP dargestellt haben
		 * jetzt gibt es mehrere Tripel zu einem (F)BGP
		 * (d.h. FRAGE Filter werden erst lokal angewendet, muss dafür auch auf das Mapping geachtet werden oder passiert es erst im Ergebnis, 
		 * wo man dann auf object etc zurückgreift? Letzteres wäre sinnvoll.)
		 * vorher konnte man einfach alle Triple den SG zuweisen, da Join per se gemacht werden muss. 
		 * jetzt brauche ich ein Unterscheidungsmerkmal zu welchem BGP das Triple gehört (war ja vorher nicht nötig, da es nur ein BGP gab)
		 * 
		 *   Ist es sinnvoll hierfür die SG zu verwenden und ein entsprechendes Merkmal zu etablieren?
		 *   Merkmal kann in der SG leicht hinterlegt werden
		 *   Die Frage ist, wo diese SG ausgewertet werden?
		 *   Ist es in der SG möglicherweise schon zu spät? Die SGs stellen auf keine Fall sicher, dass ähnliche
		 *   Triple aus gemeinsam zusammengehören in der Auswertung. Ein Merkmal ähnlich mit reicht in der SG nicht aus!
		 *   
		 *   Es muss eine neue Struktur her. Sinnvoll ist eine Hashmap aus Index und triplelist. similarTripleLists = hashmap(index, triples) 
		 *   Dazu müssen die SGs umgebaut werden, oder?
		 *   SG ist eine konkurrienden/parallele Struktur dazu! Inwiefern ist es sinnvoll eine solche Hashmap in die SG zu integrieren?
		 *   Doch nur dann, wenn der Plan dadurch optimiert wird. Hier fehlt wieder das Wissen über die Abarbeitung der Triples bei der Anfrage 
		 *   bzw. wie die Anfrageplanung aussieht. 
		 *   Die Frage ist, wo man das Union unterbringen kann. Da es über mehrere SGs hinweg ausgewertet werden muss, entweder Hinweis mit-
		 *   schleifen bis zum Ergebnis (Resultset) und dann zusammenführen + filtern oder das Union vorher im Plan einbauen, was der bessere
		 *   Weg wäre. Bloss wo ist das?
		 *   Neue Struktur similarTripleMap angelegt mit Triple als Key und einem Integerwert. Identische Integerwerte bedeuten, dass die 
		 *   Tripel ähnlich sind.
		 *    
		 *   
		 */
		
//		HashMap<Triple, MapServiceGroup> oneService = new HashMap<Triple, MapServiceGroup>();
//		//Speichert ein Triple pro Service
//		HashMap<Triple, MultipleServiceGroup> multipleServices = new HashMap<Triple, MultipleServiceGroup>();

		// add filters to service groups and to plan (filters are also applied
		// locally because we don't trust the remote services)
		for (PlanFilter f : filters) {
			acc.add(f);
			if (optimize) { // do we optimize?
				for (MapServiceGroup sg : oneService.values()) { //durchsucht alle SG und fügt Filter hinzu
					sg.addFilter(f.getExpr());
				}
				for (MapMultipleServiceGroup sg : multipleServices.values()) {
					sg.addFilter(f.getExpr());
				}
			}
		}

//		com.hp.hpl.jena.query.engine1.plan.PlanUnion
		
		// build new subplan
		if (oneService.size() > 0 || multipleServices.size() > 0) {

			ArrayList<MapServiceGroup> al = new ArrayList<MapServiceGroup>(oneService.values());
			al.addAll(multipleServices.values());
			//al ist ein Liste aus SG und MSG (de facto RSs)
					
			if (optimize) { // run optimizer
				PlanElement optimizedPlan = null;
				try {		
					MapPlanOperatorBase mapPlanOperatorBase = config.getMapPlanOptimizer().getCheapestPlan(al,similarTripleMap);
					MapFedQueryEngineFactory.logExplain(mapPlanOperatorBase);
					optimizedPlan = mapPlanOperatorBase.toARQPlanElement(context); 					
				} catch (PlanUnfeasibleException e) {
					throw new QueryBuildException("No feasible plan: "
							+ e);
				}
				acc.add(0,optimizedPlan);
				log.debug("selected: \n"
						+ optimizedPlan.toString());
				
			} else { // no optimization -> just add elements 
				int pos = 0;
				for (MapServiceGroup sg : al) {

					if (sg instanceof MapMultipleServiceGroup) {
						acc.add(pos, MapFedPlanMultipleService.make(context,
								(MapMultipleServiceGroup) sg, null));
					} else
						acc.add(pos, MapFedPlanService.make(context, sg, null));
					pos++;

				}
			}
		}

		// unmatched patterns are executed locally 
		if (unmatchedTriples.getPattern().size() > 0)
			acc.add(0, unmatchedTriples);
		
		PlanElement ex = PlanGroup
				.make(planElt.getContext(), acc, false);
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
	//Was ist notwendig? SG muss erweitern werden MapSG
	//MapMSG muss erzeugt werden. 
	//Funktion muss um similar erweitert werden
	//Variable muss um similar erweitert werden?
	//Wieso brauche ich das in der SG?
	//Idee: Wenn keine MapSG existiert, dann erstelle eine
	//prüfe, ob es bereits ein similarTriple gibt
	//nein, dann füge Triple hinzu
	//ja, dann erstelle neue MapSG
	//FRAGE könnten mich mehrere SG mit dem selben RS in Schwierigkeiten bringen? 
	private void putIntoGroupedTriples(RemoteService s, Triple t) {
		
		
		MapServiceGroup tg = oneService.get(t); //holt RS
		if (tg == null) { //RS vorhanden?
			tg = new MapServiceGroup(s); //Nein, neue SG mit RS erstellen
			oneService.put(t, tg); //fügt RS mit SG in Hashmap ein
			tg.addB(t, similarTripleMap.get(t));//füge Triple in SG ein und Gruppe der ähnlichen Triple
		}
		else{
			System.err.println("Warning [MAPDARQTRANSFORM] Triple already added.");
		}		
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
		MapMultipleServiceGroup msg = multipleServices.get(t);
		if (msg == null) { //Triple vorhanden?
			msg = new MapMultipleServiceGroup();//nein, neue MSG erstellen
			multipleServices.put(t, msg); //fügt Triple+MSG in Hashmap ein
			msg.addB(t, similarTripleMap.get(t));//fügt Triple zu MSG hinzu
		}
		msg.addService(s); //fügt RS zu MSG hinzu
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
	 * 
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
		
		HashSet<Rule> rules = new HashSet<Rule>();
		
		if (!subjectVariable) {
			subjects.add(URI.create(triple.getSubject().getURI())); // original subject
			subjects.addAll(subjectEquClass);
			subjects.addAll(subjectSubClass);
			subjects.addAll(subjectEquProp);
			subjects.addAll(subjectSubProp);
			subjects.addAll(subjectSameIndi);

			/* analog für predicate / object */
			for (URI subjectURI : subjectRules.keySet()){
				rules = subjectRules.get(subjectURI);
				for (Rule rule : rules){
					if (rule.isMultiply()){
						subjects.add(subjectURI); 
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
						}
					}
				} else {//Subject is bound, Predicate is Variable, Object is Variable
					
					//  ---> 4.Case S <---
					System.err.println("Warning [MAPPING]: Predicate not bound in query");
					for (URI subject : subjects) {
						newTriple = Triple.create(Node.create(subject.toString()), triple.getPredicate(), triple.getObject());
						System.out.println("[S] "+newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());//TEST
						triples.add(newTriple);
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
						}
					}
				} else {//Subject is Variable, Predicate is bound, Object is Variable

					//  ---> 6.Case P <---
					for (URI predicate : predicates) {
						newTriple = Triple.create(triple.getSubject(), Node.create(predicate.toString()), triple.getObject());
						System.out.println("[P] "+newTriple.getSubject() + " " + newTriple.getPredicate() + " " + newTriple.getObject());//TEST
						triples.add(newTriple);
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
					}
				} else {//Subject is Variable, Predicate is Variable, Object is Variable

					//  ---> 8.Case Nothing	 <---
					System.err.println("Warning: [MAPPING] Predicate not bound in query");
					triples.add(triple);
				}
			}
		}
	}

	

	/*
	 * Auswertung der Regeln
	 */
	
////	private static void Rules(URI reference, OWLOntology ontology) {
//	private  URI Rules(Set ruless) {	
//		Rule rule;
//		HashSet<URI> rulesURI;
//		ArrayList<RulePart> partList;
////		Multiply multiplyPart;
//		boolean[] subCls = new boolean[2];
//		boolean[] subDataproperty = new boolean[2];
//		boolean[] subObjectproperty = new boolean[2];
//		boolean[] body = new boolean[2];
//		boolean head = false;
//		boolean swrl = false;
//		
//		boolean multiply = false;
//		boolean stringConcat = false;
//		boolean subClass = false;
//		boolean subDataProperty = false;
//		boolean subObjectProperty = false;
//		
//		double multiplier =0;
//		boolean divideHead = false;
//		Set<URI> bodyClasses = new HashSet<URI>();
//		Set<URI> headClasses = new HashSet<URI>();
//		int i, j;
//
////		if (!init || (init && !ontologyURI.equals(ontology.getURI())) ) Init(ontology);
////		System.out.println("--------- Starting Rule Analyses ---------");
//
//		rulesURI = searchIndex.get(reference);
////		System.out.println(rulesURI.size() + " Regeln gefunden.");
//		if (rulesURI != null){ 
//			/* rules found */
//			for (URI ruleURI : rulesURI) {
//				rule = rules.get(ruleURI);
//				partList = rule.getRulePartList();
//
//				/* for subclass */
//				subCls[0] = false;
//				subCls[1] = false;
//				body[0] = false;
//				i = 0;
//				
//				/* for subdataproperty */
//				subDataproperty[0] = false;
//				subDataproperty[1] = false;
//
//				/* for subobjectproperty */
//				subObjectproperty[0] = false;
//				subObjectproperty[1] = false;
//				
//				
//				/* for Multiply (additional) */
//				multiply = false;
//				swrl = false;
//				divideHead = false;
//				body[1] = false;
//				head = false;
//				multiplier = 0;
//				j = 0;
//				bodyClasses.clear();
//				headClasses.clear();
//				/*for stringconcat (additional)*/
//				stringConcat = false;
//				
//				
////				System.out.println("Regel in Bearbeitung: "+ruleURI);
////				System.out.println("Größe PartList: " + partList.size());
//				
//				for (RulePart part : partList) {
//					/* analyze type of rule */
//					
//					/* Subclass */
//					/*
//					 * Kriterien: 
//					 * -die Regel darf nur aus 2 Teilen bestehen 
//					 * -gesuchte Klasse muss im Body sein 
//					 * -eine Klasse muss im Head stehen 
//					 * -es muss sich um 2 Klassen handeln
//					 * -analog für Properties
//					 */
//					if (partList.size() == 2) {
//						if (part.isBody()) body[0] = part.getUri().equals(reference);
//						if (part.isHead()) head = true;
//						if (part.getType().equals(MapSearch.OWL_CLASS)){
//							subCls[i] = true;
//							subClass = true;
//							i++;
//						}
//						if (part.getType().equals(MapSearch.OWL_DATAPROPERTY)){
//							subDataproperty[i] = true;
//							subDataProperty= true;
//							i++;
//						}
//						if (part.getType().equals(MapSearch.OWL_OBJECTPROPERTY)){
//							subObjectproperty[i] = true;
//							subObjectProperty = true;
//							i++;
//						}
//					}
//					
//					/* multiply */
//					/*
//					 * Kriterien: 
//					 * -BodySize == 2 
//					 * -HeadSize == 1 
//					 * -Body: Klasse + SWRLBuiltIn(multiply) 
//					 * -Head: Klasse 
//					 * -gesuchte Klasse im Head: 
//					 * 		-neue Abfrage mit Klasse aus Body 
//					 * 		-Ergebnisse mit Multiplier multiplizieren
//					 * -gesuchte Klasse im Body: 
//					 * 		- neue Abfrage mit Klasse aus Head 
//					 * 		- Ergebnis der Abfrage durch Multiplier teilen
//					 */
//					/*
//					 * 1. Fall: multiply 
//					 * 2. Fall: gesuchte Klasse steht im Body 
//					 * 3. Fall: gesuchte Klasse steht nicht im Body
//					 */
//
////					System.out.println("---------MULTIPLY---------");
////					System.out.println("Rule URI: "+ rule.getRuleURI());
//
//					else if (rule.isMultiply()) {
//						multiply = true;
//						if (part.isBody()){
//							if (part.getType().equals(MapSearch.SWRL_MULTIPLY)) {
//								swrl = true;
//								multiplier = part.getMultiplier(); 
//								// hope this works, TEST
////								System.out.println("multiplier: " + multiplier);
//							} else if (part.getUri().equals(reference)) {
//								divideHead = true;
//							}
////							System.out.println("PartSize: " + partList.size());
////							System.out.println("PartURI: "+part.getUri());
//							bodyClasses.add(part.getUri()); 
//							body[j] = true;
//							j++;
//						}
//						if (part.isHead()) {
//							// if (part.getUri().equals(cls.getURI())){
//							/* sucht nach der übergebenen Klasse */
//							/* sollte überflüssig sein, da per default false */
////							divideHead = false;
//							// }
//							head = true;
//							headClasses.add(part.getUri());
//						}
//					}
////					System.out.println("--------- END MULTIPLY---------");
//					
//
//					/* String Concat */
//					/*
//					 * Kriterien:
//					 * -mindestens 4 Komponenten 
//					 * -Builtin im Body
//					 * -mindestens 2 DataTypeProperties im Body
//					 * -max. eine DataTypeProperty im Head
//					 */
//					/* Es kann vorkommen, dass sich auch Klassen in StringConcat befinden, 
//					 * diese interessieren aber nicht, da der Stringconcat nur zwischen Datatype
//					 * Properties ausgeführt werden kann. Daher ist es nicht sinnvoll, bei den 
//					 * Klassen Stringconcat zu erkennen (auch, wenn es möglich wäre)
//					 * Beispiel 
//					 * Person(?x) & hasSurname(?x,?y) & hasLastName(?x,?z) & swrlb:stringConcat(?w,?x,?y) --> hasName(?x,?w)
//					 * hasSurname(?x,?y) & hasLastName(?x,?z) & swrlb:stringConcat(?w,?x,?y) --> hasName(?x,?w)*/
//					
////					System.out.println("StringConcat?: "+ rule.isStrincConcat());
//					else if (rule.isStrincConcat()) {
//						stringConcat = true;
//
////						System.out.println("Size: " + partList.size());
//						if (partList.size() >=4){
////							System.out.println("Body? " + part.isBody());
//							if (part.isBody()){
////								System.out.println("Typ: " + part.getType());
//								if (part.getType().equals(MapSearch.OWL_DATAPROPERTY)){
//									bodyClasses.add(part.getUri());
////									System.out.println("BodyClassSize: " + bodyClasses.size());
//									body[i] = true;
//									i++;
//								}
//								if (part.getType() == MapSearch.SWRL_STRINGCONCAT){
//									swrl = true;
//								}
//							}
////							System.out.println("Head? " + part.isHead());
//							if (part.isHead()){
////								System.out.println("Typ: " + part.getType());
//								if (part.getType().equals(MapSearch.OWL_DATAPROPERTY)){
//									head = true;
//								}										
//							}									
//						}								
//					}
//					
//					/* Anything else */
//					else {
////						System.err.println("Warning [MAPSEARCH] The rule " +  rule + " is not supported.");
//					}
//
//				}// End For Part List
//
//				/*-----------------------------------        Auswertung              -----------------------------*/
//				
//				/* subclass */
//				if (subClass && body[0] && head && subCls[0] && subCls[1]&&partList.size()==2) {
//					System.out.println("Subclass: " + ruleURI);
//				}
//				
//				/* subdataproperty */
//				else if (subDataProperty && body[0] && head && subDataproperty[0] && subDataproperty[1]) {
//					System.out.println("SubDataProperty: " + ruleURI);
//				}
//				
//				/* subobjectproperty */
//				else if (subObjectProperty && body[0] && head && subObjectproperty[0] && subObjectproperty[1]) {
//					System.out.println("SubDataProperty: " + ruleURI);
//				}
//				
//				/* stringconcat */
//				else if(stringConcat && head && body[0] && body[1] && swrl ){
//					
////					System.out.println("Klasse: " + clsURI);
//					System.out.println("StringConcat: " + ruleURI);
//					/* eine der DataTypeProperties ist im Body
//					 * , es müßte also  ein Split im Head durchgeführt werden
//					 * 2 Möglichkeiten: Split am Leerzeichen machen, 
//					 * sofern eins vorhanden, ansosnten Fehlermeldung, 
//					 * sonst Split einfach verweigern
//					 * 
//					 */
//					if (bodyClasses.contains(reference)){
//						System.out.println("Warning [MAPSEARCH]: DatatypeProperty is in the body of a string concat rule. Split of the datatypeproperty in the head is not supported.");
//					}
//				}
//				
//				/* multiply */
////				System.out.println("----- BEGIN multiply ------");
////				System.out.println("Klasse: " + clsURI);
////				System.out.println("Rule: " + ruleURI);
////				System.out.println("SWRL: " + swrl);
////				System.out.println("Head: " + head);
////				System.out.println("Body[0]: " + body[0]);
////				System.out.println("Body[1]: " + body[1]);
////				System.out.println("----- END multiply ------");
//				else if (multiply && swrl && head && body[0] && body[1]) {
//					/* do what ever is to do */
//					if (divideHead) { 
//						/* divide head */
//						System.out.println("Multiply: " + headClasses.iterator().next()+ "/" + multiplier);
//						System.out.println("Regel: " + ruleURI);
//
//					} 
//					else { 
//						/* multiply body */
//						System.out.println("Multiply: " + bodyClasses.iterator().next() + "*" + multiplier);
//						System.out.println("Regel: " + ruleURI);
//						/*-gesuchte Klasse im Head: 
//						 * 		-neue Abfrage mit Klasse aus Body
//						 * 		-Ergebnisse mit Multiplier multiplizieren*/
//					}
//				}
//				else{
//					System.out.println("Error [MAPSEARCH] Rule "+ruleURI+" is not supported.");//wirklich?
//				}
//				
//			}//End For found rules
//		}
//
//		/* Individuals */
//		for (OWLIndividual cls : ontology.getReferencedIndividuals()) {
//			for (URI clsURI : searchIndex.keySet()) {
//				if (cls.getURI().equals(clsURI)) {
//					System.out.println("Suche Erfolgreich: (individuals)" + clsURI);
//				}
//			}
//		}
//	}
	
	
	
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
}

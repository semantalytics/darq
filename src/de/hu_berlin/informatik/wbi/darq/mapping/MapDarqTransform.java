package de.hu_berlin.informatik.wbi.darq.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryBuildException;
import com.hp.hpl.jena.query.darq.config.Configuration;
import com.hp.hpl.jena.query.darq.config.ServiceRegistry;
import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.DarqTransform;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngineFactory;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanMultipleService;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanService;
import com.hp.hpl.jena.query.darq.engine.optimizer.PlanUnfeasibleException;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.PlanOperatorBase;
import com.hp.hpl.jena.query.engine.Plan;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.plan.PlanBasicGraphPattern;
import com.hp.hpl.jena.query.engine1.plan.PlanBlockTriples;
import com.hp.hpl.jena.query.engine1.plan.PlanFilter;
import com.hp.hpl.jena.query.engine1.plan.PlanGroup;
import com.hp.hpl.jena.query.util.Context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owl.model.OWLOntology;

public class MapDarqTransform extends DarqTransform {
	
	protected Plan plan = null;
	private Context context = null;
	private ServiceRegistry registry = null;
	private Configuration config = null;
	private OWLOntology mapping = null;
	Log log = LogFactory.getLog(MapDarqTransform.class);
	boolean optimize = true;

	// The return stack
	private Stack<PlanElement> retStack = new Stack<PlanElement>();

	HashMap<RemoteService, ServiceGroup> groupedTriples = new HashMap<RemoteService, ServiceGroup>();
	//Speicher für mehrere Tripel aus der Anfrage. Diese werden dann an eine Quelle geschickt 
	HashMap<Triple, MultipleServiceGroup> queryIndividuallyTriples = new HashMap<Triple, MultipleServiceGroup>();
	//Speicher für Tripel, die an mehrere Services geschickt werden. 
	
	public MapDarqTransform(Context cntxt, Configuration conf, OWLOntology ontology) {
		super(cntxt, conf);
		context = cntxt;
		config = conf;
		registry = conf.getServiceRegistry();
		mapping = ontology;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.hpl.jena.query.engine1.plan.TransformBase#transform(com.hp.hpl.jena.query.engine1.plan.PlanBasicGraphPattern,
	 *      java.util.List)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public PlanElement transform(PlanBasicGraphPattern planElt, List newElts) {

		groupedTriples.clear(); // new for each PlanBasicGraphPattern !
		queryIndividuallyTriples.clear(); // "

		PlanBlockTriples unmatchedTriples = new PlanBlockTriples(context);
		//PlanBlockTriples sind Tripels, die per Klammern eingeschlossen sind

		List<PlanFilter> filters = new ArrayList<PlanFilter>();
		//Filter aus der Query

		List<PlanElement> acc = new ArrayList<PlanElement>();
		//Operatoren des Plans
		
//		OWLOntology owlOntology = null; //eventuell manager.createOntology(URI);
//		String map = "N:/Studium/Diplomarbeit/Ontologien/Buch.owl";
//		if (map != null){
//			 owlOntology = MapLoadOntologies.loadCommandline(map);
//		}//hier prüfen, ob die Parameter aus der Kommandozeile i.O.?

		
		//Idee: die Tripelliste mit den neu generierten Tripels vorher erzeugen
		//Problem dabei wird sein, dass es kein Planelement gibt

//		Set<Triple> triples;//sammelt Triples in einer Menge --> keine doppelten Triples
//		List<Triple> listTriple  = (List<Triple>) ((PlanBlockTriples) el).getPattern();
//		for (Triple t : listTriple) {
//		
//			Set<Set<OWLClass>> subClassesSets = SearchMapping.SearchSubclass(URI.create(t.getSubject().getURI()), owlOntology );
//			Set<OWLClass> equClassesSet = SearchMapping.SearchEquivalentClass(URI.create(t.getSubject().getURI()), owlOntology );		
//			Set<Set<OWLObjectProperty>> subPropertySets = SearchMapping.SearchSubProperty(URI.create(t.getPredicate().getURI()), owlOntology );
//			Set<OWLObjectProperty> equPropertySet = SearchMapping.SearchEquivalentProperty(URI.create(t.getSubject().getURI()), owlOntology );
//			
//			Node s,p,o;
//			Set<OWLClass> subClassesSet = OWLReasonerAdapter.flattenSetOfSets(subClassesSets);			
//			Set<OWLObjectProperty> subPropertySet = OWLReasonerAdapter.flattenSetOfSets(subPropertySets);
//			
//			for(OWLClass csub : subClassesSet) {
//				for(OWLObjectProperty psub : subPropertySet){
//					Triple newtriple = newtriple.create(p.createURI(csub.getURI().toString()), p.createURI(psub.getURI().toString()), t.getObject());
//					triples.add(newtriple); 
//				}
//			}
//			for(OWLClass csub : subClassesSet) {
//				for(OWLObjectProperty pequ : equPropertySet){
//					Triple newtriple = newtriple.create(s.createURI(csub.getURI().toString()), p.createURI(pequ.getURI().toString()) , t.getObject());
//					triples.add(newtriple); 
//				}
//			}
//			for(OWLClass cequ: equClassesSet) {
//				for(OWLObjectProperty psub : subPropertySet){
//					Triple newtriple = newtriple.create(p.createURI(cequ.getURI().toString()), p.createURI(psub.getURI().toString()), t.getObject());
//					triples.add(newtriple); 
//				}
//			}
//			for(OWLClass cequ: equClassesSet) {
//				for(OWLObjectProperty pequ : equPropertySet){
//					Triple newtriple = newtriple.create(p.createURI(cequ.getURI().toString()), p.createURI(pequ.getURI().toString()), t.getObject());
//					triples.add(newtriple); 
//				}
//			}

//					Triple modtriple = modtriple.create(p.createURI(cls.getURI().toString()), t.getPredicate(), t.getObject());
//					//FRAGE erzeugt hoffentlich das neue Tripel, wieder über den Umweg URI, hoffe das ist der richtige Node
					//2 verschiedene Modelle für identischen Inhalt
					// Ich brauche eine Klasse bzw. eine vollständige URI
					// URI von Node? URI von Node entspricht hoffentlich gleicher URI 
					// wie URI in Ontologie --> hoffentlich gelöst
//				}
//			}
//		}
		
		//FRAGE: Ist es sinnvoll stur alle Unterklassen in der Query zu ersetzen?
		// Was ist mit Unterklassen in der selben Ontologie, diese sollten keinen
		// Mehrwert bieten?! (Was ist, wenn eine andere Quelle die selbe Ontologie nutzt und
		// nur diese Unterklassen bietet?)
			

		
		
		// planning
		for (PlanElement el : (List<PlanElement>) newElts) {

			if (el instanceof PlanBlockTriples) {
				for (Triple t : (List<Triple>) ((PlanBlockTriples) el)
						.getPattern()) {
					
					//fuer jedes Tripel mache ...
					
					//... an dieser Stelle Mappings suchen
					/* hier muss also ein Aufruf für das Mapping stattfinden
					 * mit "t" als Parameter. "t" muesste aus Subjekt, Praedikat
					 * und Objekt bestehen. Ein Teil des Tripel sollten Variablen bzw. 
					 * Werte enthalten. Werte (Literale bzw. Ressourcen sind erstmal 
					 * uninteressant unter der Annahme, dass sie eindeutig sind. 
					 * Ansonsten muss man im Mapping explizit "same as" fuer den jeweiligen
					 * Wert bzw. die Ressource angeben. 
					 * Alle Nichtvariablen müssen einzeln zum Mapping geschickt werden. Aus 
					 * den Ergebnissen muss dann wieder ein Tripel t´ zusammengesetzt werden, 
					 * welches hier dann weiter verarbeitet wird. Es kann passieren, dass mehrere
					 * t´ hier erzeugt werden.  
					 *
					 * Das Praedikat entspricht in Protege den Object Properties (doh)
					 *   
					 */
					List<RemoteService> services = selectServices(registry
							.getMatchingServices(t));
					/* hier wird in den SD nachgeschaut, ob die Ressourcen (capabilities) existieren
					 * davor muss also das Mapping ausgeführt werden
					 */
					
					if (services.size() == 1) {
						putIntoGroupedTriples(services.get(0), t);
					} else if (services.size() > 1) {
						/*
						 * if there are more than one service, the triple has to
						 * be passed to the services individually. This is
						 * because ... TODO
						 */
						for (int j = 0; j < services.size(); j++) {
							putIntoQueryIndividuallyTriples(t, services.get(j));
						}

					} else {
						/* Die Fehlermeldung sollte korrekt sein, da durch das Mapping
						 * ja nur Tripels erzeugt werden, die auf jeden Fall zu einem 
						 * Webservice gehoeren. 
						 */
						unmatchedTriples.addTriple(t);
						log.warn("No service found for statement: " + t
								+ " - it will be queried locally.");

					}

				}

			} else if (el instanceof PlanFilter) {
				filters.add((PlanFilter) el);
			} else {
				acc.add(0, el);

			}

		}

		// add filters to servcie groups and to plan (filters are also applied
		// locally because we don't trust the remote services)
		for (PlanFilter f : filters) {
			acc.add(f);
			if (optimize) { // do we optimize?
				for (ServiceGroup sg : groupedTriples.values()) {
					sg.addFilter(f.getExpr());
				}
				for (ServiceGroup sg : queryIndividuallyTriples.values()) {
					sg.addFilter(f.getExpr());
				}
			}
		}

		// build new subplan
		if (groupedTriples.size() > 0 || queryIndividuallyTriples.size() > 0) {

			ArrayList<ServiceGroup> al = new ArrayList<ServiceGroup>(
					groupedTriples.values());
			al.addAll(queryIndividuallyTriples.values());
			
			
			if (optimize) { // run optimizer
				PlanElement optimizedPlan = null;
				try {
					PlanOperatorBase planOperatorBase = config.getPlanOptimizer().getCheapestPlan(al);
					FedQueryEngineFactory.logExplain(planOperatorBase);
					optimizedPlan = planOperatorBase.toARQPlanElement(context);
					
				} catch (PlanUnfeasibleException e) {
					throw new QueryBuildException("No feasible plan: "
							+ e);
				}
				acc.add(0,optimizedPlan);
				log.debug("selected: \n"
						+ optimizedPlan.toString());
				
			} else { // no optimization -> just add elements 
				int pos = 0;
				for (ServiceGroup sg : al) {

					if (sg instanceof MultipleServiceGroup) {
						acc.add(pos, FedPlanMultipleService.make(context,
								(MultipleServiceGroup) sg, null));
					} else
						acc.add(pos, FedPlanService.make(context, sg, null));
					pos++;

				}
			}
		}

		// unmatched patterns are executed locally 
		if (unmatchedTriples.getPattern().size() > 0)
			acc.add(0, unmatchedTriples);

		
		PlanElement ex = PlanGroup
				.make(planElt.getContext(), (List) acc, false);
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

	private void putIntoGroupedTriples(RemoteService s, Triple t) {
		ServiceGroup tg = groupedTriples.get(s);
		if (tg == null) {
			tg = new ServiceGroup(s);
			groupedTriples.put(s, tg);
		}

		tg.addB(t);
	}

	private void putIntoQueryIndividuallyTriples(Triple t, RemoteService s) {
		MultipleServiceGroup msg = queryIndividuallyTriples.get(t);
		if (msg == null) {
			msg = new MultipleServiceGroup();
			queryIndividuallyTriples.put(t, msg);
			msg.addB(t);
		}
		msg.addService(s);

	}
}

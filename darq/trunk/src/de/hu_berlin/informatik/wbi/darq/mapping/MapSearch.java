package de.hu_berlin.informatik.wbi.darq.mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.mindswap.pellet.owlapi.Reasoner;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.inference.OWLReasonerAdapter;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.inference.UnsupportedReasonerOperationException;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLClassAxiom;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLEntity;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObject;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.SWRLAtom;
import org.semanticweb.owl.model.SWRLRule;
import org.semanticweb.owl.util.OWLEntityCollector;
import org.semanticweb.owl.vocab.SWRLBuiltInsVocabulary;

import de.hu_berlin.informatik.wbi.darq.mapping.Rule;
import de.hu_berlin.informatik.wbi.darq.mapping.RulePart;


/**
 * @author Alexander Musidlowski
 * @version  1.0
 * 
 */
public class MapSearch {
	
	private static OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();
	private static OWLReasoner reasoner;
	private static Set<OWLOntology> importsclosure;
	private static URI ontologyURI = null;
	private static OWLOntology ontology;
	private static boolean init = false;
	
	public static final String OWL_CLASS = "owl_class";
	public static final String OWL_OBJECTPROPERTY = "owl_objectproperty";
	public static final String OWL_DATAPROPERTY = "owl_dataproperty";
	public static final String OWL_INDIVIDUAL = "owl_individual";
	public static final String SWRL_MULTIPLY = "http://www.w3.org/2003/11/swrlb#multiply";
	public static final String SWRL_STRINGCONCAT = "swrl_stringconcat";
	
	private static HashMap<URI, HashSet<URI>> searchIndex = new HashMap<URI, HashSet<URI>>();
	// contains a Class URI and the rules where it is in (URI of Class, URIs of Rules)
	private static HashMap<URI, Rule> rules = new HashMap<URI, Rule>();
	// stores the rules by URI with all parts of the rule (URI of Rule, Rule)
	
	
	
	
	
/* Idee: bekomme Ressource aus der Anfrage uebergeben, zusaetzlich eventuell noch 
	 * die Mapping-Ontologie.
	 * Eine Ressource kann sowohl Subjekt, Praedikat und Objekt sein, d.h. für die
	 * weitere Verarbeitung muss ich ueberpruefen, was vorliegt. 
	 * Sofern es Subjekte oder Praedikate sind, kann ich nach equivalenten Subjekten,
	 * Praedikaten suchen. (Class = Menge von Subjekt gleicher Eigenschaft, Praedikat = Property?, Individual = Subjekt)
	 * Analog Subclass, Subproperty
	 * Es ist eventuell noch zu ueberpruefen, ob das Praefix des Suchstrings mit den 
	 * Ergebnisse uebereinstimmen. Die Uebereinstimmungen koennen geloescht werden, da es 
	 * sich um Ressourcen aus der eigenen Ontologie handelt, diese also sowieso im Ergebnis
	 * enthalten sein muessten. 
	 * 
	 * 
	 * An dieser Stelle noch keine SWRL Regeln verarbeitet!
	 * 
	 */
	
//	P3
//	public Collection SearchSubclass(Cls resource, JenaOWLModel owlModel)
//	{
//		return owlModel.getDirectSubclasses(resource);
//		OntModel.listSubclasses();
//		http://jena.sourceforge.net/javadoc/com/hp/hpl/jena/ontology/OntClass.html
//		mit Iterator auslesen
//	}


	
	//P4 - OWL API
	/**
	 * looks for Subclasses of the given URI in the given ontology
	 * @return Set of URIs or an empty set if there is no subclass
	 * @param resource resource of a triple from the SPARQL query
	 * @param ontology the ontology contains the mapping where you look for subclasses
	 * 
	 */
	public static Set<URI> SearchSubclass(URI resource, OWLOntology ontology)
	{
		Set<Set<OWLClass>> subClsSets = new HashSet<Set<OWLClass>>();
		Set<OWLClass> subClsesSet = new HashSet<OWLClass>();;
		Set<URI> subClsesURISet= new HashSet<URI>();
		
		if (!init || (init && !ontologyURI.equals(ontology.getURI())) ) init(ontology);
		try {
			OWLClass nothing = owlOntologyManager.getOWLDataFactory().getOWLNothing();
			/* get Class from URI */
			OWLClass cLass = owlOntologyManager.getOWLDataFactory().getOWLClass(resource);
			
			/* get Subclasses from Class */
			subClsSets = reasoner.getSubClasses(cLass); //TODO: erzeugt ne Menge INFOs http://pellet.owldl.com/faq/logging/
			subClsesSet = OWLReasonerAdapter.flattenSetOfSets(subClsSets);
			
			/*
			 * Returns owl:nothing if there is no subclass because owl:nothing is a subclass 
			 * of every class by definition. For further processing (consistent to the other methods) 
			 * it is converted to null 
			 */
			if (!reasoner.getSubClasses(cLass).contains(nothing)){
				subClsesSet.clear();
			}
			else{
				for(OWLClass cls:subClsesSet){
					subClsesURISet.add(cls.getURI());
				}
			}
		}
		catch(OWLReasonerException ex) {
				  System.out.println("Error:[REASONER] " + ex.getMessage());
		}
		return subClsesURISet;
}
	
	/**
	 * looks for subproperties
	 * @return set of URIs or an empty set if there is no subproperty
	 * @param resource resource of a triple from the SPARQL query
	 * @param ontology the ontology contains the mapping where you look for subproperties
	 */
	public static Set<URI>  searchSubObjectProperty(URI resource, OWLOntology ontology)
	{
		Set<Set<OWLObjectProperty>> subPropSets = new HashSet<Set<OWLObjectProperty>>();
		Set<OWLObjectProperty> subProps = new HashSet<OWLObjectProperty>();
		Set<URI> subURIProps=new HashSet<URI>();
		
		if (!init || (init && !ontologyURI.equals(ontology.getURI())) ) init(ontology);
		try {
			System.out.println(reasoner.getLoadedOntologies()); //TESTAUSGABE
			OWLObjectProperty property = owlOntologyManager.getOWLDataFactory().getOWLObjectProperty(resource);
			subPropSets = reasoner.getSubProperties(property); 
						
			/*
			 * Returns an empty set if there is no subclass.
			 */
			if (!subPropSets.isEmpty()){ 
				subProps= OWLReasonerAdapter.flattenSetOfSets(subPropSets);
				for(OWLObjectProperty prop:subProps){
					subURIProps.add(prop.getURI());
				}
			}
		}
//		catch(UnsupportedOperationException exception) {
//				     System.out.println("Error:[MAPSEARCH] Unsupported reasoner operation.");
//		}
		catch(OWLReasonerException ex) {
				  System.out.println("Error:[MAPSEARCH] " + ex.getMessage());
		}
		return subURIProps;
	}
		
	public static Set<URI>  searchSubDataProperty(URI resource, OWLOntology ontology)
	{
		Set<Set<OWLDataProperty>> subPropSets = new HashSet<Set<OWLDataProperty>>();
		Set<OWLDataProperty> subProps = new HashSet<OWLDataProperty>();
		Set<URI> subURIProps=new HashSet<URI>();
		
		if (!init || (init && !ontologyURI.equals(ontology.getURI())) ) init(ontology);
		try {
			System.out.println(reasoner.getLoadedOntologies()); //TESTAUSGABE
			OWLDataProperty property = owlOntologyManager.getOWLDataFactory().getOWLDataProperty(resource);
			subPropSets = reasoner.getSubProperties(property); 
						
			/*
			 * Returns an empty set if there is no subclass.
			 */
			if (!subPropSets.isEmpty()){ 
				subProps= OWLReasonerAdapter.flattenSetOfSets(subPropSets);
				for(OWLDataProperty prop:subProps){
					subURIProps.add(prop.getURI());
				}
			}
		}
//		catch(UnsupportedOperationException exception) {
//				     System.out.println("Error:[MAPSEARCH] Unsupported reasoner operation.");
//		}
		catch(OWLReasonerException ex) {
				  System.out.println("Error:[MAPSEARCH] " + ex.getMessage());
		}
		return subURIProps;
	}
	
	
	
	/**
	 * looks for equivalent classes
	 * @return Set of URIs or an empty set if there is no equivalent class
	 * @param resource resource of a triple from the SPARQL query
	 * @param ontology the ontology contains the mapping where you look for equivalent classes
	 */
	public static Set<URI> searchEquivalentClass(URI resource, OWLOntology ontology)
	{
		Set<OWLClass> equClsSet = new HashSet<OWLClass>();
		Set<URI> equClsURISet = new HashSet<URI>();
		OWLClass cls = null;
		if (!init || (init && !ontologyURI.equals(ontology.getURI())) ) init(ontology);
		
		try {
			cls = owlOntologyManager.getOWLDataFactory().getOWLClass(resource);
			equClsSet = reasoner.getEquivalentClasses(cls);
			if (!equClsSet.isEmpty()){ 
				for(OWLClass clss : equClsSet){
					equClsURISet.add(clss.getURI());
				}
			}
		}
//		catch(UnsupportedOperationException exception) {
//				     System.err.println("Error:[REASONER] Unsupported reasoner operation.");
//		}
		catch(OWLReasonerException ex) {
				  System.err.println("Error:[REASONER] : " + ex.getMessage());
		}
		catch(RuntimeException ex){ 
			equClsURISet.clear();
			System.err.println("Warning: [Equivalent Class] Class" + resource + " not found");
			/* 
			 * if there is no equivalent class a RTE is thrown instead of an empty set
			 * Bugreport for Pellet exists. maybe fixed in a new version
			 * http://cvsdude.com/trac/clark-parsia/pellet-devel/ticket/90
			 */ 
		}
		return equClsURISet;		
	}
	
	
	
	/**
	 * looks for equivalent Properties
	 * @return Set of URIs or an empty set if there is no equivalent property
	 * @param resource resource of a triple from the SPARQL query
	 * @param ontology the ontology contains the mapping where you look for equivalent properties
	 */
	public static Set<URI> searchEquivalentObjectProperty(URI resource, OWLOntology ontology)
	{
		Set<OWLObjectProperty> equPropSet = new HashSet<OWLObjectProperty>();
		Set<URI> equPropURISet = new HashSet<URI>();
		if (!init || (init && !ontologyURI.equals(ontology.getURI())) ) init(ontology);
		try {			
			
			OWLObjectProperty property = owlOntologyManager.getOWLDataFactory().getOWLObjectProperty(resource);			 							 
			equPropSet = reasoner.getEquivalentProperties(property); //returns RuntimeException if nothing found
			for(OWLObjectProperty prop:equPropSet){
				equPropURISet.add(prop.getURI());
			}
		}
//		catch(UnsupportedOperationException exception) {
//				     System.err.println("Error:[REASONER] Unsupported reasoner operation.");
//		}
		catch(OWLReasonerException ex) {
				  System.err.println("Error:[REASONER] " + ex.getMessage());
		}
		catch(RuntimeException ex){// catches the exception, when property is not found in the ontology 
			equPropURISet.clear();//returns emtpy set as the other do
			System.err.println("Warning: [Equivalent Property] Property " + resource + " not found");
		}
		return equPropURISet;	
	}
	
	public static Set<URI> searchEquivalentDataProperty(URI resource, OWLOntology ontology)
	{
		Set<OWLDataProperty> equPropSet = new HashSet<OWLDataProperty>();
		Set<URI> equPropURISet = new HashSet<URI>();
		if (!init || (init && !ontologyURI.equals(ontology.getURI())) ) init(ontology);
		try {			
			
			OWLDataProperty property = owlOntologyManager.getOWLDataFactory().getOWLDataProperty(resource);			 							 
			equPropSet = reasoner.getEquivalentProperties(property); //returns RuntimeException if nothing found
			for(OWLDataProperty prop:equPropSet){
				equPropURISet.add(prop.getURI());
			}
		}
//		catch(UnsupportedOperationException exception) {
//				     System.err.println("Error:[REASONER] Unsupported reasoner operation.");
//		}
		catch(OWLReasonerException ex) {
				  System.err.println("Error:[REASONER] " + ex.getMessage());
		}
		catch(RuntimeException ex){// catches the exception, when property is not found in the ontology 
			equPropURISet.clear();//returns emtpy set as the other do
			System.err.println("Warning: [Equivalent Property] Property " + resource + " not found");
		}
		return equPropURISet;	
	}

	/**
	 * looks for equivalent Individuals (same as relationship)
	 * @param resource resource of a triple from the SPARQL query
	 * @param ontology the ontology contains the mapping where you look for individuals
	 */
	public static Set<URI> searchSameIndividual(URI resource, OWLOntology ontology)
	{
		Set<OWLIndividual> sameIndiSet = new HashSet<OWLIndividual>();
		Set<URI> sameIndiURISet = new HashSet<URI>();
		
		if (!init || (init && !ontologyURI.equals(ontology.getURI())) ) init(ontology);
		try {
			
			OWLIndividual individual = owlOntologyManager.getOWLDataFactory().getOWLIndividual(resource);
			/* sameIndiSet = reasoner.getSameAsIndividuals(individual);
			 * 
			 * function does not yet exist in the OWL API, Bugreport generated
			 * https://sourceforge.net/tracker/index.php?func=detail&aid=1903936&group_id=90989&atid=595534
			 * 
			 * Alternative direct action on pellet without OWL API
			 */ 
			sameIndiSet = getSameAsIndividuals(individual, ontology);
			
			for(OWLIndividual indi:sameIndiSet){
				sameIndiURISet.add(indi.getURI());
			}
		}
		catch(UnsupportedOperationException exception) {
				     System.err.println("Error:[MAPSEARCH] Unsupported reasoner operation.");
		}
		owlOntologyManager.removeOntology(ontology.getURI());
		return sameIndiURISet;	
	}
	
	/*
	 * Workaround: As long as getSameAsIndividuals(OWLIndividual) is not implemented in the OWL API
	 * i have to use direct access to pellet reasoner
	 */
	private static Set<OWLIndividual> getSameAsIndividuals(OWLIndividual individual, OWLOntology ontology){
		Set<OWLIndividual> individuals = new HashSet<OWLIndividual>();
		Reasoner pellet = new Reasoner(owlOntologyManager);		
		pellet.setOntology(ontology);
		pellet.classify();
		individuals = pellet.getSameAsIndividuals(individual); //TODO TEST //return Set Individuals not OWLIndividuals		
		return individuals;
	}
	
	/*
	 * Idee: SWRL Ontologie wird übergeben, Regeln also auch. Es muss eine neue Struktur aus den Regeln aufgebaut werden.
	 * Wie kann man verhindern, dass es jedesmal neu gemacht wird?
	 * Reicht es wenn ich eine Methode init schreibe, wo die Struktur aufgebaut wird und dann init abfrage? 
	 */
	
	
	
	/*
	 * Init loads the ontology, creates a reasoner and puts the rules in 
	 * a searchable structure.
	 * Init is called if no ontology is available or the ontology has changed
	 * 
	 */
	private static void init(OWLOntology ontology){
		
		Set<SWRLRule> ontologyRules = ontology.getRules();
		ontologyURI = ontology.getURI();
		MapSearch.ontology = ontology;

		//create reasoner and commit the ontology
		try{	
			reasoner = createReasoner(owlOntologyManager);
			Set<OWLOntology> importsClosure = owlOntologyManager.getImportsClosure(ontology);
			reasoner.loadOntologies(importsClosure);
		}
		catch(UnsupportedOperationException exception) {
			System.err.println("Error:[MAPSEARCH] Unsupported reasoner operation.");
			init= false;
		}
		catch(OWLReasonerException ex) {
			System.err.println("Error:[MAPSEARCH] " + ex.getMessage());
			init = false;
		}

		//create new structure for rules
		
		for (SWRLRule swrlRule : ontologyRules) {
			if (swrlRule.isLogicalAxiom()) {
				analyzeBody(swrlRule);
				analyzeHead(swrlRule);	
			}
		}
		/* kann frühestens in AddRulePart geschehen, 
		 * da dort die Regel erstellt wird. Dies würde
		 * AddRulePart aber komplexer gestalten, daher der
		 * einfache Weg die Builtins nachträglich zu setzen
		 */
		for(Rule rule : rules.values()){
			for (RulePart part : rule.getRulePartList()) {
				if (part.getType().equals(SWRL_MULTIPLY)) {
					rule.setMultiply();
				}
				if (part.getType().equals(SWRL_STRINGCONCAT)) {
					rule.setStringConcat();
				}
			}
		}
		init = true;
	}

	/*
	 * Liefert die Regeln zurück, in denen die übergebene URI 
	 * enthalten ist. 
	 */
	public static HashSet<Rule> searchRules(URI reference, OWLOntology ontology) {
		HashSet<URI> rulesURI = null;
		HashSet<Rule> foundRules = new HashSet<Rule>();
		if (!init || (init && !ontologyURI.equals(ontology.getURI())) ) init(ontology);
		rulesURI =searchIndex.get(reference); 
		if (rulesURI != null){
			for (URI ruleURI : rulesURI) {
				foundRules.add(rules.get(ruleURI));
			}
		}/* else: no rules exists */
		return(foundRules);
	}
	
	
	//TODO Fehlerbehandlung: Wenn im Body Fehler aufgetreten sind, Head nicht weiter bearbeiten
	//klappt derzeit, weil nur Regeln ohne Kopf als Fehler existieren
	// sollte nicht mehr nötig sein, sofern Throw Exception funktioniert bzw.
	//muss diese dann beim Durchlaufen der Regeln abgefangen werden. 
	private static void analyzeHead(SWRLRule rule) {
		Set<SWRLAtom> head;
		OWLObject predicate;
//		OWLClass owlClass;
//		OWLDataProperty dataProperty;
//		OWLObjectProperty objectProperty;
//		OWLIndividual owlIndividual;
		RulePart part;
		Set<URI> uri = new HashSet<URI>();
		URI ruleURI, partURI;
//		System.out.println("----------------------------  Start HEAD ------------------------------");
		head = rule.getHead();
		ruleURI = rule.getURI();
		// System.out.println("Kopf: " + head); TESTAUSGABE
		if (head.size() == 0) {
			try {
				throw new UnsupportedReasonerOperationException("Error [SWRL]: An empty head is not allowed!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (head.size() == 1) {
			// System.out.println("HeadSize 1 "); TESTAUSGABE
			if (head.iterator().next().getPredicate() instanceof SWRLBuiltInsVocabulary) {
				System.err.println("Error [SWRL]: Rule " + rule + " might be correct but is not supported.");
			} else {
				predicate = (OWLObject) head.iterator().next().getPredicate();
				// System.out.println("Predicate: " + predicate); //TESTAUSGABE
				uri.addAll(getURI(predicate));
				if (uri.size() == 1) {
					// System.out.println("URISize 1");//TESTAUSGABE
					partURI = uri.iterator().next();
					/* neuen Regelteil erzeugen */
					part = new RulePart(partURI, ruleURI, "h");
					addRulePart(partURI, part);
					/* Typ des Regelteils feststellen */
					setType(part);
				} else if (uri.size() > 1) {
					// System.out.println("URI Size > 1");//TESTAUSGABE
					System.err.println("Error [SWRL]: The rule " + rule + " might be correct but is not supported");
					// TODO Was bedeutet das? Body = 1 und Uri > 1? gibt es den
					// Fall?
					// 3 intwerte bei multiply?
				} else {// URIsize == 0
				// System.out.println("URI Size == 0");//TESTAUSGABE
					System.err.println("Error [SWRL]: The rule " + rule + " might be correct but is not supported");
				}
			}
		} /*-----------------------------------        Head Size > 1              -----------------------------*///TESTAUSGABE
		else {// Head besteht aus mehrere Atomen
			System.err.println("Error [SWRL]: Rule is not supported " + rule + ". Just one part in the head allowed");

			// for (SWRLAtom headAtom : head) {
			// predicate = (OWLObject) headAtom.getPredicate();
			// }
		}
//		System.out.println("----------------------------  END HEAD ------------------------------");//TESTAUSGABE
	}
	
	// antecedent
	/*
	 * Wie kann der Body aussehen? 1. Fall enthält eine Klasse, hat nur ein
	 * Argument ?x (Variable) 2. Fall enthält ein Prädikat (Beziehung), hat
	 * genau zwei Arguemtn ?x,?y 3. Fall enthält ein SWRLBuiltin
	 * 
	 * Argument beginnen mit "?" Es können sowohl Variablen als auch Klassen,
	 * Prädikate als auch Literale (zahlen) sind eingeschlossen in " "
	 * 
	 * Wie kann der Head aussehen? 1. Fall enthält Klasse, hat nur ein Argument
	 * ?x (Variable) --> do nothing (wird bereits mit getsubclass erledigt) 2.
	 * Fall enthält ein Prädikat (Beziehung), hat genau zwei Arguemtn ?x,?y,
	 * analog zu Fall 1 3. Fall Unterscheidung der Builtin ins, muss hier
	 * überhaupt etwas gemacht werden? Sollte der Head nicht leer sein? Stimmt
	 * es überhaupt, dass hier Builtins überprüft werden müssen? 4. Fall
	 */
	private static void analyzeBody(SWRLRule rule) {
		Set<SWRLAtom> body;

		URI ruleURI, partURI;

		int startPosition, endPosition;
		String multiply;
		double multiplier = 0;

		Set<URI> uri = new HashSet<URI>();
		RulePart part = new RulePart(null, null, null);

		OWLObject predicate = null;
		ruleURI = rule.getURI();
		body = rule.getBody();
//			System.out.println("----------------------------  START BODY ------------------------------");
//		 System.out.println("Body: " + body); //TESTAUSGABE
		if (body.size() == 0) {
			try {
				throw new UnsupportedReasonerOperationException("Error [SWRL]: An empty body is not allowed!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (body.size() == 1) {
//			 System.out.println("BodySize 1");//TESTAUSGABE

			/*------  Builtin  -------*/
			if (body.iterator().next().getPredicate() instanceof SWRLBuiltInsVocabulary) {
				System.err.println("Error [SWRL]: Rule " + rule + " might be correct but is not supported.");
			}

			/*------ anything else than Builtin -------*/
			else {
				predicate = (OWLObject) body.iterator().next().getPredicate();
//				 System.out.println("Predicate: " + predicate);//TESTAUSGABE
				uri.addAll(getURI(predicate));
				// TODO URI auf Class, Property usw. überprüfen
				if (uri.size() == 1) {
//					 System.out.println("URISize 1");//TESTAUSGABE
					partURI = uri.iterator().next();
					part = new RulePart(partURI, ruleURI, "b");
					addRulePart(partURI, part);
					setType(part);

				} else if (uri.size() > 1) {
//					 System.out.println("Uri Size > 1");//TESTAUSGABE
					System.err.println("Error [SWRL][1]: The rule " + rule + " might be correct but is not supported");
					// TODO Was bedeutet das? Body = 1 und Uri > 1? gibt es den
					// Fall?
					// 3 intwerte bei multiply?
				} else {// URI = Empty [] null, es handelt sich um ein Builtin?!
					// Kann auch ne Variable sein
					// throw new IllegalArgumentException( "Error [MAPSEARCH]: Illegal body in rule: " + rule);
					/*
					 * wenn bodysize = 1, darf es kein SWRLVocabulary sein!
					 * (Eventuell separat prüfen)
					 */
					System.err.println("Error [MAPSEARCH][2]: Illegal body in rule: " + rule);
				}
			}// End Instanceof
		}
		/*-----------------------------------        Body Size > 1              -----------------------------*/

		/*
		 * erst auf BuiltIn prüfen, ansonsten muss es ein OWLObject sein
		 */
		else {// sollten erstmal nur Builtins sein
//		 System.out.println("BodySize > 1");// TESTAUSGABE
			// Collection<SWRLAtomObject> arguments = new HashSet<SWRLAtomObject>();
			for (SWRLAtom bodyAtom : body) { // hier wird der Body durchlaufen
				RulePart part1; // TEST ob, dann sich im part der richtige String wiederfindet
				uri.clear();

				/*------ Builtin -------*/
				if (bodyAtom.getPredicate() instanceof SWRLBuiltInsVocabulary) {
					SWRLBuiltInsVocabulary builtin = (SWRLBuiltInsVocabulary) bodyAtom.getPredicate();
					partURI = builtin.getURI();
//					System.out.println("BuiltIn: "+ partURI);//TESTAUSGABE
					part1 = new RulePart(partURI, ruleURI, "b");
					addRulePart(partURI, part1);
					if (builtin.name().equals("MULTIPLY")) {
						
						part1.setType(SWRL_MULTIPLY);
						/* get Multiplier off the string */
						multiply = bodyAtom.getAllArguments().toString();
						startPosition = multiply.indexOf('"') + 1;
						endPosition = multiply.indexOf('"', startPosition);
						multiply = multiply.substring(startPosition, endPosition);
						
						try {
							multiplier = Double.valueOf(multiply).doubleValue();
						} catch (NumberFormatException e) {
							System.err.println("Error [SWRL]: Multiplier in rule " + rule + " could not be converted into a double value.");
						}
						part1.setMultiplier(multiplier);

					} 
					else if (builtin.name().equals("STRING_CONCAT")) {
						part1.setType(SWRL_STRINGCONCAT);
					} 
					else {
						System.err.println("Error [SWRL][3]: SWRL BuiltIn " + builtin.getShortName() + " not supported.");
					}
				}

				/*------ anything else than Builtin -------*/
				else { // dürften nur noch OWLObject sein
					try {
						predicate = (OWLObject) bodyAtom.getPredicate();
//						 System.out.println("Predicate: " +predicate);//TESTAUSGABE
					} catch (ClassCastException e) {
						// wenn der Cast nicht klappt, ist es kein
						// OWLObject(sollte eigentlich nicht passieren)
						try {
							throw new Exception("Error: [SWRL][4]: Rule is not supported " + rule);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					uri.addAll(getURI(bodyAtom));
					
					if (uri.size() == 1) {
						partURI = uri.iterator().next();
//						System.out.println("PartURI " + partURI); //TESTAUSGABE
						part1 = new RulePart(partURI, ruleURI, "b");
						addRulePart(partURI, part1);
						setType(part1);
					} 
					else { // URIsize != 1
						System.err.println("Error [SWRL][5]: The rule " + rule + " might be correct but is not supported");
					}
				}// End Else Instanceof
			} // EndFor BodyAtoms
		}// End Else BodySize
		
//		System.out.println("----------------------------  END BODY ------------------------------");//TESTAUSGABE

	} // Method close

	
	/*
	 * Kann SWRLAtom mehr als ein Object haben, wenn Bodysize == 1? TEST es wird
	 * nicht nur ein SWRLAtom übergeben, sondern auch Predicate (SWRLObject) und
	 * Argumente (SWRLObject?)
	 */
	private static Set<URI> getURI(OWLObject atom) {
		Set<URI> uri = new HashSet<URI>();
		OWLEntityCollector collector = new OWLEntityCollector();
		Set<OWLEntity> atomParts = new HashSet<OWLEntity>();
		atom.accept(collector);
		atomParts = collector.getObjects();
		if (atomParts.size() == 1) {
			uri.add(atomParts.iterator().next().getURI());
		} else {
			for (OWLEntity object : atomParts) {
				System.err.println("Error [SWRL][6]: There is more than one URI for this part of the rule: " + object.getURI());
				uri.add(object.getURI());
			}
		}
		return uri;
	}

	/*
	 * checks the object type the URI belongs to and saves it into the
	 * RulePart, built ins a handled separatly
	 */
	private static void setType(RulePart part) {
		boolean owlClass, dataProperty, objectProperty, owlIndividual;
		URI partURI = part.getUri();
		// System.out.println("------------ SET TYPE ------------");//TESTAUSGABE
		// System.out.println("Part: " + part.getUri());
		owlClass = ontology.containsClassReference(partURI);
		if (!owlClass) {
			dataProperty = ontology.containsDataPropertyReference(partURI);
			if (!dataProperty) {
				objectProperty = ontology.containsObjectPropertyReference(partURI);
				if (!objectProperty) {
					owlIndividual = ontology.containsIndividualReference(partURI);
					if (!owlIndividual) {
						System.err.println("Error [SWRL] Unsupported object in rule" + part.getRuleURI());
						// Was hat denn noch eine URI?
					} 
					else {part.setType(OWL_INDIVIDUAL);}
				} 
				else {part.setType(OWL_OBJECTPROPERTY);}
			} 
			else {part.setType(OWL_DATAPROPERTY);}
		} 
		else {part.setType(OWL_CLASS);}
		
//		TESTAUSGABE
		// System.out.println(" Type : " + part.getUri());
		// System.out.println(" Type : " + part.getType());
		// System.out.println("------------ END SET TYPE ------------");
	}

	/**
	 * There are two structures 'searchIndex' and 'rules'. 'searchIndex'
	 * contains all the possible parts (Class, Property, Individual,
	 * SWRLBuiltIn) of a rule and the URI of the rule. Rules contains all rules,
	 * accessible by their URI, with the parts of the rule.
	 */
	/*
	 * Improvement: There are already Subclasses of RulePart (like Multiply or
	 * StringConcat) which contain additional usefull information. These
	 * Information should be set through the constructor, now it is set through
	 * set methods
	 */
	private static void addRulePart(URI partURI, RulePart part) {
		ArrayList<RulePart> partList;
		Rule rule;
		HashSet<URI> rulesURI;
		URI ruleURI = part.getRuleURI();
//		 System.out.println("--------------- Add Rule Part -------------");\\TESTAUSGABE

		/* 1.Fall Klasse noch nicht vorhanden */
		if (searchIndex.get(partURI) == null) {
			/* URI der Klasse existiert noch nicht in der HashMap */
			rulesURI = new HashSet<URI>(); // new HashSet for the rules
			rulesURI.add(ruleURI); // add current rule to HashSet
			searchIndex.put(partURI, rulesURI);
			// update searchIndex Class and List of rules belonging to the class

			/* 2.Fall Klasse nicht vorhanden, aber Regel exisitiert schon */
			// (erzeugt von einer anderen Klasse)
			if (rules.containsKey(ruleURI)) {
				// if this rule already exists, add part
//				 System.out.println("2.Fall Klasse existiert nicht, Regel schon"); TESTAUSGABE
				rules.get(ruleURI).addPart(part);
			} else {
				// else create new PartList, add part and put into the Hashmap
				partList = new ArrayList<RulePart>(); // new PartList
				partList.add(part); // add RulePart to PartList
				rule = new Rule(ruleURI, partList); // create new Rule
				rules.put(ruleURI, rule); // store rule with partList

//				TESTAUSGABE
//				 System.out.println("1. Fall (alles neu)");
//				 System.out.println("Key: " + partURI);
//				 System.out.println("Rule: " + ruleURI);
//				 System.out.println("Part: " + part.getUri());
			}
		}

		/* prüfen, ob die Regel schon existiert */
		/* 3.Fall Klasse vorhanden und Regel nicht vorhanden */
		else if (!rules.containsKey(ruleURI)) {
			rulesURI = searchIndex.get(partURI);
			/* List of all rules connected to the class */
			// get HashSet with rules from the class
			rulesURI.add(ruleURI); // add current rule to HashSet of rules,
			// (Relationship between rules and
			// searchindex)
			partList = new ArrayList<RulePart>(); // new PartList
			partList.add(part); // add RulePart to PartList
			rule = new Rule(ruleURI, partList); // create new Rule
			rules.put(ruleURI, rule); // store rule with partList

//			TESTAUSGABE
//			 System.out.println("2.Fall Klasse existiert, neue Regel");
//			 System.out.println("Key: " + partURI);
//			 System.out.println("Rule: " + ruleURI);
//			 System.out.println("Part: " + part.getUri());
		}

		/* 4. Fall Klasse vorhanden, Regel vorhanden */
		else {
			if (!searchIndex.get(partURI).contains(ruleURI)) {
				// searchIndex already contains the class but not the rule
				rulesURI = searchIndex.get(partURI);
				// get HashSet with rules from the class
				rulesURI.add(ruleURI); // add current rule to HashSet
			}
			rule = rules.get(ruleURI); // get the rule
			rule.addPart(part); // add part

//			TESTAUSGABE
//			 System.out.println("3.Fall part hinzufügen"); 
//			 System.out.println("Key: " + partURI);
//			 System.out.println("Rule: " + ruleURI);
//			 System.out.println("Part: " + part.getUri());
		}
//		 System.out.println("--------------- End Rule Part -------------");

	}
	
	//Test End 
	
	
	/**
	 * loading Reasoner
	 * @author Mathew Horridge
	 * @return OWLReasoner
	 */
	
	private static OWLReasoner createReasoner(OWLOntologyManager man) {
        try {
            // The following code is a little overly complicated.  The reason for using
            // reflection to create an instance of pellet is so that there is no compile time
            // dependency (since the pellet libraries aren't contained in the OWL API repository).
            // Normally, one would simply create an instance using the following incantation:
            //
            //     OWLReasoner reasoner = new Reasoner()
            //
            // Where the full class name for Reasoner is org.mindswap.pellet.owlapi.Reasoner
            //
            // Pellet requires the Pellet libraries  (pellet.jar, aterm-java-x.x.jar) and the
            // XSD libraries that are bundled with pellet: xsdlib.jar and relaxngDatatype.jar
            String reasonerClassName = "org.mindswap.pellet.owlapi.Reasoner";
            Class reasonerClass = Class.forName(reasonerClassName);
            Constructor<OWLReasoner> con = reasonerClass.getConstructor(OWLOntologyManager.class);
            return con.newInstance(man);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    } 

	/*
	 * writes all rules with all parts of a rule to the command line
	 * (method just for test purpose) 
	 */
	private static void output() {

		ArrayList<RulePart> partList;
		Rule rule;
		HashSet<URI> rulesURIout;
		System.out.println("--------------- Content -------------");
		int i = 0;
		for (URI uri : searchIndex.keySet()) {
			rulesURIout = searchIndex.get(uri);
			System.out.println("Key " + uri);
			// holt sich die Regellisten zur Klasse
			for (URI ruleURIout : rulesURIout) {
				// holt sich die URI aus den Regellisten
				rule = rules.get(ruleURIout);
				partList = rule.getRulePartList();
				System.out.println("Regel: " + ruleURIout);
				for (RulePart partout : partList) {
					i++;
					System.out.println("	Regelteil : " + partout.getUri() + "(" + i + ")");
					System.out.println("	Type : " + partout.getType());
				}
			}
		}
		System.out.println("--------------- Content -------------");
	}
	
	
//Test Begin
public static void allAxiomsfromClass(URI clsURI, OWLOntology ontology){
	Set<OWLAxiom> Axioms = new HashSet<OWLAxiom>();
	Set<OWLClassAxiom> ClassAxioms = new HashSet<OWLClassAxiom>();;
	OWLClass cls;
	if (!init) init(ontology);
	try{
	cls = owlOntologyManager.getOWLDataFactory().getOWLClass(clsURI);
	
//		owlOntologyManager.getOWLDataFactory().getOWLClass(arg0
			Axioms = ontology.getAxioms();
			ClassAxioms = ontology.getAxioms(cls);
			//Test Begin
			System.out.println("Axiome von OWLClass: "+cls); 
			for(OWLClassAxiom axiom : ClassAxioms) { 
			  System.out.println("Axioms    " + axiom);
			}
			for(OWLAxiom axiom : Axioms) { 
				  System.out.println("Axioms    " + axiom);
			}
			//Test End
		
			for(OWLClass clas : ontology.getReferencedClasses()) {
//				System.out.println("referencedClass: " + clas.getURI());
				Set<OWLClass> equClsSet = reasoner.getEquivalentClasses(clas);
				System.out.println("Äquivalente Ref-Klassen von "+clas.getURI());
				for(OWLClass subcls : equClsSet) {
				  	System.out.println("    " + subcls.getURI());
			  }
			}
			for(OWLClass clas : ontology.getReferencedClasses()) {
				System.out.println("referencedClass: " + clas.getURI());
			}				
	}
	catch(UnsupportedOperationException exception) {
	     System.err.println("Unsupported reasoner operation.");
	}
	catch(OWLReasonerException ex) {
	  System.err.println("Error:[REASONER]  " + ex.getMessage());
	}
	catch(RuntimeException ex){// catches the exception, when property is not found in the ontology (subs returns null in this case, so there is no need for that) 
		System.err.println("Warning:[MAPSEARCH] Runtime Exception"+ex);
	}
	catch(Exception e){
		System.err.println("Error:[MAPSEARCH] "+e);
	}
}


}

//Objectproperty vs Dataproperty?
//Objectproperty sind Relationen zwischen Individuen
//DataProperty sind auf Datentypen bezogen, welchen Datentyp (xsd^^int) verlangt die Property

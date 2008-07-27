package de.hu_berlin.informatik.wbi.darq.mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
	public static final String SWRL_STRINGCONCAT = "http://www.w3.org/2003/11/swrlb#stringConcat";
	
	private static HashMap<URI, HashSet<URI>> searchIndex = new HashMap<URI, HashSet<URI>>();
	// contains a Class URI and the rules where it is in (URI of Class, URIs of Rules)
	private static HashMap<URI, Rule> rules = new HashMap<URI, Rule>();
	// stores the rules by URI with all parts of the rule (URI of Rule, Rule)
	
	static Log log = LogFactory.getLog(MapSearch.class);
	
	
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
			log.error("Problems with reasoner "+ ex.getMessage());
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
			log.debug(reasoner.getLoadedOntologies()); //TESTAUSGABE
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
//				     log.error("Unsupported reasoner operation.");
//		}
		catch(OWLReasonerException ex) {
			log.error("Problems with reasoner " + ex.getMessage());
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
			log.debug(reasoner.getLoadedOntologies()); //TESTAUSGABE
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
//				     log.error("Unsupported reasoner operation.");
//		}
		catch(OWLReasonerException ex) {
			log.error("Problems with reasoner " + ex.getMessage());
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
//				     log.error("Unsupported reasoner operation.");
//		}
		catch(OWLReasonerException ex) {
			log.error("Problems with reasoner " + ex.getMessage());
		}
		catch(RuntimeException ex){ 
			equClsURISet.clear();
			log.warn("Equivalent class " + resource + " not found.");
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
//				     log.error("Unsupported reasoner operation.");
//		}
		catch(OWLReasonerException ex) {
			log.error("Problems with reasoner " + ex.getMessage());
		}
		catch(RuntimeException ex){// catches the exception, when property is not found in the ontology 
			equPropURISet.clear();//returns emtpy set as the other do
			log.warn("Equivalent property " + resource + " not found.");
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
//				    log.error("Unsupported reasoner operation.");
//		}
		catch(OWLReasonerException ex) {
			log.error("Problems with reasoner " + ex.getMessage());
		}
		catch(RuntimeException ex){// catches the exception, when property is not found in the ontology 
			equPropURISet.clear();//returns emtpy set as the other do
			log.warn("Equivalent property " + resource + " not found.");
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
			log.error("Unsupported reasoner operation.");
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
	
	
	/**
	 * Init loads the ontology, creates a reasoner and puts the rules in 
	 * a searchable structure.
	 * Init is called if no ontology is available or the ontology has changed
	 * @param ontology mapping ontology
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
			log.error("Unsupported reasoner operation.");
			init= false;
		}
		catch(OWLReasonerException ex) {
			log.error("Problems with reasoner " + ex.getMessage());
			init = false;
		}

		//create new structure for rules
		for (SWRLRule swrlRule : ontologyRules) {
			if (swrlRule.isLogicalAxiom()) {
				analyzeBody(swrlRule);
				analyzeHead(swrlRule);	
			}
		}

		/* set rule type */
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

	/**
	 * return rules for the given URI (of part of a triple)
	 * @param reference Part of triple
	 * @param ontology Mapping ontology 
	 * @return Set of Rules for the given URI
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
	
	/**
	 * Analyzes the head of a rule and puts it into 
	 * RulePart
	 * @param rule SWRL rule
	 * */
	private static void analyzeHead(SWRLRule rule) {
		Set<SWRLAtom> head;
		OWLObject predicate;
		RulePart part;
		Set<URI> uri = new HashSet<URI>();
		URI ruleURI, partURI;
		log.debug("----------------------------  Start HEAD ------------------------------");
		head = rule.getHead();
		ruleURI = rule.getURI();
		log.debug("Head: " + head); //TESTAUSGABE
		if (head.size() == 0) {
			try {
				throw new UnsupportedReasonerOperationException("Error [SWRL]: An empty head is not allowed!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (head.size() == 1) {
			log.debug("HeadSize 1 "); //TESTAUSGABE
			if (head.iterator().next().getPredicate() instanceof SWRLBuiltInsVocabulary) {
				log.error("[SWRL]: Rule " + rule + " might be correct but is not supported.");
			} else {
				predicate = (OWLObject) head.iterator().next().getPredicate();
				log.debug("Predicate: " + predicate); //TESTAUSGABE
				uri.addAll(getURI(predicate));
				if (uri.size() == 1) {
					log.debug("URISize 1");//TESTAUSGABE
					partURI = uri.iterator().next();
					/* create new RulePart */
					part = new RulePart(partURI, ruleURI, "h");
					addRulePart(partURI, part);
					/* get the type of the RulePart */
					setType(part);
				} else if (uri.size() > 1) {
					log.debug("URI Size > 1");//TESTAUSGABE
					log.error("[SWRL]: The rule " + rule + " might be correct but is not supported");				
				} else {// URIsize == 0
				log.debug("URI Size == 0");//TESTAUSGABE
					log.error("[SWRL]: The rule " + rule + " might be correct but is not supported");
				}
			}
		} /*-----------------------------------        Head Size > 1              -----------------------------*/
		else {// Head consists of multiple atoms
			log.error("[SWRL]: Rule is not supported " + rule + ". Just one part in the head allowed");
		}
		log.debug("----------------------------  END HEAD ------------------------------");//TESTAUSGABE
	}
	
	/**
	 * Analyze the Body of a rule and puts it into
	 * a RulePart
	 * @param rule SWRL rule
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
		log.debug("----------------------------  START BODY ------------------------------");
		log.debug("Body: " + body); //TESTAUSGABE
		if (body.size() == 0) {
			try {
				throw new UnsupportedReasonerOperationException("Error [SWRL]: An empty body is not allowed!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (body.size() == 1) {
			 log.debug("BodySize 1");//TESTAUSGABE

			/*------  Builtin  -------*/
			if (body.iterator().next().getPredicate() instanceof SWRLBuiltInsVocabulary) {
				log.error("[SWRL]: Rule " + rule + " might be correct but is not supported.");
			}

			/*------ anything else than Builtin -------*/
			else {
				predicate = (OWLObject) body.iterator().next().getPredicate();
				 log.debug("Predicate: " + predicate);//TESTAUSGABE
				uri.addAll(getURI(predicate));
				if (uri.size() == 1) {
					log.debug("URISize 1");//TESTAUSGABE
					partURI = uri.iterator().next();
					part = new RulePart(partURI, ruleURI, "b");
					addRulePart(partURI, part);
					setType(part);

				} else if (uri.size() > 1) {
					log.debug("Uri Size > 1");//TESTAUSGABE
					log.error("[SWRL]: The rule " + rule + " might be correct but is not supported");
				} else {
					log.error("[SWRL]: Illegal body in rule: " + rule);
				}
			}// End Instanceof
		}
		/*-----------------------------------        Body Size > 1              -----------------------------*/

		/*
		 * first check if it is a builtin otherwise it has to be an OWLObject
		 */
		else {// now it should be just builtins
		 log.debug("BodySize > 1");// TESTAUSGABE

			for (SWRLAtom bodyAtom : body) { // runs through body
				RulePart part1; 
				uri.clear();

				/*------ Builtin -------*/
				if (bodyAtom.getPredicate() instanceof SWRLBuiltInsVocabulary) {
					SWRLBuiltInsVocabulary builtin = (SWRLBuiltInsVocabulary) bodyAtom.getPredicate();
					partURI = builtin.getURI();
					log.debug("BuiltIn: "+ partURI);//TESTAUSGABE
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
							log.error("[SWRL]: Multiplier in rule " + rule + " could not be converted into a double value.");
						}
						part1.setMultiplier(multiplier);

					} 
					else if (builtin.name().equals("STRING_CONCAT")) {
						part1.setType(SWRL_STRINGCONCAT);
					} 
					else {
						log.error("[SWRL]: BuiltIn " + builtin.getShortName() + " not supported.");
					}
				}

				/*------ anything else than Builtin -------*/
				else { // just OWLObjects
					try {
						predicate = (OWLObject) bodyAtom.getPredicate();
						log.debug("Predicate: " +predicate);//TESTAUSGABE
					} catch (ClassCastException e) {
						try {
							throw new Exception("Error: [SWRL][4]: Rule is not supported " + rule);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					uri.addAll(getURI(bodyAtom));
					
					if (uri.size() == 1) {
						partURI = uri.iterator().next();
						log.debug("PartURI " + partURI); //TESTAUSGABE
						part1 = new RulePart(partURI, ruleURI, "b");
						addRulePart(partURI, part1);
						setType(part1);
					} 
					else { // URIsize != 1
						log.error("[SWRL]: The rule " + rule + " might be correct but is not supported");
					}
				}// End Else Instanceof
			} // EndFor BodyAtoms
		}// End Else BodySize
		
		log.debug("----------------------------  END BODY ------------------------------");//TESTAUSGABE

	} // Method close

	
	/**
	 *  get the URI of the Atom
	 *  @param atom Part of the Builtin*/
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
				log.error("[SWRL]: There is more than one URI for this part of the rule: " + object.getURI());
				uri.add(object.getURI());
			}
		}
		return uri;
	}

	/**
	 * checks the object type the URI belongs to and saves it into the
	 * RulePart, built ins a handled separatly
	 * @param part Part of a Rule
	 */
	private static void setType(RulePart part) {
		boolean owlClass, dataProperty, objectProperty, owlIndividual;
		URI partURI = part.getUri();
		log.debug("------------ SET TYPE ------------");//TESTAUSGABE
		log.debug("Part: " + part.getUri());
		owlClass = ontology.containsClassReference(partURI);
		if (!owlClass) {
			dataProperty = ontology.containsDataPropertyReference(partURI);
			if (!dataProperty) {
				objectProperty = ontology.containsObjectPropertyReference(partURI);
				if (!objectProperty) {
					owlIndividual = ontology.containsIndividualReference(partURI);
					if (!owlIndividual) {
						log.error("[SWRL] Unsupported object in rule" + part.getRuleURI());
					} 
					else {part.setType(OWL_INDIVIDUAL);}
				} 
				else {part.setType(OWL_OBJECTPROPERTY);}
			} 
			else {part.setType(OWL_DATAPROPERTY);}
		} 
		else {part.setType(OWL_CLASS);}
		
//		TESTAUSGABE
		log.debug(" Type : " + part.getUri());
		log.debug(" Type : " + part.getType());
		log.debug("------------ END SET TYPE ------------");
	}

	/*
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
	
	/**
	 * Gets a part of a SWRL Rule and puts it into a RulePart
	 * 
	 *@param partURI URI of the part to add to the rule
	 *@param part Part of a rule 
	 */
	private static void addRulePart(URI partURI, RulePart part) {
		ArrayList<RulePart> partList;
		Rule rule;
		HashSet<URI> rulesURI;
		URI ruleURI = part.getRuleURI();
		 log.debug("--------------- Add Rule Part -------------");//TESTAUSGABE

		/* 1st case class does not exist */
		if (searchIndex.get(partURI) == null) {
			/* URI of the class is not in HashMap */
			rulesURI = new HashSet<URI>(); // new HashSet for the rules
			rulesURI.add(ruleURI); // add current rule to HashSet
			searchIndex.put(partURI, rulesURI);
			// update searchIndex Class and List of rules belonging to the class

			/* 2nd case class does not exist, but rule (created by an other class) */
			if (rules.containsKey(ruleURI)) {
				// if this rule already exists, add part
				log.debug("2nd case class does not exist but rule"); //TESTAUSGABE
				rules.get(ruleURI).addPart(part);
			} else {
				// else create new PartList, add part and put into the Hashmap
				partList = new ArrayList<RulePart>(); // new PartList
				partList.add(part); // add RulePart to PartList
				rule = new Rule(ruleURI, partList); // create new Rule
				rules.put(ruleURI, rule); // store rule with partList

//				TESTAUSGABE
				 log.debug("1st case all created ");
				 log.debug("Key: " + partURI);
				 log.debug("Rule: " + ruleURI);
				 log.debug("Part: " + part.getUri());
			}
		}


		/* 3rd class exist but not rule */
		else if (!rules.containsKey(ruleURI)) {
			rulesURI = searchIndex.get(partURI);
			/* List of all rules connected to the class */
			// get HashSet with rules from the class
			rulesURI.add(ruleURI); 
			/*
			 add current rule to HashSet of rules,
			 (Relationship between rules and
			 searchindex) */
			partList = new ArrayList<RulePart>(); // new PartList
			partList.add(part); // add RulePart to PartList
			rule = new Rule(ruleURI, partList); // create new Rule
			rules.put(ruleURI, rule); // store rule with partList

//			TESTAUSGABE
			 log.debug("3rd case class exist, create rule");
			 log.debug("Key: " + partURI);
			 log.debug("Rule: " + ruleURI);
			 log.debug("Part: " + part.getUri());
		}

		/* 4th case class and rule exist*/
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
			 log.debug("4th Add rule part "); 
			 log.debug("Key: " + partURI);
			 log.debug("Rule: " + ruleURI);
			 log.debug("Part: " + part.getUri());
		}
		 log.debug("--------------- End Rule Part -------------");

	}//Test End 
	
	
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

	/**
	 * writes all rules with all parts of a rule to the command line
	 * (method just for debugging) 
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
				System.out.println("rule: " + ruleURIout);
				for (RulePart partout : partList) {
					i++;
					System.out.println("	RulePart: " + partout.getUri() + "(" + i + ")");
					System.out.println("	Type : " + partout.getType());
				}
			}
		}
		System.out.println("--------------- Content -------------");
	}
	
	
//Test Begin
	/**
	 * method write all axioms of the mapping ontology for
	 * the given URI to commandline.
	 * Just for debugging. 
	 */
	public static void allAxiomsfromClass(URI clsURI, OWLOntology ontology){
		Set<OWLAxiom> Axioms = new HashSet<OWLAxiom>();
		Set<OWLClassAxiom> ClassAxioms = new HashSet<OWLClassAxiom>();;
		OWLClass cls;
		if (!init) init(ontology);
		try{
			cls = owlOntologyManager.getOWLDataFactory().getOWLClass(clsURI);

//			owlOntologyManager.getOWLDataFactory().getOWLClass(arg0
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
				System.out.println("equivalent classes of "+clas.getURI());
				for(OWLClass subcls : equClsSet) {
					System.out.println("    " + subcls.getURI());
				}
			}
			for(OWLClass clas : ontology.getReferencedClasses()) {
				System.out.println("referencedClass: " + clas.getURI());
			}				
		}
		catch(UnsupportedOperationException exception) {
			log.error("Unsupported reasoner operation.");
		}
		catch(OWLReasonerException ex) {
			log.error("Problems with reasoner " + ex.getMessage());
		}
		catch(RuntimeException ex){// catches the exception, when property is not found in the ontology (subs returns null in this case, so there is no need for that) 
			log.error("Runtime Exception"+ex);
		}
		catch(Exception e){
			log.error("Error in MapSearch "+e);
		}
	}

}

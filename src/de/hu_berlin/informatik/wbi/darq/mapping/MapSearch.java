package de.hu_berlin.informatik.wbi.darq.mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.impl.Log4JLogger;
import org.mindswap.pellet.owlapi.Reasoner;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.inference.OWLReasonerAdapter;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLClassAxiom;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;


/**
 * @author Alexander Musidlowski
 * @version  1.0
 * 
 */
public class MapSearch {
	private static OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();


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


	
	//P4
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
		try {
			/*
			 * create Reasoner
			 */
			OWLReasoner reasoner = createReasoner(owlOntologyManager);
			Set<OWLOntology> importsClosure = owlOntologyManager.getImportsClosure(ontology);
			reasoner.loadOntologies(importsClosure);
			OWLClass nothing = owlOntologyManager.getOWLDataFactory().getOWLNothing();
			/*
			 * get Class from URI
			 */
			OWLClass cLass = owlOntologyManager.getOWLDataFactory().getOWLClass(resource);
			
			/*
			 * get Subclasses from Class
			 */
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
		catch(UnsupportedOperationException exception) {
				     System.out.println("Error:[REASONER] Unsupported reasoner operation.");
		}
		catch(OWLReasonerException ex) {
				  System.out.println("Error:[REASONER] " + ex.getMessage());
		}
		owlOntologyManager.removeOntology(ontology.getURI());
		return subClsesURISet;
	}
	
	
	/**
	 * looks for subproperties
	 * @return set of URIs or an empty set if there is no subproperty
	 * @param resource resource of a triple from the SPARQL query
	 * @param ontology the ontology contains the mapping where you look for subproperties
	 */
	public static Set<URI>  SearchSubProperty(URI resource, OWLOntology ontology)
	{
		Set<Set<OWLObjectProperty>> subPropSets = new HashSet<Set<OWLObjectProperty>>();
		Set<OWLObjectProperty> subProps = new HashSet<OWLObjectProperty>();
		Set<URI> subURIProps=new HashSet<URI>();
		try {
			OWLReasoner reasoner = createReasoner(owlOntologyManager);
			Set<OWLOntology> importsClosure = owlOntologyManager.getImportsClosure(ontology);
			reasoner.loadOntologies(importsClosure);
			System.out.println(reasoner.getLoadedOntologies()); //TEST
			
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
		catch(UnsupportedOperationException exception) {
				     System.out.println("Error:[REASONER] Unsupported reasoner operation.");
		}
		catch(OWLReasonerException ex) {
				  System.out.println("Error:[REASONER] " + ex.getMessage());
		}
		owlOntologyManager.removeOntology(ontology.getURI());
		return subURIProps;
	}
		
	
	
	/**
	 * looks for equivalent classes
	 * @return Set of URIs or an empty set if there is no equivalent class
	 * @param resource resource of a triple from the SPARQL query
	 * @param ontology the ontology contains the mapping where you look for equivalent classes
	 */
	public static Set<URI> SearchEquivalentClass(URI resource, OWLOntology ontology)
	{
		Set<OWLClass> equClsSet = new HashSet<OWLClass>();
		Set<URI> equClsURISet = new HashSet<URI>();
		OWLClass cls = null;
		
		try {
			OWLReasoner reasoner = createReasoner(owlOntologyManager);
			Set<OWLOntology> importsClosure = owlOntologyManager.getImportsClosure(ontology);
			reasoner.loadOntologies(importsClosure);
			
			cls = owlOntologyManager.getOWLDataFactory().getOWLClass(resource);
			equClsSet = reasoner.getEquivalentClasses(cls);
			if (!equClsSet.isEmpty()){ 
				for(OWLClass clss : equClsSet){
					equClsURISet.add(clss.getURI());
				}
			}
		}
		catch(UnsupportedOperationException exception) {
				     System.err.println("Error:[REASONER] Unsupported reasoner operation.");
		}
		catch(OWLReasonerException ex) {
				  System.err.println("Error:[REASONER] : " + ex.getMessage());
		}
		catch(RuntimeException ex){ 
			equClsURISet.clear();
			System.err.println("Warning: [Equivalent Class] Class" + resource + " not found");
			/* 
			 * if there is no equivalent class a RTE is thrown instead of an empty set
			 * Bugreport for Pellet exists. maybe fixed in a new version
			 */ 
		}
		owlOntologyManager.removeOntology(ontology.getURI());
		return equClsURISet;		
	}
	
	
	
	/**
	 * looks for equivalent Properties
	 * @return Set of URIs or an empty set if there is no equivalent property
	 * @param resource resource of a triple from the SPARQL query
	 * @param ontology the ontology contains the mapping where you look for equivalent properties
	 */
	public static Set<URI> SearchEquivalentProperty(URI resource, OWLOntology ontology)
	{
		Set<OWLObjectProperty> equPropSet = new HashSet<OWLObjectProperty>();
		Set<URI> equPropURISet = new HashSet<URI>();
		try {
			OWLReasoner reasoner = createReasoner(owlOntologyManager);
			Set<OWLOntology> importsClosure = owlOntologyManager.getImportsClosure(ontology);
			reasoner.loadOntologies(importsClosure);
			
			OWLObjectProperty property = owlOntologyManager.getOWLDataFactory().getOWLObjectProperty(resource);			 							 
			equPropSet = reasoner.getEquivalentProperties(property); //returns RuntimeException if nothing found
			for(OWLObjectProperty prop:equPropSet){
				equPropURISet.add(prop.getURI());
			}
		}
		catch(UnsupportedOperationException exception) {
				     System.err.println("Error:[REASONER] Unsupported reasoner operation.");
		}
		catch(OWLReasonerException ex) {
				  System.err.println("Error:[REASONER] " + ex.getMessage());
		}
		catch(RuntimeException ex){// catches the exception, when property is not found in the ontology 
			equPropURISet.clear();//returns emtpy set as the other do
			System.err.println("Warning: [Equivalent Property] Property " + resource + " not found");
		}
		owlOntologyManager.removeOntology(ontology.getURI());
		return equPropURISet;	
	}

	/**
	 * looks for equivalent Individuals (same as relationship)
	 * @param resource resource of a triple from the SPARQL query
	 * @param ontology the ontology contains the mapping where you look for individuals
	 */
	public static Set<URI> SearchSameIndividual(URI resource, OWLOntology ontology)
	{
		Set<OWLIndividual> sameIndiSet = new HashSet<OWLIndividual>();
		Set<URI> sameIndiURISet = new HashSet<URI>();
		try {
			OWLReasoner reasoner = createReasoner(owlOntologyManager);
			Set<OWLOntology> importsClosure = owlOntologyManager.getImportsClosure(ontology);
			reasoner.loadOntologies(importsClosure);
			
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
		catch(OWLReasonerException ex) {
				  System.err.println("Error:[MAPSEARCH] " + ex.getMessage());
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
	
	

	
	//ontology.getRules() Get SWRL Rules
	//Test Begin
	public static void AllAxiomsfromClass(URI clsURI, OWLOntology ontology){
		Set<OWLAxiom> Axioms = new HashSet<OWLAxiom>();
		Set<OWLClassAxiom> ClassAxioms = new HashSet<OWLClassAxiom>();;
		OWLClass cls;
		try{
		OWLReasoner reasoner = createReasoner(owlOntologyManager);
		Set<OWLOntology> importsClosure = owlOntologyManager.getImportsClosure(ontology);
		reasoner.loadOntologies(importsClosure);
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
//					System.out.println("referencedClass: " + clas.getURI());
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
}


//Objectproperty vs Dataproperty?
//Objectproperty sind Relationen zwischen Individuen
//DataProperty sind auf Datentypen bezogen, welchen Datentyp (xsd^^int) verlangt die Property

//defaultowlModel.getMatchingFrames(arg0, arg1, arg2, arg3, arg4)
//defaultowlModel.getMatchingReferences(arg0, arg1);
//defaultowlModel.getMatchingResources(property, matchString, maxMatches)

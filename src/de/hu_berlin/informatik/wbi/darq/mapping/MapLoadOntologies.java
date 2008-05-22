package de.hu_berlin.informatik.wbi.darq.mapping;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.protege.editor.owl.model.refactor.ontology.OntologyMerger;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.util.AutoURIMapper;

import arq.cmd.CmdException;


/*
 * Class loads all given ontologies and returns a merged one.
 */

/*  
 * Beim Laden von Ontologien, die wiederum Ontologien importieren, muessen
 * diese ebenfalls geladen werden. In P4 ist momentan nur möglich die zu importierenden 
 * Ontologien manuell anzugeben.   
 * Workaround: mit P3 laden, log + phy URI holen und dann mit P4 laden
 */


public class MapLoadOntologies {

	private static OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();

	/*
	 * loads the ontology, which is given by the command line/user
	 */
	
	public static OWLOntology loadCommandline(String[] maps){
				
		Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
		OWLOntology ontology = null;

		/*
		 * local Folder for getting imported ontologies
		 */
		File ontologyFolder  = new File(System.getProperty("user.home"));
//		System.out.println(System.getProperty("user.home"));
		AutoURIMapper autoURIMapper = new AutoURIMapper(ontologyFolder, true);
		owlOntologyManager.addURIMapper(autoURIMapper); //TODO TEST
		URI physicalURI = null;
		File fileMap = null;
		/*
		 * loads all files given by the commandline argument maps 
		 */
		for (String map : maps) { //durchläuft alle Pfade aus der Kommandozeile
			try {				
				fileMap = new File(map);
				physicalURI = fileMap.toURI(); //Datei der Ontologie
				ontology = owlOntologyManager.loadOntologyFromPhysicalURI(physicalURI); 
				ontologies.add(ontology);		
			}catch (OWLOntologyCreationException e) {
				System.err.println("Warning: [MAPPING] The mapping ontology "+map+" could not be created: " + e.getMessage());				
			}catch (Exception e) {
				System.err.println("Warning: [MAPPING] The mapping ontology "+map+" could not be loaded: " + e.getMessage());
			}
			
		}
		if (ontologies.size() == 0) {
			throw new CmdException("Error: [MAPPING] No mapping file loaded, see warnings above");
		}
		/*
		 * merges loaded ontologies (if more than one is given by commandline
		 */
		if (ontologies.size() > 1) {
			
			OWLOntology mergedOntologies = null; 		
			try {
				mergedOntologies = owlOntologyManager.createOntology(ontology.getURI());
				//			TODO anstatt ontology.getURI() Name für zusammengeführte Ontology wählen
			} catch (OWLOntologyCreationException e1) {
				System.err.println("Warning: [MAPPING] The joined mapping ontology could not be created: " + e1.getMessage());
			}
			OntologyMerger ontologyMerger = new OntologyMerger(owlOntologyManager,ontologies, mergedOntologies);
			ontologyMerger.mergeOntologies();
		
			return mergedOntologies;
		}
		else return ontology;
	}
}

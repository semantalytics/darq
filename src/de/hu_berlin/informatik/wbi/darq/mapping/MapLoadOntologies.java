package de.hu_berlin.informatik.wbi.darq.mapping;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.protege.editor.owl.model.refactor.ontology.OntologyMerger;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.util.AutoURIMapper;

import sun.util.logging.resources.logging;

import com.hp.hpl.jena.query.darq.engine.compiler.iterators.DarqQueryIterator;

import arq.cmd.CmdException;


/**
 * Class loads all given ontologies and returns a merged one.
 * On loading ontologies which imports other ontologies their 
 * have to be also available in repository or by its URI.  
 */


public class MapLoadOntologies {

	private static OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();
	static Log log = LogFactory.getLog(MapLoadOntologies.class);

	/**
	 * loads the ontology, which is given by the command line/user
	 * @param maps Path(s) to mapping ontologies
	 *
	 */
	/* just file paths are allowed. If you like to load an ontology
	 * from web, create a local ontology which imports the one from
	 * web. */
	public static OWLOntology loadCommandline(String[] maps){
				
		Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
		OWLOntology ontology = null;

		/*
		 * local Folder for getting imported ontologies
		 */
		File ontologyFolder  = new File(System.getProperty("user.home"));
		log.debug("local repository: " + System.getProperty("user.home"));
		AutoURIMapper autoURIMapper = new AutoURIMapper(ontologyFolder, true);
		owlOntologyManager.addURIMapper(autoURIMapper); 
		URI physicalURI = null;
		File fileMap = null;
		/*
		 * loads all files given by the commandline argument maps 
		 */
		for (String map : maps) { //run through all paths given by commandline
			try {				
				fileMap = new File(map);
				physicalURI = fileMap.toURI(); //file of the ontology
				ontology = owlOntologyManager.loadOntologyFromPhysicalURI(physicalURI); 
				ontologies.add(ontology);		
			}catch (OWLOntologyCreationException e) {
				log.warn("The mapping ontology "+map+" could not be created: " + e.getMessage());				
			}catch (Exception e) {
				log.warn("The mapping ontology "+map+" could not be loaded: " + e.getMessage());
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
			} catch (OWLOntologyCreationException e1) {
				log.warn("The joined mapping ontology could not be created: " + e1.getMessage());
			}
			OntologyMerger ontologyMerger = new OntologyMerger(owlOntologyManager,ontologies, mergedOntologies);
			ontologyMerger.mergeOntologies();
		
			return mergedOntologies;
		}
		else return ontology;
	}
}

package de.hu_berlin.informatik.wbi.darq.mapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.net.URI;
import java.net.URL;

import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;

import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.util.SimpleURIMapper;

import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.jena.JenaOWLModel;
import edu.stanford.smi.protegex.owl.repository.impl.LocalFolderRepository;
import edu.stanford.smi.protegex.owl.repository.impl.HTTPRepository;
import edu.stanford.smi.protegex.owl.repository.impl.FTPRepository;

import org.protege.editor.owl.model.refactor.ontology.OntologyMerger;

import arq.cmd.CmdException;

import com.hp.hpl.jena.util.FileUtils;


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
	private static HashMap <String,String> collectedImports =  new HashMap <String,String>();
	
	/*
	 * loads the ontology, which is given by the command line/user
	 */
	
	public static OWLOntology loadCommandline(String[] maps){
				
		Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
		OWLOntology ontology = null;
		OWLOntology importedOntology = null;
		SimpleURIMapper simpleURIMapper = null;
		URI ontURI, phyURI; 
		
		/*
		 * loads all files given by maps 
		 */
		for (String map : maps) { //durchläuft alle Pfade aus der Kommandozeile
			try {
				
				File fileMap = new File(map);
				URI physicalURI = fileMap.toURI(); //Datei der Ontologie
				/*
				 * Workaround: link URI to file of the to be imported ontologies 
				 */
				getPhysicalURI(map); //holt sich alle URIs und Dateien der zu importierenden Ontologien
				Iterator <String> it = collectedImports.keySet().iterator();
				while (it.hasNext()) {//durchläuft alle zu importierenden Ontologien
				  String importURI = it.next();
				  File importFile = new File (collectedImports.get(importURI));
				  phyURI = importFile.toURI(); //URI.create funktioniert nicht, da "\" anstatt "/" enthalten sind
				  //FRAGE Was passiert, wenn es keine Datei im Dateisystem ist, sondern ftp oder http?
				  // bis jetzt gibt es jedoch nur das lokale Repository
				  ontURI = URI.create(importURI);
				  if (importFile.exists()) {//FRAGE wirft exists keine IOException? 
					  simpleURIMapper = new SimpleURIMapper(ontURI,phyURI); //verknüpft URI mit Datei
					  owlOntologyManager.addURIMapper(simpleURIMapper);
				  }
				  else{
					  throw new IOException("ERROR: File "+importFile+" belonging to"+ importURI +"does not exist.");
				  }
				}
				ontology = owlOntologyManager.loadOntologyFromPhysicalURI(physicalURI); 
				//FRAGE berücksichtigt load Imports, welche über die URI der importieren Ontologie erreichbar sind?
//				Ja, werden berücksichtigt. (ML OWL API Mathew Horridge)
				ontologies.add(ontology);		
			}catch (OWLOntologyCreationException e) {
				System.err.println("WARNING: [Mapping] The mapping ontology "+map+" could not be created: " + e.getMessage());				
			}catch (Exception e) {
				System.err.println("WARNING: [Mapping] The mapping ontology "+map+" could not be loaded: " + e.getMessage());
			}
			
		}
		if (ontologies.size() == 0) {
			throw new CmdException("ERROR: [Mapping] No mapping file loaded, see warnings above");
		}
		/*
		 * merges loaded ontologies 
		 */
		if (ontologies.size() > 1) {
			
			OWLOntology mergedOntologies = null; 		
			try {
				mergedOntologies = owlOntologyManager.createOntology(ontology.getURI());
				//			TODO anstatt ontology.getURI() Name für zusammengeführte Ontology wählen
			} catch (OWLOntologyCreationException e1) {
				System.err.println("WARNING: [Mapping] The joined mapping ontology could not be created: " + e1.getMessage());
			}
			OntologyMerger ontologyMerger = new OntologyMerger(owlOntologyManager,ontologies, mergedOntologies);
			ontologyMerger.mergeOntologies();
		
			return mergedOntologies;
		}
		else return ontology;
	}
	

	/*
	 * Workaround
	 */	
	private static void getPhysicalURI(String map)
	{
		String map1; //TEST: muss spaeter von der Konsole uebergeben werden
		map1 = "N:/Studium/Diplomarbeit/Ontologien/mapping.owl";	
	
		//TODO entsprechenden Wert suchen bzw. von Nutzer vorgeben lassen
		URL httpRepositoryURL = null;  
		URI ftpRepositoryURI = null; 
		URI ftpLocalURI = null;
		File path = new File(System.getProperty("user.home"));
//		System.out.println(System.getProperty("user.home"));//TEST
//		System.out.println(path);
		//FRAGE lokale Kopie des FTP Verzeichnisses
//		ftpLocalURI.create(System.getProperty("user.home")); erzeugt Fehler, falscher Parameter?
		
		//path = N:\\studium\\diplom\\darq
		LocalFolderRepository localRepository = new LocalFolderRepository(path, true);
//		HTTPRepository httpRepository = new HTTPRepository(httpRepositoryURL); //FRAGE Will der mich verarschen?
		//Anstatt des Repositories einfach load from URL
//		FTPRepository ftpRepository = new FTPRepository(ftpRepositoryURI, ftpLocalURI);
		
		FileInputStream is  = null;
		JenaOWLModel owlModel = ProtegeOWL.createJenaOWLModel();
		owlModel.getRepositoryManager().addProjectRepository(localRepository);

		
		/*
		 * try to load the ontology from disk and get imports via Protege 3 function
		 */
		
	   	String importFile = null;
	   	URI ontURI = null;
		File file = new File( map );  
		try { //lädt die aktuelle Datei/Ontologie
			is = new FileInputStream(file);     
			owlModel.load(is, FileUtils.langXMLAbbrev);
			//holt sich die URIs der importierten Ontologien
			Set <String> imports = owlModel.getAllImports(); //FRAGE liefert nur ein Set zurück, wie konvertieren?
			
			//durchläuft alle importierten Ontologien und speichert URI sowie
			//Speicherort (Datei) ab
			for (String importURI :  imports){
		
				ontURI = URI.create(importURI);
				importFile = localRepository.getOntologyLocationDescription(ontURI);
				
				if (importFile == ""){
//						importFile = httpRepository.getOntologyLocationDescription(ontURI);
				}
				else{
						if (importFile == ""){
//							importFile = ftpRepository.getOntologyLocationDescription(ontURI);
						}
						else{
							if (importFile==""){
							System.err.println("WARNING: [Mapping] File "+importFile+" not found in any repository, using URI");
							}
						}
				}
		     	//speichert URI und Datei aller importierten Ontologien
				collectedImports.put(importURI, importFile);
			}
		}
		catch (FileNotFoundException e) {
			System.err.println("ERROR: [Mapping] Map file not found" + e); 
		}
		catch (IOException e) {
			System.err.println("ERROR: [Mapping] Could not read map file" + e);
		}
		catch (Exception e) {
			throw new CmdException("ERROR: [Mapping] Unable to parse file",e);
		}
		finally 
		{ 
			if ( is != null ) 
				try { is.close(); } catch ( IOException e ) { e.printStackTrace(); } 
		}
	}
}

package de.hu_berlin.informatik.wbi.darq.mapping;


import arq.cmd.CmdException;
import edu.stanford.smi.protegex.owl.model.OWLNamedClass;
import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.repository.impl.LocalFolderRepository;
import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.jena.JenaOWLModel;
import java.io.*;
import java.util.*;
import com.hp.hpl.jena.util.FileUtils;   

/**
 * @deprecated
 */
public class Mapping {

	/*
	 * laedt Mappingdatei, ueberprueft, ob Datei existiert
	 * leerer Uebergabeparameter bereits in query.java geprueft
	 * gibt OWLModel zurück
	 */
	/*
	 * Wenn man von einer erweiterbaren Mappingontologie ausgeht, muss
	 * hier ebenfalls die "normale" Mappingdatei geladen werden.
	 * 
	 */
	   //FRAGE Ist die Fehlerbehandlung hier richtig? 
	
	public static JenaOWLModel loadCommandline(String map)
	{
		String map1; //TEST: muss spaeter von der Konsole uebergeben werden
		map1 = "N:/Studium/Diplomarbeit/Ontologien/mapping.owl";

		/*
		 * checks if the file exists and try to load the ontology from disk
		 */
		//TEST map1 fuer tests, map fuer Uebergabe aus Konsole
		//TODO Das muss als Fehlerbehandlung implementiert werden
		
		File file = new File( map1 );  
		return load(file); 
				
	}
	
	/*
	 * Hier einfach die Mappingdatei laden sofern die entsprechende Option 
	 * in der Kommandozeile gewählt wurde. Eine extra Argument ist einfacher
	 * zu haendeln als die Erweiterung von "map" 
	 * "dmap" for loading default mapping
	 * TODO Argument dmap einfuehren, + Behandlung, wenn beide Argumente aufgerufen werden
	 * da das Mapping aus der geladenen Datei sowieso hinzugefügt werden soll, 
	 * muss der Code ebenfalls geschrieben werden 
	 */
    //FRAGE Welches Verzeichnis fuer die default Mappingontologie nehmen? 
	public static JenaOWLModel loadDefault()
	{
		File userDir = new File( System.getProperty("user.dir") );		
		File mappingDB = new File(userDir, "DarqDefaultMapping.owl"); 
		return load(mappingDB);
	}

	public static JenaOWLModel loadDefaultAndCommandline (String map){
		
		JenaOWLModel owlModel = Mapping.loadCommandline(map);
		JenaOWLModel defaultowlModel = Mapping.loadDefault();
		//TODO beide Modelle zusammenführen
		// nur manuell in P3 möglich
		
		
		/*
		 * erweiterte MappingDB gleich abspeichern 
		 * (bleibt hoffentlich weiterhin offen)
		 */
		Collection errors = new ArrayList();
		File userDir = new File( System.getProperty("user.dir") );		
		File mappingDB = new File(userDir, "DarqDefaultMapping.owl"); 
		
		try {
				defaultowlModel.save(mappingDB.toURI(),
				FileUtils.langXMLAbbrev, errors);
				System.err.println("INFO: File saved with " + errors.size() + " errors.");
			} catch (Exception e) {
				// FRAGE Fehlerbehandlung richtig?
				throw new CmdException("ERROR: Default mapping file could not be saved " , e);
			}
				
		return defaultowlModel; 
	}
	
	
	private static JenaOWLModel load(File file){
		 FileInputStream is  = null;
		 JenaOWLModel owlModel = ProtegeOWL.createJenaOWLModel();
		/* TODO 
		 * Beim Laden von Ontologien, die wiederum Ontologien importieren, muessen
		 * diese ebenfalls geladen werden. Dazu müssen diese in einem Repository liegen.
		 * Zum TEST erstmal lokale Verzeichnis genommen, später muessten die importierten 
		 * Ontologien von den Webservices bereitgestellt werden. 
		 * Dazu am besten HTTPRepository verwenden. 
		 * 
		 */
		
		File path = new File("N:/Studium/Diplomarbeit/Ontologien/"); 
		LocalFolderRepository repository = new LocalFolderRepository(path, true);
		owlModel.getRepositoryManager().addProjectRepository(repository);
		
		try {
	         is = new FileInputStream(file);     
	         owlModel.load(is, "de");
		}
	    catch (FileNotFoundException e) {
	    	 System.err.println("ERROR: Map file not found" + e); 
	    }
	    catch (IOException e) {
	    	System.err.println("ERROR: Could not read map file" + e);
	    }
	    catch (Exception e) {
	    	throw new CmdException("ERROR: Unable to parse file",e);
	    }
	    finally 
	    { 
	      if ( is != null ) 
	        try { is.close(); } catch ( IOException e ) { e.printStackTrace(); } 
	    }
	    return owlModel;		
	}
}

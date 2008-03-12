package darq;

import arq.cmd.CmdException;
import arq.cmdline.ArgDecl;
import arq.cmdline.ArgModuleGeneral;
import arq.cmdline.CmdArgModule;
import arq.cmdline.CmdGeneral;

import java.io.File;
import java.lang.String;
import java.util.regex.*;

public class ModMapping implements ArgModuleGeneral {


    protected final ArgDecl argDarqMap = new ArgDecl(ArgDecl.HasValue, "map") ;
    protected final ArgDecl argDarqMapTrans = new ArgDecl(ArgDecl.HasValue, "t") ;
    /* Liefert zurück, ob "map" einen Wert hat (d.h., ob es in der Kommandozeile 
     * aufgerufen wurde).
     */     
    private String[] maps = null;
    private int transitivity = 0;
    
    /*
     * (non-Javadoc)
     * @see arq.cmdline.ArgModuleGeneral#registerWith(arq.cmdline.CmdGeneral)
     * Output for help  
     */
    public void registerWith(CmdGeneral cmdLine) {
        cmdLine.add(argDarqMap, "--map", "Mapping File") ;
        cmdLine.add(argDarqMapTrans, "--t", "depth of search for transitivity");
    }

  /**
   * returns String from command line otherwise throws exception
   */
    public void processArgs(CmdArgModule cmdLine) throws IllegalArgumentException {
		File file;
		String map;
		map = cmdLine.getValue(argDarqMap);
		String transitivity = cmdLine.getValue(argDarqMapTrans);
		/*FRAGE
		 * hier ist noch eine Fehler bzw. im Aufruf Was passiert, wenn kein
		 * --map vorhanden ist. das klappt noch nicht
		 */
		if (map != null) {
			/*
			 * splits String
			 */
			Pattern p = Pattern.compile(System.getProperty("path.separator")); // Windows
			// ":"
			maps = p.split(map);

			for (String filepath : maps) {
				file = new File(filepath);
				if (!file.exists())
					throw new CmdException("Error [MODMAPPING]: Map file " + filepath + " not found.");
			}

			if (transitivity != null) {
				try {
					this.transitivity = Integer.parseInt(transitivity);
				} catch (Exception ex) {
					System.err.println("Warning [MODMAPPING]: Transitivity has to be an integer! Value ignored, using default value(0).");
				}
			}
		} else {//Das ist der Fall, wo es keine cmdline option map gibt. FRAGE Wo ist der Fall, dass sie leer ist?
			if (transitivity != null) {

				System.err.println("Warning [MODMAPPING]: A value for transitivity does not make sense without a mapping.");
			}
		}
	}

       /**
		 * returns path's of files
		 */
    public String[] getMapping() {
        return maps;
    }
    public Integer gettransitivity(){
    	return transitivity;
    }
    
}

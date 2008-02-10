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
    // TODO Auto-generated method stub

    protected final ArgDecl argDarqMap = new ArgDecl(ArgDecl.HasValue, "map") ;
    /* Liefert zurück, ob "map" einen Wert hat (d.h., ob es in der Kommandozeile 
     * aufgerufen wurde).
     */     
    private String[] maps = null;
    
    /*
     * (non-Javadoc)
     * @see arq.cmdline.ArgModuleGeneral#registerWith(arq.cmdline.CmdGeneral)
     * Output for help  
     */
    public void registerWith(CmdGeneral cmdLine) {
        cmdLine.add(argDarqMap, "--map", "Mapping File") ;
    }

  /**
   * returns String from command line otherwise throws exception
   */
    public void processArgs(CmdArgModule cmdLine)throws IllegalArgumentException {
       File file;
       String map;
       map=cmdLine.getValue(argDarqMap); 
      /* hier ist noch eine Fehler bzw. im Aufruf
       * Was passiert, wenn kein --map vorhanden ist. das klappt noch nicht
       */
       if (map!=null){
          /*
           * splits String
           */  
    	  Pattern p = Pattern.compile(System.getProperty("path.separator"));
    	  maps = p.split(map);
   
    	  for (String filepath : maps) {
    		  file = new File(filepath); 
    		  if (!file.exists())	throw new CmdException("Argument Error: Map file " +filepath+ " not found.");
    	  }
      }
   }

       /**
        * returns path's of files 
        */
    public String[] getMapping() {
        return maps;
    }
}

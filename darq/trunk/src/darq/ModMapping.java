package darq;

import arq.cmd.CmdException;
import arq.cmdline.ArgDecl;
import arq.cmdline.ArgModuleGeneral;
import arq.cmdline.CmdArgModule;
import arq.cmdline.CmdGeneral;

import java.io.File;
import java.lang.String;
import java.util.regex.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.hu_berlin.informatik.wbi.darq.mapping.MapLoadOntologies;

public class ModMapping implements ArgModuleGeneral {


    protected final ArgDecl argDarqMap = new ArgDecl(ArgDecl.HasValue, "map") ;
    protected final ArgDecl argDarqMapTrans = new ArgDecl(ArgDecl.HasValue, "t") ;
    /* return a value of map if called by commandline  */     
    private String[] maps = null;
    private int transitivity = 1;
    static Log log = LogFactory.getLog(ModMapping.class);
    
    /*
     * (non-Javadoc)
     * @see arq.cmdline.ArgModuleGeneral#registerWith(arq.cmdline.CmdGeneral)
     * Output for help  
     */
    public void registerWith(CmdGeneral cmdLine) {
        cmdLine.add(argDarqMap, "--map(<file>)*", "Mapping File(s)") ;
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

		if (map != null) {
			/*
			 * splits String
			 */
			Pattern p = Pattern.compile(System.getProperty("path.separator")); 
			// Windows ":"
			log.debug("path separator: " + System.getProperty("path.separator"));
			maps = p.split(map);

			for (String filepath : maps) {
				file = new File(filepath);
				if (!file.exists())
					throw new CmdException("Error [MODMAPPING]: Map file " + filepath + " not found.");
			}

			if (transitivity != null) {
				try {
					this.transitivity = Integer.parseInt(transitivity);
					if(this.transitivity < 1){
						log.warn("The value for transitivity has be greater than zero, using default value(1).");
					}
				} catch (Exception ex) {
					log.warn("Transitivity has to be an integer! Value ignored, using default value(1).");
				}
			}
		} else {
			if (transitivity != null) {

				log.warn("A value for transitivity does not make sense without a mapping.");
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

package darq;

import arq.cmdline.ArgDecl;
import arq.cmdline.ArgModuleGeneral;
import arq.cmdline.CmdArgModule;
import arq.cmdline.CmdGeneral;

public class ModDarq implements ArgModuleGeneral {


    protected final ArgDecl argDarqConfig = new ArgDecl(ArgDecl.HasValue, "config") ;
   // protected final ArgDecl argDarqVerbose = new ArgDecl(ArgDecl.NoValue, "verbose") ;
    
    private String config = null;
    private boolean verbose = false;
    
    public void registerWith(CmdGeneral cmdLine) {
        cmdLine.getUsage().startCategory("DARQ") ;
        cmdLine.add(argDarqConfig, "--config", "Service Description File") ;
  //      cmdLine.add(argDarqVerbose, "--verbose", "extra output from query processor") ;
    }

    public void processArgs(CmdArgModule cmdLine) {
       config=cmdLine.getValue(argDarqConfig);
       verbose=cmdLine.contains("--verbose");
       
    }

    public String getConfig() {
        return config;
    }

    public boolean isVerbose() {
        return verbose;
    }
    
    

}

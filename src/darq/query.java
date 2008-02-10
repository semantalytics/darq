/*
 * (c) Copyright 2006, 2007 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package darq;

import arq.cmd.CmdException;
import arq.cmd.QueryCmdUtils;
import arq.cmdline.*;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.query.core.ARQInternalErrorException;
import com.hp.hpl.jena.query.darq.core.DarqDataset;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngineFactory;
import com.hp.hpl.jena.query.engineHTTP.HttpQuery;
import com.hp.hpl.jena.query.engineHTTP.QueryExceptionHTTP;
import com.hp.hpl.jena.query.resultset.ResultSetException;
import com.hp.hpl.jena.query.util.IndentedWriter;
import com.hp.hpl.jena.query.util.Utils;
import com.hp.hpl.jena.shared.JenaException;
//import de.hu_berlin.informatik.wbi.darq.mapping.Mapping;
import de.hu_berlin.informatik.wbi.darq.mapping.MapLoadOntologies;
//import edu.stanford.smi.protegex.owl.model.OWLModel;
import org.semanticweb.owl.model.OWLOntology;
import de.hu_berlin.informatik.wbi.darq.mapping.MapFedQueryEngineFactory;

public class query extends CmdARQ
{
    ArgDecl argRepeat = new ArgDecl(ArgDecl.HasValue, "repeat") ;
    int repeatCount = 1 ; 
    
    ModTime       modTime =     new ModTime() ;
    ModQueryIn    modQuery =    new ModQueryIn() ;
    //ModDataset    modDataset =  new ModDataset() ;
    ModDataset    modDataset =  new ModAssembler() ;    // extends ModDataset
    ModResultsOut modResults =  new ModResultsOut() ;
    ModRemote     modRemote =   new ModRemote() ;
    
    // DARQ 
    
    ModDarq modDarq = new ModDarq();
    ModMapping modMapping = new ModMapping();
    
    
    public static void main (String [] argv)
    {
        new query(argv).mainAndExit() ;
    }
    
    protected query(String[] argv)
    {
        super(argv) ;
        super.addModule(modQuery) ;
        super.addModule(modResults) ;
        super.addModule(modDataset) ;
        super.addModule(modRemote) ;
        super.addModule(modTime) ;
        super.addModule(modDarq);
        super.addModule(modMapping);
        super.add(argRepeat) ;
    }

    protected void processModulesAndArgs()
    {
        super.processModulesAndArgs() ;
        if ( contains(argRepeat) )
            try {
                repeatCount = Integer.parseInt(getValue(argRepeat)) ;
            } catch (NumberFormatException ex)
            {
                throw new CmdException("Can't parse "+getValue(argRepeat)+" as an integer", ex) ;
            }
    }
    
    protected void exec()
    {
        for ( int i = 0 ; i < repeatCount ; i++ )
        {
            if ( modRemote.getServiceURL() == null )
                queryExecLocal() ;
            else
                queryExecRemote() ;
        }
    }

    protected String getCommandName() { return Utils.className(this) ; }
    
    protected String getSummary() { return getCommandName()+" --data=<file> --query=<query>" ; }
    
    private void queryExecLocal()
    {
        try{
            Query query = modQuery.getQuery() ;
            if ( isVerbose() )
            {
                IndentedWriter out = new IndentedWriter(System.out, true) ;
                query.serialize(out) ;
                out.flush() ;
                System.out.println();
            }
            
/*
 * \Begin { MAPPING }
 */           
//            ModQueryin erzeugt Fehler, wenn --map mehr als ein Argument hat.
//            bei mehr als einem --map bekommt man nur Fehler des letzten Map
            String[] mappings = modMapping.getMapping();            
            OWLOntology ontology = null;
            if ( mappings !=null )
            {
            	ontology = MapLoadOntologies.loadCommandline(mappings);	            	
                MapFedQueryEngineFactory.register(modDarq.getConfig(), ontology);
                //hier den Weg verfolgen
               
            } //else throw new CmdException("Argument Error: No map file. use --map=<file>") ; ist an dieser Stelle nicht
//            korrekt, da es aufgerufen wird, wenn kein --map angegeben wird. 
/*
* \End{ MAPPING }
*/
            
            if ( modDarq.getConfig()!=null )
            {
                FedQueryEngineFactory.register(modDarq.getConfig());
                
            } else throw new CmdException("Argument Error: No config file. use --config=<file>") ;
            
            
            
            
            // XXX ?
            if (query.hasDatasetDescription()) throw new CmdException("Argument Error: No config file. use --config=<file>") ;
             
            Dataset dataset = new DarqDataset();
            modTime.startTimer() ;
            QueryExecution qe = QueryExecutionFactory.create(query, dataset) ;
            
            // Check there is a dataset
            
            if ( dataset == null && ! query.hasDatasetDescription() )
            {
                System.err.println("Dataset not specified in query nor provided on command line.");
                return ;
            }
            QueryCmdUtils.executeQuery(query, qe, modResults.getResultsFormat()) ;
            long time = modTime.endTimer() ;
            if ( modTime.timingEnabled() )
                System.out.println("Time: "+modTime.timeStr(time)) ;
            qe.close() ;
        }
        catch (ARQInternalErrorException intEx)
        {
            System.err.println(intEx.getMessage()) ;
            if ( intEx.getCause() != null )
            {
                System.err.println("Cause:") ;
                intEx.getCause().printStackTrace(System.err) ;
                System.err.println() ;                
            }
            intEx.printStackTrace(System.err) ;
        }
        catch (ResultSetException ex)
        {
            System.err.println(ex.getMessage()) ;
            ex.printStackTrace(System.err) ;
        }
        catch (QueryException qEx)
        {
            //System.err.println(qEx.getMessage()) ;
            throw new CmdException("Query Exeception", qEx) ;
        }
        catch (JenaException ex) { throw ex ; } 
        catch (CmdException ex) { throw ex ; } 
        catch (Exception ex)
        {
            throw new CmdException("Exception", ex) ;
        }
    }    
    
    private void queryExecRemote()
    {
        Query query = modQuery.getQuery() ;
        
        try {
            String serviceURL = modRemote.getServiceURL() ;
            QueryExecution qe = QueryExecutionFactory.sparqlService(serviceURL, query,
                                                                    modDataset.getGraphURLs(),
                                                                    modDataset.getNamedGraphURLs()) ;
            
            if ( modRemote.usePost() )
                HttpQuery.urlLimit = 0 ;
            
            QueryCmdUtils.executeQuery(query, qe, modResults.getResultsFormat()) ;
        } catch (QueryExceptionHTTP ex)
        {
            throw new CmdException("HTTP Exeception", ex) ;
        }
        catch (Exception ex)
        {
            System.out.flush() ;
            ex.printStackTrace(System.err) ;
        }
    }

}

/*
 * (c) Copyright 2006, 2007 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
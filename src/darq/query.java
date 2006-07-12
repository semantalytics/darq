/*
 * (c) Copyright 2004, 2005, 2006 Hewlett-Packard Development Company, LP
 * [See end of file]
 */


package darq;

import java.util.Iterator;
import java.util.List;

import arq.cmd.*;
import arq.cmdline.* ;

import com.hp.hpl.jena.Jena;
import com.hp.hpl.jena.util.* ;
import com.hp.hpl.jena.query.darq.core.DarqDataset;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngine;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngineFactory;
import com.hp.hpl.jena.query.darq.engine.compiler.FedQueryIterService;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.PlanFormatter;
import com.hp.hpl.jena.query.util.*;
import com.hp.hpl.jena.query.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/** A program to execute queries from the command line.
 *
 *  Queries can specify the source file so this can be used as a simple
 *  script engine.
 *
 *  <pre>
 *  Usage: [options] --config URL [queryString | --query file] ;
 *  where options are:
 *     --query file             Read one query from a file
       --config file            Config file with Service Descriptions    
 *     --service URL            Execute remote query
 *     --post                   Force the use of HTTP POST 
 *     --syntax SYNTAX          One of SPARQL (default), RDQL, ARQ (or their URIs)
 *     --lmap                   Location mapping config file
 *     --fmt FMT                DataSource format: RDF/XML (default), N3, N-TRIPLES, TURTLE
 *                              Note file extensions used to guess the format as well:
 *     --dir DIR                Set the current working directory
 *     --base URI               Set the base URI for resolving relative URIs
 *     --results FMT            Result format (table format for SELECT queries, RDF serialization for CONSTRUCT and DESCRIBE)
 *     --verbose                Verbose - more messages
 *     --quiet                  Quiet - less messages
 *     --version                Print version information and exit     
 *     --planning ON or OFF     Turn off/on query planning
 *     --show WHAT              Print various internals details (parse trees, planning trees, timings) 
 * Options for data and format override the query specified source (FROM)
 * </pre>
 *
 * @author  Andy Seaborne
 * @version $Id: query.java,v 1.50 2006/06/15 19:48:23 andy_seaborne Exp $
 */

public class query
{
    static { CmdUtils.setLog4j() ; CmdUtils.setN3Params() ; }
    
    static Syntax defaultSyntax = Syntax.syntaxSPARQL ;
    
//    String dbUser     = "" ;
//    String dbPassword = "" ;
//    String dbType     = "" ;
//    String dbName     = "" ;
//    String dbDriver   = null ;

    static protected Log log = LogFactory.getLog( query.class );
    
    // TODO Convert to use CmdMain.
    // new query(argv).mainEndExit() ;
    
    public static void main (String [] argv)
    {
        try {
            main2(argv) ;
        }
        catch (CmdException ex)
        {
            if ( ex.getCause() != null )
                System.err.println(ex.getCause().getMessage()) ;
            else
                System.err.println(ex.getMessage()) ;
        }
        catch (TerminateException ex) { System.exit(ex.getCode()) ; }
    }
        
    // Like main() except from hereonin code shoudlnot call throw new TerminateException
    // Call throw new TerminateException(code) instead
    
    public static void main2(String [] argv)
    {
        boolean printParseTree = false ;
        boolean printPlan      = false ;
        boolean printTimings   = false ;
        
        if ( argv.length == 0 )
        {
            usage() ;
            throw new TerminateException(0) ;
        }
        
        CmdLineArgs cl = new CmdLineArgs(argv) ;
        
        ArgDecl helpDecl = new ArgDecl(ArgDecl.NoValue, "h", "help") ;
        cl.add(helpDecl) ;
        
        ArgDecl verboseDecl = new ArgDecl(ArgDecl.NoValue, "v", "verbose") ;
        cl.add(verboseDecl) ;
        
        ArgDecl versionDecl = new ArgDecl(ArgDecl.NoValue, "ver", "version", "V") ;
        cl.add(versionDecl) ;
        
        ArgDecl quietDecl = new ArgDecl(ArgDecl.NoValue, "q", "quiet") ;
        cl.add(quietDecl) ;

        ArgDecl noExecDecl = new ArgDecl(ArgDecl.NoValue, "n", "noExec", "noexec") ;
        cl.add(noExecDecl) ;

        ArgDecl graphDecl = new ArgDecl(ArgDecl.HasValue, "graph", "data") ;
        cl.add(graphDecl) ;
        
        ArgDecl namedGraphDecl = new ArgDecl(ArgDecl.HasValue, "named", "namedgraph", "namedGraph") ;
        cl.add(namedGraphDecl) ;
        
        ArgDecl serviceDecl = new ArgDecl(ArgDecl.HasValue, "service") ;
        cl.add(serviceDecl) ;

        // Or --serviceType GET, POST, SOAP
        ArgDecl postServiceDecl = new ArgDecl(ArgDecl.NoValue, "post", "POST") ;
        cl.add(postServiceDecl) ;
        
        ArgDecl queryFileDecl = new ArgDecl(ArgDecl.HasValue, "query", "file") ;
        cl.add(queryFileDecl) ;
        
        ArgDecl querySyntaxDecl = new ArgDecl(ArgDecl.HasValue, "syntax", "syn", "in") ;
        cl.add(querySyntaxDecl) ;
        
        ArgDecl dataFmtDecl = new ArgDecl(ArgDecl.HasValue, "fmt", "format") ;
        cl.add(dataFmtDecl) ;
        
        ArgDecl resultsFmtDecl = new ArgDecl(ArgDecl.HasValue, "results", "out", "rfmt") ;
        cl.add(resultsFmtDecl) ;

        ArgDecl planningDecl = new ArgDecl(ArgDecl.HasValue, "planning") ;
        cl.add(planningDecl) ;
        
        ArgDecl showDecl = new ArgDecl(ArgDecl.HasValue, "show") ;
        cl.add(showDecl) ;
        
        ArgDecl dirDecl = new ArgDecl(ArgDecl.HasValue, "dir") ;
        cl.add(dirDecl) ;

        ArgDecl baseDecl = new ArgDecl(ArgDecl.HasValue, "base") ;
        cl.add(baseDecl) ;

        ArgDecl lmapDecl = new ArgDecl(ArgDecl.HasValue, "lmap") ;
        cl.add(lmapDecl) ;
        
        
        // ---------------------------------------------------------------------- DARQ 
        ArgDecl config = new ArgDecl(ArgDecl.HasValue, "config") ;
        cl.add(config) ;
        
        
        


        try {
            cl.process() ;
        } catch (IllegalArgumentException ex)
        {
            System.err.println(ex.getMessage()) ;
            usage(System.err) ;
            throw new TerminateException(2) ;
        }

        if ( cl.contains(helpDecl) )
        {
            usage() ;
            throw new TerminateException(0) ;
        }
        
        if ( cl.contains(versionDecl) )
        {
            System.out.println("ARQ Version: "+ARQ.VERSION+" (Jena: "+Jena.VERSION+")") ;
            throw new TerminateException(0) ;
        }
        
        // Now set all the arguments.
        QCmd qCmd = new QCmd() ;

        // ==== General things
        boolean verbose = cl.contains(verboseDecl) ;
        if ( verbose ) { 
            qCmd.setMessageLevel(qCmd.getMessageLevel()+1) ;
            
        }
        

        boolean quiet = cl.contains(quietDecl) ;
        if ( quiet ) { 
            qCmd.setMessageLevel(qCmd.getMessageLevel()-1) ;
            Logger.getLogger(FedQueryIterService.class).setLevel(Level.ERROR);
        } else {
            Logger.getLogger(FedQueryIterService.class).setLevel(Level.TRACE);
        }
        
        
        
        boolean executeQuery = true ;
        if ( cl.contains(noExecDecl) )
            executeQuery = false ;
        
        if ( cl.contains(dirDecl) )
        {
            String dir = cl.getValue(dirDecl) ;
            qCmd.setFileManager(new FileManager()) ;
            qCmd.getFileManager().addLocatorFile(dir) ;
            qCmd.getFileManager().addLocatorURL() ;
        }
        else
            qCmd.setFileManager( FileManager.get() ) ;
        
        if ( cl.contains(baseDecl) )
        {
            qCmd.setBaseURI(cl.getValue(baseDecl)) ;
            RelURI.setBaseURI(qCmd.getBaseURI()) ;
        }
        
        if ( cl.contains(lmapDecl) )
        {
            String lmapFile = cl.getValue(lmapDecl) ;
            LocationMapper locMap = new LocationMapper(lmapFile) ;
            qCmd.getFileManager().setLocationMapper(locMap) ;
        }
            
        if ( cl.contains(planningDecl) )
        {
            String v = cl.getValue(planningDecl) ;
            if ( v.equalsIgnoreCase("on") || v.equalsIgnoreCase("yes") )
                ARQ.getContext().set(ARQ.orderPlanning, "true") ;
            else if ( v.equalsIgnoreCase("off") || v.equalsIgnoreCase("no" ) )
                ARQ.getContext().set(ARQ.orderPlanning, "false") ;
            else
                argError("Unrecognized planning control: "+v) ;
        }

        if ( cl.contains(showDecl) )
        {
            List x = cl.getValues(showDecl) ;
            for ( Iterator iter = x.iterator() ; iter.hasNext(); )
            {
                String v = (String)iter.next() ;
                if ( v.equalsIgnoreCase("plan") )
                    printPlan = true ;
                if ( v.equalsIgnoreCase("query") )
                    printParseTree = true ;
                if ( v.equalsIgnoreCase("time") )
                    qCmd.setTimings(true) ;
            }
        }

        // ==== Service/remote
        
        if ( cl.contains(serviceDecl) )
        {
            String s = cl.getValue(serviceDecl) ;
            qCmd.setServiceURL(s) ;
        }
        
        if ( cl.contains(postServiceDecl) )
        {
            qCmd.setForcePost(true) ;
        }
        // ==== Query
        qCmd.setDefaultSyntax(defaultSyntax) ;
        if ( cl.contains(querySyntaxDecl) )
        {
            // short name
            String s = cl.getValue(querySyntaxDecl) ;
            Syntax syn = Syntax.lookup(s) ;
            if ( syn == null )
                argError("Unrecognized syntax: "+syn) ;
            qCmd.setInSyntax(syn) ;
        }

        String queryFile = cl.getValue(queryFileDecl) ;
        
        if ( cl.getNumPositional() == 0 && queryFile == null )
            argError("No query string or query file") ;

        if ( cl.getNumPositional() > 1 )
            argError("Only one query string allowed") ;
        
        if ( cl.getNumPositional() == 1 && queryFile != null )
            argError("Either query string or query file - not both") ;

        if ( queryFile != null )
            qCmd.setQueryFilename(queryFile) ;
        else
        {
            // One positional argument.
            String qs = cl.getPositionalArg(0) ;
            if ( cl.matchesIndirect(qs) ) 
                qCmd.setSyntaxHint(qs) ;
            
            qs = cl.indirect(qs) ;
            qCmd.setQueryString(qs) ;
        }

        // ==== Data
        
        if ( cl.contains(graphDecl) )
            argError("DARQ does not support the graph option. Use ARQ instead.");
            //qCmd.setGraphURLs(cl.getValues(graphDecl)) ;
        
        if ( cl.contains(dataFmtDecl) )
        {
            String dSyn = cl.getValue(dataFmtDecl) ;
            qCmd.setDataSyntax(DataFormat.lookup(dSyn)) ;
            if ( qCmd.getDataSyntax() == null )
                argError("Unrecognized syntax for data: "+dSyn) ;
        }

        //==== Named graph
        if ( cl.contains(namedGraphDecl) )
        {
            argError("DARQ does not support the named graph option. Use ARQ instead.");
            //qCmd.setNamedGraphURLs(cl.getValues(namedGraphDecl)) ;
        }
        
        // ==== Results presentation

        if ( cl.contains(resultsFmtDecl) )
        {
            String rFmt = cl.getValue(resultsFmtDecl) ;
            qCmd.setOutputFormat( ResultsFormat.lookup(rFmt) ) ;
            if ( qCmd.getOutputFormat() == null )
                argError("Unrecognized output format: "+rFmt) ;
        }
        
        // ------------------------------------------------------------------------------- DARQ
        if ( cl.contains(config) )
        {
            FedQueryEngineFactory.register(cl.getValue(config));
            qCmd.setDataset(new DarqDataset());
        } else argError("Argument Error: No config file. use --config=<file>") ;
        
        
            
        // ==== Do it
        try {
            qCmd.setLineNumbers(true) ;
            qCmd.parseQuery() ;
            if ( qCmd.getQuery() == null )
                return ;
            if ( qCmd.getMessageLevel() > 0 )
            {
                qCmd.printQuery() ;
                System.out.println() ;
            }
            if ( printParseTree )
            {
                qCmd.getQuery().serialize(System.out, Syntax.syntaxPrefix) ;
                System.out.println() ;
            }
            
            if ( printPlan )
            {
                PlanElement plan = QueryUtils.generatePlan(qCmd.getQuery()) ;
                PlanFormatter.out(System.out, qCmd.getQuery(), plan) ;
                System.out.println() ;
            }
            

            
            // Do it!
            if ( executeQuery )
                qCmd.query() ;
        } 
        catch (CmdException ex) { throw ex ; }
        catch (TerminateException ex) { throw ex ; }
        catch (Exception ex)
        {
            System.err.println("Exception from QCmd: "+ex.getMessage()) ;
            ex.printStackTrace(System.err) ;
            throw new TerminateException(9) ;
        }
        //throw new TerminateException(0) ;
    }

    static void usage() { usage(System.out) ; }
    
    static void usage(java.io.PrintStream out)
    {
        out.println("Usage: [--config URL] [queryString | --query file]") ;
        out.println("   --query file         Read one query from a file") ;
        out.println("   --syntax SYN         Query syntax") ;
        out.println("                            SPARQL (default), ARQ, RDQL, N3QL") ;
        out.println("                            File suffixes imply syntax:") ;
        out.println("                            .rq / SPARQL,  .arq / ARQ, .rdql / RDQL") ;
        //out.println("   --rdfs               Use an RDFS reasoner around the data") ;
        //out.println("   --reasoner URI       Set the reasoner URI explicitly.") ;
        //out.println("   --vocab URL | File   Specify a separate vocabulary (may also be in the data)") ;
        out.println("   --config file        File containing Service Descriptions");
        out.println("   --post               Force the use of HTTP POST") ;
        out.println("   --fmt FMT            Data source format: RDF/XML (default),") ;
        out.println("                            N3, N-TRIPLES, TURTLE") ;
        out.println("   --results FORM       Format of result");
        out.println("                          SELECT queries") ;
        out.println("                            text [default], count, tuples, none for display") ;
        out.println("                            rs, rs/graph, xml, json, rs/text for the result set ") ;
        out.println("                          CONSTRUCT and DESCRIBE queries") ;
        out.println("                            RDF, RDF/XML, N3, N-TRIPLES") ;
        out.println("                          ASK queries") ;
        out.println("                            xml, text") ;
        out.println("   --verbose            Verbose - more messages") ;
        out.println("   --quiet              Quiet - less messages") ;
        out.println("   --version            Print version information and exit") ;
        out.println("   -n --noExec          Don't actually execute the query") ;
        out.println("   --planning on | off  Turn off/on query planning") ;
        out.println("   --show WHAT          Print various internals details (parse trees, planning trees)") ; 
        out.println("                        WHAT is currently 'plan' or 'query'") ;
        
    }
    
    // Does not return
    static void argError(String s)
    {
        System.err.println("Argument Error: "+s) ;
        //usage(System.err) ;
        throw new TerminateException(3) ;
    }
 }

/*
 *  (c) Copyright 2004, 2005, 2006 Hewlett-Packard Development Company, LP
 *  All rights reserved.
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

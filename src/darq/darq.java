/*
 * (c) Copyright 2004, 2005, 2006 Hewlett-Packard Development Company, LP
 * [See end of file]
 */


package darq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.hp.hpl.jena.query.darq.engine.FedQueryEngine;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngineFactory;

import arq.cmd.CmdUtils;

/** A program to execute queries from the command line in ARQ mode.
  *
 * @author  Andy Seaborne
 * @version $Id: arq.java,v 1.8 2006/06/07 16:53:21 andy_seaborne Exp $
 */

public class darq
{
    static { CmdUtils.setLog4j() ; CmdUtils.setN3Params() ; }

    //static protected Log logger = LogFactory.getLog( arq.class );

    public static void main (String [] argv)
    { 
    	String [] zeile; 
    	String eingabe = "";
    	if (argv.length == 0){
			System.out.println("Welcome to DARQ!");
		}
    	do{
    		if (argv.length != 0){
    			zeile = argv;
    		}
    		else{
    			System.out.println("");
    			System.out.println("Type '--help' for help");
    			System.out.println("or 'exit' to quit");

    			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
    			zeile = null;
    			try {
    				eingabe = console.readLine();
    				zeile = eingabe.split(" ");
    			} catch (IOException e) {
    				// Sollte eigentlich nie passieren
    				e.printStackTrace();
    			}
    		}
    		if(!eingabe.equals("exit")){
//    			for(String part:zeile){ //TESTAUSGABE
//    				System.out.println(part);
//    			}
    			query.main(zeile);
    			FedQueryEngineFactory.unregister();
    			argv  = new String[0];
    		}
    		
    	}while(!eingabe.equals("exit"));
    }
}
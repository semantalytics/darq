package demo;

import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.darq.core.DarqDataset;
import com.hp.hpl.jena.query.darq.engine.DarqTransform;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngine;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngineFactory;
import com.hp.hpl.jena.query.darq.engine.optimizer.CostBasedPlanOptimizer;
import com.hp.hpl.jena.query.resultset.ResultSetMem;

public class JucDemoSingleQuery {

    public static final String configFile = "src/demo/Demo1SD.n3";

    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        Logger.getLogger(DarqTransform.class).setLevel(Level.DEBUG);
        Logger.getLogger(FedQueryEngine.class).setLevel(Level.DEBUG);
        //Logger.getLogger(BasicPlanOptimizer.class).setLevel(Level.DEBUG);
        Logger.getLogger(CostBasedPlanOptimizer.class).setLevel(Level.DEBUG);
        
        
        String [] q = new String[]{ 
                "PREFIX dc: <http://purl.org/dc/elements/1.1/#>",
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/#>",
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
                "SELECT ?n ?mbox ?date {",
                "  ?x dc:title 'Jena: Implementing the Semantic Web Recommendations' .", 
                "  ?x dc:creator ?n .", 
                "  ?y foaf:name ?n .",
                "  ?y foaf:mbox ?mbox .",
                "  ?y rdf:type foaf:Person ." ,
             //   "FILTER (REGEX(?n, 'abc') )", 
               "  OPTIONAL {?x dc:date ?date .}",
               
                "}"
        };
 
        
        
        
        String querystring = getQueryString(q);

        // output query and wait for key
        System.out.println("Query: \n------ \n" + querystring);
        waitForKey();

       // register new FedQueryEngineFactory and load configuration from file
       FedQueryEngineFactory.register(configFile);

       // create query
       Query query = QueryFactory.create(querystring);
       
       
       // create model - it is used for the local parts of the query -
       Dataset ds = new DarqDataset();
       
       // get FedQueryEngineFactory
       QueryExecution qe = QueryExecutionFactory.create(query, ds);

       // execute
       ResultSet rs = qe.execSelect();
       
       ResultSetMem rsm = new ResultSetMem(rs);
       
       
       // output results
       ResultSetFormatter.out(System.out, rsm, query);
       System.out.println("Results: "+rsm.size()); 

    
         
         
      

    }
    
    protected static String getQueryString(String [] in) {
        String s ="";
            for (String sub:in) {
                s+=sub+"\n";
            }
        return s;
    }

    
    public  static void waitForKey() {
        InputStreamReader reader = new InputStreamReader (System.in);

            System.out.println("- press ENTER to continue -");
                try    {
                    reader.read();
                }
                catch (IOException e)    {}

    }

}


// --- 1
/*   String qs = "PREFIX dc: <http://purl.org/dc/elements/1.1/#>"
           + "PREFIX foaf: <http://xmlns.com/foaf/0.1/#>"
           + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
           + "PREFIX hp: <http://jena.hpl.hp.com/schemas/hpcorp#>"
           + "SELECT ?n ?mbox  ?managername{ \n" +
           // "OPTIONAL {?a foaf:homepage ?c . }"+
           "?x dc:title \"Jena: Implementing the Semantic Web Recommendations\" ."+ 
           "?x dc:creator ?n ." + 
           "?y foaf:name ?n ." +
           "?y foaf:mbox ?mbox ." +
           "?y hp:manager ?manager ." +
           "?manager foaf:name ?managername ." +
           "?y rdf:type foaf:Person" + 
           "}";*/
    
/*   String qs = "PREFIX dc: <http://purl.org/dc/elements/1.1/#>"
       + "PREFIX foaf: <http://xmlns.com/foaf/0.1/#>"
       + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
       + "PREFIX hp: <http://jena.hpl.hp.com/schemas/hpcorp#>"
       + "SELECT ?name ?mbox ?title { \n" +
       // "OPTIONAL {?a foaf:homepage ?c . }"+
       "?b foaf:name \"Bastian Ruben Quilitz\" ." + 
       "?b hp:manager ?manager ." +
       "?y hp:manager ?manager ." +
       "?y foaf:name ?name . "+
       "?y foaf:mbox ?mbox ." +
       "?p dc:creator ?name ." +
       "?p dc:title ?title ." +
       "?y rdf:type foaf:Person" + 
       "}";  */
       
   
   
   
   
/*    String qs = "PREFIX dc: <http://purl.org/dc/elements/1.1/#> \n"
       + "PREFIX foaf: <http://xmlns.com/foaf/0.1/#> \n"
       + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
       + "SELECT ?title ?n ?mbox { \n" +
       "?x dc:title ?title . \n"+ 
       "?x dc:creator ?n . \n" + 
       "?y foaf:name ?n . \n" +
       "?y foaf:mbox ?mbox . \n" +
       "?y rdf:type foaf:Person \n" + 
       "}";*/
   
   
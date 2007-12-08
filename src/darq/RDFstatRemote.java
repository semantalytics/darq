package darq;

/**
 * currently this is specific for Openlink Virtuoso 5.0.2.
 * We use the sparql aggregate extensions the come with their query enginge...
 */
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.darq.config.schema.DOSE;
import com.hp.hpl.jena.query.engine1.QueryEngine;
import com.hp.hpl.jena.query.engineHTTP.HttpQuery;
import com.hp.hpl.jena.query.engineHTTP.QueryEngineHTTP;
import com.hp.hpl.jena.query.resultset.ResultSetMem;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;


public class RDFstatRemote {
    
    
    
    private static String schemaURI= "http://darq.sf.net/dose/0.1#";
    
    String lastquery = null;
    
    
  
    public String getLastquery() {
		return lastquery;
	}




	private void report(String endpointURL, String graph) throws Exception {
        
    	 Model model = ModelFactory.createDefaultModel();
         model.setNsPrefix("sd","http://darq.sf.net/dose/0.1#");
         model.setNsPrefix("rdfs","http://www.w3.org/2000/01/rdf-schema#");
         model.setNsPrefix("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");

         Resource r = model.createResource();
         
         
         
         model.add(r,RDF.type,model.createResource("sd:Service"));
         model.add(r,DOSE.url,model.createResource(endpointURL));
         
    	lastquery = "SELECT DISTINCT ?p WHERE {?s ?p ?o}";
    	QueryEngineHTTP qe_predicates = new QueryEngineHTTP(endpointURL,lastquery);
    	qe_predicates.addDefaultGraph(graph);
    	ResultSet rs_predicates  = qe_predicates.execSelect();
    	ResultSetMem rsm_predicates = new ResultSetMem(rs_predicates);
    	
    	
    	long totalcount=0;
    	
    	while (rsm_predicates.hasNext()) {
    		QuerySolution s = rsm_predicates.nextSolution();
    		
    		lastquery = "SELECT count(*) WHERE {?s <"+s.getResource("p")+"> ?o}";
    		QueryEngineHTTP qe_count = new QueryEngineHTTP(endpointURL,lastquery);
    		qe_count.addDefaultGraph(graph);
    		long count=0;
    		ResultSet rs_count  = qe_count.execSelect();
    		if (rs_count.hasNext()) {
    			count= rs_count.nextSolution().getLiteral("callret-0").getLong();
    			totalcount+=count;
    		} else throw new Exception("error getting total size");
    		
    		if (!s.get("p").equals(RDF.type)) {
    		
    			lastquery = "select count distinct ?o  WHERE {?s <"+s.getResource("p")+"> ?o. filter (bif:length(str(?o)) <= 1024) }"; 
    		QueryEngineHTTP qe_objects = new QueryEngineHTTP(endpointURL,lastquery);
    		qe_objects.addDefaultGraph(graph);
        	ResultSet rs_objects  = qe_objects.execSelect();
    		double osel =0;
    		if (rs_objects.hasNext()) {
    			long no = rs_objects.nextSolution().getLiteral("callret-0").getLong();
    			osel = 1.0/(new Double(no));
    		} else throw new Exception("error getting #objects");
    		
    		
    		lastquery= "select count distinct ?s  WHERE {?s <"+s.getResource("p")+"> ?o }";
    		QueryEngineHTTP qe_subjects = new QueryEngineHTTP(endpointURL,lastquery);
    		qe_subjects.addDefaultGraph(graph);
        	ResultSet rs_subjects  = qe_subjects.execSelect();
        	double ssel =0;
    		if (rs_subjects.hasNext()) {
    			long no = rs_subjects.nextSolution().getLiteral("callret-0").getLong();
    			ssel = 1.0/(new Double(no));
    		} else throw new Exception("error getting #subjects");
    	
    		 Resource capability = model.createResource();
             model.add(r,DOSE.capability,capability);
             model.add(capability,DOSE.predicate,s.getResource("p"));
             model.add(capability,DOSE.sofilter,model.createLiteral(""));
             
             
             //model.add(capability,model.createProperty("sd:","triples"),model.createLiteral(new Integer(sum)));
             model.add(capability,DOSE.triples,model.createTypedLiteral(new Long(count),XSD.integer.getURI()));
             
             //model.add(capability,model.createProperty("sd:","objectSelectivity"),model.createLiteral(new Double(value)));
             model.add(capability,model.createProperty(schemaURI,"objectSelectivity"),model.createTypedLiteral(new Double(osel),XSD.xdouble.getURI()));
//           model.add(capability,model.createProperty("sd:","objectSelectivity"),model.createLiteral(new Double(value)));
             model.add(capability,model.createProperty(schemaURI,"subjectSelectivity"),model.createTypedLiteral(new Double(ssel),XSD.xdouble.getURI()));
             	
    		
    		} else {
    			
    		}
    		
    		
    	}
    	
    	
 
    	model.add(r,DOSE.totalTriples,model.createTypedLiteral(new Long(totalcount)));
        
        
        
    	 model.write(System.out,"N3");
   
        
  
        
        
        /*line();
        System.out.println("Number of different predicates: "+predicates.size());
        line();
        
        System.out.println("Number of classes:" +classes.size());
        line();*/
        
        
       /* int classcount =0;
        String filterstring="";
        
        for (RDFNode n:classes.keySet()) {
            if (classcount!=0) filterstring+=" || "; 
            filterstring+= "REGEX(STR(?object),'"+n.toString()+"')";
            classcount+=classes.get(n);
        }
        
        Resource capability = model.createResource(); 
        model.add(r,DOSE.capability,capability);
        model.add(capability,DOSE.predicate ,RDF.type);
        model.add(capability,DOSE.sofilter,model.createTypedLiteral(filterstring));
        model.add(capability,DOSE.triples,model.createTypedLiteral(classcount,XSD.integer.getURI()));
*/
        
  
       
    }
    
   
    
    
    public static void main(String[] args) {
        
        if (args.length!=2) {
            System.out.println("usage: rdfstat endpointurl graph");
            System.out.println("eg: rdfstat http://localhost/sparql mygraph");
            System.exit(1);
        }
        RDFstatRemote stat = new RDFstatRemote();
        try {
			stat.report(args[0],args[1]);
		} catch (Exception e) {
			System.err.println("Error for query: "+ stat.getLastquery());
			e.printStackTrace(System.err);
		}
      
    }
    
    
    
    

}

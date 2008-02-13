/**
 * 
 */
package de.hu_berlin.informatik.wbi.darq.mapping;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.darq.config.MapConfiguration;
import com.hp.hpl.jena.query.darq.core.DarqDataset;
//import com.hp.hpl.jena.query.darq.engine.FedQueryEngine;
import de.hu_berlin.informatik.wbi.darq.mapping.MapFedQueryEngine;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.PlanOperatorBase;
import com.hp.hpl.jena.query.darq.util.DARQLogHook;
import com.hp.hpl.jena.query.engine.QueryEngineFactory;
import com.hp.hpl.jena.query.engine.QueryEngineRegistry;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.rdf.model.Model;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.semanticweb.owl.model.OWLOntology; 

/**
 * @author Alexander Musidlowski
 * @version $ID$
 *
 */
public class MapFedQueryEngineFactory implements QueryEngineFactory{

	private OWLOntology ontology;
	private MapConfiguration config;
	 
    private static MapFedQueryEngineFactory instance = null;
    private static DARQLogHook loghook = null;

    static boolean registered = false;
    
    

	private MapFedQueryEngineFactory(MapConfiguration conf, OWLOntology ontology) {
				// TODO Auto-generated constructor stub
		this.config = conf;
		this.ontology = ontology;
	}

	/*
	 * erzeugt genau ein Objekt von MapFedQueryEngineFactory 
	 */
	 
	public static void register(MapConfiguration conf, OWLOntology ontology) {
		if (!registered) { 
            instance = new MapFedQueryEngineFactory(conf, ontology);
            QueryEngineRegistry.addFactory(instance);
            registered=true;
        }	    
	}
	 
	public static void register(String configFileName, OWLOntology ontology) {
	        MapConfiguration c = new MapConfiguration(configFileName);
	        register(c, ontology);
	}
	
	 /**
	 * @return Returns the instance.
	 */
	public static MapFedQueryEngineFactory getInstance() {
		return instance;
	}
	
	 public QueryExecution create(Query query, Dataset dataset) {
	        MapFedQueryEngine fqe= new MapFedQueryEngine(query, config, ontology);
	        fqe.setDataset(dataset);
	        return fqe;
	 }
	 
	 public OWLOntology getOntology() {
		   
         return ontology;
	 }
	 
	 /**********************************/	    
	    
	    public static void unregister() {
	        QueryEngineRegistry.removeFactory(instance);
	        registered=false;
	    }

	    public boolean accept(Query query, Dataset dataset) {
//	      TODO check whether all triples match at least one service or the local datasets
	        if (dataset instanceof DarqDataset) return true;
	        return false; 
	    }

	    public static void logPlan(Query query, PlanElement planElement) {
	        if (loghook!= null) {
	            loghook.logPlan( query, planElement);
	        }
	    }
	    
	    public static void logPlanOptimized(Query query, PlanElement planElement) {
	        if (loghook!= null) {
	            loghook.logPlanOptimized(query, planElement);
	        }
	    }

	    public static void logSubquery(Query query) {
	        if (loghook!= null) {
	            loghook.logSubquery(query);
	        }
	    }    
	    public static void logExplain(PlanOperatorBase pob) {
	        if (loghook!= null) {
	            loghook.logOptimizerExplain(pob);
	        }
	    }
	    
	    public Model getConfigModel() {
	   
	            return config.getModel();
	    }
	    /**
	     * @return the config
	     */
	    public MapConfiguration getConfig() {
	        return config;
	    }

	    /**
	     * @param loghook The loghook to set.
	     */
	    public static void setLoghook(DARQLogHook lh) {
	        loghook = lh;
	    }

	 
	  
}

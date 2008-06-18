/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.engine;

import org.semanticweb.owl.model.OWLOntology;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.darq.config.Configuration;
import com.hp.hpl.jena.query.darq.core.DarqDataset;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.PlanOperatorBase;
import com.hp.hpl.jena.query.darq.util.DARQLogHook;
import com.hp.hpl.jena.query.engine.QueryEngineFactory;
import com.hp.hpl.jena.query.engine.QueryEngineRegistry;
import com.hp.hpl.jena.query.engine1.PlanElement;

import de.hu_berlin.informatik.wbi.darq.cache.Caching;

public class FedQueryEngineFactory implements QueryEngineFactory {
  
    static boolean registered = false; 
    
    private Configuration config;
    private static FedQueryEngineFactory instance = null;
    private static DARQLogHook loghook = null;

    /* mapping */
	private OWLOntology ontology;
	private Integer transitivity;
    
	/* caching */
	private Caching cache;
	private Boolean cacheEnabled;
	
    public static void register(Configuration conf, OWLOntology ontology, Integer transitivity, Caching cache, Boolean cacheEnabled) {
        // register only once
        if (!registered) { 
            instance = new FedQueryEngineFactory(conf, ontology, transitivity, cache,cacheEnabled);
            QueryEngineRegistry.addFactory(instance);
            registered=true;
        }
    }
    
    public static void unregister() {
        QueryEngineRegistry.removeFactory(instance);
        registered=false;
    }
    
    
    
    /**
     * @return Returns the instance.
     */
    public static FedQueryEngineFactory getInstance() {
        return instance;
    }



    public static void register(String configFileName, OWLOntology ontology, Integer transitivity, Caching cache,Boolean cacheEnabled) {
        Configuration c=new Configuration(configFileName);
        register(c, ontology, transitivity, cache,cacheEnabled);
    }
    
    
    protected FedQueryEngineFactory(Configuration conf, OWLOntology ontology, Integer transitivity, Caching cache,Boolean cacheEnabled) {
        this.config=conf;
        this.ontology = ontology;
        this.transitivity = transitivity;
        this.cache=cache;
        this.cacheEnabled=cacheEnabled;
    }
    
    

    public boolean accept(Query query, Dataset dataset) {
//      TODO check whether all triples match at least one service or the local datasets
        if (dataset instanceof DarqDataset) return true;
        return false; 
    }

    public QueryExecution create(Query query, Dataset dataset) {
        FedQueryEngine fqe= new FedQueryEngine(query,config, ontology, transitivity,cache,cacheEnabled);
        fqe.setDataset(dataset);
        return fqe;
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
    
   /* public Model getConfigModel() {
   
            return config.getModel();

        
    }*/
    
    
    

    /**
     * @return the config
     */
    public Configuration getConfig() {
        return config;
    }

    /**
     * @param loghook The loghook to set.
     */
    public static void setLoghook(DARQLogHook lh) {
        loghook = lh;
    }

	public OWLOntology getOntology() {
		return ontology;
	}

	public void setOntology(OWLOntology ontology) {
		this.ontology = ontology;
	}

	public Integer getTransitivity() {
		return transitivity;
	}

	public void setTransitivity(Integer transitivity) {
		this.transitivity = transitivity;
	}


    
}
/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
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
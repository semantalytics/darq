/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owl.model.OWLOntology;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.darq.config.Configuration;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.QueryEngine;
import com.hp.hpl.jena.query.util.Context;

import de.hu_berlin.informatik.wbi.darq.cache.Caching;


public class FedQueryEngine extends QueryEngine {
    
    static Log log = LogFactory.getLog(FedQueryEngine.class) ;
    
    /* Map */ 
    OWLOntology ontology;
  	Integer transitivity;
  	
  	/* Cache */ 
  	Caching cache;
    Boolean cacheEnabled = false;
  	
    Query query ;
    Configuration config;
    private long transformTime =0; 
    boolean optimize = true;

    /**
     * @return the optimize
     */
    public boolean isOptimize() {
        return optimize;
    }

    /**
     * @param optimize the optimize to set
     */
    public void setOptimize(boolean optimize) {
        this.optimize = optimize;
    }

    public FedQueryEngine(Query q) {
        this(q,null,null, null, 0, null, false);     
    }
    public FedQueryEngine(Query q, Boolean cacheEnabled) {
        this(q,null,null, null, 0, null, cacheEnabled);     
    }
    
    public FedQueryEngine(Query q, Caching cache,Boolean cacheEnabled) {
        this(q,null,null, null, 0, cache, cacheEnabled);     
    }
    
    public FedQueryEngine(Query q, Caching cache) {
        this(q,null,null, null, 0, cache, true);     
    }
    
    public FedQueryEngine(Query q, Configuration conf) {
        this(q,null,conf, null, 0,null, false);     
    }
    
    public FedQueryEngine(Query q, Configuration conf, Boolean cacheEnabled) {
        this(q,null,conf, null, 0,null, cacheEnabled);     
    }
    
    public FedQueryEngine(Query q, Configuration conf, Caching cache, Boolean cacheEnabled) {
        this(q,null,conf, null, 0,cache, cacheEnabled);     
    }

    public FedQueryEngine(Query q, Configuration conf, Caching cache) {
        this(q,null,conf, null, 0,cache, true);     
    }
    
    public FedQueryEngine(Query q, OWLOntology ontology, Boolean cacheEnabled) {
        this(q,null,null, ontology, 0, null, cacheEnabled);     
    }
    
    public FedQueryEngine(Query q, OWLOntology ontology) {
        this(q,null,null, ontology, 0, null, false);     
    }

    
    public FedQueryEngine(Query q, OWLOntology ontology, Caching cache) {
        this(q,null,null, ontology, 0, cache, true);     
    }
    
    public FedQueryEngine(Query q, OWLOntology ontology, Caching cache, Boolean cacheEnabled) {
        this(q,null,null, ontology, 0, cache, cacheEnabled);     
    }

    public FedQueryEngine(Query q, OWLOntology ontology, Integer transitivity) {
        this(q,null,null, ontology, transitivity,null, false);     
    }

    public FedQueryEngine(Query q, OWLOntology ontology, Integer transitivity, Boolean cacheEnabled) {
        this(q,null,null, ontology, transitivity,null, cacheEnabled);     
    }

    public FedQueryEngine(Query q, OWLOntology ontology, Integer transitivity, Caching cache) {
        this(q,null,null, ontology, transitivity,cache,true);     
    }
    public FedQueryEngine(Query q, OWLOntology ontology, Integer transitivity, Caching cache,Boolean cacheEnabled) {
        this(q,null,null, ontology, transitivity,cache, cacheEnabled);     
    }

    public FedQueryEngine(Query q, Configuration conf, OWLOntology ontology, Integer transitivity) {
        this(q,null,conf, ontology, transitivity,null,false);     
    }

    public FedQueryEngine(Query q, Configuration conf, OWLOntology ontology, Integer transitivity,Boolean cacheEnabled) {
        this(q,null,conf, ontology, transitivity,null,cacheEnabled);     
    }

    public FedQueryEngine(Query q, Configuration conf, OWLOntology ontology, Integer transitivity, Caching cache) {
        this(q,null,conf, ontology, transitivity,cache,true);     
    }

    public FedQueryEngine(Query q, Configuration conf, OWLOntology ontology, Integer transitivity, Caching cache,Boolean cacheEnabled) {
        this(q,null,conf, ontology, transitivity,cache,cacheEnabled);     
    }

    public FedQueryEngine(Query q, Configuration conf, OWLOntology ontology) {
        this(q,null,conf, ontology, 0,null,false);     
    }
    
    public FedQueryEngine(Query q, Configuration conf, OWLOntology ontology,Boolean cacheEnabled) {
        this(q,null,conf, ontology, 0,null,cacheEnabled);     
    }

    public FedQueryEngine(Query q, Configuration conf, OWLOntology ontology, Caching cache) {
        this(q,null,conf, ontology, 0,cache,true);     
    }
    
    public FedQueryEngine(Query q, Configuration conf, OWLOntology ontology, Caching cache,Boolean cacheEnabled) {
        this(q,null,conf, ontology, 0,cache,cacheEnabled);     
    }

    public FedQueryEngine(Query q,Context p,Configuration conf, OWLOntology ontology, Integer transitivity, Caching cache, Boolean cacheEnabled) {
        super(q,p);
        query=q;
        config=conf;
        this.ontology = ontology;
        this.transitivity = transitivity;
        this.cache = cache;
        this.cacheEnabled = cacheEnabled;
    }
    
    public void setConfig(Configuration c) {
        config=c;
    }
    
    public Configuration getConfiguration() {
        return config;
    }
    
    
    /* (non-Javadoc)
     * @see com.hp.hpl.jena.query.engine1.QueryEngine#queryPlanHook(com.hp.hpl.jena.query.util.Context, com.hp.hpl.jena.query.engine1.PlanElement)
     */
    @Override
    protected PlanElement queryPlanHook(Context context, PlanElement planElt) {
        FedQueryEngineFactory.logPlan(query,planElt);
        PlanElement pe;
        if ( (ontology == null) && (transitivity == 0) ){
        	DarqTransform t = new DarqTransform(context,config, cache, cacheEnabled);
        	t.setOptimize(optimize);
        	long t2;
        	long t1=System.nanoTime();
        	pe = DarqTransformer.transform(t,planElt);
        	t2=System.nanoTime();
        	transformTime=t2-t1;
        }
        else{
        	MapDarqTransform t = new MapDarqTransform(context,config, ontology, transitivity,cache,cacheEnabled);
        	t.setOptimize(optimize);
        	long t2;
        	long t1=System.nanoTime();
        	pe = MapDarqTransformer.transform(t,planElt);
        	t2=System.nanoTime();
        	transformTime=t2-t1;
        }
        FedQueryEngineFactory.logPlanOptimized(query,pe);
        // log.debug("PLAN: \n" + OutputUtils.PlanToString(pe));
        return pe;
    }

    /**
     * @return the transformTime
     */
    public long getTransformTime() {
        return transformTime;
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
    
    
 /*   protected PlanElement makePlanForQueryPattern(Plan plan)
    {
        FedQueryCompiler fqc = new FedQueryCompiler(config);
        PlanElement pe = fqc.makePlan(plan, query.getQueryBlock()) ;
        
        log.debug("PLAN: \n" + OutputUtils.PlanToString(pe));
        // if (log.isDebugEnabled()) JucDemo.waitForKey();  //  REMOVE JUST FOR DEMO !!  
        return pe;
    }*/
    
    

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
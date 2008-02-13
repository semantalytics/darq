package de.hu_berlin.informatik.wbi.darq.mapping;

/**
 * @author Alexander Musidlowski
 * @version $ID$
 *
 */
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.darq.config.MapConfiguration;
//import com.hp.hpl.jena.query.darq.engine.DarqTransform;
import de.hu_berlin.informatik.wbi.darq.mapping.MapDarqTransform;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngine;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngineFactory;
//import com.hp.hpl.jena.query.engine.QueryEngineFactory;
import de.hu_berlin.informatik.wbi.darq.mapping.MapFedQueryEngineFactory;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.plan.Transformer;
import com.hp.hpl.jena.query.util.Context;
import org.semanticweb.owl.model.OWLOntology;


public class MapFedQueryEngine extends FedQueryEngine {

	  Query query ;
	  MapConfiguration config;
	  OWLOntology ontology;
	  private long transformTime =0;
	  boolean optimize = true;
	    
	  
	public MapFedQueryEngine(Query q) {
		this(q,null,null,null);
		// TODO Auto-generated constructor stub
	}
	//FRAGE ist diese Methode sinnvoll?
	public MapFedQueryEngine(Query q, OWLOntology ontology) {
		this(q,null,null, ontology);
	}
	
	public MapFedQueryEngine(Query q, MapConfiguration conf) {
		this(q,null,conf,null);
		// TODO Auto-generated constructor stub
	}

	public MapFedQueryEngine(Query q, Context p, MapConfiguration conf) {
		this(q,p,conf,null);
		// TODO Auto-generated constructor stub
	}
	
	public MapFedQueryEngine(Query q, MapConfiguration conf, OWLOntology ontology) {
		this(q,null, conf,ontology);
		// TODO Auto-generated constructor stub
	}
	
	public MapFedQueryEngine(Query q, Context p, MapConfiguration conf, OWLOntology ontology) {
		super(q, p, conf);
		this.ontology = ontology;
		this.config = conf;
		// TODO Auto-generated constructor stub
	}
	
	public OWLOntology getOntology() {
		return ontology;
	}
	
	public void setOntology(OWLOntology ontology) {
		this.ontology = ontology;
	}

	 /* (non-Javadoc)
     * @see com.hp.hpl.jena.query.engine1.QueryEngine#queryPlanHook(com.hp.hpl.jena.query.util.Context, com.hp.hpl.jena.query.engine1.PlanElement)
     */
    @Override
    protected PlanElement queryPlanHook(Context context, PlanElement planElt) {
        MapFedQueryEngineFactory.logPlan(query,planElt);
        MapDarqTransform t = new MapDarqTransform(context,config, ontology);
        t.setOptimize(optimize);
        long t2;
        long t1=System.nanoTime();
        PlanElement pe = Transformer.transform(t,planElt);
        t2=System.nanoTime();
        transformTime=t2-t1;
        
        MapFedQueryEngineFactory.logPlanOptimized(query,pe);
       // log.debug("PLAN: \n" + OutputUtils.PlanToString(pe));
        return pe;
    }

	
}

package com.hp.hpl.jena.query.darq.engine.compiler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.core.Binding;
import com.hp.hpl.jena.query.core.BindingMap;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngineFactory;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterConcat;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterDistinct;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterPlainWrapper;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterRepeatApply;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.sun.org.apache.bcel.internal.generic.DADD;

/**
 * Abstract Class DarqQueryIterator
 * @author Bastian Quilitz
 * @version $ID$
 *
 */
public abstract class DarqQueryIterator extends QueryIterRepeatApply {

    Log log = LogFactory.getLog(DarqQueryIterator.class);

    

    // Node sourceNode ;
    protected PlanElement subPattern;  //should be null ?

    protected ServiceGroup serviceGroup = null;
    
    protected QueryExecution qexec=null;
    

   
    public DarqQueryIterator(QueryIterator input, ServiceGroup sg,
            ExecutionContext context, PlanElement subComp) {
        super(input, context);
        // sourceNode = _sourceNode ;
        this.serviceGroup = sg;
        subPattern = subComp;
    }

    /**
     * Query the remote Service
     * Query q - the query to be sent
     */
    protected abstract ResultSet ExecRemoteQuery(Query q) ;

   

    @Override
    protected QueryIterator nextStage(Binding binding) {

        RemoteQuery remoteQuery = new RemoteQuery(serviceGroup,
                getExecContext(), binding);

        long noResults = 0;
        
        QueryIterConcat concatIterator = new QueryIterConcat(getExecContext());

        while (remoteQuery.hasNextQuery(noResults)) {

            Query query = remoteQuery.getNextQuery();

            ResultSet remoteResults = ExecRemoteQuery(query);
            
            List<Binding> newBindings = new ArrayList<Binding>();
               
            while (remoteResults.hasNext()) {
                
                noResults++;

                BindingMap bm = new BindingMap(binding);
                QuerySolution sol = remoteResults.nextSolution();

                for (Iterator solVars = sol.varNames(); solVars.hasNext();) {
                    String varName = (String) solVars.next();

                    // XXX CHECK if VARIABLE EXISTS IN BINDING !??
                    RDFNode obj = sol.get(varName);
                    if (obj != null)
                        bm.add(varName, obj.asNode());
                }

                newBindings.add(bm);

            }
            
            if (newBindings.size()>0) concatIterator.add(new QueryIterPlainWrapper(newBindings.iterator(), null));

        }
        if (qexec!=null) qexec.close();
        
        return new QueryIterDistinct(concatIterator,getExecContext());
    }
}

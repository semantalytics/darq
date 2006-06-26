/*
 * (c) Copyright 2004, 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.query.darq.engine.compiler;

import java.io.InputStream;
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
import com.hp.hpl.jena.util.FileManager;

/**
 * Process a remote Service
 * 
 * @author Bastian Quilitz
 * @version $ID$
 */

public class FedQueryIterService extends QueryIterRepeatApply {

    Log log = LogFactory.getLog(FedQueryIterService.class);

    private static String TESTING_STRING = "_testing_";

    // Node sourceNode ;
    PlanElement subPattern;  //should be null

    private ServiceGroup serviceGroup = null;

    public FedQueryIterService(QueryIterator input, ServiceGroup sg,
            ExecutionContext context, PlanElement subComp) {
        super(input, context);
        // sourceNode = _sourceNode ;
        this.serviceGroup = sg;
        subPattern = subComp;

    }

    /**
     * Query the remote Service
     */
    private ResultSet ExecRemoteQuery(Query q) {

        String url = serviceGroup.getService().getUrl();
        // check for testing
        if (url.contains(TESTING_STRING)) {
            // execute query locally
            return execRemoteQueryTesting(q);

        }

        ResultSet remoteResults = null;
        QueryExecution qexec = QueryExecutionFactory.sparqlService(url, q);
        try {

            remoteResults = qexec.execSelect();

        } finally {
            qexec.close();
        }
        return remoteResults;
    }

    /**
     * Executes a query locally for testing
     */
    private ResultSet execRemoteQueryTesting(Query q) {
        Model model = ModelFactory.createDefaultModel();

        String url = serviceGroup.getService().getUrl();

        String filename = url.replace(TESTING_STRING, "");

/*        InputStream in = FileManager.get().open(filename);
        if (in == null) {
            throw new IllegalArgumentException("File: " + filename
                    + " not found");
        }

        */

        model.read(filename,"N3");
        
       /* model.write(System.out,"N3");
        System.out.println("---");
        System.out.println(q.toString());*/
        
        ResultSet remoteResults = null;

        QueryExecution qexec = QueryExecutionFactory.create(q, model);
        try {
            remoteResults = qexec.execSelect();

        } finally {
          //  qexec.close();
        }
        return remoteResults;
    }

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

        return new QueryIterDistinct(concatIterator,getExecContext());
    }
}



/*
 * (c) Copyright 2004, 2005, 2006 Hewlett-Packard Development Company, LP All
 * rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. The name of the author may not
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


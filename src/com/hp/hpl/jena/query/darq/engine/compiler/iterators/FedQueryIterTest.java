/*
 * (c) Copyright 2004, 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.query.darq.engine.compiler.iterators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engineHTTP.QueryEngineHTTP;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Process a remote Service
 * 
 * @author Bastian Quilitz
 * @version $ID$
 */

public class FedQueryIterTest extends DarqQueryIterator {

    Log log = LogFactory.getLog(FedQueryIterTest.class);

    private static String TESTING_STRING = "_testing_";

  

    public FedQueryIterTest(QueryIterator input, ServiceGroup sg,
            ExecutionContext context, PlanElement subComp) {
        super(input, sg, context,subComp);
    }
    
    
    

    /**
     * Query the remote Service
     */
    protected ResultSet ExecRemoteQuery(Query q) {

        
            return execRemoteQueryTesting(q);

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
          
        }
        return remoteResults;
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


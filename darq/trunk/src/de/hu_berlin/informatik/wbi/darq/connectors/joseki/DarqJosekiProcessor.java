package de.hu_berlin.informatik.wbi.darq.connectors.joseki;


import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joseki.DatasetDesc;
import org.joseki.JosekiServerException;
import org.joseki.QueryExecutionException;
import org.joseki.Request;
import org.joseki.Response;
import org.joseki.ReturnCodes;
import org.joseki.module.Loadable;
import org.joseki.processors.QueryCom;
import org.joseki.processors.QueryExecutionClose;


import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.query.core.DataSourceImpl;
import com.hp.hpl.jena.query.darq.config.Configuration;
import com.hp.hpl.jena.query.darq.core.DarqDataset;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngine;
import com.hp.hpl.jena.query.darq.engine.FedQueryEngineFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.util.FileManager;

public class DarqJosekiProcessor extends QueryCom implements Loadable {

    Log log = LogFactory.getLog(DarqJosekiProcessor.class);
    

    
    Configuration config = null;

  //  @Override
    public void execQuery(Request request, Response response,
            DatasetDesc datasetDesc) throws QueryExecutionException {
        // TODO Auto-generated method stub

        try {

            String queryString = request.getParam("query");

            if (queryString == null) {
                log.debug("No query argument");
                throw new QueryExecutionException(
                        ReturnCodes.rcQueryExecutionFailure, "No query string");
            }

            if (queryString.equals("")) {
                log.debug("Empty query string");
                throw new QueryExecutionException(
                        ReturnCodes.rcQueryExecutionFailure,
                        "Empty query string");
            }

            // ---- Query
            log.info("Query: " + queryString);

            Query query = null;
            try {

                query = QueryFactory.create(queryString, Syntax.syntaxARQ);
            } catch (QueryException ex) {
                String tmp = queryString + "\n\r" + ex.getMessage();
                throw new QueryExecutionException(
                        ReturnCodes.rcQueryParseFailure, "Parse error: \n"
                                + tmp);
            } catch (Throwable thrown) {
                log.info("Query unknown error during parsing: " + queryString,
                        thrown);
                throw new QueryExecutionException(
                        ReturnCodes.rcQueryParseFailure, "Unknown Parse error");
            }

            
            //TODO prüfen, ob Aufruf richtig, erstmal nur Aufruf angepasst
            FedQueryEngineFactory.register(config,null,0,null,false);
            
            QueryExecution qexec = QueryExecutionFactory.create(query, new DarqDataset());
            
      /*      DataSourceImpl ds = new DataSourceImpl();
            ds.setDefaultModel(ModelFactory.createDefaultModel());
            
            //set dummy dataset
            qexec.setDataset(ds);*/

            response.setCallback(new QueryExecutionClose(qexec));

            if (query.isSelectType()) {
                response.setResultSet(qexec.execSelect());
                log.info("OK/select");
                return;
            }

            if (query.isConstructType()) {
                Model model = qexec.execConstruct();
                response.setModel(model);
                log.info("OK/construct");
                return;
            }

            if (query.isDescribeType()) {
                Model model = qexec.execDescribe();
                response.setModel(model);
                log.info("OK/describe");
                return;
            }

            if (query.isAskType()) {
                boolean b = qexec.execAsk();
                response.setBoolean(b);
                log.info("OK/ask");
                return;
            }

            log.warn("Unknown query type - " + queryString);
        } catch (QueryException qEx) {
            log.info("Query execution error: " + qEx);
            QueryExecutionException qExEx = new QueryExecutionException(
                    ReturnCodes.rcQueryExecutionFailure, qEx.getMessage());
            throw qExEx;
        } catch (NotFoundException ex) {
            // Trouble loading data
            log.info(ex.getMessage());
            QueryExecutionException qExEx = new QueryExecutionException(
                    ReturnCodes.rcResourceNotFound, ex.getMessage());
            throw qExEx;
        } catch (JenaException ex) { // Parse exceptions
            log.info("JenaException: " + ex.getMessage());
            QueryExecutionException qExEx = new QueryExecutionException(
                    ReturnCodes.rcArgumentUnreadable, ex.getMessage());
            throw qExEx;
        } catch (RuntimeException ex) { // Parse exceptions
            log.info("Exception: " + ex.getMessage());
            QueryExecutionException qExEx = new QueryExecutionException(
                    ReturnCodes.rcInternalError, ex.getMessage());
            throw qExEx;
        }

    }

    private Configuration getConfigFileName(Model config2) {
        // TODO Auto-generated method stub
        return null;
    }

    public void init(Resource service, Resource implementation) {

        Statement s = service.getProperty(ResourceFactory.createProperty("http://darq.sf.net/darq#darqConfigFile"));
        
        if (s == null) throw new JosekiServerException("DARQ config not specified.");
        
        String filename = s.getObject().toString();
        InputStream in = FileManager.get().open(filename);
        if (in == null) {
            throw new JosekiServerException("Error loading DARQ. File: " + filename
                    + " not found");
        }

        config = new Configuration(ModelFactory.createDefaultModel().read(in, "", "N3"));
       
        
        log.info("DARQ Joseki processor loaded.");
        
        

    }

}

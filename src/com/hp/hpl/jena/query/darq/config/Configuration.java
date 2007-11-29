/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.query.darq.config;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.darq.core.Capability;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.RequiredBinding;
import com.hp.hpl.jena.query.darq.engine.compiler.iterators.FedQueryIterServiceFactory;
import com.hp.hpl.jena.query.darq.engine.compiler.iterators.IDarqQueryIteratorFactory;
import com.hp.hpl.jena.query.darq.engine.optimizer.CostBasedBasicOptimizer;
import com.hp.hpl.jena.query.darq.engine.optimizer.BasicOptimizer;
import com.hp.hpl.jena.query.darq.engine.optimizer.DynProgPlanGenerator;
import com.hp.hpl.jena.query.darq.engine.optimizer.OptimizedPlanGenerator;
import com.hp.hpl.jena.query.darq.engine.optimizer.SelectivityFunction;
import com.hp.hpl.jena.query.darq.mapping.rewriting.TripleRewriter;
import com.hp.hpl.jena.query.darq.util.ClassLoadingUtils;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class Configuration {

    
    static Log log = LogFactory.getLog(Configuration.class);

    private Model model;

    ServiceRegistry serviceRegistry = null;

    private Resource objectBindingCompare;
    private Resource subjectBindingCompare;
    
    private OptimizedPlanGenerator planOptimizer = new DynProgPlanGenerator();

    private IDarqQueryIteratorFactory darqQueryIteratorFactory = new FedQueryIterServiceFactory();
    
    public Configuration(String configFile) {

        this(FileManager.get().loadModel(configFile));
       

    }
    
    public Configuration(Model m) {
        model = m;
        serviceRegistry = new ServiceRegistry();

        objectBindingCompare = model.createProperty(model.getNsPrefixURI("sd"),
                "objectBinding");
        subjectBindingCompare = model.createProperty(model.getNsPrefixURI("sd"),
        "subjectBinding");

        findServices();
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    private void findServices() {

        String q[] = new String[] {
                "SELECT *",
                "{",
                "  ?service  a sd:Service ;",
                "            sd:url   ?url ;",
                "            sd:totalTriples   ?triples ;",
                "            OPTIONAL { ?service rdfs:comment ?description } .",
                "            OPTIONAL { ?service rdfs:label ?label } .",
                "            OPTIONAL { ?service sd:isDefinitive ?definitive } .",
                "            OPTIONAL { ?service sd:selectivityFunction ?sel ."
                        + "                   ?sel sd:javaClass ?selclass .} ", 
                "}" };

        try {

            Query query = makeQuery(q);
            QueryExecution qexec = QueryExecutionFactory.create(query, model);
            
            for (ResultSet rs = qexec.execSelect(); rs.hasNext();) {
                QuerySolution qs = rs.nextSolution();

                // Resource serviceRes = qs.getResource("service");

                String url;
                RDFNode urlNode = qs.getResource("url");
                if (urlNode != null) {
                    url = urlNode.toString();
                        
                } else {
                    throw new Exception("Error: Services must have an URL!");
                }

                int tripleCount = 0;
                RDFNode triplesNode = qs.getLiteral("triples");
                if (triplesNode != null && triplesNode.isLiteral()) {
                    tripleCount = ((Literal) triplesNode).getInt();
                } else {
                    throw new Exception("Error: totalTriples must be provided!");
                }

                String label = null;
                RDFNode labelNode = qs.get("label");
                if (labelNode != null && labelNode.isLiteral()) {
                    label = ((Literal) labelNode).getString();
                }

                String description = null;
                RDFNode descrNode = qs.get("description");
                if (descrNode != null && descrNode.isLiteral()) {
                    description = ((Literal) descrNode).getString();
                }

                boolean isDefinitive = false;
                RDFNode definitiveNode = qs.get("definitive");
                if (definitiveNode != null && definitiveNode.isLiteral()) {
                    String definitive = ((Literal) definitiveNode).getString();
                    isDefinitive = definitive.equalsIgnoreCase("true")
                            || definitive.equalsIgnoreCase("yes");
                }

                RemoteService s = new RemoteService(url, label, description,
                        isDefinitive);
                s.setTripleCount(tripleCount);

                String selclass = null;
                RDFNode selclassNode = qs.get("selclass");
                if (selclassNode != null && selclassNode.isLiteral()) {
                    selclass = ((Literal) selclassNode).getString();
                    SelectivityFunction sel = new ClassLoadingUtils<SelectivityFunction>()
                            .loadClass(selclass);
                    sel.init(model);
                    s.setSelectivityFunction(sel);
                }

                findCapability(s);
                findPreferedFilter(s); // TODO REMOVE?
                findRequiredBindings(s);

                serviceRegistry.add(s);
                log.debug("Service " + url + "added.");

            }
        } catch (Exception e) {
            System.err.println(e); // TODO don't catch exception here. 
        }
        
        if (serviceRegistry.getAvailableServices().size()==0) log.warn("No Services loaded from config file!");
    }

    private void findCapability(RemoteService s) {

        String q[] = new String[] { "SELECT *", "{",
                "?service sd:url <" + s.getUrl() + "> .",
                "?service sd:capability ?capability .",
                "?capability sd:predicate ?p .",
                "?capability sd:triples ?count",
                "OPTIONAL {?capability sd:sofilter ?o .} ",
                "OPTIONAL {?capability sd:objectSelectivity ?osel .} ",
                "OPTIONAL {?capability sd:subjectSelectivity ?ssel .} ",
                "OPTIONAL {?capability sd:triplerewriter ?rewriter.",
                " ?rewriter sd:rewriterClass ?rewriterclass .} ", "}" };

        try {

            Query query = makeQuery(q);

            log.debug(query);

            QueryExecution qexec = QueryExecutionFactory.create(query, model);
            int capabilitycount=0;
            for (ResultSet rs = qexec.execSelect(); rs.hasNext();) {
                QuerySolution qs = rs.nextSolution();

                String predicate;
                Resource predicateRes = qs.getResource("p");
                if (predicateRes != null) {
                    predicate = predicateRes.getURI();
                } else {
                    throw new Exception("Capability must include a predicate!");
                }

                
                String objectFilter = null;
                RDFNode objectNode = qs.get("o");
                if (objectNode != null && objectNode.isLiteral()) {
                    objectFilter = ((Literal) objectNode).getString();
                }
                
                int count = 0;
                objectNode = qs.get("count");
                if (objectNode != null && objectNode.isLiteral()) {
                    try {
                        count  = ((Literal) objectNode).getInt();
                    } catch (Exception e) {
                        throw new Exception("Could not read triples. (No number?)");
                    }
                } else {
                    throw new Exception("Capability must include the number of triples!");
                }
                

                Double osel = null;
                objectNode = qs.get("osel");
                if (objectNode != null && objectNode.isLiteral()) {
                    try {
                        osel = ((Literal) objectNode).getDouble();
                    } catch (Exception e) {
                        log.warn("could not read objectselectivity. (No double?)");

                    }
                }
                
                Double ssel = null;
                objectNode = qs.get("ssel");
                if (objectNode != null && objectNode.isLiteral()) {
                    try {
                        ssel = ((Literal) objectNode).getDouble();
                    } catch (Exception e) {
                        log.warn("could not read subjectselectivity. (No double?)");

                    }
                }
                

                s.addCapability(new Capability(predicate, objectFilter, ssel,osel, count));
                capabilitycount++;
                
                String rewriterclass = null;
                objectNode = qs.get("rewriterclass");
                if (objectNode != null && objectNode.isLiteral()) {
                    rewriterclass = ((Literal) objectNode).getString();
                }

                if (rewriterclass != null) {
                    TripleRewriter rewriter = new ClassLoadingUtils<TripleRewriter>()
                            .loadClass(rewriterclass);
                    if (rewriter != null)
                        s.addTripleRewriter(predicate, rewriter);
                }

              
                }
            if (capabilitycount==0) { 
                log.warn("No capabilities found for service with url: "+s.getUrl());
            }
        } catch (Exception e) {
            log.error("Exception in findCapability(): " + e);
        }

    }

    private void findPreferedFilter(RemoteService s) {

        String q[] = new String[] { "SELECT *", "{",
                "?service sd:url \"" + s.getUrl() + "\" .",
                "?service sd:preferedFilter ?predicate .", "}" };

        try {

            Query query = makeQuery(q);

            log.debug(query);

            QueryExecution qexec = QueryExecutionFactory.create(query, model);

            for (ResultSet rs = qexec.execSelect(); rs.hasNext();) {
                QuerySolution qs = rs.nextSolution();

                String predicate;
                Resource predicateRes = qs.getResource("predicate");
                if (predicateRes != null) {
                    predicate = predicateRes.getURI();
                } else {
                    throw new Exception("Filter must be a URI!");
                }

                s.addPreferedFilter(predicate);

            }
        } catch (Exception e) {
            log.error("Exception in findPreferedFilter(): " + e);
        }

    }

    private void findRequiredBindings(RemoteService s) {

        String q[] = new String[] { "SELECT *", "{",
                "?service sd:url \"" + s.getUrl() + "\" .",
                "?service sd:requiredBindings ?bindinglist .",
                "?bindinglist ?p ?predicate .", "}" };

        
        
        try {

            Query query = makeQuery(q);

            log.debug(query);

            QueryExecution qexec = QueryExecutionFactory.create(query, model);

            Set<RequiredBinding> rl = new HashSet<RequiredBinding>();
            Resource bindinglist = null;

            for (ResultSet rs = qexec.execSelect(); rs.hasNext();) {
                QuerySolution qs = rs.nextSolution();

                Resource listRessource = qs.getResource("bindinglist");
                if (bindinglist == null) {
                    bindinglist = listRessource;
                }
                if (!bindinglist.equals(listRessource)) {
                    s.addRequiredBinding(rl);
                    rl = new HashSet<RequiredBinding>();
                    bindinglist = listRessource;
                }

                Node predicate;
                Resource predicateRes = qs.getResource("predicate");
                if (predicateRes != null) {
                    predicate = predicateRes.asNode();
                } else {
                    throw new Exception("must be an URI!");
                }

                Resource nodeRes = qs.getResource("p");
                if (predicateRes != null) {
                    if (nodeRes.equals(objectBindingCompare)) {
                        rl.add(new RequiredBinding(predicate,
                                RequiredBinding.OBJECT_BINDING));
                    } else if (nodeRes.equals(subjectBindingCompare)) {
                        rl.add(new RequiredBinding(predicate,
                                RequiredBinding.SUBJECT_BINDING));
                    }
                } else {
                    throw new Exception("must be a URI!");
                }

            }
            if (rl.size() > 0) {
                s.addRequiredBinding(rl);
            }
        } catch (Exception e) {
            log.error("Exception in findPreferesBound(): " + e);
        }
        // TODO close qexec

    }

    private static void stdNS(StringBuffer sBuff, String prefix,
            String namespace) {
        sBuff.append("PREFIX ");
        sBuff.append(prefix);
        sBuff.append(":");
        sBuff.append("  ");
        sBuff.append("<");
        sBuff.append(namespace);
        sBuff.append(">");
        sBuff.append("\n");
    }

    private void stdHeaders(StringBuffer sBuff) {
        Map prefixmap = model.getNsPrefixMap();

        for (Iterator it = prefixmap.keySet().iterator(); it.hasNext();) {
            String prefix = (String) it.next();
            String namespace = (String) prefixmap.get(prefix);
            // model.getNS
            stdNS(sBuff, prefix, namespace);
        }
        
        if (!prefixmap.containsKey("sd")) stdNS(sBuff,"sd","http://darq.sf.net/dose/0.1#");
        if (!prefixmap.containsKey("rdf")) stdNS(sBuff,"rdf",RDF.getURI());
        if (!prefixmap.containsKey("rdfs")) stdNS(sBuff,"rdfs",RDFS.getURI());

        

    }

    private Query makeQuery(String[] a) throws ConfigurationException {
        StringBuffer sBuff = new StringBuffer();

        stdHeaders(sBuff);
        sBuff.append("\n");

        for (int i = 0; i < a.length; i++) {
            if (i != 0)
                sBuff.append("\n");
            sBuff.append(a[i]);
        }

        String qs = sBuff.toString();
        return makeQuery(qs);
    }

    private Query makeQuery(String qs) throws ConfigurationException {
        try {
            Query query = QueryFactory.create(qs);
            return query;
        } catch (QueryParseException ex) {
            System.out.println(qs);
            log.fatal("INTERNAL ERROR: " + ex.getMessage());
            throw new ConfigurationException("Internal Error");
        }
    }

    /**
     * @return Returns the planOptimizer.
     */
    public OptimizedPlanGenerator getPlanOptimizer() {
        return planOptimizer;
    }

    /**
     * @param planOptimizer The planOptimizer to set.
     */
    public void setPlanOptimizer(OptimizedPlanGenerator planOptimizer) {
        this.planOptimizer = planOptimizer;
    }

    public Model getModel() {
        Model retModel= ModelFactory.createDefaultModel();
        retModel.add(model);
        return retModel;
        
    }

    /**
     * @return Returns the darqQueryIteratorClass.
     */
    public IDarqQueryIteratorFactory getDarqQueryIteratorFactory() {
        return darqQueryIteratorFactory;
    }

    /**
     * @param darqQueryIteratorFactory The darqQueryIteratorClass to set.
     */
    public void setDarqQueryIteratorClass(IDarqQueryIteratorFactory darqQueryIteratorFactory) {
        this.darqQueryIteratorFactory = darqQueryIteratorFactory;
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
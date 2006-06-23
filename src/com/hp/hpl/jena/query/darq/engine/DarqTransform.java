/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.core.ARQInternalErrorException;
import com.hp.hpl.jena.query.darq.config.Configuration;
import com.hp.hpl.jena.query.darq.config.ServiceRegistry;
import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanMultipleService;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanService;
import com.hp.hpl.jena.query.darq.engine.optimizer.PlanUnfeasibleException;
import com.hp.hpl.jena.query.darq.util.OutputUtils;
import com.hp.hpl.jena.query.engine1.Plan;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.plan.PlanBasicGraphPattern;
import com.hp.hpl.jena.query.engine1.plan.PlanFilter;
import com.hp.hpl.jena.query.engine1.plan.PlanGroup;
import com.hp.hpl.jena.query.engine1.plan.PlanTriplePattern;
import com.hp.hpl.jena.query.engine1.plan.TransformBase;

public class DarqTransform extends TransformBase {

    Log log = LogFactory.getLog(FedQueryCompilerVisitor.class);
    
    protected Plan plan = null ;
    
    private ServiceRegistry registry = null;
    
    private Configuration config = null; 
    
    // The return stack
    private Stack<PlanElement> retStack = new Stack<PlanElement>() ;
    
    HashMap<RemoteService,ServiceGroup> groupedTriples =  new HashMap<RemoteService,ServiceGroup>();
    HashMap<Triple,MultipleServiceGroup> queryIndividuallyTriples =  new HashMap<Triple,MultipleServiceGroup>();
    
    Set varsMentioned = new  HashSet();
    
    
    /* (non-Javadoc)
     * @see com.hp.hpl.jena.query.engine1.plan.TransformBase#transform(com.hp.hpl.jena.query.engine1.plan.PlanBasicGraphPattern, java.util.List)
     */
    @Override
    public PlanElement transform(PlanBasicGraphPattern planElt, List newElts) {
        
        groupedTriples.clear(); // new for each Group ! 
        queryIndividuallyTriples.clear(); // "
        
        List <PlanFilter> filters = new ArrayList<PlanFilter>();
        
        List<PlanElement> acc = new ArrayList<PlanElement>() ;
        
        
     
        for ( PlanElement ex2: (List<PlanElement>)newElts )
        {

            if (ex2 instanceof PlanTriplePattern) {
                PlanTriplePattern ptp = (PlanTriplePattern)ex2;
                
                List<RemoteService> services = selectServices(registry.getMatchingServices(ptp.getTriple()));
                if (services.size()==1) {
                    putIntoGroupedTriples(services.get(0),ptp.getTriple());
                }
                else if (services.size()>1) {
                    /* if there are more than one service, the triple has to be passed to the services individually.
                     * This is because ... TODO
                     */
                    for (int j=0; j< services.size();j++) {
                        putIntoQueryIndividuallyTriples(ptp.getTriple(),services.get(j));
                    }

                } else { 
                    acc.add(0,ex2);
                    
                    log.warn("No service found for statement: " + ptp.getTriple()+" - it will be queried locally.");
                    
                }
                
            } else if (ex2 instanceof PlanFilter) {
                filters.add((PlanFilter)ex2);
            } else acc.add(0,ex2);

        }
        
        // add filters to servcie groups and to plan (filters are also applied locally because we don't trust the remote services)
        for (PlanFilter f:filters) {
            acc.add(f);
            for (ServiceGroup sg:groupedTriples.values()) {
                sg.addFilter(f.getConstraint());
            }
            for (ServiceGroup sg:queryIndividuallyTriples.values()) {
                sg.addFilter(f.getConstraint());
            } 
        }

        if ( groupedTriples.size() > 0 || queryIndividuallyTriples.size() >0 ) { 
            
            ArrayList<ServiceGroup> al = new ArrayList<ServiceGroup>(groupedTriples.values());
            al.addAll(queryIndividuallyTriples.values());

            
            List<ServiceGroup> cheapestPlan;
            try {
                cheapestPlan = config.getPlanOptimizer().getCheapestPlan(al);
            } catch (PlanUnfeasibleException e) {
                throw new ARQInternalErrorException("No feasible plan: " + e); 
            } 
            
            
            log.debug("selected: \n" + OutputUtils.serviceGroupListToString(cheapestPlan));
            
            int pos = 0;
            for (ServiceGroup sg:cheapestPlan ) {
                
                if (sg instanceof MultipleServiceGroup) {
                    acc.add(pos,FedPlanMultipleService.make(plan,(MultipleServiceGroup)sg,null));
                } else acc.add(pos,FedPlanService.make(plan,sg,null));
                pos++;
                
            }
        }
        
        PlanElement ex = PlanGroup.make(planElt.getContext(),(List)acc,false) ;
        return ex;
    }

    /* (non-Javadoc)
     * @see com.hp.hpl.jena.query.engine1.plan.TransformBase#transform(com.hp.hpl.jena.query.engine1.plan.PlanGroup, java.util.List)
     */
    @Override
    public PlanElement transform(PlanGroup planElt, List newElts) {
        // TODO Auto-generated method stub
        return super.transform(planElt, newElts);
    }

    private List<RemoteService> selectServices(List<RemoteService> services){
        ArrayList<RemoteService> result = new ArrayList<RemoteService>();
        for (Iterator<RemoteService> it=services.iterator(); it.hasNext() ;) {
           RemoteService rs=it.next();
           
           // if there is a Definitive Service only use this one
           if (rs.isDefinitive()) {
                   result.clear();
                   result.add(rs);
                   break;
           }
           
           result.add(rs);
               
        }
        return result;
    }
    
    
   private void putIntoGroupedTriples(RemoteService s, Triple t) {
       ServiceGroup tg = groupedTriples.get(s);
       if (tg==null) {
           tg = new ServiceGroup(s);
           groupedTriples.put(s,tg);
       } 
       
       tg.addB(t);
   }
   
   
   
   private void putIntoQueryIndividuallyTriples(Triple t, RemoteService s) {
       MultipleServiceGroup msg = queryIndividuallyTriples.get(t);
       if (msg==null) {
           msg = new MultipleServiceGroup();
           queryIndividuallyTriples.put(t,msg);
       }
       msg.addService(s);
        
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
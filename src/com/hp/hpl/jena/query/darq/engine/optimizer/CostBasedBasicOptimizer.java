/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.engine.optimizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.core.Var;
import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.MultiplyMultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.RequiredBinding;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.core.StringConcatMultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.UnionServiceGroup;
import com.hp.hpl.jena.query.expr.Expr;


/**
 * Basic cost-based Optimizer. !! complexity of n! (n= #servicegroups) !!
 * 
 * @author Bastian Quilitz
 * @version $ID$
 * 
 */
public class CostBasedBasicOptimizer implements BasicOptimizer {

   // static Log log = LogFactory.getLog(CostBasedBasicOptimizer.class);
    private int count = 0;

    private double lastCosts = PLAN_UNFEASIBLE_RESULTS;

    /**
     * returns the cheapest plan (execution order) for a list of service groups
     */
    public List<ServiceGroup> getCheapestPlan(List<ServiceGroup> servicegroups) throws PlanUnfeasibleException{

        count=0;
        
        List<ServiceGroup> sgs = new ArrayList<ServiceGroup>(servicegroups);

        List<OptimizerElement<ServiceGroup>> plan = new ArrayList<OptimizerElement<ServiceGroup>>();
        double costs = 1;

        Set<String> bound = new HashSet<String>();

        while (!sgs.isEmpty()) {

            OptimizerElement<ServiceGroup> cheapest = new OptimizerElement<ServiceGroup>(null, PLAN_UNFEASIBLE_RESULTS);

            for (ServiceGroup sg : sgs ) {

                count++;

                OptimizerElement<ServiceGroup> rsg = null;

                if (sg instanceof UnionServiceGroup){

                	UnionServiceGroup usg = (UnionServiceGroup)sg;
                	boolean b = true;
                	boolean requiredBindingMSG=true;
                	boolean requiredBindingMuMSG = true;
                	boolean requiredBindingScMSG = true;
                	boolean requiredBindingSG=true;
                	for (ServiceGroup serviceGroup: usg.getServiceGroups().values() ){

                		if (serviceGroup instanceof MultipleServiceGroup) {
                			requiredBindingMSG=true;
                			for (RemoteService s:((MultipleServiceGroup)serviceGroup).getServices()){
                				if (!checkInput(serviceGroup.getTriples(), bound, s)) requiredBindingMSG=false; 
                				//looks if RequiredBindings from Service with Triple fit, if not get next service
                			}                				
                			if (!requiredBindingMSG) continue; //if RB does not fit, get next MSG
                		}
                		
                		else if (serviceGroup instanceof MultiplyMultipleServiceGroup) {
                			requiredBindingMuMSG=true;
                			for (RemoteService s:((MultiplyMultipleServiceGroup)serviceGroup).getServices()){
                				if (!checkInput(serviceGroup.getTriples(), bound, s)) requiredBindingMuMSG=false; 
                			}                				
                			if (!requiredBindingMuMSG) continue; 
                		}
                		
                		else if (serviceGroup instanceof StringConcatMultipleServiceGroup) {
                			requiredBindingScMSG=true;
                			for (RemoteService s:((StringConcatMultipleServiceGroup)serviceGroup).getServices()){
                				if (!checkInput(serviceGroup.getTriples(), bound, s)) requiredBindingScMSG=false; 
                			}                				
                			if (!requiredBindingMuMSG) continue; 
                		}
                		
                		else {/* SG, MuSG, ScSG */
                			requiredBindingSG=true;
                			if (!checkInput(serviceGroup.getTriples(), bound, serviceGroup.getService())) requiredBindingSG = false;
                			if (!requiredBindingSG) continue;
                		}
                	}
                	b = requiredBindingMSG && requiredBindingSG && requiredBindingMuMSG && requiredBindingScMSG;
                	// all RB have to fit, if true build plan
                	if (!b) continue;
                	rsg = getCheapestPlanForUnionServiceGroup((UnionServiceGroup)sg, bound);
                }
                else if (sg instanceof MultipleServiceGroup) {
                    
                    boolean b=true;
                    for (RemoteService s:((MultipleServiceGroup)sg).getServices()) 
                        if (!checkInput(sg.getTriples(), bound, s)) b=false;
                    if (!b) continue;
                    
                    rsg = getCheapestPlanForMultipleServiceGroup((MultipleServiceGroup) sg, bound);
                    
                } else {
                    
                    if (!checkInput(sg.getTriples(), bound, sg.getService()))
                        continue;
                    
                    rsg = getCheapestPlanForServiceGroup(sg, bound); // get the best order for the triples in the group.
                }

                if (rsg.getRankvalue() < cheapest.getRankvalue()) {
                    cheapest = rsg;
                }
            }

          if (cheapest.getRankvalue() == PLAN_UNFEASIBLE_RESULTS) throw new PlanUnfeasibleException();

            // costs*= cheapest.getRankvalue();
            for (OptimizerElement<ServiceGroup> e : plan) {
                for (String var : e.getElement().getUsedVariables()) {
                    if (cheapest.getElement().getUsedVariables().contains(var)) {
                        cheapest.addDependency(e);
                        // costs*= e.getRankvalue();
                    }

                }
            }

            plan.add(cheapest);

            // costs*=cheapest.getRankvalue();
            sgs.remove(cheapest.getOrgElement());
            bound.addAll(cheapest.getElement().getUsedVariables());

        }

        List<ServiceGroup> result = new ArrayList<ServiceGroup>();

    for (OptimizerElement<ServiceGroup> e : plan) {
            result.add(e.getElement());
        }

        if (plan.size() > 0) costs = plan.get(plan.size() - 1).calcCosts();
        else costs = plan.get(0).calcCosts();
        
        lastCosts = costs;

        System.err.println("count="+count+"/"+servicegroups.size());
        //System.err.println("Costs: " + costs);
        return result;
        

    }

    public static OptimizerElement<ServiceGroup> getCheapestPlanForUnionServiceGroup(UnionServiceGroup usg, Set<String> bound) throws PlanUnfeasibleException{
        double costs = 0;

        OptimizerElement<ServiceGroup> rsg = null;
        
        MultipleServiceGroup msg;
        
        for (ServiceGroup serviceGroup: usg.getServiceGroups().values()){
        	
        	if (serviceGroup instanceof MultipleServiceGroup){
        		msg = (MultipleServiceGroup)serviceGroup;
        		for (RemoteService service : msg.getServices()) {
                    rsg = getCheapestPlanForServiceGroup(msg.getServiceGroup(service), bound); //holt sich den günstigens Plan für Service + boundVariables
                    costs += rsg.getRankvalue();
                }		
        	}
        	
        	else if (serviceGroup instanceof MultiplyMultipleServiceGroup) {
				MultiplyMultipleServiceGroup muMSG = (MultiplyMultipleServiceGroup) serviceGroup;
				for (RemoteService service : muMSG.getServices()) {
                    rsg = getCheapestPlanForServiceGroup(muMSG.getServiceGroup(service), bound); //holt sich den günstigens Plan für Service + boundVariables
                    costs += rsg.getRankvalue();
                }
			}
        	
        	else if (serviceGroup instanceof StringConcatMultipleServiceGroup) {
        		StringConcatMultipleServiceGroup scMSG = (StringConcatMultipleServiceGroup) serviceGroup;
				for (RemoteService service : scMSG.getServices()) {
                    rsg = getCheapestPlanForServiceGroup(scMSG.getServiceGroup(service), bound); //holt sich den günstigens Plan für Service + boundVariables
                    costs += rsg.getRankvalue();
                }
			}
        
        	else{//instanceof ServiceGroup or MultiplyServiceGroup
        		rsg = getCheapestPlanForServiceGroup(serviceGroup, bound); //holt sich den günstigens Plan für Service + boundVariables
        		costs += rsg.getRankvalue();
        	}
        }
        
        UnionServiceGroup resultsg = usg.clone();
        resultsg.setTriples(rsg.getElement().getTriples());
        OptimizerElement<ServiceGroup> result = new OptimizerElement<ServiceGroup>(resultsg, costs,usg); // TODO return optimized triples!!
        return result;
    } 
    
 
    public static OptimizerElement<ServiceGroup> getCheapestPlanForMultipleServiceGroup(MultipleServiceGroup sg, Set<String> bound) throws PlanUnfeasibleException{
        double costs = 0;

        OptimizerElement<ServiceGroup> rsg = null;

        for (RemoteService s : sg.getServices()) {
            rsg = getCheapestPlanForServiceGroup(sg.getServiceGroup(s), bound);
            costs += rsg.getRankvalue();
        } 
        MultipleServiceGroup resultsg = sg.clone();
        resultsg.setTriples(rsg.getElement().getTriples());

        OptimizerElement<ServiceGroup> result = new OptimizerElement<ServiceGroup>(resultsg, costs,sg); // TODO return optimized triples!!

        return result;
    }

    /**
     * 
     * @param sg
     * @param bound
     * @return
     */
    @SuppressWarnings("unchecked")
	public static OptimizerElement<ServiceGroup> getCheapestPlanForServiceGroup(ServiceGroup sg, Set<String> bound) throws PlanUnfeasibleException{

        if (sg instanceof MultipleServiceGroup) throw new PlanUnfeasibleException("wrong parameter for ServiceGroup!");
        if (sg instanceof UnionServiceGroup) throw new PlanUnfeasibleException("wrong parameter for ServiceGroup!");
        if (sg instanceof MultiplyMultipleServiceGroup) throw new PlanUnfeasibleException("wrong parameter for ServiceGroup!");
        if (sg instanceof StringConcatMultipleServiceGroup) throw new PlanUnfeasibleException("wrong parameter for ServiceGroup!"); 
        List<Triple> triples = sg.getTriples();

        Map<String, ArrayList<Triple>> tripleGroups = new HashMap<String, ArrayList<Triple>>();

        // group by subject
        for (Triple t : triples) {
            Node subject = t.getSubject();
            String groupName = "";

            if (!subject.isConcrete()) {
                groupName = subject.getName();
            }

            ArrayList<Triple> group = tripleGroups.get(groupName);
            if (group == null) {
                group = new ArrayList<Triple>();
            }
            group.add(t);
            tripleGroups.put(groupName, group);

        }

        Set<String> bv = new HashSet<String>(bound);
        
        //  FIXME this is very imprecise. we consider all variables mentioned in filters to be bound. independent of their actual form, e.g =, >, or <
        for (Expr f : sg.getFilters()) {
        	for (Var v:(Set<Var>)f.getVarsMentioned()) {
        	bv.add(v.toString().substring(1));}
       }

        List<OptimizerElement<List<Triple>>> plan = new ArrayList<OptimizerElement<List<Triple>>>();
        double costs = 1;

        while (!tripleGroups.isEmpty()) {

            OptimizerElement<List<Triple>> cheapest = new OptimizerElement<List<Triple>>(null, PLAN_UNFEASIBLE_RESULTS);
            String cheapestGroupName = null;

            for (String groupName : tripleGroups.keySet()) {
                OptimizerElement<List<Triple>> rtr = getCheapestPlanForTripleGroup(tripleGroups.get(groupName), bv, sg.getService()); // ------------
                if (rtr.getRankvalue() < cheapest.getRankvalue()) {
                    cheapest = rtr;
                    cheapestGroupName = groupName;
                }

            }

            if (cheapest.getRankvalue() == PLAN_UNFEASIBLE_RESULTS) throw new PlanUnfeasibleException();
               // return new OptimizerElement<ServiceGroup>(null, PLAN_UNFEASIBLE_RESULTS);

            // costs*= cheapest.getRankvalue();
            Set<String> cheapestUsedVariables = getUsedVariables(cheapest.getElement());
            for (OptimizerElement<List<Triple>> e : plan) {
                for (String var : getUsedVariables(e.getElement())) {
                    if (cheapestUsedVariables.contains(var))
                        cheapest.addDependency(e);
                    // costs*= e.getRankvalue();

                }
            }
   //         log.debug("getCheapestPlanForServiceGroup costs " + costs);

            // costs*=cheapest.getRankvalue();
            plan.add(cheapest);
            tripleGroups.remove(cheapestGroupName);

            bv.addAll(getUsedVariables(cheapest.getElement()));

        }

        // clone service group and replace triples
        ServiceGroup nsg = sg.clone();
        List<Triple> result = new ArrayList<Triple>();
        for (OptimizerElement<List<Triple>> e : plan) {
            result.addAll(e.getElement());
        }
        nsg.setTriples(result);

        if (plan.size() > 0) costs = plan.get(plan.size() - 1).calcCosts();
        else costs = plan.get(0).calcCosts();
        

        return new OptimizerElement<ServiceGroup>(nsg, costs, sg);
    }

    /**
     * 
     * @param triples
     * @param bound
     * @param service
     * @return
     */
    public static OptimizerElement<List<Triple>> getCheapestPlanForTripleGroup(List<Triple> triples, Set<String> bound, RemoteService service) throws PlanUnfeasibleException{

        List<Triple> tripleList = new ArrayList<Triple>(triples);
        Set<String> bv = new HashSet<String>(bound);
        List<OptimizerElement<Triple>> plan = new ArrayList<OptimizerElement<Triple>>();

        // double m= service.getTripleCount();

        String predicate = null;

        List<OptimizerElement<Triple>> minimumList = new ArrayList<OptimizerElement<Triple>>();
        List<OptimizerElement<Triple>> otherList = new ArrayList<OptimizerElement<Triple>>();

        while (!tripleList.isEmpty()) {

            double cheapestCosts = PLAN_UNFEASIBLE_RESULTS;
            Triple cheapestTriple = null;
            boolean cheapestObjectBound = false;

            boolean subjectBound;
            boolean objectBound;
            boolean predicateBound;

            for (Triple t : tripleList) {

                subjectBound = false;
                objectBound = false;
                predicateBound = true; // TODO unbound predicate not supported
                // at the moment

                if (t.getSubject().isConcrete() || bv.contains(t.getSubject().getName())) {
                    subjectBound = true;
                }

                if (t.getObject().isConcrete() || bv.contains(t.getObject().getName())) {
                    objectBound = true;
                }

                double sel = service.getTriples(t);

                if (subjectBound) {
                    if (!objectBound) {
                        Double subjectSelectivity = service.getSubjectSelectivity(t.getPredicate().getURI());
                        if (subjectSelectivity != null) {
                            sel *= subjectSelectivity;
                        } else {
                            sel = 1;
                        }
                    } else
                        sel = 1; // s,p,o bound -> there is only one triple
                } else {
                    if (objectBound) {
                        Double objectSelectivity = service.getObjectSelectivity(t.getPredicate().getURI());
                        if (objectSelectivity != null)
                            sel *= objectSelectivity;
                    }
                }

                if (sel < cheapestCosts) {
                    cheapestCosts = sel;
                    cheapestTriple = t;
                    cheapestObjectBound = objectBound;
                }

            }

            if (cheapestCosts == PLAN_UNFEASIBLE_RESULTS) throw new PlanUnfeasibleException();
                //return new OptimizerElement<List<Triple>>(null, PLAN_UNFEASIBLE_RESULTS);

            tripleList.remove(cheapestTriple);

            bv.addAll(getUsedVariables(cheapestTriple));

           

            OptimizerElement<Triple> el = new OptimizerElement<Triple>(cheapestTriple, cheapestCosts);

            Set<String> cheapestUsedVariables = getUsedVariables(cheapestTriple);
            for (OptimizerElement<Triple> e : plan) {
                for (String var : getUsedVariables(e.getElement())) {
                    if (cheapestUsedVariables.contains(var))
                        el.addDependency(e);
                }
            }

            plan.add(el);

            if (cheapestObjectBound) {
                minimumList.add(el);
            } else
                otherList.add(el);

        }

        List<Triple> result = new ArrayList<Triple>();

        for (OptimizerElement<Triple> e : plan) {
            result.add(e.getElement());
        }

        double costs = 1;

        if (minimumList.size() > 0) {
            double tmp = PLAN_UNFEASIBLE_RESULTS;

            for (OptimizerElement<Triple> el : minimumList) {
                if (el.getRankvalue() < tmp)
                    tmp = el.getRankvalue();
           //     log.debug(el.getElement().toString() + " : " + el.getRankvalue());
            }
            costs = tmp;
        }

     //   log.debug("getCheapestPlanForTripleGroup min=: " + costs);

        for (OptimizerElement<Triple> el : otherList) {
            costs *= el.getRankvalue();
     //       log.debug(el.getElement().toString() + " : " + el.getRankvalue());
        }

     //   log.debug("costs= " + costs);

        return new OptimizerElement<List<Triple>>(result, costs);
    }

    /**
     * Checks the input parameters for a service given a list of triples and a
     * set of bound variables
     * 
     * @param triples
     * @param bound
     * @param service
     * @return
     */
    //FRAGE bound wird hier offensichtlich nicht mehr genutzt?! Es ist ein leeres HashSet
    public static boolean checkInput(List<Triple> triples, Set<String> bound, RemoteService service) {

        Set<String> bv = new HashSet<String>(bound);

        Set<String> predicatesWithBoundObjects = new HashSet<String>();
        Set<String> predicatesWithBoundSubjects = new HashSet<String>();

        if (service.getRequiredBindings().size() == 0)
            return true;

        for (Triple t : triples) {

            Node s = t.getSubject();
            Node o = t.getObject();

            if (s.isConcrete() || (s.isVariable() && bv.contains(s.getName()))) {
                predicatesWithBoundSubjects.add(t.getPredicate().getURI());
            } else {
                // bv.add(s.getName());
            }

            if (o.isConcrete() || (o.isVariable() && bv.contains(o.getName()))) {
                predicatesWithBoundObjects.add(t.getPredicate().getURI());
            } else {
                // bv.add(o.getName());
            }

        }

        for (Set<RequiredBinding> bs : service.getRequiredBindings()) {

            boolean tmpresult = true;

            for (RequiredBinding rb : bs) {

                switch (rb.getType()) {
                case RequiredBinding.OBJECT_BINDING:

                    if (!predicatesWithBoundObjects.contains(rb.getPredicateString()))
                        tmpresult = false;

                    break;

                case RequiredBinding.SUBJECT_BINDING:

                    if (!predicatesWithBoundSubjects.contains(rb.getPredicateString()))
                        tmpresult = false;

                    break;

                default:
                    break;
                }

                if (tmpresult)
                    return true;

            }

        }
        return false;
    }

    private static Set<String> getUsedVariables(List<Triple> l) {
        Set<String> s = new HashSet<String>();
        for (Triple t : l) {
            s.addAll(getUsedVariables(t));
        }
        return s;
    }

    private static Set<String> getUsedVariables(Triple t) {
        Set<String> s = new HashSet<String>();
        if (t.getSubject().isVariable())
            s.add(t.getSubject().getName());
        if (t.getObject().isVariable())
            s.add(t.getObject().getName());
        return s;
    }

    public double getCosts() {
        return lastCosts;
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
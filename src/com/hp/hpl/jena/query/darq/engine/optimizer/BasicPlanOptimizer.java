/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.engine.optimizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.util.OutputUtils;
import com.hp.hpl.jena.query.darq.util.Permute;

public class BasicPlanOptimizer implements PlanOptimizer {
    
    static Log log = LogFactory.getLog(BasicPlanOptimizer.class);
    double lastCosts = PLAN_UNFEASIBLE_RESULTS;

    public List<ServiceGroup> getCheapestPlan(List<ServiceGroup> servicegroups) {
        Permute<ServiceGroup>  p = new Permute<ServiceGroup> (new ArrayList<ServiceGroup>(servicegroups));
        
        List<ServiceGroup> cheapestPlan = null;
        double cheapestcosts = Double.MAX_VALUE;
        
        while (p.hasNext()) {
            List<ServiceGroup> permutation = p.next();
            double c = calcCosts(permutation);
            if (c<PlanOptimizer.PLAN_UNFEASIBLE_RESULTS) {
                log.debug("Costs: "+c+ " for plan: \n" + OutputUtils.serviceGroupListToString(permutation));
                if (c<cheapestcosts) {
                    cheapestPlan = permutation;
                    cheapestcosts = c;
                }         
            } else {
                log.debug("Unfeasible plan: \n" + OutputUtils.serviceGroupListToString(permutation));
            }
        }

            lastCosts = cheapestcosts;
            return cheapestPlan;
    }

    private double calcCosts(List<ServiceGroup> list) {
        
        Set<String> bound = new HashSet<String>();
        double costs = 0;
        
        for (int i=0; i<list.size();i++) {
            costs+= expectedResults(list.get(i),bound);
            if (costs == PlanOptimizer.PLAN_UNFEASIBLE_RESULTS) return costs; // we no not need to continue, plan is unfeasible
            bound.addAll(list.get(i).getUsedVariables());
        }
        
        return costs;
    }
    
    public double calcSelectivity(ServiceGroup sg, Set<String> boundVariables) {
        if (sg.checkInput(boundVariables)) return sg.getService().getSelectivityFunction().calcSelectivity(sg.getTriples(),boundVariables, sg.getService());
        else return PlanOptimizer.PLAN_UNFEASIBLE_SELECTIVITY;
    }
    
    public double expectedResults(ServiceGroup sg,Set<String> boundVariables) {
        double sel = calcSelectivity(sg,boundVariables);
        if (! (sel==PlanOptimizer.PLAN_UNFEASIBLE_SELECTIVITY) ) return sel;
        else return PlanOptimizer.PLAN_UNFEASIBLE_RESULTS;
    }

    public double getCosts() {
        // TODO Auto-generated method stub
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
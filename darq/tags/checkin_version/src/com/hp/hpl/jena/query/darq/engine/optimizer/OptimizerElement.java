/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.engine.optimizer;

import java.util.HashSet;
import java.util.Set;



public class OptimizerElement<E> implements Comparable<OptimizerElement<E>>{

    private E element ;
    
    private E orgElement;
    
    private double rankvalue ;
    
    private Set<OptimizerElement<E>> dependencies = new HashSet<OptimizerElement<E>>();
    
    public OptimizerElement(E sg, double rv) {
        element=sg;
        rankvalue=rv;
        orgElement=null;
    }
    
    public OptimizerElement(E sg, double rv, E org) {
        element=sg;
        rankvalue=rv;
        orgElement = org;
    }

    /**
     * @return Returns the element.
     */
    public E getElement() {
        return element;
    }
    
    public E getOrgElement() {
        return orgElement;
    }

    /**
     * @return Returns the rankvalue.
     */
    public double getRankvalue() {
        return rankvalue;
    }

    public int compareTo(OptimizerElement<E> o) {
        
        return new Double(this.getRankvalue() - o.getRankvalue()).intValue();
        
        
    }

    /**
     * @return Returns the dependencies.
     */
    public Set<OptimizerElement<E>> getDependencies() {
        return dependencies;
    }
    
    /**
     * Adds a Optimizer Element "this" depends from
     * @param e
     */
    public void addDependency(OptimizerElement<E> e) {
        dependencies.add(e);
    }
    
    
    public double calcCosts() {
        double costs =getRankvalue();
        
        for (OptimizerElement<E> o:dependencies){
            costs*=o.calcCosts();
        }

        return costs;
    }
    
    /*protected void setCosts(double costs) {
        this.rankvalue=costs ;
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
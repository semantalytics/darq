/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.core;

import com.hp.hpl.jena.query.darq.mapping.rewriting.TripleRewriter;

public class Capability {
    
    private String predicate ;
    private String objectFilter;
    private Double subjectSelecticity ;
    private Double objectSelectivity ;
    private int triples;
    private TripleRewriter tripleRewriter;
    
    
    public Capability(String predicate, String objectFilter, int triples) {
        this.predicate = predicate;
        this.objectFilter=objectFilter;
        this.triples = triples;
    }


    public Capability(String predicate, String objectFilter, Double objectSelectivity, int triples) {
        this.predicate = predicate;
        this.objectFilter=objectFilter;
        this.objectSelectivity = objectSelectivity;
        this.triples = triples;
    }


    public Capability(String predicate, String objectFilter, Double subjectSelecticity, Double objectSelectivity, int triples) {
        this.predicate = predicate;
        this.objectFilter=objectFilter;
        this.subjectSelecticity = subjectSelecticity;
        this.objectSelectivity = objectSelectivity;
        this.triples = triples;
    }


    /**
     * @return Returns the objectSelectivity.
     */
    public Double getObjectSelectivity() {
        return objectSelectivity;
    }


    /**
     * @param objectSelectivity The objectSelectivity to set.
     */
    public void setObjectSelectivity(Double objectSelectivity) {
        this.objectSelectivity = objectSelectivity;
    }


    /**
     * @return Returns the subjectSelecticity.
     */
    public Double getSubjectSelecticity() {
        return subjectSelecticity;
    }


    /**
     * @param subjectSelecticity The subjectSelecticity to set.
     */
    public void setSubjectSelecticity(Double subjectSelecticity) {
        this.subjectSelecticity = subjectSelecticity;
    }


    /**
     * @return Returns the triples.
     */
    public int getTriples() {
        return triples;
    }


    /**
     * @param triples The triples to set.
     */
    public void setTriples(int triples) {
        this.triples = triples;
    }


    /**
     * @return Returns the predicate.
     */
    public String getPredicate() {
        return predicate;
    }


    /**
     * @return Returns the tripleRewriter.
     */
    public TripleRewriter getTripleRewriter() {
        return tripleRewriter;
    }


    /**
     * @param tripleRewriter The tripleRewriter to set.
     */
    public void setTripleRewriter(TripleRewriter tripleRewriter) {
        this.tripleRewriter = tripleRewriter;
    }


    /**
     * @return Returns the objectFilter.
     */
    public String getObjectFilter() {
        return objectFilter;
    }


    /**
     * @param objectFilter The objectFilter to set.
     */
    public void setObjectFilter(String objectFilter) {
        this.objectFilter = objectFilter;
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
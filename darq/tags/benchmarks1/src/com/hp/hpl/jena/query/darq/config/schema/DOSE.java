/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */


package com.hp.hpl.jena.query.darq.config.schema;

/* CVS $Id: $ */
 
import com.hp.hpl.jena.rdf.model.*;
 
/**
 * Vocabulary definitions from /home/quilitz/workspace/FederatedQueries/RDFS/ServiceSchema.n3 
 * @author Auto-generated by schemagen on 19 Jun 2006 11:36 
 */
public class DOSE {
    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static Model m_model = ModelFactory.createDefaultModel();
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://darq.sf.net/dose/0.1#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    
    /** <p>Number of triples with the predicate defined for the Capability</p> */
    public static final Property triples = m_model.createProperty( "http://darq.sf.net/dose/0.1#triples" );
    
    /** <p>filter for restrictions on subject and object</p> */
    public static final Property sofilter = m_model.createProperty( "http://darq.sf.net/dose/0.1#sofilter" );
    
    /** <p>Predicate that an endpoint knows about</p> */
    public static final Property predicate = m_model.createProperty( "http://darq.sf.net/dose/0.1#predicate" );
    
    /** <p>Number of triples in dataset</p> */
    public static final Property totalTriples = m_model.createProperty( "http://darq.sf.net/dose/0.1#totalTriples" );
    
    public static final Property requiredBindings = m_model.createProperty( "http://darq.sf.net/dose/0.1#requiredBindings" );
    
    /** <p>defines if the service is definitive</p> */
    public static final Property isDefinitive = m_model.createProperty( "http://darq.sf.net/dose/0.1#isDefinitive" );
    
    public static final Property capability = m_model.createProperty( "http://darq.sf.net/dose/0.1#capability" );
    
    public static final Property url = m_model.createProperty( "http://darq.sf.net/dose/0.1#url" );
    public static final Property graph = m_model.createProperty( "http://darq.sf.net/dose/0.1#graph" );
    
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
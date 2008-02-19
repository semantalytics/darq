/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package test.darq;

import com.hp.hpl.jena.query.DataFormat;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.query.junit.QueryTestException;
import com.hp.hpl.jena.query.junit.SerializerTest;
import com.hp.hpl.jena.query.junit.SyntaxTest;
import com.hp.hpl.jena.query.junit.TestFactory;
import com.hp.hpl.jena.query.junit.TestItem;
import com.hp.hpl.jena.query.junit.TestUtils;
import com.hp.hpl.jena.query.vocabulary.TestManifestX;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;

import junit.framework.*;

public class QueryTestSuiteFactory extends TestFactory 
{
    private FileManager fileManager = FileManager.get() ;
    private Model model = null ;
    

    /** Make a test suite from a manifest file */
    static public TestSuite make(String filename) 
    {
        QueryTestSuiteFactory tFact = new QueryTestSuiteFactory() ;
        tFact.model =  tFact.fileManager.loadModel(filename) ;
        return tFact.process(filename) ;
    }

    /** Make a single test */
    static public TestSuite make(String query, String data, String result)
    {
        TestItem item = new TestItem(query, query, data, result) ;
        
        QueryTest t = new QueryTest(item.getResource().getModel(), item.getName(), FileManager.get(), item) ;
        TestSuite ts = new TestSuite() ;
        ts.setName(TestUtils.safeName(query)) ;
        ts.addTest(t) ;
        return ts ;
    }
    
    public Test makeTest(Resource manifest, Resource entry, String testName, Resource action, Resource result)
    {
        // Defaults.
        Syntax querySyntax = TestUtils.getQuerySyntax(manifest)  ;
        
        if ( querySyntax != null )
        {
            if ( ! querySyntax.equals(Syntax.syntaxRDQL) &&
            ! querySyntax.equals(Syntax.syntaxARQ) &&
            ! querySyntax.equals(Syntax.syntaxSPARQL) )
                throw new QueryTestException("Unknown syntax: "+querySyntax) ;
        }
        
        // May be null
        Resource defaultTestType = TestUtils.getResource(manifest, TestManifestX.defaultTestType) ;
        // test name
        // test type
        // action -> query specific query[+data]
        // results
        
        TestItem item = new TestItem(entry, defaultTestType, querySyntax, DataFormat.langXML) ;
        
        //Done by TestItem itself
//        if ( querySyntax != null )
//            item.setQueryFileSyntax(querySyntax) ;
        
        TestCase test = null ;
        
        if ( item.getTestType() != null )
        {
            // Good syntax
            if ( item.getTestType().equals(TestManifestX.TestSyntax) )
                test = new SyntaxTest(testName, item) ;
            
            // Bad syntax
            if ( item.getTestType().equals(TestManifestX.TestBadSyntax) )
                test = new SyntaxTest(testName, item, false) ;
            
            if ( item.getTestType().equals(TestManifestX.TestSerialization) )
                test = new SerializerTest(testName, item) ;
            
            if ( item.getTestType().equals(TestManifestX.TestQuery) )
                test = new QueryTest(item.getResource().getModel(), testName, fileManager, item) ;
        }
        // Default 
        if ( test == null )
            test = new QueryTest(item.getResource().getModel(), testName, fileManager, item) ;
        return test ;
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
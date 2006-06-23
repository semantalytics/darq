package test.darq;

import java.util.Collection;
import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.darq.config.Configuration;
import com.hp.hpl.jena.query.darq.core.RemoteService;

public class SchemaTest {

    /**
     * @param args
     */
    public static void main(String[] args)
    {
   

                   
       
        String filename="ExampleRDF/ServiceDescr1.n3";
/*
        Model model = ModelFactory.createDefaultModel();
        InputStream in = FileManager.get().open(filename);
       if (in == null) {
           throw new IllegalArgumentException(
                                        "File: " + filename + " not found");
       }
       
       model.read(in, "","N3");
       
      StmtIterator iter = model.listStatements();
       while(iter.hasNext()) {
       Statement st= iter.nextStatement();
       //if(node.isLiteral())
       System.out.println(st.toString());
       //}
       }
*/
       
       
       
       Configuration configuration =  new Configuration(filename);

       Triple triple = new Triple(Node.createAnon(),Node.createURI("http://xmlns.com/foaf/0.1/#name"),Node.create("8"));
       
       Collection<RemoteService> services = configuration.getServiceRegistry().getAvailableServices();
       
       for (Iterator<RemoteService> it = services.iterator(); it.hasNext(); ) {
           
           RemoteService remoteService = it.next();
           
            System.out.println(remoteService.getLabel()+": "+remoteService.hasCapability(triple));
           
       }
       
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
/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ClassLoadingUtils<E> {
    
    
    private static ClassLoader classLoader = ClassLoader.getSystemClassLoader() ;
    private static final Log log = LogFactory.getLog(ClassLoadingUtils.class);
    
    
    @SuppressWarnings("unchecked")
    public E loadClass(String classname) {
    try {            
        
        log.trace("Load module: " + classname);
        Class classObj = null ;
        try {
            classObj = classLoader.loadClass(classname);
        } catch (ClassNotFoundException ex)
        {
            throw new ClassNotFoundException("Class not found: "+classname);
        }
        
        if ( classObj == null )
            throw new Exception("Null return from classloader");
        
        log.debug("Loaded: "+classname) ;
        
        E obj = (E)classObj.newInstance();
        log.trace("New Instance created") ;

        return obj;
    }
    catch (Exception ex ) {
        log.error("Class not loaded: "+ex);
        return null;
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
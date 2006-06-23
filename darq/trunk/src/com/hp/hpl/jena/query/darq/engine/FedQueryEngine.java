/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.darq.config.Configuration;
import com.hp.hpl.jena.query.darq.util.OutputUtils;
import com.hp.hpl.jena.query.engine1.Plan;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.QueryEngine;
import com.hp.hpl.jena.query.util.Context;

public class FedQueryEngine extends QueryEngine {
    
    static Log log = LogFactory.getLog(FedQueryEngine.class) ;
    
    Query query ;
    Configuration config;

    public FedQueryEngine(Query q) {
        this(q,null,null);     
    }
    
    public FedQueryEngine(Query q, Configuration conf) {
        this(q,null,conf);     
    }
    
    public FedQueryEngine(Query q,Context p,Configuration conf) {
        super(q,p);
        query=q;
        config=conf;
    }
    
    public void setConfig(Configuration c) {
        config=c;
    }
    
    
    protected PlanElement makePlanForQueryPattern(Plan plan)
    {
        FedQueryCompiler fqc = new FedQueryCompiler(config);
        PlanElement pe = fqc.makePlan(plan, query.getQueryBlock()) ;
        
        log.debug("PLAN: \n" + OutputUtils.PlanToString(pe));
        // if (log.isDebugEnabled()) JucDemo.waitForKey();  //  REMOVE JUST FOR DEMO !!  
        return pe;
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
/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.util;
 
import java.io.OutputStream;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanMultipleService;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanService;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanUnionService;
import com.hp.hpl.jena.query.darq.engine.compiler.PlanNestedLoopJoin;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.PlanFormatterVisitor;
import com.hp.hpl.jena.query.expr.Expr;
import com.hp.hpl.jena.query.util.IndentedWriter;
import com.hp.hpl.jena.shared.PrefixMapping;
 
public class FedPlanFormatter extends PlanFormatterVisitor implements FedPlanVisitor {

    IndentedWriter out = null;
    PrefixMapping prefixMapping = null;
    
    public FedPlanFormatter(IndentedWriter w, PrefixMapping pmap) {
        super(w,pmap);
        out=w;
        prefixMapping=pmap;
        
    }

    public void visit(FedPlanService planElt) {
        out.println("Service:"+planElt.getServiceGroup().getService().getLabel()+"("+planElt.getServiceGroup().getService().getUrl()+")");
        out.incIndent();
        for (Triple t: planElt.getServiceGroup().getTriples()) {
            out.println(t.toString(prefixMapping));
        }
        for (Expr f: planElt.getServiceGroup().getFilters()) {
            out.println(f.toString());
        }
        
        out.decIndent();
    }
    
    public void visit(FedPlanMultipleService planElt) {
        out.print("Multiple Services:");
        out.incIndent();
        for (RemoteService s:planElt.getServiceGroup().getServices()) {
            
            out.println("* "+s.getLabel()+"("+s.getUrl()+")");
        }
        out.incIndent();
        for (Triple t: planElt.getServiceGroup().getTriples()) {
            out.println(t.toString(prefixMapping));
        }
        for (Expr f: planElt.getServiceGroup().getFilters()) {
            out.println(f.toString());
        }
        out.decIndent();
        
        
        out.decIndent();
        
    }
    
    public void visit(FedPlanUnionService planElt) {
    	out.print("Union Service:");
    	out.incIndent();
    	for (ServiceGroup sg : planElt.getServiceGroup().getServiceGroups().values()){
    		if (sg instanceof MultipleServiceGroup){
    			MultipleServiceGroup msg = (MultipleServiceGroup) sg;
    			out.print("Multiple Services(USG):");
    	        out.incIndent();
    	        for (RemoteService s: msg.getServices()) {
    	            
    	            out.println("* "+s.getLabel()+"("+s.getUrl()+")");
    	        }
    	        out.incIndent();
    	        for (Triple t: msg.getTriples()) {
    	            out.println(t.toString(prefixMapping));
    	        }
    	        for (Expr f: msg.getFilters()) {
    	            out.println(f.toString());
    	        }
    	        out.decIndent();    	        
    	        out.decIndent();
    		}
    		else if (sg instanceof ServiceGroup){
    			 out.println("Service(USG):"+sg.getService().getLabel()+"("+sg.getService().getUrl()+")");
    		        out.incIndent();
    		        for (Triple t: sg.getTriples()) {
    		            out.println(t.toString(prefixMapping));
    		        }
    		        for (Expr f: sg.getFilters()) {
    		            out.println(f.toString());
    		        }
    		        out.decIndent();
    		}
    	}
    	
    }
    public void visit(PlanNestedLoopJoin planElt) {
    	 out.print("NestedLoop: ");
         out.incIndent();
         planElt.getLeft().visit(this);
         planElt.getRight().visit(this);
         out.decIndent();
    }
    
    
    
    static public void out(IndentedWriter w, PlanElement pElt)
    {
        out(w, null,pElt) ; 
    }
    
    
  
    
   
    static public void out(IndentedWriter w, PrefixMapping pmap, PlanElement pElt)
    {
        PlanFormatterVisitor fmt = new FedPlanFormatter(w, pmap) ;
        
        //fmt.startVisit() ;
        pElt.visit(fmt) ;
        //fmt.finishVisit() ;
        
    }
    static public void out(OutputStream ps, PlanElement pElt)
    {
        out(new IndentedWriter(ps), PrefixMapping.Standard,pElt) ;
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
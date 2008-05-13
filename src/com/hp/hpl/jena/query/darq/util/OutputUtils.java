/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.util;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.core.UnionServiceGroup;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.util.IndentedWriter;
import com.hp.hpl.jena.shared.PrefixMapping;

public class OutputUtils {
    
    
        public static void printServiceGroupArrayList(List<ServiceGroup> l) {
            IndentedWriter out = new IndentedWriter(System.out);
            outServiceGroupList(l,out);
            out.flush();
        }
    
        public static String  serviceGroupListToString(List<ServiceGroup> l) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            IndentedWriter out = new IndentedWriter(os);
            outServiceGroupList(l,out);
            out.flush();
            return new String(os.toByteArray());
        }
        
        public static String  serviceGroupToString(ServiceGroup sg) {
            ArrayList<ServiceGroup> l = new ArrayList<ServiceGroup>();
            l.add(sg);
            
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            IndentedWriter out = new IndentedWriter(os);
            outServiceGroupList(l,out);
            out.flush();
            return new String(os.toByteArray());
        }
        
        public static void outServiceGroupList(List<ServiceGroup> l, IndentedWriter out) {
            
            
            for (ServiceGroup sg:l) {
                outServiceGroup(sg,out);
            }

        }
        
        public static void outServiceGroup(ServiceGroup sg, IndentedWriter out) {
            
        	if(sg instanceof UnionServiceGroup){
        		UnionServiceGroup usg = (UnionServiceGroup) sg;
        		for (ServiceGroup xsg : usg.getServiceGroups().values()){
        			
        			if (xsg instanceof MultipleServiceGroup) {
                        for (RemoteService s:((MultipleServiceGroup)sg).getServices()) out.println("+"+s.getLabel() + " ("+ s.getUrl() + ")");
                    }
                    else {
                        
                    out.println(sg.getService().getLabel() + " ("+ sg.getService().getUrl() + ")");
                    }
                    out.incIndent();
                    
                    
                    for ( Triple t: sg.getTriples()) {
                        out.println(t.toString());
                    }
                    
                    out.decIndent();
        		}
        	}
        	
            if (sg instanceof MultipleServiceGroup) {
                for (RemoteService s:((MultipleServiceGroup)sg).getServices()) out.println("+"+s.getLabel() + " ("+ s.getUrl() + ")");
            }
            else {
                
            out.println(sg.getService().getLabel() + " ("+ sg.getService().getUrl() + ")");
            }
            out.incIndent();
            
            
            for ( Triple t: sg.getTriples()) {
                out.println(t.toString());
            }
            
            out.decIndent();
            
        }
        
        
        public static String PlanToString(PlanElement planElement, PrefixMapping pm) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            IndentedWriter out = new IndentedWriter(os);
            FedPlanFormatter.out(out,pm, planElement);
            out.flush();
            return new String(os.toByteArray());
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
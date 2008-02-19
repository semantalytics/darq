package com.hp.hpl.jena.query.darq.util;

import com.hp.hpl.jena.query.util.IndentedWriter;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.engine.compiler.MapFedPlanMultipleService;
import com.hp.hpl.jena.query.darq.engine.compiler.MapFedPlanService;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.PlanFormatterVisitor;

public class MapFedPlanFormatter extends FedPlanFormatter {

	    IndentedWriter out = null;
	    PrefixMapping prefixMapping = null;
	    
	    public MapFedPlanFormatter(IndentedWriter w, PrefixMapping pmap) {
	        super(w,pmap);
	        out=w;
	        prefixMapping=pmap;
	        
	    }

	    public void visit(MapFedPlanService planElt) {
	        out.println("Service:"+planElt.getServiceGroup().getService().getLabel()+"("+planElt.getServiceGroup().getService().getUrl()+")");
	        out.incIndent();
	        for (Triple t: planElt.getServiceGroup().getTriples()) {
	            out.println(t.toString(prefixMapping));
	        }
	        out.decIndent();
	    }
	    
	    public void visit(MapFedPlanMultipleService planElt) {
	        out.print("Multiple Services:");
	        out.incIndent();
	        for (RemoteService s:planElt.getServiceGroup().getServices()) {
	            
	            out.println("* "+s.getLabel()+"("+s.getUrl()+")");
	        }
	        out.incIndent();
	        for (Triple t: planElt.getServiceGroup().getTriples()) {
	            out.println(t.toString(prefixMapping));
	        }
	        out.decIndent();
	        
	        
	        out.decIndent();
	        
	    }

	    static public void out(IndentedWriter w, PrefixMapping pmap, PlanElement pElt)
	    {
	        PlanFormatterVisitor fmt = new MapFedPlanFormatter(w, pmap) ;
	        
	        //fmt.startVisit() ;
	        pElt.visit(fmt) ;
	        //fmt.finishVisit() ;
	        
	    }
}
	
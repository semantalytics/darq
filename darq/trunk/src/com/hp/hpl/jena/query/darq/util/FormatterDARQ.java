package com.hp.hpl.jena.query.darq.util;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.serializer.FormatterARQ;
import com.hp.hpl.jena.query.serializer.SerializationContext;
import com.hp.hpl.jena.query.util.IndentedWriter;

public class FormatterDARQ extends FormatterARQ {

	  public FormatterDARQ(IndentedWriter out, SerializationContext context) {
		super(out, context);
		// TODO Auto-generated constructor stub
	}

	  	@Override
	protected String slotToString(SerializationContext context, Node n)
	    {
	//	System.out.println("FormatterDARQ.slotToString");
	        return DarqFmtUtils.stringForNode(n, context) ;
	    }
	
}

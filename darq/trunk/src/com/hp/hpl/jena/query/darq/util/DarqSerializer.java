package com.hp.hpl.jena.query.darq.util;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.serializer.FmtExpr;
import com.hp.hpl.jena.query.serializer.FmtExprARQ;
import com.hp.hpl.jena.query.serializer.FmtTemplateARQ;
import com.hp.hpl.jena.query.serializer.FormatterElement;
import com.hp.hpl.jena.query.serializer.FormatterTemplate;
import com.hp.hpl.jena.query.serializer.QuerySerializer;
import com.hp.hpl.jena.query.serializer.SerializationContext;
import com.hp.hpl.jena.query.serializer.Serializer;
import com.hp.hpl.jena.query.util.IndentedWriter;
import com.hp.hpl.jena.query.util.NodeToLabelMap;

public class DarqSerializer extends Serializer {

    static public void serializeDARQ(Query query, IndentedWriter writer)
    {
        // For the query pattern
        SerializationContext cxt1 = new SerializationContext(query, query.getPrefixMapping(), new NodeToLabelMap("b", false) ) ;
        // For the construct pattern
        SerializationContext cxt2 = new SerializationContext(query, query.getPrefixMapping(),  new NodeToLabelMap("c", false)  ) ;
        
        serializeDARQ(query, writer, 
                     new FormatterDARQ(writer, cxt1),
                     new FmtExprARQ(writer, cxt1),
                     new FmtTemplateARQ(writer, cxt2)) ;
    }
    
    
    static private void serializeDARQ(Query query, 
                                     IndentedWriter writer, 
                                     FormatterElement eltFmt,
                                     FmtExpr    exprFmt,
                                     FormatterTemplate templateFmt)
    {
   // 	System.out.println("DarqSerializer.serializeDARQ");
        QuerySerializer serilizer = new QuerySerializer(writer, eltFmt, exprFmt, templateFmt) ;
        query.visit(serilizer) ;
    }
}

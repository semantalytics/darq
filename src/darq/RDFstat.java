package darq;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.darq.config.schema.DOSE;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;


public class RDFstat {
    
    
    
    private static String schemaURI= "http://darq.sf.net/dose/0.1#";
    
    
    /**
     * Stores the number of statements for a predicate
     * <RDFNode,Integer>
     *   predicate   count
     */
    Map<RDFNode,Integer> predicates = new HashMap<RDFNode,Integer>();
    
    
    /**
     * stores the Number of statements with the same object for a predicate
     * <RDFNode,Map<RDFNode,Integer>>
     *   predicate   object    count
     */
    Map<RDFNode,Map<String,Integer>> predicateObjects = new HashMap<RDFNode,Map<String,Integer>>();
    Map<RDFNode,Map<String,Integer>> predicateSubjects= new HashMap<RDFNode,Map<String,Integer>>();
    
    Map<RDFNode,Integer> classes = new HashMap<RDFNode,Integer>();
    
    
    int statements = 0;
    
    private void analyse(String url, String format) {
        
        
        Model model = FileManager.get().loadModel(url);//ModelFactory.createDefaultModel();
        
        System.out.print("# loading model ("+url+") ....");
        
        
        //model.read(url,format);
        System.out.println("# done.");


        
     /* // for testing
        model.add(model.createResource("http://test/1"),model.createProperty("http://test/#prop1"),"a");
        model.add(model.createResource("http://test/1"),model.createProperty("http://test/#prop1"),"b");
        model.add(model.createResource("http://test/2"),model.createProperty("http://test/#prop1"),"a");
        model.add(model.createResource("http://test/1"),model.createProperty("http://test/#prop2"),"123");
        model.add(model.createResource("http://test/1"),model.createProperty("http://test/#prop2"),"111");
        model.add(model.createResource("http://test/2"),model.createProperty("http://test/#prop2"),"111");
        */
        
        StmtIterator it = model.listStatements();
        
        while (it.hasNext()) {
            statements++;
            Statement s = it.nextStatement();
            countPredicate(s.getPredicate());
            countPredicateObjects(s.getPredicate(),s.getObject());
            countPredicateSubjects(s.getPredicate(),s.getSubject());
            findClasses(s.getPredicate(),s.getObject());
        }
        
    }
    
    private void report() {
        
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("sd","http://darq.sf.net/dose/0.1#");
        model.setNsPrefix("rdfs","http://www.w3.org/2000/01/rdf-schema#");
        model.setNsPrefix("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");

        Resource r = model.createResource();
        
        model.add(r,RDF.type,model.createResource("sd:Service"));
        model.add(r,DOSE.url,model.createResource("CHANGE_THIS"));
        
        
        
        
        
        System.out.println("# Number of statements: "+statements);
        
        model.add(r,DOSE.totalTriples,model.createLiteral(statements));
        
        
        /*line();
        System.out.println("Number of different predicates: "+predicates.size());
        line();
        
        System.out.println("Number of classes:" +classes.size());
        line();*/
        
        
        int classcount =0;
        String filterstring="";
        
        for (RDFNode n:classes.keySet()) {
            if (classcount!=0) filterstring+=" || "; 
            filterstring+= "REGEX(STR(?object),'"+n.toString()+"')";
            classcount+=classes.get(n);
        }
        
        Resource capability = model.createResource(); 
        model.add(r,DOSE.capability,capability);
        model.add(capability,DOSE.predicate ,RDF.type);
        model.add(capability,DOSE.sofilter,model.createTypedLiteral(filterstring));
        model.add(capability,DOSE.triples,model.createTypedLiteral(classcount,XSD.integer.getURI()));

        
        for (RDFNode n: predicateObjects.keySet()) {
            Map<String,Integer> m = predicateObjects.get(n);
            //System.out.println("predicate <"+n.toString()+">");
            
             
            
           
            
            int sum = 0;
            for (String s: m.keySet()) {
                sum+=m.get(s);
            }
            //System.out.println(m.size()+" distinct objects");
            //System.out.println(sum+" total objects");
            
            
            
            double o = new Double(sum);
            double u = new Double(statements);
            
            //System.out.println("Selectivity of predicate= " + o / u); // TODO right??
            
            o = new Double(m.size());
            u = new Double(sum);
            double objectsSel = u/(o*u);
            //System.out.println("Average Selectivity of object= " + objectsSel); // TODO right??
            
            
            o = new Double(predicateSubjects.get(n).size());
            u = new Double(sum);
            double subjectSel = u/(o*u);
       /*     System.out.println("Average Selectivity of subject= " + subjectSel); // TODO right??
            
            
            System.out.println("if object is bound = " + objectsSel*sum); // TODO right??
            System.out.println("if subject is bound = " + subjectSel*sum); // TODO right??
            System.out.println("if subject and object are bound = " + subjectSel*sum*objectsSel); // TODO right??
            
            
            line();*/
            
            if (!n.equals(RDF.type)) {
                capability = model.createResource();
                model.add(r,DOSE.capability,capability);
                model.add(capability,DOSE.predicate,n);
                model.add(capability,DOSE.sofilter,model.createLiteral(""));
                
                
                //model.add(capability,model.createProperty("sd:","triples"),model.createLiteral(new Integer(sum)));
                model.add(capability,DOSE.triples,model.createTypedLiteral(new Integer(sum),XSD.integer.getURI()));
                
                //model.add(capability,model.createProperty("sd:","objectSelectivity"),model.createLiteral(new Double(value)));
                model.add(capability,model.createProperty(schemaURI,"objectSelectivity"),model.createTypedLiteral(new Double(objectsSel),XSD.xdouble.getURI()));
//              model.add(capability,model.createProperty("sd:","objectSelectivity"),model.createLiteral(new Double(value)));
                model.add(capability,model.createProperty(schemaURI,"subjectSelectivity"),model.createTypedLiteral(new Double(subjectSel),XSD.xdouble.getURI()));
                
                }
            
        }
        line();
        System.out.println("# N3 description:");
        line();
        model.write(System.out,"N3");
    }
    
    private void line() {
        System.out.println("# ----------------------------------------------------");
    }
    
    
    private void countPredicate(RDFNode p) {
        Integer count = predicates.get(p);
        if (count==null) { 
            count = 1;
        } else {
            count++;
        }
        predicates.put(p,count);
    }
    
    private void countPredicateObjects(RDFNode p, RDFNode o) {
        Map<String,Integer> m = predicateObjects.get(p);
        if (m==null) { 
            m= new HashMap<String,Integer>();
            m.put(o.toString(),1);
        } else {
            Integer count = m.get(o.toString());
            if (count==null) { 
                count = 1;
            } else {
                count++;
            }
            m.put(o.toString(),count);
        }
        predicateObjects.put(p,m);
    }
    
    private void countPredicateSubjects(RDFNode p, RDFNode s) {
        Map<String,Integer> m = predicateSubjects.get(p);
        if (m==null) { 
            m= new HashMap<String,Integer>();
            m.put(s.toString(),1);
        } else {
            Integer count = m.get(s.toString());
            if (count==null) { 
                count = 1;
            } else {
                count++;
            }
            m.put(s.toString(),count);
        }
        predicateSubjects.put(p,m);
    }
    
    
    private void findClasses(RDFNode p, RDFNode o) {
        if (!p.equals(RDF.type)) return;
        
        Integer count = classes.get(o);
        if (count==null) { 
            count = 1;
        } else {
            count++;
        }
        classes.put(o,count);
        
    }
    
    
    
    public static void main(String[] args) {
        
        if (args.length!=2) {
            System.out.println("usage: rdfstat url format");
            System.out.println("eg: rdfstat file:///home/me/file.n3 N3");
            System.exit(1);
        }
        RDFstat stat = new RDFstat();
        stat.analyse(args[0],args[1]);
        stat.report();
      
    }
    
    
    
    

}

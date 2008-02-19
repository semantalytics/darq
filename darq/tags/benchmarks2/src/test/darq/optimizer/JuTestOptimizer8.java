package test.darq.optimizer;



import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.darq.core.Capability;
import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;

public class JuTestOptimizer8 extends AbstractOptimizerTest {
    
    private final Node varX = Node.createVariable("x");
    private final Node varY = Node.createVariable("y");
    private final Node varZ = Node.createVariable("z");
    private final Node varO = Node.createVariable("o");
    
    private final Node predP = Node.createURI("p");
    private final Node predQ = Node.createURI("q");
    private final Node predR = Node.createURI("r");
    private final Node predS = Node.createURI("s");
    private final Node predT = Node.createURI("t");
    private final Node predU = Node.createURI("u");
    
    RemoteService service1 ;
    RemoteService service2 ;
    RemoteService service3 ;
        
    @Override
    public List<ServiceGroup> getTestInput() {
      
        ServiceGroup sg1 ;
        List<Triple> tl1 ;
        MultipleServiceGroup sg2 ;
        List<Triple> tl2 ;
        service1 = new RemoteService("service1","service1","service1",false);
        service1.addCapability(new Capability("p","",0.01,0.02, 100));
        service1.addCapability(new Capability("q","",0.02,0.1,100));
        service1.addCapability(new Capability("r","",0.01,0.01,100));
        service1.addCapability(new Capability("s","",0.1,0.1,10));

        service2 = new RemoteService("service2","service2","service2",false);
        service2.addCapability(new Capability("t","",0.01,0.1,100));
        service2.addCapability(new Capability("u","",2.0/50.0,0.1,50));
        
        service3 = new RemoteService("service3","service3","service3",false);
        service3.addCapability(new Capability("t","",0.01,0.1,100));
        service3.addCapability(new Capability("u","",2.0/50.0,0.1,50));
        
        sg1 = new ServiceGroup(service1);
        sg2 = new MultipleServiceGroup();
        sg2.addService(service2);
        sg2.addService(service3);
        
        tl1 = new ArrayList<Triple>();
        
      
        
       
        
        tl1 = new ArrayList<Triple>();
        tl1.add(new Triple(varX,predP,Node.createLiteral("test")));
        tl1.add(new Triple(varX,predQ,varY));
        sg1.setTriples(tl1);
        
        tl2 = new ArrayList<Triple>();
        tl2.add(new Triple(varY,predT, varZ));
        tl2.add(new Triple(varY,predU,varO));
        sg2.setTriples(tl2);
        
        List<ServiceGroup> sglist = new ArrayList<ServiceGroup>();
        sglist.add(sg1);
        sglist.add(sg2);
        return sglist;
    }


    @Override
    public List<ServiceGroup> getExpectedResult() {
        ServiceGroup rsg1 = new ServiceGroup(service1);
        List<Triple> rtl1 ;
        MultipleServiceGroup rsg2 = new MultipleServiceGroup();
        rsg2.addService(service2);
        rsg2.addService(service3);
        List<Triple> rtl2 ;
        
        

        
    
        rtl1 = new ArrayList<Triple>();
        rtl1.add(new Triple(varX,predP,Node.createLiteral("test")));
        rtl1.add(new Triple(varX,predQ,varY));
        rsg1.setTriples(rtl1);
        
        rtl2 = new ArrayList<Triple>();
        rtl2.add(new Triple(varY,predT, varZ));
        rtl2.add(new Triple(varY,predU,varO));
        rsg2.setTriples(rtl2);
    

        List<ServiceGroup> result = new ArrayList<ServiceGroup>();
        result.add(rsg1);
        result.add(rsg2);
        
        return result;
    }


    @Override
    public double getExpectedCosts() {
        // TODO Auto-generated method stub
        return 16.0;
    }

    
    
    



}

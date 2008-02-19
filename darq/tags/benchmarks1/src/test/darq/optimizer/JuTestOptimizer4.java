package test.darq.optimizer;



import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.darq.core.Capability;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;

public class JuTestOptimizer4 extends AbstractOptimizerTest {
    
    private final Node varX = Node.createVariable("x");
    private final Node varY = Node.createVariable("y");
    private final Node varZ = Node.createVariable("z");
    
    private final Node predP = Node.createURI("p");
    private final Node predQ = Node.createURI("q");
    private final Node predR = Node.createURI("r");
    
    RemoteService service1 ;
        
    @Override
    public List<ServiceGroup> getTestInput() {
      
        ServiceGroup sg1 ;
        List<Triple> tl1 ;
        service1 = new RemoteService("service1","service1","service1",false);
        service1.addCapability(new Capability("p","",0.01,0.02, 100));
        service1.addCapability(new Capability("q","",0.02,0.1,100));
        service1.addCapability(new Capability("r","",0.01,0.01,100));
        service1.addCapability(new Capability("s","",0.1,0.1,10));
        
        
       /* service2 = new RemoteService("service2","service2","service2",false);
        service2.addCapability(new Capability("t","",0.01,0.1,100));
        service2.addCapability(new Capability("u","",2.0/50.0,0.1,50));*/
        
        sg1 = new ServiceGroup(service1);
    //    sg2 = new ServiceGroup(service2);
        
        tl1 = new ArrayList<Triple>();
        
      
        
       
        
        tl1 = new ArrayList<Triple>();
        tl1.add(new Triple(varX,predP,Node.createLiteral("test")));
        tl1.add(new Triple(varX,predR,Node.createLiteral("test")));
        tl1.add(new Triple(varX,predQ,varY));
        
        sg1.setTriples(tl1);
        
        List<ServiceGroup> sglist = new ArrayList<ServiceGroup>();
        sglist.add(sg1);
        return sglist;
    }


    @Override
    public List<ServiceGroup> getExpectedResult() {
        List<ServiceGroup> result ;
        result = new ArrayList<ServiceGroup>();
        
        List<Triple> restriples = new ArrayList<Triple>();
        
        restriples.add(new Triple(varX,predR,Node.createLiteral("test")));
        restriples.add(new Triple(varX,predP,Node.createLiteral("test")));
        restriples.add(new Triple(varX,predQ,varY));
        
        ServiceGroup resg1 = new ServiceGroup(service1);
        resg1.setTriples(restriples);
        
        result.add(resg1);
        
        return result;
    }


    @Override
    public double getExpectedCosts() {
        // TODO Auto-generated method stub
        return 2.0;
    }

    /* (non-Javadoc)
     * @see test.federatedQueries.optimizer.AbstractOptimizerTest#getDoCheapestPlanForServiceGroupTest()
     */
    @Override
    protected boolean getDoCheapestPlanForServiceGroupTest() {
        return true;
    }
    
    



}

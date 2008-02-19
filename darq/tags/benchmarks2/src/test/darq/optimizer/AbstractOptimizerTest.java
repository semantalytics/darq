package test.darq.optimizer;

import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.optimizer.CostBasedBasicOptimizer;
import com.hp.hpl.jena.query.darq.engine.optimizer.OptimizerElement;
import com.hp.hpl.jena.query.darq.engine.optimizer.PlanUnfeasibleException;
import com.hp.hpl.jena.query.darq.util.Permute;

public abstract class AbstractOptimizerTest extends TestCase {

    CostBasedBasicOptimizer optimizer;

    public abstract List<ServiceGroup> getTestInput();

    public abstract List<ServiceGroup> getExpectedResult();

    public abstract double getExpectedCosts();

    /*
     * Test method for
     * 'com.hp.hpl.jena.query.federated.engine.optimizer.CostBasedPlanOptimizer.getCheapestPlan(ArrayList<ServiceGroup>)'
     */
    public void testGetCheapestPlan() {
        Permute<ServiceGroup> permute = new Permute<ServiceGroup>(getTestInput());
        while (permute.hasNext()) {
            try {
                List<ServiceGroup> result = optimizer.getCheapestPlan(permute.next());
                assertEquals("wrong costs :" + optimizer.getCosts() + "!=" + getExpectedCosts(), optimizer.getCosts(), getExpectedCosts());
                assertTrue("wrong order", getExpectedResult().equals(result));
            } catch (PlanUnfeasibleException e) {
                assertTrue(e.toString(), false);
            }

        }
    }

    public void testgetCheapestPlanForServiceGroup() {
        if (getDoCheapestPlanForServiceGroupTest()) {
            Permute<ServiceGroup> permute = new Permute<ServiceGroup>(getTestInput());
            while (permute.hasNext()) {

                ServiceGroup sg = permute.next().get(0);

                Permute<Triple> tpermute = new Permute<Triple>(sg.getTriples());

                while (tpermute.hasNext()) {
                    ServiceGroup tsg = sg.clone();
                    tsg.setTriples(tpermute.next());
                    try {
                        OptimizerElement<ServiceGroup> result = optimizer.getCheapestPlanForServiceGroup(tsg, new HashSet<String>());
                        assertTrue("wrong order", getExpectedResult().get(0).getTriples().equals(result.getElement().getTriples()));
                    } catch (PlanUnfeasibleException e) {
                        assertTrue(e.toString(), false);
                    }
                }

            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        optimizer = new CostBasedBasicOptimizer();

        super.setUp();
    }

    protected boolean getDoCheapestPlanForServiceGroupTest() {
        return false;
    }

}

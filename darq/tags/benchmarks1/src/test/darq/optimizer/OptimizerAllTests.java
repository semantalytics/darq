package test.darq.optimizer;

import junit.framework.Test;
import junit.framework.TestSuite;

public class OptimizerAllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite(
                "Test for test.federatedQueries.optimizer");
        //$JUnit-BEGIN$
        
        suite.addTestSuite(JuTestOptimizer1.class);
        suite.addTestSuite(JuTestOptimizer2.class);
        suite.addTestSuite(JuTestOptimizer3.class);
        suite.addTestSuite(JuTestOptimizer4.class);
        suite.addTestSuite(JuTestOptimizer5.class);
        suite.addTestSuite(JuTestOptimizer6.class);
        suite.addTestSuite(JuTestOptimizer7.class);
        suite.addTestSuite(JuTestOptimizer8.class);
     //   suite.addTestSuite(CopyOfJuTestOptimizer8.class);
        //$JUnit-END$
        return suite;
    }

}

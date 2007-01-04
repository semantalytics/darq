package com.hp.hpl.jena.query.darq.engine.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import sun.awt.windows.ThemeReader;

import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterConcat;

public class QueryIterConcatParallel extends QueryIterConcat {

    List<QueryIterator> iterators = new ArrayList<QueryIterator>();

    boolean initialized = false;

    public QueryIterConcatParallel(ExecutionContext context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void init() {

        // exec iterators in thread
        if (!initialized) {
            
            ExecutorService executor = Executors.newFixedThreadPool(10);
            
            
          
            
         //   ThreadGroup group = new ThreadGroup(this.toString());

            ArrayList<ParallelHelper> threadlist = new ArrayList<ParallelHelper>();

            for (QueryIterator qi : iterators) {

                ParallelHelper h = new ParallelHelper(qi);
            /*    Thread t = new Thread(group, h);
                t.start(); */
                threadlist.add(h);
                
                executor.submit(h);
                

            }
            executor.shutdown();
            
            while (!executor.isTerminated()) {
               try {
                executor.awaitTermination(3600, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            }

/*            try {
               join(group);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
*/
            boolean error = false;

            for (ParallelHelper h : threadlist) {
                if (h.isError()) {
                    error = true;
                    System.err.println(h.getException().getMessage());
                }
            }
                
            
            if (!error) {
                for (ParallelHelper h : threadlist) {

                    try {
                        super.add(h.getResultIterator());
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        System.err.println("WOW !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    }
                }
                super.init();
            }
            initialized = true;
        }
    }

    void join(ThreadGroup tg) throws InterruptedException {
        synchronized (tg) {
            while (tg.activeCount() > 0)
                tg.wait(10);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.hp.hpl.jena.query.engine1.iterator.QueryIterConcat#add(com.hp.hpl.jena.query.engine.QueryIterator)
     */
    @Override
    public void add(QueryIterator qIter) {
        // TODO Auto-generated method stub
        iterators.add(qIter);
    }

}

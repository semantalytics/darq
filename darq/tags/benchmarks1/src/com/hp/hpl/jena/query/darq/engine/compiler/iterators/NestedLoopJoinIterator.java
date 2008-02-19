package com.hp.hpl.jena.query.darq.engine.compiler.iterators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.hp.hpl.jena.query.core.ARQInternalErrorException;
import com.hp.hpl.jena.query.core.Var;
import com.hp.hpl.jena.query.engine.Binding;
import com.hp.hpl.jena.query.engine.BindingMap;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine.QueryIteratorBase;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.iterator.QueryIterPlainWrapper;

public class NestedLoopJoinIterator extends QueryIteratorBase {

	private boolean initialized = false;
	private ExecutionContext context;

//	private QueryIterator input;
	private QueryIterator left;
	private QueryIterator right;
	private QueryIterator resultIterLeft;
	private QueryIterator resultIterRight;
	private List<Binding> resultLeft ;
	private List<Binding> resultRight;
	
	private boolean initerror = false;

	
	private Binding currentLeftBinding=null;
	private Binding currentRightBinding=null;
	private Binding nextBinding=null;
	private boolean moveLeft = true;

	public NestedLoopJoinIterator(ExecutionContext context, QueryIterator left, QueryIterator right) {
		super();
		this.context = context;
		this.left = left;
		this.right = right;
		
//		input = qIter;
	}

	private void init()  {

		if (initialized)
			return;

		ExecutorService executor = Executors.newFixedThreadPool(2);

		ArrayList<ParallelHelper> threadlist = new ArrayList<ParallelHelper>();

		threadlist.add(new ParallelHelper(left));
		threadlist.add(new ParallelHelper(right));

		for (ParallelHelper ph : threadlist) {
			executor.submit(ph);
		}

		
		executor.shutdown();
		
	//	System.out.println(this+" shutdown initiated");

		while (!executor.isTerminated()) {
			try {
			//	System.out.println(this+" waiting for threads to terminate");
				executor.awaitTermination(3600, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new ARQInternalErrorException(e);
			}
		}

	//	System.out.println(this+" terminated");
		
		boolean error = false;
		for (ParallelHelper h : threadlist) {
			if (h.isError()) {
				error=true;
				throw new ARQInternalErrorException(h.getException());
				
			}
		}

		if (!error) {
			try {
				resultIterLeft = threadlist.get(0).getResultIterator();
				resultLeft= threadlist.get(0).getResults();
			} catch (Exception e) {
				throw new ARQInternalErrorException(e);
			}
			try {
				resultIterRight = threadlist.get(1).getResultIterator();
				resultRight= threadlist.get(1).getResults();
			} catch (Exception e) {
				throw new ARQInternalErrorException(e);
			}
		} else initerror=true;
		/*else {
			resultIterLeft= new QueryIterNullIterator(context);
			resultIterRight= new QueryIterNullIterator(context);
			resultLeft=new ArrayList<Binding>();
			resultRight=new ArrayList<Binding>();
		}*/

		initialized = true;
	}

	// @Override
	protected boolean hasNextBinding() {
		
		if (isFinished()) return false;
		init();
		
		if (initerror) return false;
		
		if (nextBinding!=null) return true;
		
		
	
		while(doOuterLoop()) {
			while (doInnerLoop()) {
				
				Binding b= join(currentLeftBinding,currentRightBinding);
				if ( b != null ) {
					nextBinding = b;
					return true;
				}
			}
			
		}
		
		
		
		 return false ;
		
	}
	
	/**
	 * Joins two Bindings
	 * @param left
	 * @param right
	 * @return joined binding - or null if values for shared variables are not equal 
	 */
	public static Binding join(Binding left,
			Binding right) {
		
		BindingMap bm = new BindingMap();
		Iterator<Var> vars = left.vars();
		while (vars.hasNext()) {
			Var v = vars.next();
			if (right.contains(v) && !left.get(v).equals(right.get(v)) ) return null;
			bm.add(v, left.get(v));
		}
		
		vars = right.vars();
		while (vars.hasNext()) {
			Var v = vars.next();
			if (!left.contains(v)) bm.add(v, right.get(v));
		}
		

		return bm;
	}

	private boolean doOuterLoop() {
		if (moveLeft) {  // only move outer cursor if inner is at the end
				if (resultIterLeft.hasNext()) {
					currentLeftBinding = resultIterLeft.nextBinding();
					
					// reset inner/right
					resultIterRight=new QueryIterPlainWrapper(resultRight.iterator());
					
					
					return true;
				} else {
					nextBinding=null;
					return false;
				}
		} else return true;

	}
	
	private boolean doInnerLoop() {
		if (resultIterRight.hasNext()) {
			currentRightBinding=resultIterRight.nextBinding();
			moveLeft=false;  // we must not move the outer cursor 
			return true;
		} else {
			moveLeft=true;  // inner at the end, we can now move the outer cursor
			return false;
		}
	}
	
	// @Override
	protected Binding moveToNextBinding()  {
	
			init();
		
	
		   if ( initerror || !hasNext() )
	            throw new NoSuchElementException(this.getClass().getName()+".nextBinding") ;
	        
	        Binding ret = nextBinding ;
	        nextBinding = null ;
	        return ret ;
	}

	// @Override
	protected void closeIterator() {
		//input = null;
		
		left = null;
		right = null;
		resultIterLeft = null;
		resultIterRight = null;
		
		resultLeft=null;
		resultRight=null;

		initerror = false;
		initialized = false;

//		super.close();
	}

}

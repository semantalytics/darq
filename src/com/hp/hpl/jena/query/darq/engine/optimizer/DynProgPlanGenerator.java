package com.hp.hpl.jena.query.darq.engine.optimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.darq.core.Capability;
import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.BindJoin;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.Join;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.NestedLoopJoin;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.OperatorServiceGroup;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.PlanOperatorBase;

public class DynProgPlanGenerator implements OptimizedPlanGenerator {

	private double costs = -1;

	public static int blocksize = 5;
	
	List<PlanOperatorBase> candidates = new LinkedList<PlanOperatorBase>();
	HashMap<Integer,List<PlanOperatorBase>> CandidateSizeIndex = new HashMap<Integer, List<PlanOperatorBase>>();
	
	int finalPlanSize = 0;

	public PlanOperatorBase getCheapestPlan(List<ServiceGroup> sgs)
			throws PlanUnfeasibleException {

		finalPlanSize = sgs.size();
		
		
		for (ServiceGroup sg : sgs) {
			addCandidates(new OperatorServiceGroup(sg));
		}
		/*
		 * // build binary pairs List<ServiceGroup> tmp = new ArrayList<ServiceGroup>(sgs);
		 * while (!tmp.isEmpty()) { ServiceGroup sg1 = tmp.get(0);
		 * tmp.remove(sg1); for (ServiceGroup sg2:tmp) {
		 * candidates.addAll(enumerate(new OperatorServiceGroup(sg1), new
		 * OperatorServiceGroup(sg2))); } }
		 */
		candidates = prune(candidates);
		//System.out.println(candidates.size());

		for (int i = 2; i <= sgs.size(); i++) {

			List<PlanOperatorBase> tmp = new LinkedList<PlanOperatorBase>(
					candidates);
			while (!tmp.isEmpty()) {
				PlanOperatorBase p1 = tmp.iterator().next();
				tmp.remove(p1);
				
				List<PlanOperatorBase> tmp2 =  new LinkedList<PlanOperatorBase>(CandidateSizeIndex.get(i-p1.size()));
				tmp2.remove(p1);
				
				for (PlanOperatorBase p2 : tmp2) {
					List<PlanOperatorBase> toadd = enumerate(p1, p2);
					
					addCandidates(toadd);
		//			System.err.println(candidates.size());
		//			System.err.print(".");
				}

			//	System.err.println("Before Pruning: " + candidates.size());
				//candidates = prune(candidates);
		//		System.err.println("After Pruning: " + candidates.size());
			}
			candidates = prune(candidates);
	//		System.out.println(candidates.size());
		}

		PlanOperatorBase selectedPlan = selectPlan(candidates, sgs.size());
		
		candidates.clear();
		CandidateSizeIndex.clear();
		
	/*	System.out.println("Selected:");
		PrintVisitor.printPlan(selectedPlan);*/
		return selectedPlan;
	}

	private void addCandidates(PlanOperatorBase elt) {
		List<PlanOperatorBase> l= new LinkedList<PlanOperatorBase>();
		l.add(elt);
		addCandidates(l);
		
	}

	private void addCandidates(List<PlanOperatorBase> toadd) {
		candidates.addAll(toadd);
		
		// update index
		for (PlanOperatorBase pob:toadd) {
			List<PlanOperatorBase> list= CandidateSizeIndex.get(pob.getServiceGroups().size());
			if (list == null) { 
				list = new LinkedList<PlanOperatorBase>();
				CandidateSizeIndex.put(pob.getServiceGroups().size(),list);
			}
			list.add(pob);
		}
	}
	
	// 
	private void removeCandidates(List<PlanOperatorBase> toremove) {
		candidates.removeAll(toremove);
		
		//update index
		for (PlanOperatorBase pob:toremove) {
			List<PlanOperatorBase> list= CandidateSizeIndex.get(pob.getServiceGroups().size());
			if (list == null) { 
				System.err.println("Index out of sync!!");
			}
			list.remove(pob);
		}
	}
	
	private void removeCandidatesBySize(int i) {
		if (CandidateSizeIndex.get(i)!=null) {                  
			candidates.removeAll(CandidateSizeIndex.get(i));
			CandidateSizeIndex.remove(i);
		}
	}

	

	private PlanOperatorBase selectPlan(List<PlanOperatorBase> candidates, int n)
			throws PlanUnfeasibleException {
		PlanOperatorBase result = null;
		for (PlanOperatorBase pob : candidates) {
		//	PrintVisitor.printPlan(pob); System.out.println("----------");
			if (pob.getServiceGroups().size() == n) {
				if (result == null)
					result = pob;
				else if (result.getCosts() > pob.getCosts())
					result = pob;
			}
		}
		return result;
	}

	private List<PlanOperatorBase> enumerate(PlanOperatorBase p1,
			PlanOperatorBase p2) {

		List<PlanOperatorBase> result = new LinkedList<PlanOperatorBase>();

		if (p1.overlaps(p2))
			return result;

		Join j = null;

		if ( (p2 instanceof OperatorServiceGroup) && p1.joins(p2)) {
			if (p2.canBeRight()) {
				j = new BindJoin(p1, p2);
				try {
					j.getCosts();
					result.add(j);
				} catch (PlanUnfeasibleException e) {

				}
			}

			if (p1.canBeRight()) {
				j = new BindJoin(p2, p1);
				try {
					j.getCosts();
					result.add(j);
				} catch (PlanUnfeasibleException e) {

				}
			}
		}

		j = new NestedLoopJoin(p1, p2);
		try {
			j.getCosts();
			result.add(j);
		} catch (PlanUnfeasibleException e) {

		}

		/*
		 * j = new NestedLoopJoin(p2, p1); try { j.getCosts(); result.add(j); }
		 * catch (PlanUnfeasibleException e) {
		 *  }
		 */

	//	result=prune(result);

		return result;

	}

	private List<PlanOperatorBase> prune(List<PlanOperatorBase> candidates) {
	/* List<PlanOperatorBase> result = new LinkedList<PlanOperatorBase>(candidates); */

		List<PlanOperatorBase> tmp = new LinkedList<PlanOperatorBase>(
				candidates);
		Collections.sort(tmp);
		List<PlanOperatorBase> remove = new LinkedList<PlanOperatorBase>();
		PlanOperatorBase j1;
		
		int maxPlanSize = 0;
		while (!tmp.isEmpty()) {
			j1 = tmp.iterator().next();
			tmp.remove(j1);

			remove.clear();
			
			maxPlanSize = Math.max(maxPlanSize, j1.getServiceGroups().size());
			
			List<PlanOperatorBase> tmp2 = new LinkedList<PlanOperatorBase>(CandidateSizeIndex.get(j1.getServiceGroups().size()));
			tmp2.remove(j1);

			for (PlanOperatorBase j2 : tmp2) {
				if (j1.isCompatible(j2)) {
					remove.add(j2);
					// System.out.println("pruning: "+ j2.toString() +"
					// Keeping:" +j1.toString() );
				}
			}
			//result.removeAll(remove);
			removeCandidates(remove);
			tmp.removeAll(remove);
		}
		
/*		for (int i=maxPlanSize-2; i>finalPlanSize-(maxPlanSize-2);i--) {
			removeCandidatesBySize(i);
			System.out.println("removing " + i + "at " + maxPlanSize );
		}*/ 
		
		return candidates;

	}



	/**
	 * returns the costs of the last optimized plan, -1 if not yet optimized or
	 * optimization failed
	 */
	public double getCosts() {
		// TODO Auto-generated method stub
		return costs;
	}

	private List<ServiceGroup> getExpectedResult() {
		final Node varX = Node.createVariable("x");
		final Node varY = Node.createVariable("y");
		final Node varZ = Node.createVariable("z");
		final Node varO = Node.createVariable("o");

		final Node predP = Node.createURI("p");
		final Node predQ = Node.createURI("q");
		final Node predR = Node.createURI("r");
		final Node predS = Node.createURI("s");
		final Node predT = Node.createURI("t");
		final Node predU = Node.createURI("u");

		RemoteService service1;
		RemoteService service2;
		RemoteService service3;
		ServiceGroup sg1;
		List<Triple> tl1;
		MultipleServiceGroup sg2;
		List<Triple> tl2;
		ServiceGroup sg3;
		service1 = new RemoteService("service1", "service1", "service1", false);
		service1.addCapability(new Capability("p", "", 0.01, 0.02, 100));
		service1.addCapability(new Capability("q", "", 0.02, 0.1, 100));
		service1.addCapability(new Capability("r", "", 0.01, 0.01, 100));
		service1.addCapability(new Capability("s", "", 0.1, 0.1, 10));

		service2 = new RemoteService("service2", "service2", "service2", false);
		service2.addCapability(new Capability("t", "", 0.01, 0.1, 100));
		service2.addCapability(new Capability("u", "", 2.0 / 50.0, 0.1, 50));

		service3 = new RemoteService("service3", "service3", "service3", false);
		service3.addCapability(new Capability("t", "", 0.01, 0.1, 100));
		service3.addCapability(new Capability("u", "", 2.0 / 50.0, 0.1, 50));

		sg1 = new ServiceGroup(service1);
		sg2 = new MultipleServiceGroup();
		sg2.addService(service2);
		sg2.addService(service3);
		sg3 = new ServiceGroup(service3);

		tl1 = new ArrayList<Triple>();

		tl1 = new ArrayList<Triple>();
		tl1.add(new Triple(varX, predP, Node.createLiteral("test")));
		tl1.add(new Triple(varX, predQ, varY));
		sg1.setTriples(tl1);

		tl2 = new ArrayList<Triple>();
		tl2.add(new Triple(varY, predT, varZ));
		tl2.add(new Triple(varY, predU, varO));
		sg2.setTriples(tl2);

		tl2 = new ArrayList<Triple>();
		tl2.add(new Triple(varY, predU, varO));
		sg3.setTriples(tl2);

		List<ServiceGroup> sglist = new ArrayList<ServiceGroup>();
		/*
		 * sglist.add(sg1); sglist.add(sg2); sglist.add(sg3);
		 */

		for (int i = 0; i < 6; i++) {

			service1 = new RemoteService("servicei" + i, "service1" + i,
					"service1" + i, false);
			service1.addCapability(new Capability("p", "", 0.01, 0.02,
					100 * (10 - i)));
			service1.addCapability(new Capability("q", "", 0.02, 0.1, 100 * i));
			service1.addCapability(new Capability("r", "", 0.01, 0.01, 100));
			service1.addCapability(new Capability("s", "", 0.1, 0.1, 10));

			sg1 = new ServiceGroup(service1);
			tl1 = new ArrayList<Triple>();
			//tl1.add(new Triple(varX, predP, Node.createLiteral("test" + i)));
			//tl1.add(new Triple(varX, predQ, varY));
			tl1.add(new Triple(Node.createVariable("v"+i), predP, Node.createLiteral("test" + i)));
			tl1.add(new Triple(Node.createVariable("v"+i), predQ, Node.createVariable("v"+new Double(Math.abs((i-((Math.random()*2))))).intValue())));
			sg1.setTriples(tl1);
			sglist.add(sg1);
		}

		return sglist;
	}

	public static void main(String[] args) {
		DynProgPlanGenerator pg = new DynProgPlanGenerator();

		try {
			long end = 0;
			long start = System.currentTimeMillis();
			pg.getCheapestPlan(pg.getExpectedResult());
			end = System.currentTimeMillis();
			System.out.println("Time: "+ (end-start) +" ms");
		} catch (PlanUnfeasibleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

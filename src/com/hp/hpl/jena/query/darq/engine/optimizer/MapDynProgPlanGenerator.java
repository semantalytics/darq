package com.hp.hpl.jena.query.darq.engine.optimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.darq.core.Capability;
import com.hp.hpl.jena.query.darq.core.MapServiceGroup;
import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.MapBindJoin;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.MapJoin;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.MapNestedLoopJoin;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.MapOperatorServiceGroup;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.MapPlanOperatorBase;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.Union;

public class MapDynProgPlanGenerator implements MapOptimizedPlanGenerator {

//	HashMap<Integer,List<PlanOperatorBase>> CandidateSizeIndex = new HashMap<Integer, List<PlanOperatorBase>>();
	//enthält POBs in Abhängigkeit von der Anzahl der SG/RS, die sie beinhalten!!!
	List<MapPlanOperatorBase> candidates = new LinkedList<MapPlanOperatorBase>();
	HashMap<Integer,List<MapPlanOperatorBase>> CandidateSizeIndex = new HashMap<Integer, List<MapPlanOperatorBase>>();
	
	private double costs = -1;

	int finalPlanSize = 0;
	public static int blocksize = 5;
		
	public MapPlanOperatorBase getCheapestPlan(List<MapServiceGroup> sgs, HashMap<Triple,Integer> similarTripleList)
			throws PlanUnfeasibleException {

		finalPlanSize = sgs.size();

		/*
		 *  durchläuft alle SG/MSG
		 *  ereugt aus der SG einen Operator des Plan mit SG, Operator im Sinne von Operator + Operator
		 *  trägt alle POB/SG in candidates ein
		 *	zusätzlich wird der POB in einer Liste eingetragen, die POB mit der gleichen Größe enthält. Dei Größe wird als Index genutzt
		 *	und die Liste damit in einer Hashmap gespeichert
		 * @return MapPlanOperatorBase (sind aber Instanzen von Kindklasse MapOperatorServiceGroup
		 *  
		 */
		for (MapServiceGroup sg : sgs) { 
			addCandidates(new MapOperatorServiceGroup(sg)); 
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
		//entfernt Services die doppelt auftauchen

		for (int i = 2; i <= sgs.size(); i++) {

			List<MapPlanOperatorBase> tmp = new LinkedList<MapPlanOperatorBase>(
					candidates);//macht eine Kopie von Candidates
			while (!tmp.isEmpty()) {
				MapPlanOperatorBase p1 = tmp.iterator().next(); //holt sich ein POB aus tmp (Candidates)
				tmp.remove(p1); //löscht ihn aus 
				//Auf welcher Grundlage wird der Index berechnet? Anzahl SG/RS 
				//Was ist der Unterschied zwischen Candidates und CandidateSizeIndex?
				//Candidates = Liste von POB, CandidateSizeIndex = Hashmap(Index, Liste von POB) 
				//tmp2 holt sich also eine Liste von POB aus CSI. Wann und wie wird CSI befüllt? siehe addCandidates
				/*
				 * Idee: Geht los mit Liste von SGs (= Liste von POBs --> gleiche Anzahl) Verabeitet jede SG/POB mit jeder anderen SG/POB
				 * deshalb geht es auch erst ab 2 los, da man die erste Liste ja gleich nimmt und dann mit der zweiten vergleichen muss
				 * 
				 * Hat List von POB (tmp), erzeugt sich davon eine Kopie (tmp2)
				 * Nimm sich Element aus erster Liste (tmp) und kürzst diese Liste um dieses Element (weil es ja jetzt verarbeitet wird)
				 * holt sich aus der Hashmap Liste von POB mit bestimmter Größe. Löscht das POB, welches aus der ersten Liste stammt in 
				 * dieser Liste (sonst würde es mit sich selbst verarbeitet). 
				 * Durchläuft nun alle Elemente der Liste und macht enumerate! (Joins etc.)
				 * 
				 */
				List<MapPlanOperatorBase> tmp2 =  new LinkedList<MapPlanOperatorBase>(CandidateSizeIndex.get(i-p1.size()));
				//holt sich die POB Liste aus CSI, wieso i-p1.size? p1.size nicht richtig? Wieso läuft i bei 2 los
				//Vergleich erste Liste mit zweiter und ff. vergleichen
				//Wieso i-p1.size ? 
				tmp2.remove(p1);
				
				for (MapPlanOperatorBase p2 : tmp2) {
					List<MapPlanOperatorBase> toadd = enumerate(p1, p2,similarTripleList);
					
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

		MapPlanOperatorBase selectedPlan = selectPlan(candidates, sgs.size());

		candidates.clear();
		CandidateSizeIndex.clear();
		
	/*	System.out.println("Selected:");
		PrintVisitor.printPlan(selectedPlan);*/
		return selectedPlan;
	}

	/*
	 * erzeugt eine Liste aus dem übergebenen POB und ruft addCandidates(List) auf 
	 */
	private void addCandidates(MapPlanOperatorBase elt) {
		List<MapPlanOperatorBase> l= new LinkedList<MapPlanOperatorBase>();
		l.add(elt);
		addCandidates(l);
		
	}

	private void addCandidates(List<MapPlanOperatorBase> toadd) {
		candidates.addAll(toadd); //Liste wird zu Candidates hinzugefügt
		
		// update index
		//Was macht dieser Index?
		for (MapPlanOperatorBase pob:toadd) {//durchläuft Liste
			List<MapPlanOperatorBase> list= CandidateSizeIndex.get(pob.getServiceGroups().size());
			//Schaut, ob an dem Index (Anzahl der SG) schon eine Liste vorhanden ist
			if (list == null) { //nein, erzeugt an dieser Stelle neue Liste
				list = new LinkedList<MapPlanOperatorBase>();
				CandidateSizeIndex.put(pob.getServiceGroups().size(),list);//trägt Liste von POB(=SG=RS) ein 
			}
			list.add(pob); //ja, Liste an diesem Index wird POB angehangen
		}
	}
	
	// 
	private void removeCandidates(List<MapPlanOperatorBase> toremove) {
		candidates.removeAll(toremove);
		
		//update index
		for (MapPlanOperatorBase pob:toremove) {
			List<MapPlanOperatorBase> list= CandidateSizeIndex.get(pob.getServiceGroups().size());
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



	private MapPlanOperatorBase selectPlan(List<MapPlanOperatorBase> candidates, int n)
			throws PlanUnfeasibleException {
		MapPlanOperatorBase result = null;
		for (MapPlanOperatorBase pob : candidates) {
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

	//erhält 2 POBs
	private List<MapPlanOperatorBase> enumerate(MapPlanOperatorBase p1,MapPlanOperatorBase p2, HashMap<Triple,Integer> similarTripleMap) {

		List<MapPlanOperatorBase> result = new LinkedList<MapPlanOperatorBase>();
		//wenn P2 (SGs) in P1 (SG) enthalten ist, dann leere Linkedlist zurückgeben
		//Was ist der Sinn dahinter? 
		if (p1.overlaps(p2)) 
			return result;

		MapJoin j = null;
		//joins() liefert true, wenn beide gleiche gebundene Variablen enthalten
		//Das ist an dieser Stelle nicht mehr richtig, oder?
		//Wenn zwei Triple die gleichen gebundenen Variablen haben, können sie auch gleich sein, 
		//d.h. es ist ein similar Triple. Daraus folgt, dass das ausgeschlossen werden muss!
		//Dazu überprüfen, ob similarTripleLists (aus MapTransform) gleichen Index für beide Triples hat
		//Es muss PlanBaseOperator Join umgeschrieben werden. 
		//Die Frage ist, kann der Fall hier eintreten? 
		//joins stammt aus PlanOperatorBase (abstract class), nutzt getBoundVariables_() (abstract method), diese
		//ist in OperatorServiceGroup (als Implementierung von PlanOperatorBase) definiert, greift auf ServiceGroup.getUsedVariables
		//zurück. Wird also beim Erzeugen der SG festgelegt. Holt sich aus den Tripeln die Variablen raus.
		//Sind in einer SG mehrere Triples? Ja für SG, Nein für MSG
		//Kann es sich dabei um similarTriples handeln? Ja, grundsätzlich schon, jedoch scheint dies nicht sinnvoll, da
		//in einem RemoteService(Ontologie) keine equivalenten Subject/Prädikate/Objekte geben sollte?! Objekte eventuell schon
		// über SameAs (Annahme muss sein, dass Objekte nicht identisch sind,
		// wenn sie verschiedene URIs haben) //TODO Tex + Implementierung?
		// d.h. es gibt verschiedene Tripels für einen RS --> Dürfen die gejoint werden? Nein, es sind UNIONS, Was passiert bei einem Join?
		//Annahme bisher: wenn eine SG (RS) mehrere Tripels hat, kann der RS jedes dieser Tripel beantwortet, es werden nur
		//verschiedene Spalten abgefragt. Damit ist ein Join berechtigt. Hier muss also Unterscheidung aus similarTripel rein! 

		//In einer MSG ist (theoretisch) die Voraussetzung für mehrere Triples nicht gegeben, daher dürfte es keine Join geben! Das ist nicht
		//sichergestellt, da mehrere Triples in einer MSG zugelassen sind. 
		
		//Wieso sollte P2 keine Instanz von MapOperatorServiceGroup(MOSG) sein? Was gibt es da noch? Sollte eigentlich immer true sein, da aus
		//jeder SG eine MOSG gemacht wird (siehe oben)
		
		//Die Frage ist, ob ich similarTriple einfach vorher abfrage oder in die Klasse Join einbaue?
		//Es wäre richtiger, es in die Klasse join einzubauen, Achtung, erbenden Klassen, müssen ebenfalls geändert werden
		//FRAGE: Gibt es einen Grund, warum du nur p2 auf MOSG prüfst?
		//FRAGE: Warum wird nicht auch beim NL Join vorher mit joins geprüft?
		if ( (p2 instanceof MapOperatorServiceGroup) && p1.joins(p2)) {
			if (p2.canBeRight()) {
				j = new MapBindJoin(p1, p2);
				try {
					j.getCosts();
					result.add(j);
				} catch (PlanUnfeasibleException e) {

				}
			}

			if (p1.canBeRight()) {
				j = new MapBindJoin(p2, p1);
				try {
					j.getCosts();
					result.add(j);
				} catch (PlanUnfeasibleException e) {
					
				}
			}
		}

		j = new MapNestedLoopJoin(p1, p2);
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

		
		/*
		 * MapOperatorServiceGroup garantees a single SG with only one Triple
		 * checks if the triples belonging to a similarity group
		 */
		Union u = null;
		u = new Union(p1,p2); //TODO Implement
		if (p2 instanceof MapOperatorServiceGroup && p1 instanceof MapOperatorServiceGroup){
			if (p2.getServiceGroups().iterator().next().getSimilar() == p1.getServiceGroups().iterator().next().getSimilar()){
				try {
					//TODO Union
					u.getCosts();
					result.add(u);
				} catch (PlanUnfeasibleException e) {}
			}
		}
			
		
		return result;
	}
	
 /*
  * entfernt doppelte SG (RS)
  */
	private List<MapPlanOperatorBase> prune(List<MapPlanOperatorBase> candidates) {
	/* List<PlanOperatorBase> result = new LinkedList<PlanOperatorBase>(candidates); */

		List<MapPlanOperatorBase> tmp = new LinkedList<MapPlanOperatorBase>(
				candidates);
		Collections.sort(tmp);
		List<MapPlanOperatorBase> remove = new LinkedList<MapPlanOperatorBase>();
		MapPlanOperatorBase j1;
		
		int maxPlanSize = 0;
		while (!tmp.isEmpty()) { //solange es POB(de facto SG/MSG) gibt
			j1 = tmp.iterator().next();//j1 ist POB aus der Liste
			tmp.remove(j1);//J1 wird aus der Templiste gelöscht

			remove.clear();//wird nur als Zwischenspeicher genutzt um mehrere Elemente zu löschen
			
			maxPlanSize = Math.max(maxPlanSize, j1.getServiceGroups().size());//holt sich Anzahl der SG (=RS) von POB?!
			//maxPlanSize for subplans which means subplan for SG (RS)
			//Was zur Hölle macht diese Liste? Get liefert Wert zum Key (j1.getServicesGroups().size())
			//holt sich den POB aus CandidateSizeIndex
			List<MapPlanOperatorBase> tmp2 = new LinkedList<MapPlanOperatorBase>(CandidateSizeIndex.get(j1.getServiceGroups().size()));
			tmp2.remove(j1);
			//erstellt Liste mit der Größe der Anzahl der SG im POB
			//offensichtlich Liste mit Elementen von POB //FRAGE Was ist da drin?
			//Wahrscheinlich, dass ein SG(RS) mehrmals vorkommt, dann wird er rausgeschmissen
			for (MapPlanOperatorBase j2 : tmp2) {
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

	public double getCosts() {
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

//	public static void main(String[] args) {
//		MapDynProgPlanGenerator pg = new MapDynProgPlanGenerator();
//
//		try {
//			long end = 0;
//			long start = System.currentTimeMillis();
//			pg.getCheapestPlan(pg.getExpectedResult()); //an dieser Stelle muss noch similarTripleMap eingefügt werden 
//			end = System.currentTimeMillis();
//			System.out.println("Time: "+ (end-start) +" ms");
//		} catch (PlanUnfeasibleException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	}
//

}

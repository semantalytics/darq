package com.hp.hpl.jena.query.darq.engine.optimizer.planoperators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.darq.core.MapMultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.MapServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.engine.optimizer.PlanUnfeasibleException;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.plan.PlanGroup;
import com.hp.hpl.jena.query.util.Context;

public class Union extends MapPlanOperatorBase {
	
	private MapPlanOperatorBase unionElement1;
	private MapPlanOperatorBase unionElement2;


	public Union(MapPlanOperatorBase p1, MapPlanOperatorBase p2) {
		this.unionElement1 = p1;
		this.unionElement2 = p2;
	}
	
	//Keine Ahnung, was diese Methode aussagt
	//FRAGE Nur wichtig für Join?
	@Override
	public boolean canBeRight() {
		return false;
	}

	@Override
	public Set<String> getBoundVariables_() {
		Set<String> bound = new HashSet<String>(unionElement1.getBoundVariables());
		bound.addAll(unionElement2.getBoundVariables());
		return bound;
	}

	/*
	 * (non-Javadoc)
	 * @see com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.MapPlanOperatorBase#getCosts_()
	 * Kostenabschätzung für Union 2 * CR + |R(unionElement1)| * CT + |R(unionElement2)| * CT
	 * 2 Anfragen + Anzahl der Ergebnistripels aus Abfrage 1 + Anzahl der Ergebnistripels aus Abfrage 2   
	 */
	@Override
	public double getCosts_() throws PlanUnfeasibleException {
		return 2* CR + unionElement1.getResultsize() * CT + unionElement1.getResultsize() * CT;
	}

	/*
	 * (non-Javadoc)
	 * @see com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.MapPlanOperatorBase#getResultsize(java.util.Set)
	 * Abschätzung der Größe des Resultset.
	 * Was ist mit MultipleServiveGroup?
	 * 2 Ideen
	 * A: über die Elemente von Union kommt man alle notwendigen Dinge ran und berechnet daraus den Wert
	 *    es muss noch für MultipleServiceGroup gemacht werden bzw. alle Fälle in denen eine MSG vorkommt
	 *    SG-SG, MSG - SG, MSG-MSG, SG - MSG (private schreiben) 
	 * B: MapOperatorServiceGroup bietet bereits eine Kostenberechnung
	 *    Wie kommt man daran? Wahrscheinlich nur, wenn man MOSG übergibt, d.h. viele umschreiben, ggf. auf superclass casten, wenn geht
	 *      
	 *    Da es sich eigentlich immer um MapOperateServiceGroups handeln sollte, kommt die
	 *    zweite Betrachtung gar nicht in Betracht (theoretisch). Es stellt auch die allgemeinere
	 *    Schätzung auf. 
	 */
	@Override
	public double getResultsize(Set<String> boundVariables) throws PlanUnfeasibleException {
		double size1, size2;
		
		//B
		if ((unionElement1 instanceof MapOperatorServiceGroup) && (unionElement2 instanceof MapOperatorServiceGroup)){
			size1 = ((MapOperatorServiceGroup) unionElement1 ).getResultsize();
			size2 = ((MapOperatorServiceGroup) unionElement2 ).getResultsize();
		}
		else{
		//A
		
		if (unionElement1.getServiceGroups() instanceof MapServiceGroup ){
			if (unionElement2.getServiceGroups() instanceof MapServiceGroup){
				size1 = getResultsizeSG(unionElement1);
				size2 = getResultsizeSG(unionElement2);
			}
			else{
				size1 = getResultsizeSG(unionElement1);
				size2 = getResultsizeMSG(unionElement2);
			}
		}
		else{
			if (unionElement2.getServiceGroups()instanceof MapServiceGroup){
				size1 = getResultsizeMSG(unionElement1);
				size2 = getResultsizeSG(unionElement2);	
			}
			else{
				size1 = getResultsizeMSG(unionElement1);
				size2 = getResultsizeMSG(unionElement2);
			}
		}
		}
		return size1 + size2;
	}

	/*
	 * estimates the size of the result with calcSelectivity (RemoteService)
	 * for a single RemoteService
	 */
	private double getResultsizeSG(MapPlanOperatorBase unionElement){
		MapServiceGroup sg =unionElement.getServiceGroups().iterator().next();
		RemoteService rs = sg.getService();
		List<Triple> triples = new ArrayList<Triple>();
		triples.add(sg.getTriple());
		Set<String> boundVariables = unionElement.getBoundVariables();
		return sg.getService().getSelectivityFunction().calcSelectivity(triples, boundVariables, rs);
	}
	
	/*
	 * estimates the size of the result with calcSelectivity (RemoteService)
	 * for  multiple RemoteServices
	 */
	private double getResultsizeMSG(MapPlanOperatorBase unionElement){
		double size = 0;
		MapMultipleServiceGroup msg = (MapMultipleServiceGroup) unionElement.getServiceGroups();
		List<Triple> triples = new ArrayList<Triple>();
		triples.add(msg.getTriple());
		Set<String> boundVariables = unionElement.getBoundVariables();
		
		for (RemoteService rs: msg.getServices()){
			size+=rs.getSelectivityFunction().calcSelectivity(triples, boundVariables, rs);
		}
		return size;
		}
	
	//FRAGE wofür?
	@Override
	public Set<MapServiceGroup> getServiceGroups_() {
		Set<MapServiceGroup> sgs = new HashSet<MapServiceGroup>(unionElement1.getServiceGroups());
		sgs.addAll(unionElement2.getServiceGroups());
		return sgs;
	}

	//Keine Ahnung, wozu das benötigt wird FRAGE
	//die SGs müssen gleich sein, dann compatible
	//muss man hier jetzt nicht auch similarTriple betrachten?!
	@Override
	public boolean isCompatible(MapPlanOperatorBase pob) {
		if (pob.getServiceGroups().containsAll(this.getServiceGroups()) && this.getServiceGroups().containsAll(pob.getServiceGroups()))
			return true;
		else
			return false;
	}

	@Override
	public PlanElement toARQPlanElement(Context context) {
		List<PlanElement> l= new ArrayList<PlanElement>();
		l.add(getunionElement1().toARQPlanElement(context));
		l.add(getunionElement2().toARQPlanElement(context));
		return PlanGroup.make(context, l, false);
	}

	@Override
	public void visit(MapPlanOperatorVisitor visitor) {
		visitor.visit(this);

	}

	public MapPlanOperatorBase getunionElement1() {
		return unionElement1;
	}

	public void setunionElement1(MapPlanOperatorBase p1) {
		this.unionElement1 = p1;
	}

	public MapPlanOperatorBase getunionElement2() {
		return unionElement2;
	}

	public void setunionElement2(MapPlanOperatorBase p2) {
		this.unionElement2 = p2;
	}

}

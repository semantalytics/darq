package com.hp.hpl.jena.query.darq.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.expr.Expr;

/*
 * Idee: pro SG darf nur ein Triple pro ähnlicher Gruppe sein, das muss in MapSG sichergestellt werde
 * Problem dahinter ist, dass joins durchgeführt werden anhand von boundVariables. Das führt zu Problemen, 
 * wenn mehrere Triples gleiche Daten aber unterschiedliche Eingrenzungen von einem RS abfragen.  
 *
 *   3. Wo kommt SetTriples zum Einsatz? Muss ebenfalls angepasst werden (ist nur Vereinfachung zum Testen, daher gelöscht)
 */

public class MapServiceGroup {
	
	private Integer similar = null;
	Node sub = Node.NULL;
	Node pred = Node.NULL;
	Node obj = Node.NULL;
	private Triple triple = new Triple(sub, pred, obj);
	private List<Expr> filters = new ArrayList<Expr>();
	private RemoteService service;
	private Set<String> usedVariables = new HashSet<String>();

	
	public MapServiceGroup(RemoteService s) {
		service = s;
	}

	public RemoteService getService() {
        return service;
    }
	
	public void addB(Triple t, Integer similarGroup) {
	
		if (similar == null) {
			triple = t; 
			similar = similarGroup;
			buildUsedVariabels(t);
		} else {
			try{
			 throw new Exception("Error [MAPSERVICEGROUP]: Already one similar triple added, no more allowed.");
			}
			catch (Exception e){
				e.printStackTrace();
			}
		}
	}

	
	private void buildUsedVariabels(Triple t) {
		
		if (t.getObject().isConcrete()) {
			// object is bound
			// predicatesWithBoundObjects.add(t.getPredicate().getURI());

		} else {
			// it's a variable
			usedVariables.add(t.getObject().getName());
		}

		if (t.getSubject().isVariable()) {
			usedVariables.add(t.getSubject().getName());
		}
	}

	
	public Set<Set<RequiredBinding>> requiredBindings() {
		return service.getRequiredBindings();
	}

	
	public boolean checkInput(Set<String> boundVariables) {

		if (service.getRequiredBindings().size() == 0)
			return true;

		Set<String> predicatesWithBoundObjects = new HashSet<String>();
		Set<String> predicatesWithBoundSubjects = new HashSet<String>();

		Triple t = triple;
		Node o = t.getObject();
		Node s = t.getSubject();

		if (o.isConcrete() || (o.isVariable() && boundVariables.contains(o.getName())))
			predicatesWithBoundObjects.add(t.getPredicate().getURI());
		if (s.isConcrete() || (s.isVariable() && boundVariables.contains(s.getName())))
			predicatesWithBoundSubjects.add(t.getPredicate().getURI());

		for (Set<RequiredBinding> bs : service.getRequiredBindings()) {
			boolean tmpresult = true;
			for (RequiredBinding rb : bs) {
				switch (rb.getType()) {
				case RequiredBinding.OBJECT_BINDING:
					if (!predicatesWithBoundObjects.contains(rb.getPredicateString()))
						tmpresult = false;
					break;
				case RequiredBinding.SUBJECT_BINDING:
					if (!predicatesWithBoundSubjects.contains(rb.getPredicateString()))
						tmpresult = false;
					break;
				default:
					break;
				}
				if (tmpresult)
					return true;
			}
		}
		return false;
	}


	/**
	 * 
	 * @param c
	 *            Filter to add
	 * @return false if the variables in the filter are not a subset of the
	 *         variables in the statements in this group.
	 */
	public boolean addFilter(Expr c) {

		if (filters.contains(c))
			return true;
		Set<String> filtervars = new HashSet<String>();
		c.varsMentioned(filtervars);
		// is filtervars a subset of usedvars ?
		filtervars.removeAll(usedVariables);
		if (filtervars.size() == 0) {
			filters.add(c);
			return true;
		}
		return false;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public MapServiceGroup clone() {
		MapServiceGroup sg = new MapServiceGroup(service);
		sg.triple = new Triple(this.triple.getSubject(), this.triple.getPredicate(), this.triple.getObject());
		sg.filters = new ArrayList<Expr>(this.filters);
		sg.usedVariables = new HashSet<String>(usedVariables);
		return sg;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MapServiceGroup) {
			MapServiceGroup otherGroup = (MapServiceGroup) obj;
			if (service.equals(otherGroup.service) && triple.equals(otherGroup.triple) && filters.equals(otherGroup.filters))
				return true;
		}
		return false;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return service.getUrl().hashCode() ^ triple.hashCode() ^ filters.hashCode();
	}

	
	public Triple getTriple() {
		return triple;
	}

	
	public void setTriple(Triple triple) {
		this.triple = triple;
	}
	
	public void setTriples(List<Triple> ListTriple) {
		if (ListTriple.size() == 1){
			setTriple(ListTriple.get(ListTriple.size()-1));
			System.err.println("Warning [MAPSERVICEGROUP]: Use setTriple instead of setTriples!");
		}
		else{
			try{
				 throw new Exception("Error [MAPSERVICEGROUP]: Just one triple per (multiple) service group allowed, use setTriple!");
			}
			catch (Exception e){
				e.printStackTrace();
			}
		}
	}

	
	public Set<String> getUsedVariables() {
		return usedVariables;
	}

	
	public void setUsedVariables(Set<String> usedVariables) {
		this.usedVariables = usedVariables;
	}

	
	public List<Expr> getFilters() {
		return filters;
	}

	
	public void setFilters(List<Expr> filters) {
		this.filters = filters;
	}

	
	public Integer getSimilar() {
		return similar;
	}

	
	public void setSimilar(Integer similar) {
		this.similar = similar;
	}
}

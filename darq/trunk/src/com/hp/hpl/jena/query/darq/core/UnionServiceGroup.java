package com.hp.hpl.jena.query.darq.core;

import java.awt.font.MultipleMaster;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultTreeModel;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.expr.Expr;

public class UnionServiceGroup extends MultipleServiceGroup {
	
	/* Idea:
	 *- stores (M)SG where the answer is similar to the original (M)SG
	 *- used to do the union between the resultsets
	 */
	
	
	private int similar;
	private DefaultTreeModel tree;
	
	private HashMap<Triple,ServiceGroup> serviceGroups = new HashMap<Triple,ServiceGroup>();
    /* Vorsicht: Tricky ein Triple kann nur eine SG haben, aber einer SG k�nnen mehrere Triple zugeordnet sein! (insbesondere bei MSG) */ 
		
	/* FRAGE brauche ich similar �berhaupt? Schlie�lich gehen alle similarTriples 
	 * in eine USG, damit sollte es doch egal sein.  
	 * Antwort: Ja, es kann mehrere USGs geben mit verschiedenen similar Gruppen*/
	
	public UnionServiceGroup(Triple triple, ServiceGroup servicegroup, int similar) {
		serviceGroups.put(triple, servicegroup);  
		this.similar = similar;
	}
	 
	public UnionServiceGroup(Triple triple, MultipleServiceGroup servicegroup, int similar) {
        serviceGroups.put(triple, servicegroup);  
        this.similar = similar;
	}
	
	public UnionServiceGroup(Triple triple, MultiplyServiceGroup servicegroup, int similar) {
        serviceGroups.put(triple, servicegroup);  
        this.similar = similar;
	}
	
	public UnionServiceGroup(Triple triple, MultiplyMultipleServiceGroup servicegroup, int similar) {
        serviceGroups.put(triple, servicegroup);  
        this.similar = similar;
	}
	
	private UnionServiceGroup(HashMap<Triple, ServiceGroup> serviceGroups, int similar){
		this.serviceGroups = serviceGroups;
		this.similar = similar;
	}
	
	public void addServiceGroup(Triple triple, ServiceGroup servicegroup){
		serviceGroups.put(triple, servicegroup);
	}
	
	public void addServiceGroup(Triple triple, MultipleServiceGroup servicegroup){
		serviceGroups.put(triple, servicegroup);
	}
	
	public void addServiceGroup(Triple triple, MultiplyServiceGroup servicegroup){
		serviceGroups.put(triple, servicegroup);
	}
	
	public void addServiceGroup(Triple triple, MultiplyMultipleServiceGroup servicegroup){
		serviceGroups.put(triple, servicegroup);
	}
	
	 
	/* As there are only constructors with SGs, there always is a SG, otherwise this 
	   object would not exist.*/
	public Set<String> getUsedVariables() {
		Set<String> usedVariables = new HashSet<String>();
		for (ServiceGroup sg : serviceGroups.values()){
			usedVariables.addAll(sg.getUsedVariables());
		}
		return usedVariables;
	}
	
	public boolean containsTriple(Triple t){
		if (serviceGroups.get(t) == null) return false;	
		return true;
	}
	
	@Override
	public UnionServiceGroup clone(){
		 UnionServiceGroup usg = new UnionServiceGroup(serviceGroups,similar);
		 return usg;
	}
	
	public int getSimilar() {
		return similar;
	}

	public void setSimilar(int similar) {
		this.similar = similar;
	}

	public ServiceGroup getServiceGroup(Triple t){
		return serviceGroups.get(t);
	}

	/* SG kann auch MSG sein!  */ 
	/* Logik: In serviceGroups k�nnen sich auch MSGs befinden. 
	 * Diese erzeugen einen Fehler bei getService(), daher m�ssen
	 * sie ausgeschlossen werden. Da eine MSG aber auch eine SG ist
	 * und instanceof keine Negation kennt, muss es in den else Zweig */

	/* Idee:  liefert in jedem Fall nur eine (Multiply) SG aus der USG zur�ck
	 * (filtert praktisch die (Multiply) SGs aus der USG), f�r (Multiply) MSG gibt es 
	 * msg.getServiceGroup, die dazu ein passende SG 
	 * bastelt 
	 */ 
	public ServiceGroup getServiceGroup(RemoteService s){
		for( ServiceGroup sg :  serviceGroups.values()){
			if (sg instanceof MultipleServiceGroup || sg instanceof MultiplyMultipleServiceGroup){}
			else{
				if (sg.getService().equals(s)) return sg;
			}
		}
		return null;
	}
	
	public HashMap<Triple, ServiceGroup> getServiceGroups() {
		return serviceGroups;
	}

	public void setServiceGroups(HashMap<Triple, ServiceGroup> serviceGroups) {
		this.serviceGroups = serviceGroups;
	}
	
	/* checks if a SG is contained in this USG by calling
	 * equal method of (M)SG for every (M)SG of the USG 
	 */
	public boolean containsServiceGroup(ServiceGroup sg){
		MultipleServiceGroup otherMSG=null;
		MultiplyServiceGroup otherMuSG=null;
		MultiplyMultipleServiceGroup otherMuMSG=null; 
		
		if (sg instanceof MultipleServiceGroup) {
			otherMSG = (MultipleServiceGroup) sg;
		}
		if (sg instanceof MultiplyServiceGroup) {
			otherMuSG = (MultiplyServiceGroup) sg;
		}
		if (sg instanceof MultiplyMultipleServiceGroup) {
			otherMuMSG = (MultiplyMultipleServiceGroup) sg;
		}
		
		for(ServiceGroup serviceGroup : serviceGroups.values()){
			if (serviceGroup instanceof MultipleServiceGroup){
					MultipleServiceGroup msg = (MultipleServiceGroup)serviceGroup;
				if (msg.equals(otherMSG)) return true;
			}
			if (serviceGroup instanceof MultiplyServiceGroup){
				MultiplyServiceGroup muSG = (MultiplyServiceGroup) sg;
				if(muSG.equals(otherMuSG)) return true;
			}
			if (serviceGroup instanceof MultiplyMultipleServiceGroup) {
				MultiplyMultipleServiceGroup muMSG = (MultiplyMultipleServiceGroup) serviceGroup;
				if(muMSG.equals(otherMuMSG)) return true;
			}
			else{				
				if (serviceGroup.equals(sg)) return true;
			}
		}
		return false;
	}
	
	@Override
	@Deprecated
	public  RemoteService getService(){
		 throw new UnsupportedOperationException("this is a UnionServiceGroup - use getServiceGroups");
	}
	
	@Override
	@Deprecated
	public  Set<RemoteService> getServices(){
		 throw new UnsupportedOperationException("this is a UnionServiceGroup - use getServiceGroups");
	}
	
	@Override
	@Deprecated
	public boolean addFilter(Expr c) {

     throw new UnsupportedOperationException("this is a UnionServiceGroup - use methods from (Multiple)ServiceGroup within the UnionServiceGroup");
       
   }

	@Override
	@Deprecated
	public boolean addFilters(List<Expr> lc) {
		throw new UnsupportedOperationException("this is a UnionServiceGroup - use methods from (Multiple)ServiceGroup within the UnionServiceGroup");
	}
   
	@Override
	@Deprecated
	public List<Expr> getFilters() {
		throw new UnsupportedOperationException("this is a UnionServiceGroup - use methods from (Multiple)ServiceGroup within the UnionServiceGroup");
	}

	@Override
	@Deprecated
	public void setFilters(List<Expr> c) {
		throw new UnsupportedOperationException("this is a UnionServiceGroup - use methods from (Multiple)ServiceGroup within the UnionServiceGroup");
	}
	
	
	@Override
    public boolean equals(Object obj) {
        if (obj instanceof UnionServiceGroup) {
            UnionServiceGroup otherGroup = (UnionServiceGroup) obj;
            
            /* containing different amount of (M)SG therefore can not be equal */
            if (otherGroup.serviceGroups.size() != serviceGroups.size()) return false;
            
            for (ServiceGroup serviceGroup :  otherGroup.getServiceGroups().values()){
            	/* checks RemoteServices */
            	if (!containsServiceGroup(serviceGroup)) return false;
//            	/* checks Filters - no Filters allowed in USG*/
//            	if  (getFilters().equals(serviceGroup.getFilters())) return false;
            }
            /* checks Triples */
            if (!serviceGroups.keySet().equals(otherGroup.serviceGroups.keySet())) return false;
        } 
        return true;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
    	int hc = 0;
    	
    	for (ServiceGroup sg : serviceGroups.values()){
    		if (sg instanceof MultipleServiceGroup) {
				MultipleServiceGroup msg = (MultipleServiceGroup) sg;
				hc = hc ^ msg.getTriples().hashCode() ^ msg.getFilters().hashCode();
			}
    		if (sg instanceof MultiplyServiceGroup) {
				MultiplyServiceGroup muSG = (MultiplyServiceGroup) sg;
				hc = hc ^ muSG.hashCode(); 
			}
    		if (sg instanceof MultiplyMultipleServiceGroup) {
				MultiplyMultipleServiceGroup muMSG = (MultiplyMultipleServiceGroup) sg;
				hc = hc ^ muMSG.hashCode();
			}
    		else{
    			hc = hc ^ sg.getTriples().hashCode() ^ sg.getFilters().hashCode();
    		}
    	}
        return hc;
    }
	
    public void output() {
    	System.out.println("USG");
    	System.out.println("  similar: " + similar);
    	System.out.println("  Filter: no Filters allowed");
		
    	
		for(ServiceGroup sg : serviceGroups.values()){
    		
			if (sg instanceof MultipleServiceGroup) {
				MultipleServiceGroup msg = (MultipleServiceGroup) sg;
				System.out.println("    MSG");
				for (Triple triple : msg.getTriples()){
					System.out.println("     Triple: " + triple.toString());
				}
				for(RemoteService rs : msg.getServices()){
					System.out.println("     RemoteService: " + rs.getUrl());
				}
				java.util.List<Expr> filtermsg  = msg.getFilters();
				if (filtermsg.isEmpty()) System.out.println("     No Filters.");
				for(Expr f : msg.getFilters()){
					System.out.println("     Filter: "+ f);
				}
			}
    		
			else if (sg instanceof MultiplyServiceGroup) {
				MultiplyServiceGroup muSG = (MultiplyServiceGroup) sg;
				System.out.println("   muSG");
    			for (Triple triple : muSG.getTriples()){
					System.out.println("     Triple: " + triple.toString());
				}
    			System.out.println("     RemoteService: "+ muSG.getService().getUrl());
    			
    			java.util.List<Expr> filtersg  = muSG.getFilters();
				if (filtersg.isEmpty()) System.out.println("     No Filters.");
				for(Expr f : muSG.getFilters()){
					System.out.println("     Filter: "+ f);
				}
    		}
    		
			else if (sg instanceof MultiplyMultipleServiceGroup) {
				MultiplyMultipleServiceGroup muMSG = (MultiplyMultipleServiceGroup) sg;
				System.out.println("    MSG");
				for (Triple triple : muMSG.getTriples()){
					System.out.println("     Triple: " + triple.toString());
				}
				for(RemoteService rs : muMSG.getServices()){
					System.out.println("     RemoteService: " + rs.getUrl());
				}
				java.util.List<Expr> filtermsg  = muMSG.getFilters();
				if (filtermsg.isEmpty()) System.out.println("     No Filters.");
				for(Expr f : muMSG.getFilters()){
					System.out.println("     Filter: "+ f);
				}
			}
    		
			else { /* have to be a SG */ 
    			System.out.println("   SG");
    			for (Triple triple : sg.getTriples()){
					System.out.println("     Triple: " + triple.toString());
				}
    			System.out.println("     RemoteService: "+ sg.getService().getUrl());
    			
    			java.util.List<Expr> filtersg  = sg.getFilters();
				if (filtersg.isEmpty()) System.out.println("     No Filters.");
				for(Expr f : sg.getFilters()){
					System.out.println("     Filter: "+ f);
				}
    		}
    	}
    }

	public DefaultTreeModel getTree() {
		return tree;
	}

	public void setTree(DefaultTreeModel tree) {
		this.tree = tree;
	}
	
}

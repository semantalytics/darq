package de.hu_berlin.informatik.wbi.darq.mapping;

import java.net.URI;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RulePart {

	private URI uri;
	//URI of the part of the rule, e.g. rule of the class
	private String part;
	//Part of the rule, Body or Head
	private ArrayList<String> boundVariables = new ArrayList<String>();
	//bound Variables of the rule ?x, ?y
	private String type;
	//Class, ObjectProperty, DataTypeProperty, Individual
	private URI ruleURI;
	//URI of the rule
	private double multiplier;
	
	Log log = LogFactory.getLog(RulePart.class);
	
	/**
	 * Object which is a part of rule, 
	 * @param uri URL of this part of the rule
	 * part = body (b) or head (h)
	 * @param ruleURI = URI of the rule
	 * type = type of the rulepart e.g. Class, Property, Builtin
	 */
	public RulePart(URI uri, URI ruleURI, String part){
		this.ruleURI= ruleURI;
		this.uri = uri;
		this.part = part;
	}
	
	public boolean isEmpty(){
		if (ruleURI == null && uri == null && part  == null){
			return true;
		}else return false;
	}
	/**
	 *  URI of the rule part 
	 * */
	public URI getUri() {
		return uri;
	}

	public String getPart() {
		return part;
	}
	
	public boolean isBody(){
		if (this.part == "b") return true; 
		else return false;
	}
	
	public boolean isHead(){
		if (this.part == "h") return true; 
		else return false;
	}

	/**
	 * Make the rule part to body or head of the rule
	 * @param part
	 */
	public void setPart(String part) {
		if ((part == "h") || (part == "b")){
			this.part = part;
		}
		else{
			log.error("Use 'h' for head and 'b' for body.");
		}
	}

	public ArrayList<String> getBoundVariables() {
		return boundVariables;
	}

	public void setBoundVariables(ArrayList<String> boundVariables) {
		this.boundVariables = boundVariables;
	}
 /**
  * 
  * @return Type of the part, e.g. Class, OWLDataProperty etc. see OWL Reference at W3C
  */
	public String getType() {
		return type;
	}
	 
	/**
	  * 
	  * @return set Type of the part, e.g. Class, OWLDataProperty etc. see OWL Reference at W3C
	  */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * 
	 * @return the URI of the rule this part belongs to
	 */
	public URI getRuleURI() {
		return ruleURI;
	}	
	
	public RulePart clone(){
		URI uri,ruleURI;
		String part, type;
		ArrayList<String> boundVariables = new ArrayList<String>();
		double multiplier; 
		uri = this.uri;
		ruleURI = this.ruleURI;
		part = this.part;
		boundVariables = this.boundVariables;
		type = this.type;
		multiplier = this.multiplier;
		RulePart rulePart = new RulePart(uri, ruleURI, part);
		rulePart.setType(type);
		rulePart.setBoundVariables(boundVariables);
		rulePart.setMultiplier(multiplier);
		return rulePart;
	}

	/**
	 * 
	 * @return Multiplier for swrlb:multiply rules
	 */
	public double getMultiplier() {
		return multiplier;
	}

	/**
	 * 
	 * @param set multiplier for swrlb:multiply rules
	 */
	public void setMultiplier(double multiplier) {
		this.multiplier = multiplier;
	}
}

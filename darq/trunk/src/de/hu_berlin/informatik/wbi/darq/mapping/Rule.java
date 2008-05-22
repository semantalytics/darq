package de.hu_berlin.informatik.wbi.darq.mapping;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Rule{
	
	private final String SWRL_MULTIPLY = "swrl_multiply";
	private final String SWRL_STRINGCONCAT = "swrl_stringconcat";
	private ArrayList<RulePart> rulePartList;
	private String type = new String();
	private URI ruleURI;
	
	public Rule(URI uri){
		ruleURI = uri;
		rulePartList = new ArrayList<RulePart>();
	}

	public Rule(URI uri, RulePart part){
		ruleURI = uri;
		rulePartList = new ArrayList<RulePart>();
		rulePartList.add(part);
	}

	public Rule(URI uri, ArrayList<RulePart> rulePartList){
		ruleURI = uri;
		this.rulePartList = new ArrayList<RulePart>();
		this.rulePartList.addAll(rulePartList);
	}

	public void addPart(RulePart rulePart){
		rulePartList.add(rulePart);
	}
	public  ArrayList<RulePart> getRulePartList() {
		return rulePartList;
	}

	public void setRulePartList(ArrayList<RulePart> rulePartList) {
		this.rulePartList = rulePartList;
	}
	
	public URI getRuleURI() {
		return ruleURI;
	}

	public void setRuleURI(URI ruleURI) {
		this.ruleURI = ruleURI;
	}
	
	public void setMultiply(){
		type = SWRL_MULTIPLY;
	}
	public void setStringConcat(){
		type = SWRL_STRINGCONCAT;
	}
	
	public boolean isMultiply(){
		if(type.equals(SWRL_MULTIPLY)) return true;
		else return false;
	}
	
	public boolean isStrincConcat(){
		if(type.equals(SWRL_STRINGCONCAT)) return true;
		else return false;
	}
	
	public boolean containsPart(URI part){
		for(RulePart rulePart : rulePartList){
			if(rulePart.getUri().equals(part)) return true;
		}
		return false;
	}
	
	public RulePart getPart(URI part){
		for(RulePart rulePart : rulePartList){
			if(rulePart.getUri().equals(part)) return rulePart;
		}
		return null;
	}
	
	public Set<RulePart> getHeadParts(){
		HashSet<RulePart> headParts = new HashSet<RulePart>();
		for(RulePart rulePart : rulePartList){
			if(rulePart.isHead()) headParts.add(rulePart);
		}
		return headParts;
	}
	
	public Set<RulePart> getBodyParts(){
		HashSet<RulePart> bodyParts = new HashSet<RulePart>();
		for(RulePart rulePart : rulePartList){
			if(rulePart.isHead()) bodyParts.add(rulePart);
		}
		return bodyParts;
	}
}

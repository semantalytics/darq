package de.hu_berlin.informatik.wbi.darq.cache;

import java.io.Serializable;

public class TripleStringURI implements Serializable {

	private String subject, predicate, object;

	public TripleStringURI(String subject, String predicate, String object) {
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	public String getSubject() {
		return subject;
	}

	public String getPredicate() {
		return predicate;
	}

	public String getObject() {
		return object;
	}

	public boolean equals(Object obj) {
		boolean s, p, o;
		s = false;
		p = false;
		o = false;
		if (obj instanceof TripleStringURI) {
			TripleStringURI tripleStringURI = (TripleStringURI) obj;
			if (subject != null) {
				if (subject.equals(tripleStringURI.getSubject())) {
					s = true;
				}
			} else {// s is variable, therefore always true
				s = true;
			}
			if (predicate != null) {
				if (predicate.equals(tripleStringURI.getPredicate())) {
					p = true;
				}
			} else {// p is variable, therefore always true
				p = true;
			}
			if (object != null) {
				if (object.equals(tripleStringURI.getObject())) {
					o = true;
				}
			} else {// o is variable, therefore always true
				o = true;
			}

			if (s && p && o)
				return true;
		}
		return false;
	}


	public int hashCode() {
		int hc;
		hc = subject.hashCode() ^ predicate.hashCode() ^ object.hashCode();
		return hc;
	}

	public void output(){
		System.out.println("Subj: " + subject + " Pred: " + predicate + " Obj: " + object);
	}
}

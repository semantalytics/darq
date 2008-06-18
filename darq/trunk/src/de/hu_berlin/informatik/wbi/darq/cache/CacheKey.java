package de.hu_berlin.informatik.wbi.darq.cache;

import java.io.Serializable;
import java.util.List;

public class CacheKey implements Serializable {

	private List<TripleStringURI> tripleStringURI;
	private String remoteURL;

	public CacheKey(List<TripleStringURI> tripleStringURI, String remoteURL) {
		this.tripleStringURI = tripleStringURI;
		this.remoteURL = remoteURL;
	}

	public boolean equals(Object obj){
		CacheKey cacheKey = null;
		if (obj instanceof CacheKey) {
			cacheKey = (CacheKey) obj;
			if (remoteURL.equals(cacheKey.getRemoteURL()) && tripleStringURI.equals(cacheKey.getTripleStringURI())) {
				return true;
			}
		}
		return false;
	}
	
	public int hashCode() {
		String s, p, o;
		int hc;
		hc = remoteURL.hashCode();
		for (TripleStringURI t : tripleStringURI) {

			s = t.getSubject();
			if (s!=null)
				hc = hc ^ s.hashCode();
			
			p = t.getPredicate();
			if(p!=null) hc = hc ^p.hashCode();
			
			o = t.getObject();
			if(o!=null) hc = hc ^o.hashCode();
		}
		return hc;
	}

	public List<TripleStringURI> getTripleStringURI() {
		return tripleStringURI;
	}

	public String getRemoteURL() {
		return remoteURL;
	}

}

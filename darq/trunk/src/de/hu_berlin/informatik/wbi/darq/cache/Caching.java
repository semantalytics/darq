package de.hu_berlin.informatik.wbi.darq.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.core.Var;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.engine.Binding;
import com.hp.hpl.jena.query.engine.BindingMap;
import com.hp.hpl.jena.query.expr.Expr;

public class Caching {

	private static Caching instance;
	private static String configFile;
	private static boolean on = false;
	CacheManager singletonManager;
	Cache darqCache;

	private Caching() {

		/* Cache parameters */
		/*
		 * see http://ehcache.sourceforge.net/documentation/configuration.html
		 * for explanation
		 */
		// String name = "DarqResultSetCache";
		// int maxElementsInMemory = 50; //TEST Werte testen
		// int maxElementsOnDisk = 0; // 0 == unlimited
		// boolean eternal = false; // Element will expire
		// boolean overflowToDisk = true; //write to disk after maxInMemory
		// boolean diskPersistent = true; // TODO TEST store to disk between
		// restarts of the VM
		// // mit maxElementOnDisk = unlimited --> Gefährlich !!
		// long timeToLiveSeconds = 0; // 0 == infinity
		// long timeToIdleSeconds = 0; // 0 == infinity
		// int diskSpoolBufferSizeMB = 30; //Buffer before storing to disk
		// MemoryStoreEvictionPolicy memoryStoreEvictionPolicy =
		// MemoryStoreEvictionPolicy.LFU;
		// String diskStorePath = System.getProperty("java.io.tempdir");
		// long diskExpiryThreadIntervalSeconds = 120;
		// RegisteredEventListeners registeredEventListeners = null;
		// BootstrapCacheLoader bootstrapCacheLoader = null;
		// singletonManager = CacheManager.create();
		// darqCache = new Cache(name,
		// maxElementsInMemory,memoryStoreEvictionPolicy,
		// overflowToDisk,diskStorePath, eternal, timeToLiveSeconds,
		// timeToIdleSeconds, diskPersistent,diskExpiryThreadIntervalSeconds,
		// registeredEventListeners,bootstrapCacheLoader,maxElementsOnDisk,diskSpoolBufferSizeMB);
		// singletonManager.addCache("DarqResultSetCache");
		/* create() use log, constructor does not */
		singletonManager = CacheManager.create(configFile);
		darqCache = singletonManager.getCache("DarqResultSetCache");
	}

	public synchronized static Caching getInstance(String confFile) {
		configFile = confFile;
		if (instance == null) {
			instance = new Caching();
		}
		on = true;
		return instance;
	}

	public void addElement(ServiceGroup serviceGroup, List<Binding> bindings) {
		CacheKey key;
		// if (serviceGroup.getFilters().isEmpty()){
		key = getKey(serviceGroup);
		Element el = new Element(key, bindings);
		darqCache.put(el);
		/*
		 * write to disk, QuerySolution (and all Elements within) has to be
		 * serializable
		 */
		// System.out.println("serializable: " + el.isSerializable());
		// //TESTAUSGABE
		// darqCache.flush();
		// }
	}

	/* get key for element, get value for key */
	public List<Binding> getElement(ServiceGroup serviceGroup) {
		List<Binding> bindings = new ArrayList<Binding>();
		List<Binding> newBindings = new ArrayList<Binding>();
		Element element;
		CacheKey key;

		key = getKey(serviceGroup);
		element = darqCache.get(key);
		/*
		 * result is filtered anyway, so you can look if an unfiltered result is
		 * in Cache
		 */
		if (!serviceGroup.getFilters().isEmpty() && element == null) {
			ServiceGroup sgWithoutFilter = serviceGroup.clone();
			List<Expr> filters = new ArrayList<Expr>();
			sgWithoutFilter.setFilters(filters);
			key = getKey(sgWithoutFilter);
			element = darqCache.get(key);
		}

		if (element != null) {
			bindings = (List<Binding>) element.getObjectValue();
			newBindings.addAll(changeVariables(bindings, serviceGroup, element));
		}
		return newBindings;
	}

	private List<Binding> changeVariables(List<Binding> cacheBindings, ServiceGroup serviceGroup, Element element) {

		Map<Node, Node> mappingVariables = new HashMap<Node,Node>();
		Var newVariable, cacheVariable;
		List<Binding> newBindings = new ArrayList<Binding>();

		/* get Triples from cache and query */
		List<Triple> cacheTriples = ((CacheKey) element.getObjectKey()).getServiceGroup().getTriples();
		List<Triple> newTriples = serviceGroup.getTriples();

		
		/* gets mapping for variables*/
		if (!newTriples.equals(cacheTriples)) {
			for (Triple newTriple : newTriples) {
				for (Triple cacheTriple : cacheTriples) {
					
					mappingVariables.putAll(equalTriple(newTriple, cacheTriple));
				}
			}
			/* changes variables in bindings from cache to variables from query */ 
			if (!mappingVariables.isEmpty()) {
				for (Binding cacheBinding : cacheBindings) {
					// System.out.println("Cache: " + cacheBinding.toString());//TESTAUSGABE
					BindingMap bm = new BindingMap();
					for (Iterator iter = cacheBinding.vars(); iter.hasNext();) {
						cacheVariable = (Var) iter.next(); 
						// System.out.println("CacheVariable: " + cacheVariable);//TESTAUSGABE
						Node bindingValue = (Node) cacheBinding.get(cacheVariable); 
						// System.out.println(" Wert: " + bindingValue);//TESTAUSGABE
						newVariable = (Var) mappingVariables.get(cacheVariable);
						// System.out.println("NeuVariable: " + newVariable);//TESTAUSGABE
						if (bindingValue != null) {
							bm.add(newVariable, bindingValue); 
							// System.out.println(newVariable + " = " + bindingValue); //TESTAUSGABE
						}
					}
					newBindings.add(bm);
				}
			}
		}
		else{
			newBindings = cacheBindings;
		}
		return newBindings;
	}

	private CacheKey getKey(ServiceGroup serviceGroup) {
		CacheKey key;
		Node s, p, o;
		String subj, pred, obj;
		subj = null;
		pred = null;
		obj = null;
		List<TripleStringURI> tripleList = new ArrayList<TripleStringURI>();
		System.out.println(serviceGroup.getFilters().toString());
		List<Triple> triples = serviceGroup.getTriples();
		for (Triple triple : triples) {
			s = triple.getSubject();
			if (s.isConcrete())
				subj = s.toString();
			p = triple.getPredicate();
			if (p.isConcrete())
				pred = p.toString();
			o = triple.getObject();
			if (o.isConcrete())
				obj = o.toString();
			TripleStringURI tripleURI = new TripleStringURI(subj, pred, obj);
			tripleList.add(tripleURI);
		}
		key = new CacheKey(tripleList, serviceGroup.getService().getUrl(), serviceGroup.getFilters().toString(), serviceGroup);
		return key;
	}

	private Map<Node, Node> equalTriple(Triple newTriple, Triple cacheTriple) {
		HashMap<Node, Node> mappingVariables = new HashMap<Node, Node>();
		Boolean subj, pred, obj;
		Node s, p, o, cacheS, cacheP, cacheO;
		subj = false;
		pred = false;
		obj = false;

		s = newTriple.getSubject();
		cacheS = cacheTriple.getSubject();
		if (s.isConcrete()) {
			subj = s.equals(cacheS);
		} else {
			subj = true;
			mappingVariables.put(cacheS, s);
		}

		p = newTriple.getPredicate();
		cacheP = cacheTriple.getPredicate();
		if (p.isConcrete()) {
			pred = p.equals(cacheP);
		} else {
			pred = true;
			mappingVariables.put(cacheP, p);
		}

		o = newTriple.getObject();
		cacheO = cacheTriple.getObject();
		if (o.isConcrete()) {
			obj = o.equals(cacheO);
		} else {
			obj = true;
			mappingVariables.put(cacheO, o);
		}

		if (subj && pred && obj) {
			return mappingVariables;
		} else {
			return new HashMap<Node,Node>();
		}
	}

	public void output() {
		System.out.println("Cache Memory Size: " + darqCache.getMemoryStoreSize());
		System.out.println("Cache Disk Size: " + darqCache.getDiskStoreSize());
		for (Object key : darqCache.getKeys()) {
			CacheKey cacheKey = (CacheKey) key;
			System.out.println("Key:" + key.hashCode());
			Element element = darqCache.get(cacheKey);
			List<Binding> werte = (List<Binding>) element.getValue();
			for (Binding sol : werte) {
				System.out.println("   Wert" + sol.toString());
			}
		}
	}

	public void removeCache() {
		/* write to disk */
		// darqCache.flush();
		singletonManager.removalAll();
		singletonManager.shutdown();
	}

	public boolean isEnabled() {
		return on;
	}

	public String getConfigFile() {
		return configFile;
	}

}

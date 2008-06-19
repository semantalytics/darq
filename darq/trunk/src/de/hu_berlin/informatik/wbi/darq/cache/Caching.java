package de.hu_berlin.informatik.wbi.darq.cache;

import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.engine.Binding;

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
	}

	/* get key for element, get value for key */
	public List<Binding> getElement(ServiceGroup serviceGroup) {
		List<Binding> bindings = new ArrayList<Binding>();
		Element element;
		CacheKey key = getKey(serviceGroup);
		element = darqCache.get(key);
		if (element != null) {
			bindings = (List<Binding>) element.getObjectValue();
		}
		return bindings;

	}

	private CacheKey getKey(ServiceGroup serviceGroup) {
		CacheKey key;
		Node s, p, o;
		String subj, pred, obj, filters;
		subj = null;
		pred = null;
		obj = null;
		List<TripleStringURI> tripleList = new ArrayList<TripleStringURI>();

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
		key = new CacheKey(tripleList, serviceGroup.getService().getUrl(), serviceGroup.getFilters().toString());
		return key;
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

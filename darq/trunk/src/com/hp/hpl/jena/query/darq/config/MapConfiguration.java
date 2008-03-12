package com.hp.hpl.jena.query.darq.config;


import com.hp.hpl.jena.query.darq.engine.compiler.iterators.IDarqQueryIteratorFactory;
import com.hp.hpl.jena.query.darq.engine.compiler.iterators.MapFedQueryIterServiceFactory;
import com.hp.hpl.jena.query.darq.engine.compiler.iterators.MapIDarqQueryIteratorFactory;
import com.hp.hpl.jena.query.darq.engine.optimizer.MapDynProgPlanGenerator;
import com.hp.hpl.jena.query.darq.engine.optimizer.MapOptimizedPlanGenerator;
import com.hp.hpl.jena.rdf.model.Model;


//import com.hp.hpl.jena.util.FileManager;

public class MapConfiguration extends Configuration {
	
	private MapOptimizedPlanGenerator MapPlanOptimizer = new MapDynProgPlanGenerator();
	private MapIDarqQueryIteratorFactory darqQueryIteratorFactory = new MapFedQueryIterServiceFactory();


	public MapConfiguration(String configFile) {
		super(configFile);
//		super(FileManager.get().loadModel(configFile));
	}

	public MapConfiguration(Model m) {
		super(m);
	}

	public MapOptimizedPlanGenerator getMapPlanOptimizer() {
		return MapPlanOptimizer;
	}

	public void setMapPlanOptimizer(MapOptimizedPlanGenerator planOptimizer) {
		this.MapPlanOptimizer = planOptimizer;
	}

	/**
	 * @return Returns the darqQueryIteratorClass.
	 */
	public MapIDarqQueryIteratorFactory getDarqQueryIteratorFactory() {
		return darqQueryIteratorFactory;
	}

	/**
	 * @param darqQueryIteratorFactory
	 *            The darqQueryIteratorClass to set.
	 */
	public void setDarqQueryIteratorClass(
			MapIDarqQueryIteratorFactory darqQueryIteratorFactory) {
		this.darqQueryIteratorFactory = darqQueryIteratorFactory;
	}
}

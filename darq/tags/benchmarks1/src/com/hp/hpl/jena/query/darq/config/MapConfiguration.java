package com.hp.hpl.jena.query.darq.config;


import com.hp.hpl.jena.query.darq.engine.optimizer.MapDynProgPlanGenerator;
import com.hp.hpl.jena.query.darq.engine.optimizer.OptimizedPlanGenerator;
import com.hp.hpl.jena.rdf.model.Model;

import com.hp.hpl.jena.util.FileManager;

public class MapConfiguration extends Configuration {
	
	private OptimizedPlanGenerator planOptimizer = new MapDynProgPlanGenerator();


	public MapConfiguration(String configFile) {

		super(FileManager.get().loadModel(configFile));

	}

	public MapConfiguration(Model m) {
		super(m);
	}

	
	/**
	 * @return Returns the planOptimizer.
	 */
	public OptimizedPlanGenerator getPlanOptimizer() {
		return planOptimizer;
	}

	/**
	 * @param planOptimizer
	 *            The planOptimizer to set.
	 */
	public void setPlanOptimizer(OptimizedPlanGenerator planOptimizer) {
		this.planOptimizer = planOptimizer;
	}

}

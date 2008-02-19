/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */
package com.hp.hpl.jena.query.darq.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryBuildException;
import com.hp.hpl.jena.query.core.ElementBasicGraphPattern;
import com.hp.hpl.jena.query.darq.config.Configuration;
import com.hp.hpl.jena.query.darq.config.ServiceRegistry;
import com.hp.hpl.jena.query.darq.core.MultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanMultipleService;
import com.hp.hpl.jena.query.darq.engine.compiler.FedPlanService;
import com.hp.hpl.jena.query.darq.engine.compiler.PlanGroupDarq;
import com.hp.hpl.jena.query.darq.engine.compiler.PlanNestedLoopJoin;
import com.hp.hpl.jena.query.darq.engine.optimizer.PlanUnfeasibleException;
import com.hp.hpl.jena.query.darq.engine.optimizer.planoperators.PlanOperatorBase;
import com.hp.hpl.jena.query.darq.util.FedPlanFormatter;
import com.hp.hpl.jena.query.darq.util.FedPlanVisitor;
import com.hp.hpl.jena.query.darq.util.PlanFilterAddVisitor;
import com.hp.hpl.jena.query.engine.Plan;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.plan.PlanBasicGraphPattern;
import com.hp.hpl.jena.query.engine1.plan.PlanBlockTriples;
import com.hp.hpl.jena.query.engine1.plan.PlanFilter;
import com.hp.hpl.jena.query.engine1.plan.PlanGroup;
import com.hp.hpl.jena.query.engine1.plan.TransformCopy;
import com.hp.hpl.jena.query.expr.Expr;
import com.hp.hpl.jena.query.util.Context;
import com.hp.hpl.jena.query.util.IndentedWriter;

public class DarqTransform extends TransformCopy {

	Log log = LogFactory.getLog(DarqTransform.class);

	protected Plan plan = null;

	private Context context = null;

	private ServiceRegistry registry = null;

	private Configuration config = null;

	// The return stack
	private Stack<PlanElement> retStack = new Stack<PlanElement>();

	HashMap<RemoteService, ServiceGroup> groupedTriples = new HashMap<RemoteService, ServiceGroup>();

	HashMap<Triple, MultipleServiceGroup> queryIndividuallyTriples = new HashMap<Triple, MultipleServiceGroup>();

	List<ServiceGroup> sgsPos = new LinkedList<ServiceGroup>();

	Set varsMentioned = new HashSet();

	boolean optimize = true;

	/**
	 * @return the optimize
	 */
	public boolean isOptimize() {
		return optimize;
	}

	/**
	 * @param optimize
	 *            the optimize to set
	 */
	public void setOptimize(boolean optimize) {
		this.optimize = optimize;
	}

	public DarqTransform(Context cntxt, Configuration conf) {
		super();
		context = cntxt;
		config = conf;
		registry = conf.getServiceRegistry();
	}

	
	public PlanElement transform(PlanBasicGraphPattern planElt, List newElts,PlanElement parent) {
		/*
		 * We do this only if we have a basic graph pattern with one triple:
		 * PlanGroups with one Triple will be converted to PlanBasicGraphPattern
		 * by ARQ :( We need to handle that!
		 * There are also some other cases where PlanBasicGraphPattern is not in a group... :((
		 */
		
		if (parent instanceof PlanGroup) return planElt;
	/*	 if (!( (newElts.size()==1) && (newElts.get(0) instanceof
		 PlanBlockTriples) &&
		 (((PlanBlockTriples)newElts.get(0)).getSubElements().size()==1) ) )
		 return planElt;*/
		 
		List<PlanElement> acc = new ArrayList<PlanElement>();
		acc.add(planElt);
		return make(acc, planElt.getContext());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.hpl.jena.query.engine1.plan.TransformBase#transform(com.hp.hpl.jena.query.engine1.plan.PlanBasicGraphPattern,
	 *      java.util.List)
	 */

	@Override
	public PlanElement transform(PlanGroup planElt, List newElts) {
		return make(newElts, planElt.getContext());
	}

	private PlanElement make(List newElts, Context context) {
		groupedTriples.clear(); // new for each PlanBasicGraphPattern !
		queryIndividuallyTriples.clear(); // "

		List<Triple> unmatchedTriples = new LinkedList<Triple>();

		List<PlanFilter> filters = new ArrayList<PlanFilter>();

		List<PlanElement> acc = new ArrayList<PlanElement>();

		// planning
		for (PlanElement elm : (List<PlanElement>) newElts) {

			if (elm instanceof PlanBasicGraphPattern) {

				for (PlanElement el : (List<PlanElement>) elm.getSubElements()) {

					if (el instanceof PlanBlockTriples) {
						for (Triple t : (List<Triple>) ((PlanBlockTriples) el)
								.getPattern()) {

							List<RemoteService> services = selectServices(registry
									.getMatchingServices(t));
							if (services.size() == 1) {
								putIntoGroupedTriples(services.get(0), t);
							} else if (services.size() > 1) {
								/*
								 * if there are more than one service, the
								 * triple has to be passed to the services
								 * individually. This is because ... TODO
								 */
								for (int j = 0; j < services.size(); j++) {
									putIntoQueryIndividuallyTriples(t, services
											.get(j));
								}

							} else {

								unmatchedTriples.add(t);
								log.warn("No service found for statement: " + t
										+ " - it will be queried locally.");

							}

						}
					} else if (el instanceof PlanFilter) {
						filters.add((PlanFilter) el);
					} else {
						acc.add(0, el);

					}
				}

			} else if (elm instanceof PlanFilter) {
				filters.add((PlanFilter) elm);
			} else {
				acc.add(0, elm);

			}

		}

		// add filters to servcie groups and to plan (filters are also applied
		// locally because we don't trust the remote services)
		for (PlanFilter f : filters) {
			acc.add(f);
			if (optimize) { // do we optimize?
				for (ServiceGroup sg : groupedTriples.values()) {
					sg.addFilter(f.getExpr());
				}
				for (ServiceGroup sg : queryIndividuallyTriples.values()) {
					sg.addFilter(f.getExpr());
				}
			}
		}

		// build new subplan
		if (groupedTriples.size() > 0 || queryIndividuallyTriples.size() > 0) {

			/*
			 * ArrayList<ServiceGroup> al = new ArrayList<ServiceGroup>(
			 * groupedTriples.values());
			 * al.addAll(queryIndividuallyTriples.values());
			 */

			ArrayList<ServiceGroup> al = new ArrayList<ServiceGroup>(sgsPos);

			if (optimize) { // run optimizer
				PlanElement optimizedPlan = null;
				try {
					PlanOperatorBase planOperatorBase = config
							.getPlanOptimizer().getCheapestPlan(al);
					FedQueryEngineFactory.logExplain(planOperatorBase);
					optimizedPlan = planOperatorBase.toARQPlanElement(context);

				} catch (PlanUnfeasibleException e) {
					throw new QueryBuildException("No feasible plan: " + e);
				}
				acc.add(0, optimizedPlan);
				// log.debug("selected: \n"
				// + optimizedPlan.toString());

			} else { // no optimization -> just add elements
				int pos = 0;
				for (ServiceGroup sg : al) {

					if (sg instanceof MultipleServiceGroup) {
						acc.add(pos, FedPlanMultipleService.make(context,
								(MultipleServiceGroup) sg, null));
					} else
						acc.add(pos, FedPlanService.make(context, sg, null));
					pos++;

				}
			}
		}

		// unmatched patterns are executed locally
		if (unmatchedTriples.size() > 0) {
			ElementBasicGraphPattern elementBasicGraphPattern = new ElementBasicGraphPattern();
			for (Triple t : unmatchedTriples)
				elementBasicGraphPattern.addTriple(t);
			acc.add(0, PlanBasicGraphPattern.make(context,
					elementBasicGraphPattern));
		}

		PlanElement ex = PlanGroupDarq.make(context, (List) acc, false);

		// FedPlanFormatter.out(new IndentedWriter(System.out), ex);
		// System.out.println("-----");

		return ex;
	}

	private List<RemoteService> selectServices(List<RemoteService> services) {
		ArrayList<RemoteService> result = new ArrayList<RemoteService>();
		for (Iterator<RemoteService> it = services.iterator(); it.hasNext();) {
			RemoteService rs = it.next();

			// if there is a Definitive Service only use this one
			if (rs.isDefinitive()) {
				result.clear();
				result.add(rs);
				break;
			}

			result.add(rs);

		}
		return result;
	}

	private void putIntoGroupedTriples(RemoteService s, Triple t) {
		ServiceGroup tg = groupedTriples.get(s);
		if (tg == null) {
			tg = new ServiceGroup(s);
			groupedTriples.put(s, tg);
			sgsPos.add(tg);
		}

		tg.addB(t);
	}

	private void putIntoQueryIndividuallyTriples(Triple t, RemoteService s) {
		MultipleServiceGroup msg = queryIndividuallyTriples.get(t);
		if (msg == null) {
			msg = new MultipleServiceGroup();
			queryIndividuallyTriples.put(t, msg);
			sgsPos.add(msg);
			msg.addB(t);
		}
		msg.addService(s);

	}

}
/*
 * (c) Copyright 2005, 2006 Hewlett-Packard Development Company, LP All rights
 * reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. The name of the author may not
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
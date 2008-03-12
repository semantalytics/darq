package com.hp.hpl.jena.query.darq.engine.compiler.iterators;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.core.ElementFilter;
import com.hp.hpl.jena.query.core.ElementGroup;
import com.hp.hpl.jena.query.darq.core.MapServiceGroup;
import com.hp.hpl.jena.query.darq.core.ServiceGroup;
import com.hp.hpl.jena.query.darq.engine.compiler.RewrittenTripleIterator;
import com.hp.hpl.jena.query.darq.mapping.rewriting.TripleRewriter;
import com.hp.hpl.jena.query.darq.util.QueryUtils;
import com.hp.hpl.jena.query.engine.Binding;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.expr.Expr;

public class MapRemoteQuery {

	MapServiceGroup serviceGroup;
	ExecutionContext context;
	Binding binding;

	RewrittenTripleIterator tripleIterator = null;
	long minResultsToStop = 1;

	public MapRemoteQuery(MapServiceGroup sg, ExecutionContext c, Binding b) {
		serviceGroup = sg;
		context = c;
		binding = b;
		buildTripleIterator();
	}

	public boolean hasNextQuery(long previousResults) {
		if (previousResults >= minResultsToStop)
			return false;
		return tripleIterator.hasNext();
	}

	public Query getNextQuery() {
		if (!tripleIterator.hasNext())
			throw new IndexOutOfBoundsException("No more rewritings.");
		Query remoteQuery = buildQuery();
		return remoteQuery;
	}

	private void buildTripleIterator() {
		Triple triple = serviceGroup.getTriple();

		RewrittenTripleIterator prevIterator = null;

		Node subject = QueryUtils.replacewithBinding(triple.getSubject(), binding);
		Node predicate = QueryUtils.replacewithBinding(triple.getPredicate(), binding);
		Node object = QueryUtils.replacewithBinding(triple.getObject(), binding);

		Triple newtriple = new Triple(subject, predicate, object);

		TripleRewriter tripleRewriter = serviceGroup.getService().getTripleRewriter(newtriple);

		if (tripleRewriter != null) {
			prevIterator = new RewrittenTripleIterator(tripleRewriter.getRewritings(newtriple), prevIterator);
			if (tripleRewriter.getMinimumResultsToStop() > minResultsToStop)
				minResultsToStop = tripleRewriter.getMinimumResultsToStop();
		} else {//wahrscheinlich unnötig, da keine Triplelist mehr TODO FRAGE
			ArrayList<Triple> tmpal = new ArrayList<Triple>();
			tmpal.add(triple);
			prevIterator = new RewrittenTripleIterator(tmpal, prevIterator);
		}
		tripleIterator = prevIterator;
	}

	private Query buildQuery() {

		Query remoteQuery = new Query();
		remoteQuery.setPrefixMapping(context.getQuery().getPrefixMapping());
		remoteQuery.setBaseURI(context.getQuery().getBaseURI());
		remoteQuery.setSyntax(context.getQuery().getSyntax());
		remoteQuery.setQueryType(Query.QueryTypeSelect);
		remoteQuery.setQueryResultStar(true);

		ElementGroup eg = new ElementGroup();

		for (Triple t : tripleIterator.next()) {

			Node subject = QueryUtils.replacewithBinding(t.getSubject(), binding);
			Node predicate = QueryUtils.replacewithBinding(t.getPredicate(), binding);
			Node object = QueryUtils.replacewithBinding(t.getObject(), binding);

			Triple newtriple = new Triple(subject, predicate, object);

			eg.addTriplePattern(newtriple);
		}

		for (Expr c : serviceGroup.getFilters()) {
			eg.addElementFilter(new ElementFilter(QueryUtils.replacewithBinding(c, binding, context.getQuery())));
		}

		remoteQuery.setQueryPattern(eg);

		return remoteQuery;
	}

}

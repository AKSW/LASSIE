package org.aksw.lassie.kb;

import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.reasoning.SPARQLReasoner;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public class RemoteKnowledgeBase extends AbstractKnowledgeBase {

	private SparqlEndpoint endpoint;

	public RemoteKnowledgeBase(SparqlEndpoint endpoint) {
		this(endpoint, null);
	}
	
	public RemoteKnowledgeBase(SparqlEndpoint endpoint, String namespace) {
		this.endpoint = endpoint;
		this.namespace = namespace;

		reasoner = new SPARQLReasoner(new SparqlEndpointKS(endpoint));
	}

	public SparqlEndpoint getEndpoint() {
		return endpoint;
	}

	public ResultSet executeSelect(String query) {
		QueryEngineHTTP qe = new QueryEngineHTTP(endpoint.getURL().toString(), query);
		for (String uri : endpoint.getDefaultGraphURIs()) {
			qe.addDefaultGraph(uri);
		}
		for (String uri : endpoint.getNamedGraphURIs()) {
			qe.addNamedGraph(uri);
		}
		ResultSet rs = qe.execSelect();
		return rs;
	}

	public Model executeConstruct(String query) {
		QueryEngineHTTP qe = new QueryEngineHTTP(endpoint.getURL().toString(), query);
		for (String uri : endpoint.getDefaultGraphURIs()) {
			qe.addDefaultGraph(uri);
		}
		for (String uri : endpoint.getNamedGraphURIs()) {
			qe.addNamedGraph(uri);
		}
		Model model = qe.execConstruct();
		return model;
	}

	public boolean isRemote() {
		return true;
	}
}

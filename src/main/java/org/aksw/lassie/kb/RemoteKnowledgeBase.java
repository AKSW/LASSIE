package org.aksw.lassie.kb;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;
import org.dllearner.reasoning.SPARQLReasoner;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public class RemoteKnowledgeBase extends AbstractKnowledgeBase {

	private SparqlEndpoint endpoint;
	private ExtractionDBCache cache;

	public RemoteKnowledgeBase(SparqlEndpoint endpoint) {
		this(endpoint, null, null);
	}
	
	public RemoteKnowledgeBase(SparqlEndpoint endpoint, ExtractionDBCache cache) {
		this(endpoint, cache, null);
	}
	
	public RemoteKnowledgeBase(SparqlEndpoint endpoint, String namespace) {
		this.endpoint = endpoint;
		this.namespace = namespace;

		reasoner = new SPARQLReasoner(new SparqlEndpointKS(endpoint));
	}
	
	public RemoteKnowledgeBase(SparqlEndpoint endpoint, ExtractionDBCache cache, String namespace) {
		this.endpoint = endpoint;
		this.cache = cache;
		this.namespace = namespace;

		reasoner = new SPARQLReasoner(new SparqlEndpointKS(endpoint));
	}

	public SparqlEndpoint getEndpoint() {
		return endpoint;
	}
	
	public ExtractionDBCache getCache() {
		return cache;
	}

	@Override
	public ResultSet executeSelect(String query) {
		ResultSet rs;
		if(cache != null){
			rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(endpoint, query));
		} else {
			QueryEngineHTTP qe = new QueryEngineHTTP(endpoint.getURL().toString(), query);
			for (String uri : endpoint.getDefaultGraphURIs()) {
				qe.addDefaultGraph(uri);
			}
			for (String uri : endpoint.getNamedGraphURIs()) {
				qe.addNamedGraph(uri);
			}
			rs = qe.execSelect();
			
		}
		return rs;
	}

	@Override
	public Model executeConstruct(String query) {
		Model model = null;
		if(cache != null){
			try {
				model = cache.executeConstructQuery(endpoint, query);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			QueryEngineHTTP qe = new QueryEngineHTTP(endpoint.getURL().toString(), query);
			for (String uri : endpoint.getDefaultGraphURIs()) {
				qe.addDefaultGraph(uri);
			}
			for (String uri : endpoint.getNamedGraphURIs()) {
				qe.addNamedGraph(uri);
			}
			model = qe.execConstruct();
		}
		return model;
	}

	@Override
	public boolean isRemote() {
		return true;
	}
}

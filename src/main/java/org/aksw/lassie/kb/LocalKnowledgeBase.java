package org.aksw.lassie.kb;

import org.dllearner.core.ComponentInitException;
import org.dllearner.kb.LocalModelBasedSparqlEndpointKS;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.reasoning.SPARQLReasoner;

import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public class LocalKnowledgeBase extends AbstractKnowledgeBase {

	private Model model;

	public LocalKnowledgeBase(Model model) throws ComponentInitException {
		this(model, null);
	}
	
	public LocalKnowledgeBase(Model model, String namespace) throws ComponentInitException {
		this.model = model;
		this.namespace = namespace;

        LocalModelBasedSparqlEndpointKS localModelBasedSparqlEndpointKS = new LocalModelBasedSparqlEndpointKS(model);
        localModelBasedSparqlEndpointKS.init();
		reasoner = new SPARQLReasoner(localModelBasedSparqlEndpointKS);
		reasoner.init();
	}

	public Model getModel() {
		return model;
	}

	@Override
	public ResultSet executeSelect(String query) {		
		ResultSet rs = QueryExecutionFactory.create(query, this.model).execSelect();
		return rs;
	}

	@Override
	public Model executeConstruct(String query) {
		Model model = QueryExecutionFactory.create(query, this.model).execConstruct();
		return model;
	}

	@Override
	public boolean isRemote() {
		return false;
	}
}

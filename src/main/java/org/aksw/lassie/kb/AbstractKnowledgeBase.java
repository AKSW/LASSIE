package org.aksw.lassie.kb;

import org.dllearner.reasoning.SPARQLReasoner;

public abstract class AbstractKnowledgeBase implements KnowledgeBase {

	protected SPARQLReasoner reasoner;
	protected String namespace;

	@Override
	public SPARQLReasoner getReasoner() {
		return reasoner;
	}
	
	@Override
	public String getNamespace() {
		return namespace;
	}

}

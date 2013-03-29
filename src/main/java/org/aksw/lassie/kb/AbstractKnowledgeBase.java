package org.aksw.lassie.kb;

import org.dllearner.reasoning.SPARQLReasoner;

public abstract class AbstractKnowledgeBase implements KnowledgeBase {

	protected SPARQLReasoner reasoner;
	protected String namespace;

	public SPARQLReasoner getReasoner() {
		return reasoner;
	}
	
	public String getNamespace() {
		return namespace;
	}

}

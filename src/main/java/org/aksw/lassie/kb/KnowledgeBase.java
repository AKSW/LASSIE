package org.aksw.lassie.kb;

import org.dllearner.reasoning.SPARQLReasoner;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public interface KnowledgeBase {
	
	String getNamespace();

	SPARQLReasoner getReasoner();

	ResultSet executeSelect(String query);

	Model executeConstruct(String query);

	boolean isRemote();
}

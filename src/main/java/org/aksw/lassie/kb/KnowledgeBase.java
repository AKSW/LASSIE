package org.aksw.lassie.kb;

import java.util.SortedSet;

import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;

public interface KnowledgeBase {
	
	String getNamespace();

	SPARQLReasoner getReasoner();

	ResultSet executeSelect(String query);

	Model executeConstruct(String query);

	boolean isRemote();

    SortedSet<OWLIndividual> getInstances(OWLClassExpression desc);
    
    Model getFragment(OWLIndividual ind, int recursionDepth);
    Model getFragment(SortedSet<OWLIndividual> individuals, int recursionDepth);
    
}

package org.aksw.lassie.kb;

import java.util.SortedSet;

import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

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

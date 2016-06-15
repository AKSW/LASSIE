package org.aksw.lassie.core;

import java.util.SortedSet;
import java.util.TreeSet;

import org.aksw.lassie.kb.KnowledgeBase;
import org.semanticweb.owlapi.model.OWLIndividual;

public class NegativeExamplesGenerator {

	private KnowledgeBase kb;

	public NegativeExamplesGenerator(KnowledgeBase kb) {
		this.kb = kb;
	}
	
	public SortedSet<OWLIndividual> generateNegativeExamples(SortedSet<OWLIndividual> positiveExamples){
		SortedSet<OWLIndividual> negativeExamples = new TreeSet<>();
		
		
		
		return negativeExamples;
	}

}

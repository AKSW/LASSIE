package org.aksw.lassie.core;

import java.util.SortedSet;
import java.util.TreeSet;

import org.aksw.lassie.kb.KnowledgeBase;
import org.dllearner.core.owl.Individual;

public class NegativeExamplesGenerator {

	private KnowledgeBase kb;

	public NegativeExamplesGenerator(KnowledgeBase kb) {
		this.kb = kb;
	}
	
	public SortedSet<Individual> generateNegativeExamples(SortedSet<Individual> positiveExamples){
		SortedSet<Individual> negativeExamples = new TreeSet<Individual>();
		
		
		
		return negativeExamples;
	}

}

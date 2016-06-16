/**
 * 
 */
package org.aksw.lassie.result;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.dllearner.core.EvaluatedDescription;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.selfconfig.SimpleClassifier;


/**
 * @author sherif
 *
 */
public class LassieClassRecorder {
	private static final Logger logger = Logger.getLogger(LassieClassRecorder.class.getName());
	public OWLClass namedClass;
	public double coverage;
	public double FMeasure;
	public double PFMesure;
	public List<? extends EvaluatedDescription> mapping = new ArrayList<EvaluatedDescription>();
	public SortedSet<OWLIndividual> posExamples = new TreeSet<>();
	public SortedSet<OWLIndividual> negExamples = new TreeSet<>();
	public Mapping instanceMapping = new Mapping();
	public List<SimpleClassifier> classifiers =new ArrayList<SimpleClassifier>();
	
	
	/**
	 * @return the classifiers
	 */
	public List<SimpleClassifier> getClassifiers() {
		return classifiers;
	}


	/**
	 * @param classifiers the classifiers to set
	 */
	public void setClassifiers(List<SimpleClassifier> classifiers) {
		this.classifiers = classifiers;
	}


	/**
	 * @return the instanceMapping
	 */
	public Mapping getInstanceMapping() {
		return instanceMapping;
	}


	/**
	 * @param instanceMapping the instanceMapping to set
	 */
	public void setInstanceMapping(Mapping instanceMapping) {
		
		if(this.instanceMapping.size == 0){
			this.instanceMapping = instanceMapping;
		}else{
			for( String url1 : instanceMapping.map.keySet()){
				for (String url2 : instanceMapping.map.get(url1).keySet()){
					this.instanceMapping.add(url1, url2, instanceMapping.getSimilarity(url1, url1));
				}
			}
		}
	}


	public LassieClassRecorder(){
	}
	
	
	/**
	 * @param namedClass
	 *@author sherif
	 */
	public LassieClassRecorder(OWLClass namedClass) {
		super();
		this.namedClass = namedClass;
	}
	
	
	/**
	 * @return the namedClass
	 */
	public OWLClass getOWLClass() {
		return namedClass;
	}


	/**
	 * @param namedClass the namedClass to set
	 */
	public void setOWLClass(OWLClass namedClass) {
		this.namedClass = namedClass;
	}


	/**
	 * @return the coverage
	 */
	public double getCoverage() {
		return coverage;
	}


	/**
	 * @param coverage the coverage to set
	 */
	public void setCoverage(double coverage) {
		this.coverage = coverage;
	}


	/**
	 * @return the fMeasure
	 */
	public double getFMeasure() {
		return FMeasure;
	}


	/**
	 * @param fMeasure the fMeasure to set
	 */
	public void setFMeasure(double fMeasure) {
		FMeasure = fMeasure;
	}


	/**
	 * @return the pFMesure
	 */
	public double getPFMesure() {
		return PFMesure;
	}


	/**
	 * @param pFMesure the pFMesure to set
	 */
	public void setPFMesure(double pFMesure) {
		PFMesure = pFMesure;
	}


	/**
	 * @return the mapping
	 */
	public List<? extends EvaluatedDescription> getMapping() {
		return mapping;
	}


	/**
	 * @param mapping the mapping to set
	 */
	public void setMapping(List<? extends EvaluatedDescription> mapping) {
		this.mapping = mapping;
	}


	/**
	 * @return the posExamples
	 */
	public SortedSet<OWLIndividual> getPosExamples() {
		return posExamples;
	}


	/**
	 * @param posExamples the posExamples to set
	 */
	public void setPosExamples(SortedSet<OWLIndividual> posExamples) {
		this.posExamples = posExamples;
	}


	/**
	 * @return the negExamples
	 */
	public SortedSet<OWLIndividual> getNegExamples() {
		return negExamples;
	}


	/**
	 * @param negExamples the negExamples to set
	 */
	public void setNegExamples(SortedSet<OWLIndividual> negExamples) {
		this.negExamples = negExamples;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String str = 
			" Class name:  " + namedClass.getClass().getName() + "\n" +
			"\tCoverage:    " + coverage + "\n" +
			"\tF-Measure:   " + FMeasure + "\n" +
			"\tP-F-Measure: "	+ PFMesure + "\n" +
			"\tMapping:\n";
		int i =1;
		for(EvaluatedDescription eD: mapping){
			str += "\t\t(" + i++ + ") " + eD + "\n";
		}

		str +=	"\tPositive examples:\n";
		i =1;
		for(OWLIndividual in: posExamples){
			 str += "\t\t(" + i++ + ") " + in.toStringID() + "\n";
		}
		
		str += "\tNegative examples:\n ";
		i =1;
		for(OWLIndividual in: negExamples){
			 str += "\t\t(" + i++ + ") " + in.toStringID() +"\n"; 
		}
		
		str += "\tClassifier:\n ";
		i =1;
		for(SimpleClassifier cl: classifiers){
			 str += "\t\t(" + i++ + ") " + cl +"\n"; 
		}
		
		str += "\tINSTANCE MAPPINGS(LIMES RESULTS):\n ";
		str += "\t\t" + getInstanceMapping().toString() +"\n"; 
			 
		return str;
	}
}

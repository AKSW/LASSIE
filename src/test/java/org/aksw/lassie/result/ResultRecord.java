/**
 * 
 */
package org.aksw.lassie.result;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.aksw.lassie.Evaluation;
import org.aksw.lassie.bmGenerator.Modifier;
import org.apache.log4j.Logger;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.NamedClass;
import org.h2.util.New;

import com.google.common.collect.Multimap;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.FileManager;

/**
 * @author sherif
 *
 */
public class ResultRecord {
	private static final Logger logger = Logger.getLogger(Evaluation.class.getName());
	public int nrOfClasses;////////////////
	public int nrOfInstancesPerClass;////////////////
	public int nrOfClassModifiers;/////////////////
	public int nrOfInstanceModifiers;////////////////
	public Map<Modifier, Double> instanceModefiersAndRates = new HashMap<Modifier, Double>();////////////
	public Map<Modifier, Double> classModefiersAndRates = new HashMap<Modifier, Double>();/////////////////
	public int NrOfIterations;//////////////////
	public List<IterationRecord> iterationsRecords = new ArrayList<IterationRecord>();  ////////////////////////

	//Contractors
	public ResultRecord(){
	}
	
	/**
	 * Set positive examples for a given named class in a given iteration  
	 * @param positiveExamples
	 * @param iterationNr
	 * @param nc
	 * @author sherif
	 */
	public void setPositiveExample(SortedSet<Individual> positiveExamples, int iterationNr, NamedClass nc){
		for(ClassRecord cr : iterationsRecords.get(iterationNr).classesRecords){
			if(cr.namedClass.equals(nc) ){
				cr.setPosExamples(positiveExamples);
				return;
			}
		}
	}
	
	
	/**
	 * Set negative examples for a given named class in a given iteration  
	 * @param negativeExamples
	 * @param iterationNr
	 * @param nc
	 * @author sherif
	 */
	public void setNegativeExample(SortedSet<Individual> negativeExamples, int iterationNr, NamedClass nc){
		for(ClassRecord cr : iterationsRecords.get(iterationNr).classesRecords){
			if(cr.namedClass.equals(nc) ){
				cr.setNegExamples(negativeExamples);
				return;
			}
		}
	}
	
	
	/**
	 * Set coverage for a given named class in a given iteration 
	 * @param coverage
	 * @param iterationNr
	 * @param nc
	 * @author sherif
	 */
	public void setCoverage(double coverage, int iterationNr, NamedClass nc){
		for(ClassRecord cr : iterationsRecords.get(iterationNr).classesRecords){
			if(cr.namedClass.equals(nc) ){
				cr.setCoverage(coverage);
				return;
			}
		}
	}
	
	
	/**
	 * Set F-Measure for a given named class in a given iteration 
	 * @param fMeasure
	 * @param iterationNr
	 * @param nc
	 * @author sherif
	 */
	public void setFMeasure(double fMeasure, int iterationNr, NamedClass nc){
		for(ClassRecord cr : iterationsRecords.get(iterationNr).classesRecords){
			if(cr.namedClass.equals(nc) ){
				cr.setFMeasure(fMeasure);
				return;
			}
		}
	}
	
	
	/**
	 * Set pseudo mapping for a given named class in a given iteration 
	 * @param mapping
	 * @param iterationNr
	 * @param nc
	 * @author sherif
	 */
	public void setMapping(List<? extends EvaluatedDescription> mapping, int iterationNr, NamedClass nc){
		for(ClassRecord cr : iterationsRecords.get(iterationNr).classesRecords){
			if(cr.namedClass.equals(nc) ){
				cr.setMapping(mapping);
				return;
			}
		}
	}
	
	/**
	 * Set pseudo F-Measure for a given named class in a given iteration 
	 * @param pFMesure
	 * @param iterationNr
	 * @param nc
	 * @author sherif
	 */
	public void setPFMeasure(double pFMesure, int iterationNr, NamedClass nc){
		for(ClassRecord cr : iterationsRecords.get(iterationNr).classesRecords){
			if(cr.namedClass.equals(nc) ){
				cr.setPFMesure(pFMesure);
				return;
			}
		}
	}
	

	/**
	 * @return the nrOfCLasses
	 */
	public int getNrOfCLasses() {
		return nrOfClasses;
	}



	/**
	 * @param nrOfCLasses the nrOfCLasses to set
	 */
	public void setNrOfCLasses(int nrOfClasses) {
		this.nrOfClasses = nrOfClasses;
	}



	/**
	 * @return the nrOfInstancesPerClass
	 */
	public int getNrOfInstancesPerClass() {
		return nrOfInstancesPerClass;
	}



	/**
	 * @param nrOfInstancesPerClass the nrOfInstancesPerClass to set
	 */
	public void setNrOfInstancesPerClass(int nrOfInstancesPerClass) {
		this.nrOfInstancesPerClass = nrOfInstancesPerClass;
	}



	/**
	 * @return the nrOfClassModifiers
	 */
	public int getNrOfClassModifiers() {
		return nrOfClassModifiers;
	}



	/**
	 * @param nrOfClassModifiers the nrOfClassModifiers to set
	 */
	public void setNrOfClassModifiers(int nrOfClassModifiers) {
		this.nrOfClassModifiers = nrOfClassModifiers;
	}



	/**
	 * @return the nrOfInstanceModifiers
	 */
	public int getNrOfInstanceModifiers() {
		return nrOfInstanceModifiers;
	}



	/**
	 * @param nrOfInstanceModifiers the nrOfInstanceModifiers to set
	 */
	public void setNrOfInstanceModifiers(int nrOfInstanceModifiers) {
		this.nrOfInstanceModifiers = nrOfInstanceModifiers;
	}



	/**
	 * @return the instanceModefiersAndRates
	 */
	public Map<Modifier, Double> getInstanceModefiersAndRates() {
		return instanceModefiersAndRates;
	}



	/**
	 * @param instanceModefiersAndRates the instanceModefiersAndRates to set
	 */
	public void setInstanceModefiersAndRates(
			Map<Modifier, Double> instanceModefiersAndRates) {
		this.instanceModefiersAndRates = instanceModefiersAndRates;
	}



	/**
	 * @return the classModefiersAndRates
	 */
	public Map<Modifier, Double> getClassModefiersAndRates() {
		return classModefiersAndRates;
	}



	/**
	 * @param classModefiersAndRates the classModefiersAndRates to set
	 */
	public void setClassModefiersAndRates(
			Map<Modifier, Double> classModefiersAndRates) {
		this.classModefiersAndRates = classModefiersAndRates;
	}



	/**
	 * @return the nrOfIterations
	 */
	public int getNrOfIterations() {
		return NrOfIterations;
	}



	/**
	 * @param nrOfIterations the nrOfIterations to set
	 */
	public void setNrOfIterations(int nrOfIterations) {
		NrOfIterations = nrOfIterations;
	}



	/**
	 * @return the iterationsRecords
	 */
	public List<IterationRecord> getIterationsRecords() {
		return iterationsRecords;
	}



	/**
	 * @param iterationsRecords the iterationsRecords to set
	 */
	public void setIterationsRecords(List<IterationRecord> iterationRecord) {
		this.iterationsRecords = iterationRecord;
	}

	public void addIterationRecord(IterationRecord iterationRecord) {
		this.iterationsRecords.add(iterationRecord);
	}
	
	public void addIterationRecord(int i, IterationRecord iterationRecord) {
		this.iterationsRecords.add(i, iterationRecord);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String str = "********** Results **********\n" +
				"# of classes:\t" + nrOfClasses + "\n" +
				"# of instances per class:\t" + nrOfInstancesPerClass + "\n" +
				"# of classModifiers:\t" + nrOfClassModifiers + "\n" +
				"# of instance modifiers:\t" + nrOfInstanceModifiers + "\n" +
				"Class modifier:\n";
		int i=1;
		for( Modifier m : classModefiersAndRates.keySet()){
			str += "\t" + i + ". " + m.getSimpleName() + "\tRate: " + instanceModefiersAndRates.get(m)*100 + "%\n" ;
		}
		
		str += "Instance modifiers:\n";
		
		i=1;
		for( Modifier m : instanceModefiersAndRates.keySet()){
			str += "\t" + i + ". " + m.getSimpleName() + "\tRate: " + instanceModefiersAndRates.get(m)*100 + "%\n" ;
		}
		
		str += "# of iterations:\t" + NrOfIterations + "\n";
		
		for(IterationRecord iR : iterationsRecords){
			str += iR.toString() + "\n";
		}
		return str;
	}
	
	public void saveToFile(String fileName) throws IOException{
		long startTime = System.currentTimeMillis();
		String content = this.toString();
		 
		File file = new File(fileName);

		// if file does not exists, then create it
		if (!file.exists()) {
			file.createNewFile();
		}

		BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
		bw.write(content);
		bw.close();

		long totalTime = System.currentTimeMillis() - startTime;
		logger.info("Results file writing done in " + totalTime + "ms.");
	}



	public static void main(String[] args){

	}

}

/**
 * 
 */
package org.aksw.lassie.result;

import java.util.ArrayList;
import java.util.List;


/**
 * @author sherif
 *
 */
public class LassieIterationRecorder {
	
	public int IterationNr; 
	public List<LassieClassRecorder> classesRecords = new ArrayList<LassieClassRecorder>();
	public double avgCoverage;
	public double avgFMeasure;
	public double avgPFMeasure;
	public long executionTime;
	
	
	
	/**
	 * @return the executionTime
	 */
	public long getExecutionTime() {
		return executionTime;
	}


	/**
	 * @param executionTime the executionTime to set
	 */
	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}


	/**
	 * @param iterationNr
	 *@author sherif
	 */
	public LassieIterationRecorder(int iterationNr) {
		super();
		IterationNr = iterationNr;
	}


	/**
	 * 
	 *@author sherif
	 */
	public LassieIterationRecorder() {
		super();
	}
	
	
	/**
	 * @return the iterationNr
	 */
	public int getIterationNr() {
		return IterationNr;
	}


	/**
	 * @param iterationNr the iterationNr to set
	 */
	public void setIterationNr(int iterationNr) {
		IterationNr = iterationNr;
	}


	/**
	 * @return the classesRecords
	 */
	public List<LassieClassRecorder> getClassesRecords() {
		return classesRecords;
	}


	/**
	 * @param classesRecords the classesRecords to set
	 */
	public void setClassesRecords(List<LassieClassRecorder> classRecord) {
		this.classesRecords = classRecord;
	}
	
	public void addClassRecord(LassieClassRecorder classRecord) {
		this.classesRecords.add(classRecord);
	}


	/**
	 * @return the avgCoverage
	 */
	public double getAvgCoverage() {
		if(classesRecords.size() == 0)
			return 0d;
		
		double sum = 0d;
		int count = 0;
		for(LassieClassRecorder cR: classesRecords){
			sum += cR.getCoverage();
			count++;
		}
		avgCoverage = sum/(double)count;
		return avgCoverage;
	}


	/**
	 * @param avgCoverage the avgCoverage to set
	 */
	public void setAvgCoverage(double avgCoverage) {
		this.avgCoverage = avgCoverage;
	}


	/**
	 * @return the avgFMeasure
	 */
	public double getAvgFMeasure() {
		if(classesRecords.size() == 0)
			return 0d;
		
		double sum = 0d;
		int count = 0;
		for(LassieClassRecorder cR: classesRecords){
			sum += cR.getFMeasure();
			count++;
		}
		avgFMeasure = sum/(double)count;
		return avgFMeasure;
	}


	/**
	 * @param avgFMeasure the avgFMeasure to set
	 */
	public void setAvgFMeasure(double avgFMeasure) {
		this.avgFMeasure = avgFMeasure;
	}


	/**
	 * @return the avgPFMeasure
	 */
	public double getAvgPFMeasure() {
		if(classesRecords.size() == 0)
			return 0d;
		
		double sum = 0d;
		int count = 0;
		for(LassieClassRecorder cR: classesRecords){
			sum += cR.getPFMesure();
			count++;
		}
		avgPFMeasure = sum/(double)count;
		return avgPFMeasure;
	}


	/**
	 * @param avgPFMeasure the avgPFMeasure to set
	 */
	public void setAvgPFMeasure(double avgPFMeasure) {
		this.avgPFMeasure = avgPFMeasure;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String str = "---------- " + IterationNr + ". Iteration ----------\n" +
				"Average coverage:         " + getAvgCoverage()   + "\n" +
				"Average real F-Measure:   " + getAvgFMeasure()   + "\n" +
				"Average pseudo F-Measure: " + getAvgPFMeasure()  + "\n" +
				"Execution time:           " + getExecutionTime() + "ms.\n" +
				"CLASSES' RESULTS DETAILS:\n";
		int i = 1;
		for(LassieClassRecorder cR : classesRecords){
			str += "(" + i++ + ") " + cR.toString() + "\n";
		}

		return str;
	}


}

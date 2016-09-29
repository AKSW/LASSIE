/**
 * 
 */
package org.aksw.lassie.result;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.aksw.lassie.bmGenerator.Modifier;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.ml.algorithm.euclid.SimpleClassifier;
import org.apache.log4j.Logger;
import org.dllearner.core.EvaluatedDescription;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;


/**
 * @author sherif
 *
 */
public class LassieResultRecorder {
    private static final Logger logger = Logger.getLogger(LassieResultRecorder.class);

    public int nrOfClasses;
    public int nrOfInstancesPerClass;
    public int nrOfClassModifiers;
    public int nrOfInstanceModifiers;
    public Map<Modifier, Double> instanceModefiersAndRates = new HashMap<>();
    public Map<Modifier, Double> classModefiersAndRates = new HashMap<>();
    public int NrOfIterations;
    public List<LassieIterationRecorder> iterationsRecords = new ArrayList<>(); 
    public long totalExecutionTime;
    public double totalCoverage;
    public double avgIterationExecutionTime;


    /**
     * @return the totalExecutionTime
     */
    public long getTotalExecutionTime() {
        totalExecutionTime = 0l;
        for(LassieIterationRecorder ir : iterationsRecords){
            totalExecutionTime += ir.getExecutionTime();
        }
        return totalExecutionTime;
    }

    /**
     * @param totalExecutionTime the totalExecutionTime to set
     */
    public void setTotalExecutionTime(long totalExecutionTime) {
        this.totalExecutionTime = totalExecutionTime;
    }


    /**
     * Initialize all iterations and all classes 
     * @param iterationCount
     * @param sourceClasses
     *@author sherif
     */
    public LassieResultRecorder(int iterationCount, Set<OWLClass> inputClasses) {
        NrOfIterations = iterationCount;
        nrOfClasses = inputClasses.size();
        for(int i=1 ; i <= iterationCount ; i++){
            LassieIterationRecorder ir = new LassieIterationRecorder(i);
            for(OWLClass nc : inputClasses){
                ir.addClassRecord(new LassieClassRecorder(nc));
            }
            this.addIterationRecord(ir);
        }
    }

    /**
     * @return the avgIterationExecutionTime
     */
    public double getAvgIterationExecutionTime() {
        double sum = 0d;
        for(LassieIterationRecorder ir : iterationsRecords){
            sum += ir.getExecutionTime();
        }
        avgIterationExecutionTime = sum / (double) iterationsRecords.size();
        return avgIterationExecutionTime;
    }

    /**
     * @param avgIterationExecutionTime the avgIterationExecutionTime to set
     */
    public void setAvgIterationExecutionTime(long avgIterationExecutionTime) {
        this.avgIterationExecutionTime = avgIterationExecutionTime;
    }

    /**
     * Returns specific iterationRecord giving its number as input if fund, otherwise returns null
     * @param iterationNr
     * @return
     * @author sherif
     */
    public LassieIterationRecorder getIterationRecord(int iterationNr){
        for(LassieIterationRecorder ir : iterationsRecords){
            if(ir.getIterationNr() == iterationNr){
                return ir;
            }
        }
        return null;
    }

    /**
     * Set positive examples for a given named class in a given iteration  
     * @param positiveExamples
     * @param iterationNr
     * @param nc
     * @author sherif
     */
    public void setPositiveExample(SortedSet<OWLIndividual> positiveExamples, int iterationNr, OWLClass nc){
        if(positiveExamples.size() == 0){
            logger.warn("No positive example to set");
            return;
        }
        for(LassieClassRecorder cr : getIterationRecord(iterationNr).classesRecords){
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
    public void setNegativeExample(SortedSet<OWLIndividual> negativeExamples, int iterationNr, OWLClass nc){
        if(negativeExamples.size() == 0){
            logger.warn("No negative example to set");
            return;
        }
        for(LassieClassRecorder cr : getIterationRecord(iterationNr).classesRecords){
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
    public void setCoverage(double coverage, int iterationNr, OWLClass nc){
        for(LassieClassRecorder cr : getIterationRecord(iterationNr).classesRecords){
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
    public void setFMeasure(double fMeasure, int iterationNr, OWLClass nc){
        for(LassieClassRecorder cr : getIterationRecord(iterationNr).classesRecords){
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
    public void setMapping(List<? extends EvaluatedDescription<?>> mapping, int iterationNr, OWLClass nc){

        for(LassieClassRecorder cr : getIterationRecord(iterationNr).classesRecords){
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
    public void setPFMeasure(double pFMesure, int iterationNr, OWLClass nc){
        for(int itr = iterationNr ; itr <= NrOfIterations; itr++){
            for(LassieClassRecorder cr : getIterationRecord(itr).classesRecords){
                if(cr.namedClass.equals(nc) ){
                    cr.setPFMesure(pFMesure);
                }
            }
        }
    }

    /**
     * Set instance mapping for a given named class in a given iteration 
     * @param COVERAGE
     * @param iterationNr
     * @param nc
     * @author sherif
     */
    public void setInstanceMapping(AMapping instanceMapping, int iterationNr, OWLClass nc){
        for(LassieClassRecorder cr : getIterationRecord(iterationNr).classesRecords){
            if(cr.namedClass.equals(nc) ){
                cr.setInstanceMapping(instanceMapping);
                return;
            }
        }
    }

    public void setClassifier(List<SimpleClassifier> classifiers, int iterationNr, OWLClass nc){
        for(LassieClassRecorder cr : getIterationRecord(iterationNr).classesRecords){
            if(cr.namedClass.equals(nc) ){
                cr.setClassifiers(classifiers);
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
    public List<LassieIterationRecorder> getIterationsRecords() {
        return iterationsRecords;
    }



    /**
     * @param iterationsRecords the iterationsRecords to set
     */
    public void setIterationsRecords(List<LassieIterationRecorder> iterationRecord) {
        this.iterationsRecords = iterationRecord;
    }

    public void addIterationRecord(LassieIterationRecorder iterationRecord) {
        this.iterationsRecords.add(iterationRecord);
    }

    public void addIterationRecord(int i, LassieIterationRecorder iterationRecord) {
        this.iterationsRecords.add(i, iterationRecord);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String str = "********** Results **********\n" +
                "Number of classes:             " + nrOfClasses + "\n" +
                "Number of instances per class: " + nrOfInstancesPerClass + "\n" +
                "Number of classModifiers:      " + nrOfClassModifiers + "\n" +
                "Number of instance modifiers:  " + nrOfInstanceModifiers + "\n" +
                "Class modifier:\n";

        int i=1;
        for( Modifier m : classModefiersAndRates.keySet()){
            str += "\t" + i++ + ". " + m.getSimpleName() + "\tRate: " + classModefiersAndRates.get(m)*100 + "%\n" ;
        }

        str += "Instance modifiers:\n";

        i=1;
        for( Modifier m : instanceModefiersAndRates.keySet()){
            str += "\t" + i++ + ". " + m.getSimpleName() + "\tRate: " + instanceModefiersAndRates.get(m)*100 + "%\n" ;
        }

        str += "Number of iterations:\t" + NrOfIterations + "\n";
        str += "Average iteration execution time:  " + getAvgIterationExecutionTime() + "ms.\n" ;
        str += "Total execution time:  " + getTotalExecutionTime() + "ms.\n" ;

        str += "\nitrNr\tavgCoverage\tavgF\tavgPF\tTime\n"; 
        for(LassieIterationRecorder iR : iterationsRecords){
            str += iR.getIterationNr() + "\t" + iR.getAvgCoverage()+ "\t" + iR.getAvgFMeasure()+ "\t" + iR.getAvgPFMeasure()+ "\t" + iR.getExecutionTime() + "\n";
        }
        str += "\n";

        str += "ITERATIONS' RESULTS DETAILS:\n";

        for(LassieIterationRecorder iR : iterationsRecords){
            str += iR.toString() + "\n";
        }
        return str;
    }

    public void saveToFile(String fileName) throws IOException{
        logger.info("Saving results to file: " + fileName + " ...");
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
        logger.info("Done in "+ totalTime + "ms.");
    }



    public static void main(String[] args) throws IOException{
        Set<OWLClass> inputClasses = new HashSet<OWLClass>();
        OWLDataFactory owlDataFactory = new OWLDataFactoryImpl();
        inputClasses.add(owlDataFactory.getOWLClass(IRI.create("a")));
        inputClasses.add(owlDataFactory.getOWLClass(IRI.create("b")));
        inputClasses.add(owlDataFactory.getOWLClass(IRI.create("c")));
        inputClasses.add(owlDataFactory.getOWLClass(IRI.create("d")));
        LassieResultRecorder rr = new LassieResultRecorder(3, inputClasses);
        rr.saveToFile("test.txt");

    }

}

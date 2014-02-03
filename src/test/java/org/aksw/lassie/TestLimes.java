/**
 * 
 */
package org.aksw.lassie;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.lassie.bmGenerator.ClassIdentityModifier;
import org.aksw.lassie.bmGenerator.ClassRenameModifier;
import org.aksw.lassie.bmGenerator.ClassSplitModifier;
import org.aksw.lassie.bmGenerator.InstanceIdentityModifier;
import org.aksw.lassie.bmGenerator.InstanceMisspellingModifier;
import org.aksw.lassie.bmGenerator.Modifier;
import org.aksw.lassie.core.ExpressiveSchemaMappingGenerator;
import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.kb.KnowledgebaseSampleGenerator;
import org.aksw.lassie.kb.LocalKnowledgeBase;
import org.aksw.lassie.result.LassieResultRecorder;
import org.apache.log4j.Logger;
import org.dllearner.core.owl.NamedClass;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

/**
 * @author sherif
 *
 */
public class TestLimes {
	private static final Logger logger = Logger.getLogger(TestLimes.class.getName());
	
	static int nrOfExperimentRepeats = 1;
	static Map<Modifier, Double> classModifiersAndRates 	= new HashMap<Modifier, Double>();
	static Map<Modifier, Double> instanceModifiersAndRates = new HashMap<Modifier, Double>();
	static int maxNrOfIterations = 3;
	static int nrOfClasses = 1;
	static int nrOfInstancesPerClass = 5;
	static String toyDatasetFile = "src/main/resources/datasets/toydataset/toydataset_scientist_mammal_plant.nt";
//	static String toyDatasetFile = "src/main/resources/datasets/toydataset/toydataset_scientist_mammal.nt";
//	static String toyDatasetFile = "datasets/toydataset/toydataset_scientist.nt";
//	static String toyDatasetFile = "datasets/toydataset/toydataset_mammal.nt";
	static String outputFile = "limesTestResult.txt";
	static Set<NamedClass> testClasses = new HashSet<NamedClass>();
	
	public static Model readModel(String fileNameOrUri)
	{
		long startTime = System.currentTimeMillis();
		Model model=ModelFactory.createDefaultModel();
		java.io.InputStream in = FileManager.get().open( fileNameOrUri );
		if (in == null) {
			throw new IllegalArgumentException(
					"File: " + fileNameOrUri + " not found");
		}
		if(fileNameOrUri.contains(".ttl") || fileNameOrUri.contains(".n3")){
			logger.info("Opening Turtle file");
			model.read(in, null, "TTL");
		}else if(fileNameOrUri.contains(".rdf")){
			logger.info("Opening RDFXML file");
			model.read(in, null);
		}else if(fileNameOrUri.contains(".nt")){
			logger.info("Opening N-Triples file");
			model.read(in, null, "N-TRIPLE");
		}else{
			logger.info("Content negotiation to get RDFXML from " + fileNameOrUri);
			model.read(fileNameOrUri);
		}
		logger.info("Loading " + fileNameOrUri + " is done in " + (System.currentTimeMillis()-startTime) + "ms.");
		return model;
	}
	
	public  static void test() throws IOException{
		
		Model toyDatasetModel = readModel(toyDatasetFile); 
		
		//do the experiment nrOfExperimentRepeats times for each modifier 
		for(int expNr = 0 ; expNr < nrOfExperimentRepeats ; expNr++){
			System.setOut(new PrintStream("/dev/null"));
			long startTime = System.currentTimeMillis();
//			Evaluation evaluator = new Evaluation(maxNrOfIterations, nrOfClasses, nrOfInstancesPerClass, classModifiersAndRates, instanceModifiersAndRates);

			testClasses.add(new NamedClass("http://dbpedia.org/ontology/Scientist"));
			testClasses.add(new NamedClass("http://dbpedia.org/ontology/Mammal"));
			testClasses.add(new NamedClass("http://dbpedia.org/ontology/Plant"));
			
			classModifiersAndRates.put(new ClassSplitModifier(), 1.0);
			instanceModifiersAndRates.put(new InstanceMisspellingModifier(), 0.5);
//			classModifiers.add(new ClassRenameModifier());
//			classModifiers.add(new ClassDeleteModifier());
//			classModifiers.add(new ClassIdentityModifier());
//			classModifiers.add(new ClassMergeModifier());
//			classModifiers.add(new ClassSplitModifier());
//			classModifiers.add(new ClassTypeDeleteModifier());

//			instanceModifiers.add(new InstanceAbbreviationModifier());
//			instanceModifiers.add(new InstanceAcronymModifier());
//			instanceModifiers.add(new InstanceIdentityModifier());
//			instanceModifiers.add(new InstanceMergeModifier());
//			instanceModifiers.add(new InstanceMisspellingModifier());
//			instanceModifiers.add(new InstancePermutationModifier());
//			instanceModifiers.add(new InstanceSplitModifier());
			
			LocalKnowledgeBase sourceKB = new LocalKnowledgeBase(toyDatasetModel, "http://dbpedia.org/ontology/");
//			KnowledgeBase targetKB = new LocalKnowledgeBase(toyDatasetModel, "http://dbpedia.org/ontology/");
			Model targetKBModel = (new Evaluation()).createTestDataset(sourceKB.getModel(), instanceModifiersAndRates, classModifiersAndRates, testClasses.size(), 5);
			KnowledgeBase targetKB = new LocalKnowledgeBase(targetKBModel, "http://dbpedia.org/ontology/");
			
			ExpressiveSchemaMappingGenerator generator = new ExpressiveSchemaMappingGenerator(sourceKB, targetKB, maxNrOfIterations);
			generator.setTargetDomainNameSpace("http://dbpedia.org/ontology/");
			
			LassieResultRecorder experimentResults = generator.run(testClasses, false);
			
			experimentResults.setNrOfInstancesPerClass(nrOfInstancesPerClass);
			experimentResults.setNrOfClassModifiers(classModifiersAndRates.size());
			experimentResults.setNrOfInstanceModifiers(instanceModifiersAndRates.size());
			experimentResults.setClassModefiersAndRates(classModifiersAndRates);
			experimentResults.setInstanceModefiersAndRates(instanceModifiersAndRates);
			
			logger.info("Experiment Results:\n" + experimentResults.toString());
			experimentResults.saveToFile(outputFile);
			
			long experimentTime = System.currentTimeMillis() - startTime;
			System.out.println("Experiment time: " + experimentTime + "ms.");
			logger.info("Experiment (" + (expNr+1) + ")  is done in " + experimentTime + "ms.");
		}
	}
	
	public static void main(String args[]) throws IOException{
		test();
	}
}

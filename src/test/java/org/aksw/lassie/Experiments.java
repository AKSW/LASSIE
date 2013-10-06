/**
 * 
 */
package org.aksw.lassie;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.lassie.bmGenerator.ClassDeleteModifier;
import org.aksw.lassie.bmGenerator.ClassIdentityModifier;
import org.aksw.lassie.bmGenerator.ClassMergeModifier;
import org.aksw.lassie.bmGenerator.ClassRenameModifier;
import org.aksw.lassie.bmGenerator.ClassSplitModifier;
import org.aksw.lassie.bmGenerator.ClassTypeDeleteModifier;
import org.aksw.lassie.bmGenerator.InstanceAbbreviationModifier;
import org.aksw.lassie.bmGenerator.InstanceAcronymModifier;
import org.aksw.lassie.bmGenerator.InstanceIdentityModifier;
import org.aksw.lassie.bmGenerator.InstanceMergeModifier;
import org.aksw.lassie.bmGenerator.InstanceMisspellingModifier;
import org.aksw.lassie.bmGenerator.InstancePermutationModifier;
import org.aksw.lassie.bmGenerator.InstanceSplitModifier;
import org.aksw.lassie.bmGenerator.Modifier;
import org.apache.log4j.Logger;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

/**
 * @author sherif
 *
 */
public class Experiments {
	private static final Logger logger = Logger.getLogger(Evaluation.class.getName());

	private List<Modifier> classModifiers = new ArrayList<Modifier>();
	private List<Modifier> instanceModifiers = new ArrayList<Modifier>();

	public Experiments(){
		classModifiers.add(new ClassRenameModifier());
		classModifiers.add(new ClassDeleteModifier());
		classModifiers.add(new ClassIdentityModifier());
		classModifiers.add(new ClassMergeModifier());
//		classModifiers.add(new ClassSplitModifier());
		classModifiers.add(new ClassTypeDeleteModifier());

		instanceModifiers.add(new InstanceAbbreviationModifier());
//		instanceModifiers.add(new InstanceAcronymModifier());
		instanceModifiers.add(new InstanceIdentityModifier());
		instanceModifiers.add(new InstanceMergeModifier());
		instanceModifiers.add(new InstanceMisspellingModifier());
		instanceModifiers.add(new InstancePermutationModifier());
		instanceModifiers.add(new InstanceSplitModifier());
	}

	public void runOneClassModifierExperiments(int noOfClasses, int noOfInstancePerClass,int noOfExperimentsPerModifier, 
			double modifierRate, String outputFolder) throws FileNotFoundException{
		runOneClassModifierExperiments(new HashMap<Modifier, Double>(), noOfClasses, noOfInstancePerClass, noOfExperimentsPerModifier, modifierRate, outputFolder);
	}

	public void runOneClassModifierExperiments(Map<Modifier, Double> instanceModifiersAndRates, int noOfClasses, int noOfInstancePerClass,int noOfExperimentsPerModifier, 
			double modifierRate, String outputFolder) throws FileNotFoundException{

		//create a folder for the results if not exist
		File folder = new File(outputFolder).getAbsoluteFile();
		if (!folder.exists()) {
			folder.mkdirs();
		}

		//do the experiment n times for each modifier 
		for(int expNr = 0 ; expNr < noOfExperimentsPerModifier ; expNr++){
			for(Modifier clsModifier : classModifiers){
				logger.info("Running experiment for class modifier: " + clsModifier.getSimpleName());
				long startTime = System.currentTimeMillis();

				System.setOut(new PrintStream("/dev/null"));

				Map<Modifier, Double> classModifiersAndRates = new HashMap<Modifier, Double>();
				classModifiersAndRates.put(clsModifier, modifierRate);
				Evaluation evaluator = new Evaluation(noOfClasses, noOfInstancePerClass, classModifiersAndRates, instanceModifiersAndRates);

				//store result
				evaluator.printShortResults(evaluator.run(), outputFolder + clsModifier.getSimpleName() + "_" + expNr + "_");
				long experimentTime = System.currentTimeMillis() - startTime;
				System.out.println("Experiment time: " + experimentTime + "ms.");
				logger.info("Experimrnt number " + (expNr+1) + " for class " + clsModifier.getSimpleName() + "is done in " + experimentTime + "ms.");
			}
		}
	}


	public void runExperiments(int noOfClasses, int noOfInstancesPerClass, 
			int noOfClassModifiers, int noOfInstanceModifiers, 
			double modifierDestructionRate, double instanceDestructionRate,
			int noOfExperimentRepeats, String outputFolder) throws FileNotFoundException{

		//create a folder for the results if not exist
		File folder = new File(outputFolder).getAbsoluteFile();
		if (!folder.exists()) {
			folder.mkdirs();
		}

		//do the experiment noOfExperimentRepeats times for each modifier 
		for(int expNr = 0 ; expNr < noOfExperimentRepeats ; expNr++){
			System.setOut(new PrintStream("/dev/null"));
			long startTime = System.currentTimeMillis();

			//pick random noOfClassModifiers class modifiers
			Collections.shuffle(classModifiers);
			Map<Modifier, Double> classModifiersAndRates = new HashMap<Modifier, Double>();
			for(int i = 0 ; i < noOfClassModifiers ; i++){
				classModifiersAndRates.put(classModifiers.get(i), modifierDestructionRate/(double)noOfClassModifiers);
			}

			//pick random noOfClassModifiers instance modifiers
			Collections.shuffle(instanceModifiers);
			Map<Modifier, Double> instanceModifiersAndRates = new HashMap<Modifier, Double>();
			for(int i = 0 ; i < noOfInstanceModifiers ; i++){
				instanceModifiersAndRates.put(instanceModifiers.get(i), instanceDestructionRate/(double)noOfInstanceModifiers);
			}

			logger.info("Running experiment(" + (expNr+1) + ") for " + noOfClassModifiers + 
					"class modifier(s) and " + noOfInstanceModifiers + " instance Modifier(s)") ;
			logger.info("Experiment class modifiers and rates: " + classModifiersAndRates);
			logger.info("Experiment instance modifiers and rates:" + instanceModifiersAndRates);

			Evaluation evaluator = new Evaluation(noOfClasses, noOfInstancesPerClass, classModifiersAndRates, instanceModifiersAndRates);

			//store result
			String outputFile = outputFolder + "result_" + noOfClassModifiers + "_" + noOfInstanceModifiers + "_" + expNr + "_"
					+ ((noOfClasses > 0) ? ("-" + noOfClasses + "-" + noOfInstancesPerClass) : "") + ".txt";
			evaluator.printShortResults(evaluator.run(), outputFile);
			long experimentTime = System.currentTimeMillis() - startTime;
			System.out.println("Experiment time: " + experimentTime + "ms.");
			logger.info("Experimrnt (" + (expNr+1) + ") for " + noOfClassModifiers + "class modifier(s) and "
					+ noOfInstanceModifiers + " instance Modifier(s) is done in " + experimentTime + "ms.");
		}
	}




	/**
	 * @param args
	 * @author sherif
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		Experiments experiment = new Experiments();
		Map<Modifier, Double> instanceModifiersAndRates = new HashMap<Modifier, Double>();
		instanceModifiersAndRates.put(new InstanceMisspellingModifier(), 0.2d);
		instanceModifiersAndRates.put(new InstanceAbbreviationModifier(), 0.2d);
		
		experiment.runExperiments(10, 100, 2, 2, 0.5, 0.5, 5,  "/mypartition2/Work/lassie/result/oneClassExperiment/twoClsTwoInsExperiments/");
//		experiment.runOneClassModifierExperiments(instanceModifiersAndRates, 10, 100, 1, 0.5, "/mypartition2/Work/lassie/result/oneClassExperiment/tmp/");

	}

}
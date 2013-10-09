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
import java.util.List;
import java.util.Map;

import org.aksw.lassie.bmGenerator.ClassDeleteModifier;
import org.aksw.lassie.bmGenerator.ClassIdentityModifier;
import org.aksw.lassie.bmGenerator.ClassMergeModifier;
import org.aksw.lassie.bmGenerator.ClassRenameModifier;
import org.aksw.lassie.bmGenerator.ClassTypeDeleteModifier;
import org.aksw.lassie.bmGenerator.InstanceAbbreviationModifier;
import org.aksw.lassie.bmGenerator.InstanceIdentityModifier;
import org.aksw.lassie.bmGenerator.InstanceMergeModifier;
import org.aksw.lassie.bmGenerator.InstanceMisspellingModifier;
import org.aksw.lassie.bmGenerator.InstancePermutationModifier;
import org.aksw.lassie.bmGenerator.InstanceSplitModifier;
import org.aksw.lassie.bmGenerator.Modifier;
import org.apache.log4j.Logger;


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
				evaluator.printResults(evaluator.run(), outputFolder + clsModifier.getSimpleName() + "_" + expNr + "_");
				long experimentTime = System.currentTimeMillis() - startTime;
				System.out.println("Experiment time: " + experimentTime + "ms.");
				logger.info("Experimrnt number " + (expNr+1) + " for class " + clsModifier.getSimpleName() + "is done in " + experimentTime + "ms.");
			}
		}
	}


	public void runExperiments(int nrOfClasses, int nrOfInstancesPerClass, 
			int nrOfClassModifiers, int nrOfInstanceModifiers, 
			double classesDestructionRate, double instancesDestructionRate,
			int nrOfExperimentRepeats, String outputFolder) throws FileNotFoundException{

		//create a folder for the results if not exist
		File folder = new File(outputFolder).getAbsoluteFile();
		if (!folder.exists()) {
			folder.mkdirs();
		}

		//do the experiment noOfExperimentRepeats times for each modifier 
		for(int expNr = 0 ; expNr < nrOfExperimentRepeats ; expNr++){
			System.setOut(new PrintStream("/dev/null"));
			long startTime = System.currentTimeMillis();

			//pick random noOfClassModifiers class modifiers
			Map<Modifier, Double> classModifiersAndRates = new HashMap<Modifier, Double>();
			if(nrOfClassModifiers == 0){
				classModifiersAndRates.put(new ClassIdentityModifier(), classesDestructionRate);
			}else{
				Collections.shuffle(classModifiers);
				for(int i = 0 ; i < nrOfClassModifiers ; i++){
					classModifiersAndRates.put(classModifiers.get(i), classesDestructionRate/(double)nrOfClassModifiers);
				}
			}

			//pick random noOfClassModifiers instance modifiers
			Collections.shuffle(instanceModifiers);
			Map<Modifier, Double> instanceModifiersAndRates = new HashMap<Modifier, Double>();
			for(int i = 0 ; i < nrOfInstanceModifiers ; i++){
				instanceModifiersAndRates.put(instanceModifiers.get(i), instancesDestructionRate/(double)nrOfInstanceModifiers);
			}

			logger.info("Running experiment(" + (expNr+1) + ") for " + nrOfClassModifiers + 
					" class modifier(s) and " + nrOfInstanceModifiers + " instance Modifier(s)") ;
			logger.info("Experiment class modifiers and rates: " + classModifiersAndRates);
			logger.info("Experiment instance modifiers and rates:" + instanceModifiersAndRates);

			Evaluation evaluator = new Evaluation(nrOfClasses, nrOfInstancesPerClass, classModifiersAndRates, instanceModifiersAndRates);

			//store result
			String outputFile = outputFolder + "result_" + nrOfClassModifiers + "_" + nrOfInstanceModifiers + "_" + expNr + "_"
					+ ((nrOfClasses > 0) ? ("-" + nrOfClasses + "-" + nrOfInstancesPerClass) : "") + ".txt";
			evaluator.printResults(evaluator.run(), outputFile);
			long experimentTime = System.currentTimeMillis() - startTime;
			System.out.println("Experiment time: " + experimentTime + "ms.");
			logger.info("Experimrnt (" + (expNr+1) + ") for " + nrOfClassModifiers + "class modifier(s) and "
					+ nrOfInstanceModifiers + " instance Modifier(s) is done in " + experimentTime + "ms.");
		}
	}


	/** 
	 * @param args
	 * @author sherif
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		if(args.length < 8 || args[0].equals("-?")){
			System.err.println("parameters:\narg[0] = number of Classes\narg[1] = number of instances per class\narg[2] = number of class modifiers\n" +
					"arg[3] = number of instance modifiers\narg[4] = classes destruction rate\narg[5] = instances destruction rate\n" +
					"arg[6] = number of experiment repeats\narg[7] = output folder");
		}

		Experiments experiment = new Experiments();
		experiment.runExperiments(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), 
				Integer.parseInt(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Integer.parseInt(args[6]),  args[7]);
	}

}

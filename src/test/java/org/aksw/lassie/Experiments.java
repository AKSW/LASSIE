/**
 * 
 */
package org.aksw.lassie;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import org.aksw.lassie.result.LassieResultRecorder;
import org.apache.log4j.Logger;


/**
 * @author sherif
 *
 */
public class Experiments {
	private static final Logger logger = Logger.getLogger(Experiments.class.getName());

	private List<Modifier> classModifiers = new ArrayList<Modifier>();
	private List<Modifier> instanceModifiers = new ArrayList<Modifier>();

	public Experiments(){
//		classModifiers.add(new ClassRenameModifier());
//		classModifiers.add(new ClassDeleteModifier());
		classModifiers.add(new ClassIdentityModifier());
//		classModifiers.add(new ClassMergeModifier());
		//		classModifiers.add(new ClassSplitModifier());
//		classModifiers.add(new ClassTypeDeleteModifier());

//		instanceModifiers.add(new InstanceAbbreviationModifier());
		//		instanceModifiers.add(new InstanceAcronymModifier());
		instanceModifiers.add(new InstanceIdentityModifier());
//		instanceModifiers.add(new InstanceMergeModifier());
//		instanceModifiers.add(new InstanceMisspellingModifier());
//		instanceModifiers.add(new InstancePermutationModifier());
//		instanceModifiers.add(new InstanceSplitModifier());
	}

	public void runExperiments(
			int nrOfClasses, 				int nrOfInstancesPerClass, 
			int nrOfClassModifiers, 		int nrOfInstanceModifiers, 
			double classesDestructionRate, 	double instancesDestructionRate,
			int nrOfExperimentRepeats, 		int maxNrOfIterations, String outputFolder) throws IOException{

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

			logger.info("Running experiment  (" + (expNr+1) + ") for " + nrOfClassModifiers + 
					" class modifier(s) and " + nrOfInstanceModifiers + " instance Modifier(s)") ;
			logger.info("Experiment class modifiers and rates: " + classModifiersAndRates);
			logger.info("Experiment instance modifiers and rates:" + instanceModifiersAndRates);

			Evaluation evaluator = new Evaluation(maxNrOfIterations, nrOfClasses, nrOfInstancesPerClass, classModifiersAndRates, instanceModifiersAndRates);

			//store result
			String outputFile = outputFolder + "result_" + nrOfClassModifiers + "clsMod_" + nrOfInstanceModifiers + "insMod_" + (expNr+1) + "expr_"
					+ ((nrOfClasses > 0) ? (nrOfClasses ) : "all") + "Classes_" + nrOfInstancesPerClass + "insPerCls.txt";

			//			evaluator.printResults(evaluator.run(), outputFile);
			
			LassieResultRecorder experimentResults = evaluator.runNew();
			experimentResults.setNrOfCLasses(nrOfClasses);
			experimentResults.setNrOfInstancesPerClass(nrOfInstancesPerClass);
			experimentResults.setNrOfClassModifiers(nrOfClassModifiers);
			experimentResults.setNrOfInstanceModifiers(nrOfInstanceModifiers);
			experimentResults.setClassModefiersAndRates(classModifiersAndRates);
			experimentResults.setInstanceModefiersAndRates(instanceModifiersAndRates);
			
			logger.info("Experiment Results:\n" + experimentResults.toString());
			experimentResults.saveToFile(outputFile);
			
			long experimentTime = System.currentTimeMillis() - startTime;
			System.out.println("Experiment time: " + experimentTime + "ms.");
			logger.info("Experimrnt (" + (expNr+1) + ") for " + nrOfClassModifiers + " class modifier(s) and "
					+ nrOfInstanceModifiers + " instance Modifier(s) is done in " + experimentTime + "ms.");
		}
	}


	/** 
	 * @param args
	 * @author sherif
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		if(args.length < 9 || args[0].equals("-?")){
			logger.error("Wrong parameters number\nParameters:\n\t" +
					"arg[0] = number of classes\n\t" +
					"arg[1] = number of instances per class\n\t" +
					"arg[2] = number of class modifiers\n\t" +
					"arg[3] = number of instance modifiers\n\t" +
					"arg[4] = classes destruction rate\n\t" +
					"arg[5] = instances destruction rate\n\t" +
					"arg[6] = number of experiment repeats\n\t" +
					"arg[7] = number of iterations per experment\n\t" +
					"arg[8] = output folder");
			System.exit(1);
		}

		Experiments experiment = new Experiments();
		experiment.runExperiments(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), 
				Integer.parseInt(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]), args[8]);
	}

}

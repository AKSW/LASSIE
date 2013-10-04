/**
 * 
 */
package org.aksw.lassie;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
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

/**
 * @author sherif
 *
 */
public class Experiments {
	private static final Logger logger = Logger.getLogger(Evaluation.class.getName());

	private Set<Modifier> classModifiers = new HashSet<Modifier>();
	private Set<Modifier> instanceModifiers = new HashSet<Modifier>();

	public Experiments(){
		classModifiers.add(new ClassRenameModifier());
		classModifiers.add(new ClassDeleteModifier());
		classModifiers.add(new ClassIdentityModifier());
		classModifiers.add(new ClassMergeModifier());
		classModifiers.add(new ClassSplitModifier());
		classModifiers.add(new ClassTypeDeleteModifier());

		instanceModifiers.add(new InstanceAbbreviationModifier());
		instanceModifiers.add(new InstanceAcronymModifier());
		instanceModifiers.add(new InstanceIdentityModifier());
		instanceModifiers.add(new InstanceMergeModifier());
		instanceModifiers.add(new InstanceMisspellingModifier());
		instanceModifiers.add(new InstancePermutationModifier());
		instanceModifiers.add(new InstanceSplitModifier());
	}

	public void runOneClassModifierExperiments(int noOfClasses, int noOfInstancePerClass,int noOfExperimentsPerModifier, 
			double modifierRate, String outputFolder) throws FileNotFoundException{
		//create a folder for the results 
		File folder = new File(outputFolder).getAbsoluteFile();
		if (!folder.exists()) {
			folder.mkdirs();
		}

		//do the experiment n times for each modifier 
		for(int expNr = 0 ; expNr < noOfExperimentsPerModifier ; expNr++){
			for(Modifier clsModifier : classModifiers){
				logger.info("Running experiment for class modifier: " + clsModifier.getSimpleName());
				System.setOut(new PrintStream("/dev/null"));
				Map<Modifier, Double> classModifiersAndRates = new HashMap<Modifier, Double>();
				classModifiersAndRates.put(clsModifier, modifierRate);
				Evaluation evaluator = new Evaluation(noOfClasses, noOfInstancePerClass, classModifiersAndRates, new HashMap<Modifier, Double>());
				evaluator.printShortResults(evaluator.run(), outputFolder + clsModifier.getSimpleName() + "_" + expNr + "_");
			}
		}
	}

	/**
	 * @param args
	 * @author sherif
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		Experiments experiment = new Experiments();
		experiment.runOneClassModifierExperiments(50, 100, 5, 0.5, "/mypartition2/Work/lassie/result/oneClassExperiment/");

	}

}

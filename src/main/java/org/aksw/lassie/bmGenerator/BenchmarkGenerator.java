/**
 * 
 */
package org.aksw.lassie.bmGenerator;


import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * @author Sherif
 *
 */
public class BenchmarkGenerator extends Modifier{

	/**
	 * @param m
	 *@author sherif
	 */
	BenchmarkGenerator(Model m) {
		super(m);
	}

	BenchmarkGenerator() {
	}


	/**
	 * @param basemodel
	 * @param modefiersAndRates
	 * @return destroyed model
	 * @author Sherif
	 */
	Model destroy (Model m, Map<? extends Modifier, Double> modefiersAndRates){
		baseModel= m;
		return destroyInstances(modefiersAndRates);
	}


	Model destroyInstances (Map<? extends Modifier, Double> modefiersAndRates){
		return destroyInstances (modefiersAndRates, 0d);
	}

	/**
	 * @param modefiersAndRates
	 * @return destroyed model
	 * @author Sherif
	 */
	Model destroyInstances (Map<? extends Modifier, Double> modefiersAndRates, double startPointRatio){
		Model inputModel= ModelFactory.createDefaultModel();

		if(properties.size()==0 && inputClassUri==null){   // If the modifier properties are not set and no Class is set then divide the whole Model
			inputModel=baseModel;
		}else if(inputClassUri!=null){ // if class is set the divide based on class
			destroyedClassModel = getInputClassModel(inputClassUri);
			inputModel=destroyedClassModel;
			if(properties.size()>0){    // and if the modifier properties are set and Class is set then divide the the properties of that class 
				destroyedPropertiesModel=getDestroyedPropertiesModel(destroyedClassModel);
				inputModel=destroyedPropertiesModel;
			}
		}else if(properties.size()>0){						//Else if the properties is set then divide based on the properties Model
			destroyedPropertiesModel = getDestroyedPropertiesModel(baseModel);
			inputModel = destroyedPropertiesModel;
		}

		long inputModelOffset = (long) Math.floor(startPointRatio*inputModel.size());
		if(startPointRatio > 0){
			// Add the skipped portion of the input Model (form beginning to startPoint)
			Model InputModelStartPortion = getSubModel(inputModel, inputModelOffset, 0);
			destroyedModel.add(InputModelStartPortion); 
			//			inputModel.remove(InputModelStartPortion);
		}

		for(Entry<? extends Modifier, Double> mod2rat: modefiersAndRates.entrySet() ){
			Modifier modifer = mod2rat.getKey();
			Double   rate    = mod2rat.getValue();
			long subModelSize = (long) (inputModel.size()*rate);

			if((inputModelOffset+subModelSize) > inputModel.size()){
				System.out.println("The sum of modifiers rates is grater than 100% ... exit with error");
				System.exit(1);
			}

			Model subModel = getSubModel(inputModel, subModelSize, inputModelOffset);
			modifer.destroy(subModel);
			inputModelOffset += subModelSize;
		}

		// Add the rest of the input Model (form current inputModelOffset to the end)
		Model InputModelRest = getSubModel(inputModel, inputModel.size()-inputModelOffset,inputModelOffset);
		destroyedModel.add(InputModelRest); //tell end

		// add the rest of the non-destroyed part of the base Model
		if(destroyedPropertiesModel.size()>0){
			baseModel.remove(destroyedPropertiesModel);
			destroyedModel.add(baseModel); 
		}
		if(destroyedClassModel.size()>0){
			baseModel.remove(destroyedClassModel);
			destroyedModel.add(baseModel); 
		}
		if(inputClassUri!=null && outputClassUri!=null){
			destroyedModel = renameClass(destroyedModel, inputClassUri,outputClassUri);
		}
		return destroyedModel;
	}


	Model destroyClasses (Map<? extends Modifier, Double> modefiersAndRates){
		Model resultModel = ModelFactory.createDefaultModel();
		List<String> classNames = getClasses(baseModel);
		int offset = 0; 
		for(Entry<? extends Modifier, Double> mod2rat: modefiersAndRates.entrySet() ){
			Modifier modifer = mod2rat.getKey();
			Double   rate    = mod2rat.getValue();
			Model subModel = ModelFactory.createDefaultModel();
			System.out.println("Destroying classe(s):");
			for(int i=offset ; i < (offset+(int) Math.floor(classNames.size()*rate)) ; i++){
				subModel.add(getClassInstancesModel(classNames.get(i)));
				System.out.println(classNames.get(i));
			}
			System.out.println("containing " + subModel.size() + " instances with modifier " + modifer.getClass().getSimpleName());
			resultModel.add(modifer.destroy(subModel));
			offset += Math.floor(classNames.size()*rate);
		}
		//add the rest of base model (if any)
		if(offset<classNames.size()){
			for(int i=offset ; i<classNames.size() ; i++){
				resultModel.add(getClassInstancesModel(classNames.get(i)));
			}
		}
		return resultModel;
	}


	/**
	 * @return sub model contains a certain class
	 * @author sherif
	 */
	public Model getInputClassModel(String classUri){
		Model result=ModelFactory.createDefaultModel();
		String sparqlQueryString= "CONSTRUCT {?s ?p ?o} WHERE {?s a <"+classUri+">. ?s ?p ?o}";
		QueryFactory.create(sparqlQueryString);
		QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, baseModel);
		result =qexec.execConstruct();
		return result;
	}



	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.benchmarker.Modifier#destroy(com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	Model destroy(Model subModel) {
		// TODO Auto-generated method stub
		return null;
	}


	/**
	 * this is just a test function
	 * @return Map of Modifiers and associated rates
	 * @author sherif
	 */
	private Map<? extends Modifier, Double> getModefiersAndRates(){
		Map<Modifier, Double> modefiersAndRates= new HashMap<Modifier, Double>();

		Modifier abbreviationModifier= ModifierFactory.getModifier("abbreviation");
		modefiersAndRates.put(abbreviationModifier, 0.033d);


		Modifier misspelingModifier= ModifierFactory.getModifier("misspelling");
		modefiersAndRates.put(misspelingModifier, 0.033d);

		//		Modifier acronymModifier= ModifierFactory.getModifier("acronym");
		//		modefiersAndRates.put(acronymModifier, 0.5d);

		Modifier permutationModifier= ModifierFactory.getModifier("permutation");
		modefiersAndRates.put(permutationModifier, 0.033d);

		//		Modifier splitModifier= ModifierFactory.getModifier("split");
		//		((SplitModifier) splitModifier).addSplitProperty(RDFS.label);
		//		((SplitModifier) splitModifier).addSplitProperty(RDFS.comment);
		//		modefiersAndRates.put(splitModifier, 0.5d);

		//		Modifier mergeModifier= ModifierFactory.getModifier("merge");
		//		((MergeModifier) mergeModifier).setMergeProperty(RDFS.label);
		//		modefiersAndRates.put(mergeModifier, 1d);

		return modefiersAndRates;
	}


	public Model bmPeel(Model inModel, String inClassUri, Double dRatio) throws IOException{
		baseModel = inModel;
		System.out.println("----- Base Model -----");
		System.out.println("Size: "+baseModel.size());
		//		Modifier.baseModel.write(System.out, "N-TRIPLE");
		System.out.println();
		properties.add(RDFS.label);
		properties.add(FOAF.name);
		inputClassUri=inClassUri;

		// 1. Destroy class instances
		Double modRatio = dRatio/3d;
		Map<Modifier, Double> modefiersAndRates= new HashMap<Modifier, Double>();
		Modifier abbreviationModifier= ModifierFactory.getModifier("abbreviation");
		modefiersAndRates.put(abbreviationModifier, modRatio);
		Modifier misspelingModifier= ModifierFactory.getModifier("misspelling");
		modefiersAndRates.put(misspelingModifier, modRatio);
		Modifier permutationModifier= ModifierFactory.getModifier("permutation");
		modefiersAndRates.put(permutationModifier, modRatio);

		destroyInstances(modefiersAndRates,0);
		System.out.println();
		System.out.println("----- Destroyed Instance Model -----");
		System.out.println("Size: "+ destroyedModel.size());

		// 2. Destroy class
		baseModel= destroyedModel;
		destroyedModel= ModelFactory.createDefaultModel();

		System.out.println("destroyedModel: "+ destroyedModel.size());
		destroyedModel.write(System.out, "TTL");
		System.out.println("baseModel: "+ baseModel.size());
		//		baseModel.write(System.out, "TTL");

		ClassSplitModifier classSpliter=new ClassSplitModifier();
		//		classSpliter.splitSourceClassUri = inClassUri;
		//		classSpliter.splitTargetClassUri.add("http://purl.org/ontology/mo/MusicArtistSplit1");
		//		classSpliter.splitTargetClassUri.add("http://purl.org/ontology/mo/MusicArtistSplit2");
		System.out.println("----- Split Model -----");
		Model outModel = classSpliter.destroy(null);
		System.out.println("Size: "+outModel.size());
		return outModel;
	}



	/**
	 * @param inFile
	 * @param outFile
	 * @author sherif
	 */
	private void bmPeel(String inFile, String outFile) throws IOException{
		Model inModel=loadModel(inFile);
		String inClassUri = "http://purl.org/ontology/mo/MusicArtist";
		Double dRatio = 0.1d; 
		FileWriter outF = new FileWriter(outFile);

		bmPeel(inModel, inClassUri, dRatio).write(outF, "TTL");

	}

	public static void main(String args[]) throws IOException{
		Model m= loadModel(args[0]);
		BenchmarkGenerator benchmarker= new BenchmarkGenerator(m);
		System.out.println("m.size(): "+m.size());
		Map<Modifier, Double> modefiersAndRates= new HashMap<Modifier, Double>();
		modefiersAndRates.put(new ClassSplitModifier(), 1d);
		Model desM = benchmarker.destroyClasses (modefiersAndRates);
		System.out.println("desM.size(): "+desM.size());

	}

}
























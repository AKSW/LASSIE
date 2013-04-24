/**
 * 
 */
package org.aksw.lassie.bmGenerator;


import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * @author Sherif
 *
 */
public class BenchmarkGenerator extends Modifier{

	/**
	 * @param basemodel
	 * @param modefiersAndRates
	 * @return destroyed model
	 * @author Sherif
	 */
	Model destroy (Model m, Map<? extends Modifier, Double> modefiersAndRates){
		baseModel= m;
		return destroy(modefiersAndRates);
	}


	Model destroy (Map<? extends Modifier, Double> modefiersAndRates){
		return destroy (modefiersAndRates, 0d);
	}

	/**
	 * @param modefiersAndRates
	 * @return destroyed model
	 * @author Sherif
	 */
	Model destroy (Map<? extends Modifier, Double> modefiersAndRates, double startPointRatio){
		Model inputModel= ModelFactory.createDefaultModel();

		if(properties.size()==0 && inputClassUri==null){   // If the modifier properties are not set and no Class is set then divide the whole Model
			inputModel=baseModel;
		}else if(inputClassUri!=null){ // if class is set the divide based on class
			destroyedclassModel = getInputClassModel(inputClassUri);
			inputModel=destroyedclassModel;
			if(properties.size()>0){    // and if the modifier properties are set and Class is set then divide the the properties of that class 
				destroyedPropertiesModel=getDestroyedPropertiesModel(destroyedclassModel);
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
		if(destroyedclassModel.size()>0){
			baseModel.remove(destroyedclassModel);
			destroyedModel.add(baseModel); 
		}
		if(inputClassUri!=null && outputClassUri!=null){
			destroyedModel = renameClass(destroyedModel, inputClassUri,outputClassUri);
		}
		return destroyedModel;
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
	 * @param fileNameOrUri
	 * @return Base model loaded from file or URI
	 * @author Sherif
	 */
	public Model loadBaseModel(String fileNameOrUri){
		baseModel=ModelFactory.createDefaultModel();
		java.io.InputStream in = FileManager.get().open( fileNameOrUri );
		if (in == null) {
			throw new IllegalArgumentException(
					"File/URI: " + fileNameOrUri + " not found");
		}
		if(fileNameOrUri.endsWith(".ttl")){
			System.out.println("Opening Turtle file");
			baseModel.read(in, null, "TTL");
		}else if(fileNameOrUri.endsWith(".rdf")){
			System.out.println("Opening RDFXML file");
			baseModel.read(in, null);
		}else if(fileNameOrUri.endsWith(".nt")){
			System.out.println("Opening N-Triples file");
			baseModel.read(in, null, "N-TRIPLE");
		}else{
			System.out.println("Content negotiation to get RDFXML from " + fileNameOrUri);
			baseModel.read(fileNameOrUri);
		}

		System.out.println("loading "+ fileNameOrUri + " is done!!");
		System.out.println();
		return baseModel;
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


	void bmPeel(String inputFileName, String outputFilename){
		loadBaseModel(inputFileName);
		System.out.println("----- Base Model -----");
		System.out.println("Size: "+baseModel.size());
		//		Modifier.baseModel.write(System.out, "N-TRIPLE");
		System.out.println();
		properties.add(RDFS.label);
		properties.add(FOAF.name);
		Map<? extends Modifier, Double> modefiersAndRates= getModefiersAndRates();
		destroy(modefiersAndRates,0.51);
		System.out.println();
		System.out.println("----- Destroyed Model -----");
		System.out.println("Size: "+ destroyedModel.size());
		FileWriter outFile;
		try {
			outFile = new FileWriter(outputFilename);
			destroyedModel.write(outFile, "N-TRIPLE");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	public static void main(String args[]){
		BenchmarkGenerator benchmarker= new BenchmarkGenerator();
		benchmarker.loadBaseModel(args[0]);
		System.out.println("----- Base Model -----");
		System.out.println("Size: "+baseModel.size());
		baseModel.write(System.out, "TTL");
		System.out.println();
		properties.add(RDFS.label);
		properties.add(FOAF.name);
		benchmarker.inputClassUri = "http://purl.org/ontology/mo/MusicArtist";
		benchmarker.outputClassUri = "http//example.com/Artist";
		
		Map<Modifier, Double> modefiersAndRates= new HashMap<Modifier, Double>();
		Modifier mergeModifier= ModifierFactory.getModifier("merge");
		modefiersAndRates.put(mergeModifier, 0.5d);
		benchmarker.destroy(modefiersAndRates,0.0d);
		System.out.println();
		System.out.println("----- Destroyed Model -----");
		System.out.println("Size: "+ destroyedModel.size());
			destroyedModel.write(System.out, "TTL");
	}
}
























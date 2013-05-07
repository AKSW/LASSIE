/**
 * 
 */
package org.aksw.lassie.bmGenerator;


import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author sherif
 *
 */
public class ClassMergeModifier extends Modifier{
	List<String> mergeSourceClassUris = new ArrayList<String>();
	String mergeTargetClassuri = new String();

	/**
	 * @param m
	 *@author sherif
	 */
	public ClassMergeModifier(Model m) {
		super(m);
	}
	
	public ClassMergeModifier() {
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.bmGenerator.Modifier#destroy(com.hp.hpl.jena.rdf.model.Model)
	 */
	Model destroy(Model subModel) {
		Model result = baseModel;
		for(String sourceClassUri:mergeSourceClassUris){
			Model sourceClassModel = getClassInstancesModel(sourceClassUri);
			result.remove(sourceClassModel);
			sourceClassModel = renameClass(sourceClassModel, sourceClassUri, mergeTargetClassuri);
			destroyedClassModel.add(sourceClassModel);
		}
		result.add(destroyedClassModel);
		return result;
	}

	public static void main(String[] args){
		ClassMergeModifier classMerger=new ClassMergeModifier();

		baseModel= loadModel(args[0]);
		System.out.println("----- Base Model -----");
		System.out.println("Size: "+baseModel.size());
		baseModel.write(System.out,"TTL");
		System.out.println();

		classMerger.mergeSourceClassUris.add("http://purl.org/ontology/mo/MusicArtist");
		classMerger.mergeSourceClassUris.add("http://purl.org/ontology/mo/Album");
		classMerger.mergeTargetClassuri= "http://purl.org/ontology/mo/MERGE";
		System.out.println("----- Merge Model -----");
		Model m = classMerger.destroy(null);
		System.out.println("Size: "+m.size());
		m.write(System.out,"TTL");
	}
}

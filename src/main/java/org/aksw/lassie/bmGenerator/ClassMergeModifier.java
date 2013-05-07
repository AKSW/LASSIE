/**
 * 
 */
package org.aksw.lassie.bmGenerator;


import java.util.ArrayList;
import java.util.List;

import org.apache.xerces.util.URI;
import org.apache.xerces.util.URI.MalformedURIException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * @author sherif
 *
 */
public class ClassMergeModifier extends Modifier{
	//	List<String> mergeSourceClassUris = new ArrayList<String>();
	//	String mergeTargetClassuri = new String();
	public int mergeCount = 2;



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
		Model result = ModelFactory.createDefaultModel();
		List<String> classNames = getClasses(subModel);
		List<String> mergeSourceClassUris = new ArrayList<String>();
		
		for(int i=0 ; i<classNames.size() ; i+=mergeCount){
			String mergeTargetClassUri = new String();
			mergeSourceClassUris.removeAll(mergeSourceClassUris);
			for(int j=0 ; j<mergeCount && i+j<classNames.size() ; j++){
				mergeSourceClassUris.add(j,classNames.get(i+j)); 
				mergeTargetClassUri = classNames.get(i+j).concat("MERGE"); 
			}
			for(String sourceClassUri:mergeSourceClassUris){
				Model sourceClassModel = getClassInstancesModel(sourceClassUri);
				sourceClassModel = renameClass(sourceClassModel, sourceClassUri, mergeTargetClassUri);
				result.add(sourceClassModel);
			}
		}
		return result;
	}



	public static void main(String[] args){
		Model m= loadModel(args[0]);
		ClassMergeModifier classMerger=new ClassMergeModifier(m);

		System.out.println("----- Base Model -----");
		System.out.println("Size: "+m.size());
//		baseModel.write(System.out,"TTL");
		System.out.println();

		System.out.println("----- Merge Model -----");
		Model desM = classMerger.destroy(m);
		System.out.println("Size: "+desM.size());
		desM.write(System.out,"TTL");
	}
}

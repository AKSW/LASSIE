/**
 * 
 */
package org.aksw.lassie.bmGenerator;


import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * @author sherif
 *
 */
public class ClassMergeModifier extends Modifier{
	//	List<String> mergeSourceClassUris = new ArrayList<String>();
	//	String mergeTargetClassuri = new String();
	private int mergeCount = 2;



	/**
	 * @return the mergeCount
	 */
	public int getMergeCount() {
		return mergeCount;
	}

	/**
	 * @param mergeCount the mergeCount to set
	 */
	public void setMergeCount(int mergeCount) {
		this.mergeCount = mergeCount;
	}

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
		
		List<String> classNames = new ArrayList<String>();
		if (baseClasses.size() == 0) {
			classNames = getClasses(subModel);
		} else {
			classNames = baseClasses;
		}
		
		List<String> mergeSourceClassUris = new ArrayList<String>();
		
		for(int i=0 ; i<classNames.size() ; i+=mergeCount){
			String mergeTargetClassUri = new String();
			mergeSourceClassUris.removeAll(mergeSourceClassUris);
			for(int j=0 ; j<mergeCount && i+j<classNames.size() ; j++){
				String classA = classNames.get(i+j);
				String classB = classNames.get(i+j);
				mergeSourceClassUris.add(j,classA); 
				mergeTargetClassUri = mergeTargetClassUri.concat(classNames.get(i+j));
				mergeTargetClassUri = (j+1 == mergeCount)? mergeTargetClassUri : mergeTargetClassUri.concat("_MERGE_");
			}
			modifiedClasses.add(mergeTargetClassUri);
			
			for(String sourceClassUri:mergeSourceClassUris){
				Model sourceClassModel = getClassInstancesModel(sourceClassUri);
				System.out.println();
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
		baseModel.write(System.out,"TTL");
		System.out.println();

		System.out.println("----- Merge Model -----");
		Model desM = classMerger.destroy(m);
		System.out.println("Size: "+desM.size());
		desM.write(System.out,"TTL");
		
//		Model test= ModelFactory.createDefaultModel();
//		test.add(ResourceFactory.createResource("aaaa"),RDF.type,ResourceFactory.createResource("a"));
//		test.add(ResourceFactory.createResource("bbbb"),RDF.type,ResourceFactory.createResource("b"));
//		test.add(ResourceFactory.createResource("cccc"),RDF.type,ResourceFactory.createResource("c"));
//		test.add(ResourceFactory.createResource("aaaa1"),RDF.type,ResourceFactory.createResource("a"));
//		test.add(ResourceFactory.createResource("bbbb1"),RDF.type,ResourceFactory.createResource("b"));
//		test.add(ResourceFactory.createResource("cccc1"),RDF.type,ResourceFactory.createResource("c"));
//		test.write(System.out,"TTL");
//		System.out.println("----------------------------------");
//		ClassMergeModifier classMerger=new ClassMergeModifier(test);
//		Model desM = classMerger.destroy(test);
//		System.out.println(desM.size());
//		desM.write(System.out,"TTL");
////		classMerger.renameClass(test, "a", "X").write(System.out,"TTL");
	
		
	}
}

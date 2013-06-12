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
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.engine.Rename;
import com.hp.hpl.jena.vocabulary.RDF;

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

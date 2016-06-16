/**
 * 
 */
package org.aksw.lassie.bmGenerator;


import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

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
		isClassModifier = true;
	}

	public ClassMergeModifier() {
		isClassModifier = true;
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.bmGenerator.Modifier#destroy(com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
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
				mergeSourceClassUris.add(j,classNames.get(i+j)); 
				mergeTargetClassUri = mergeTargetClassUri.concat(classNames.get(i+j));
				mergeTargetClassUri = (j+1 == mergeCount)? mergeTargetClassUri : mergeTargetClassUri.concat("_MERGE_");
			}
			modifiedClasses.add(mergeTargetClassUri);

			for(String sourceClassUri : mergeSourceClassUris){
				Model sourceClassModel = getClassInstancesModel(sourceClassUri);
				sourceClassModel = renameClass(sourceClassModel, sourceClassUri, mergeTargetClassUri);
				result.add(sourceClassModel);

				//generate optimal solution
				List<OWLClassExpression> children = new ArrayList<>();
				for (String uri : mergeSourceClassUris) {
					children.add(owlDataFactory.getOWLClass(IRI.create(uri)));
				}
				OWLObjectUnionOf optimalSolution = owlDataFactory.getOWLObjectUnionOf(new TreeSet<> (children));
				optimalSolutions.put(owlDataFactory.getOWLClass(IRI.create(mergeTargetClassUri)), optimalSolution);
				
			}
		}
		return result;
	}



	public static void main(String[] args){
		Model m= loadModel(args[0]);
		ClassMergeModifier classMerger=new ClassMergeModifier(m);

		System.out.println("----- Base Model -----\n");
		System.out.println("Size: "+m.size());
		baseModel.write(System.out,"TTL");
		System.out.println();

		System.out.println("\n\n----- Merge Model -----\n");
		Model desM = classMerger.destroy(m);
		System.out.println("Size: "+desM.size());
		desM.write(System.out,"TTL");

		//		Model test= ModelFactory.createDefaultModel();
		//		test.add(ResourceFactory.createResource("aaaa"),RDF.type,ResourceFactory.createResource("a"));
		//		test.add(ResourceFactory.createResource("bbbb"),RDF.type,ResourceFactory.createResource("b"));
		//		test.add(ResourceFactory.createResource("cccc"),RDF.type,ResourceFactory.createResource("c"));
		//		test.add(ResourceFactory.createResource("dddd"),RDF.type,ResourceFactory.createResource("d"));
		//		test.add(ResourceFactory.createResource("eeee"),RDF.type,ResourceFactory.createResource("e"));
		//		test.add(ResourceFactory.createResource("ffff"),RDF.type,ResourceFactory.createResource("f"));
		//		test.write(System.out,"TTL");
		//		System.out.println(test.size());
		//		System.out.println("----------------------------------");
		//		ClassMergeModifier classMerger=new ClassMergeModifier(test);
		//		Model desM = classMerger.destroy(test);
		//		System.out.println(desM.size());
		//		desM.write(System.out,"TTL");
		//		classMerger.renameClass(test, "a", "X").write(System.out,"TTL");


	}
}

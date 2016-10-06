/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;

/**
 * @author sherif
 *
 */
public class ClassTypeDeleteModifier extends Modifier{

	/**
	 * 
	 *@author sherif
	 */
	public ClassTypeDeleteModifier() {
		isClassModifier = true;
	}
	/**
	 * @param m
	 *@author sherif
	 */
	public ClassTypeDeleteModifier(Model m) {
		super(m);
		isClassModifier = true;
	}

	public static void main(String[] args) {
		Model m= loadModel(args[0]);
		ClassTypeDeleteModifier typeDeleter = new ClassTypeDeleteModifier(m);
		System.out.println("----- Base Model -----");
		System.out.println("Size: "+baseModel.size());
		System.out.println();
		System.out.println("----- Deleted Type Model -----");
		Model destM = typeDeleter.destroy(m);
		System.out.println("Size: "+destM.size());
		destM.write(System.out,"TTL");

	}

	/* (non-Javadoc)
	 * @see org.aksw.lassie.bmGenerator.Modifier#destroy(org.apache.jena.rdf.model.Model)
	 */
	@Override
	public Model destroy(Model subModel) {
		Model result = ModelFactory.createDefaultModel();
		List<String> classNames = new ArrayList<String>();
		if (baseClasses.size() == 0) {
			classNames = getClasses(subModel);
		} else {
			classNames = baseClasses;
		}
		
//		modifiedClasses.removeAll(classNames);
		
		for(String className: classNames){
			Model sourceClassModel = getClassInstancesModel(className, subModel);

			sourceClassModel.removeAll(null, RDF.type, null);
			result.add(sourceClassModel);
			modifiedClasses.add(className);
			OWLClass cls = owlDataFactory.getOWLClass(IRI.create(className));
			optimalSolutions.put(cls, cls);
			}
		return result;
	}

}

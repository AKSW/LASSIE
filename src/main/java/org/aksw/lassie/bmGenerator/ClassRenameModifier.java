/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.IRI;

import org.apache.jena.rdf.model.Model;

/**
 * @author sherif
 *
 */
public class ClassRenameModifier extends Modifier {


	public ClassRenameModifier(Model m) {
		super(m);
		isClassModifier = true;
	}

	public ClassRenameModifier() {
		isClassModifier = true;
	}

	/* (non-Javadoc)
	 * @see org.aksw.lassie.bmGenerator.Modifier#destroy(org.apache.jena.rdf.model.Model)
	 */
	@Override
	Model destroy(Model subModel) {
//		Model removeModel = ModelFactory.createDefaultModel();
//		Model addModel = ModelFactory.createDefaultModel();
		
		List<String> classNames = new ArrayList<String>();
		if (baseClasses.size() == 0) {
			classNames = getClasses(subModel);
		} else {
			classNames = baseClasses;
		}
		
		for (String className : classNames) {
			String renamedClassName = className+"_RENAME";
			subModel = renameClass(subModel, className,  renamedClassName);
			modifiedClasses.add(renamedClassName);
			optimalSolutions.put(owlDataFactory.getOWLClass(IRI.create(renamedClassName)), owlDataFactory.getOWLClass(IRI.create(className)));
		}
		return subModel;
	}


	public static void main(String[] args){
		Model m= loadModel(args[0]);
		ClassRenameModifier classRenamer=new ClassRenameModifier(m);

		System.out.println("----- Base Model -----");
		System.out.println("Size: "+ m.size());
		baseModel.write(System.out,"TTL");
		System.out.println();

		System.out.println("----- Remaned Model -----");
		Model desM = classRenamer.destroy(m);
		System.out.println("Size: " + desM.size());
		desM.write(System.out,"TTL");
	}
}

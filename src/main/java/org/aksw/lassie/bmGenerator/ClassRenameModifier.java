/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import java.util.ArrayList;
import java.util.List;

import org.dllearner.core.owl.NamedClass;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author sherif
 *
 */
public class ClassRenameModifier extends Modifier {


	public ClassRenameModifier(Model m) {
		super(m);
	}

	public ClassRenameModifier() {
	}

	/* (non-Javadoc)
	 * @see org.aksw.lassie.bmGenerator.Modifier#destroy(com.hp.hpl.jena.rdf.model.Model)
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
			optimalSolutions.put(new NamedClass(className), new NamedClass(renamedClassName));
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

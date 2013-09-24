/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import java.util.ArrayList;
import java.util.List;

import org.dllearner.core.owl.NamedClass;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * @author sherif
 *
 */
public class ClassDeleteModifier extends Modifier {
	/**
	 * @param m
	 *@author sherif
	 */
	public ClassDeleteModifier(Model m) {
		super(m);
		isClassModifier = true;
	}
	
	public ClassDeleteModifier() {
		isClassModifier = true;
	}
	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.bmGenerator.Modifier#destroy(com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	Model destroy(Model subModel) {
		List<String> classNames = new ArrayList<String>();
		if (baseClasses.size() == 0) {
			classNames = getClasses(subModel);
		} else {
			classNames = baseClasses;
		}
		
//		modifiedClasses.removeAll(classNames);
		
		for(String className: classNames){
//			System.out.println("className: "+className);
			Model sourceClassModel = getClassInstancesModel(className, subModel);
//			System.out.println("************* sourceClassModel *********");
//			sourceClassModel.write(System.out,"TTL");
//			System.exit(1);
			baseModel.remove(sourceClassModel);
			modifiedClasses.add(className);
			NamedClass cls = new NamedClass(className);
			optimalSolutions.put(cls, cls);
		}
		
		
		
//		baseModel.remove(subModel);
		return ModelFactory.createDefaultModel();
	}
	public static void main(String[] args){
		Model m= loadModel(args[0]);
		ClassDeleteModifier classDeleter=new ClassDeleteModifier(m);

		System.out.println("----- Base Model -----");
		System.out.println("Size: "+m.size());
		baseModel.write(System.out,"TTL");
		System.out.println();

		System.out.println("----- Merge Model -----");
		Model desM = classDeleter.destroy(m);
		System.out.println("Size: "+desM.size());
		desM.write(System.out,"TTL");

	}
}

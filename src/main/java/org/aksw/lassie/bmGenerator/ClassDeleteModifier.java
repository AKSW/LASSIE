/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;

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
	 * @see de.uni_leipzig.simba.bmGenerator.Modifier#destroy(org.apache.jena.rdf.model.Model)
	 */
	@Override
    public Model destroy(Model subModel) {
		List<String> classNames = new ArrayList<>();
		if (baseClasses.size() == 0) {
			classNames = getClasses(subModel);
		} else {
			classNames = baseClasses;
		}
		for(String className: classNames){
			Model deleteClassInstancesModel = getClassInstancesModel(className, subModel);
			baseModel.remove(deleteClassInstancesModel);
			modifiedClassesURIs.add(className);
			OWLClass cls = owlDataFactory.getOWLClass(IRI.create(className));
			optimalSolutions.put(cls, cls);
		}
		return ModelFactory.createDefaultModel();
	}
	

}

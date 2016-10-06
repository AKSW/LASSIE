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
	public Model destroy(Model subModel) {
		List<String> classNames = new ArrayList<>();
		if (baseClasses.size() == 0) {
			classNames = getClasses(subModel);
		} else {
			classNames = baseClasses;
		}
		
		for (String className : classNames) {
			String renamedClassName = className + "_RENAME";
			subModel = renameClass(subModel, className,  renamedClassName);
			modifiedClasses.add(renamedClassName);
			optimalSolutions.put(owlDataFactory.getOWLClass(IRI.create(renamedClassName)), owlDataFactory.getOWLClass(IRI.create(className)));
		}
		return subModel;
	}

}

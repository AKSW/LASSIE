/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;

/**
 * @author sherif
 *
 */
public class ClassIdentityModifier extends Modifier {

	public ClassIdentityModifier(Model m) {
		super(m);
		isClassModifier = true;
	}

	public ClassIdentityModifier() {
		isClassModifier = true;
	}

	@Override
    public Model destroy(Model subModel) {

		List<String> classNames = new ArrayList<String>();
		if (baseClasses.size() == 0) {
			classNames = getClasses(subModel);
		} else {
			classNames = baseClasses;
		}

		modifiedClassesURIs.addAll(classNames);
		return subModel;
	}
}

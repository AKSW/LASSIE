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
		Model result = ModelFactory.createDefaultModel();
		List<String> classNames = getClasses(subModel);

		for(String sourceClassUri:classNames){
			Model sourceClassModel = getClassInstancesModel(sourceClassUri);
			sourceClassModel = renameClass(sourceClassModel, sourceClassUri, sourceClassModel+"RENAME");
			result.add(sourceClassModel);
		}

		return result;
	}

}

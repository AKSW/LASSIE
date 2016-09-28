/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import org.apache.jena.rdf.model.Model;

/**
 * @author sherif
 *
 */
public class InstanceIdentityModifier extends Modifier{

	/**
	 * @param m
	 *@author sherif
	 */
	public InstanceIdentityModifier(Model m) {
		super(m);
	}

	public InstanceIdentityModifier() {
	}


	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.benchmarker.Modifier#destroy()
	 */
	@Override
	Model destroy(Model subModel) {
		System.out.println();
		System.out.println("Generating Identity (NO CHANGE) of " + subModel.size() + " triples.");
		destroyedModel.add(subModel);
		return destroyedModel;
	}

}

/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author sherif
 *
 */
public class InstanceSynonymModifier extends Modifier {

	/**
	 * @param m
	 *@author sherif
	 */
	public InstanceSynonymModifier(Model m) {
		super(m);
	}

	public InstanceSynonymModifier() {
	}
	
	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.benchmarker.Modifier#destroy()
	 */
	@Override
	Model destroy(Model subModel) {
		System.out.println();
		System.out.println("Synonym modifier not yet Implemented, return the input model as it is");
		// TODO Auto-generated method stub
		
		
		// Not yet Implemented
		destroyedModel.add(subModel);
		
		return destroyedModel;
	}

}

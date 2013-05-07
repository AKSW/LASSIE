/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author sherif
 *
 */
public class SynonymModifier extends Modifier {

	/**
	 * @param m
	 *@author sherif
	 */
	public SynonymModifier(Model m) {
		super(m);
	}

	public SynonymModifier() {
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

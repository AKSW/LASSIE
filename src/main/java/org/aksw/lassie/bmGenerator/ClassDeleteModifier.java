/**
 * 
 */
package org.aksw.lassie.bmGenerator;

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
	}
	
	public ClassDeleteModifier() {
	}
	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.bmGenerator.Modifier#destroy(com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	Model destroy(Model subModel) {
		Model result = ModelFactory.createDefaultModel();
		result.remove(subModel);
		return result;
	}
	public static void main(String[] args){

	}
}

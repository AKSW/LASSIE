/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author sherif
 *
 */
public class ClassDeleteModifier extends Modifier {
	String deleteClassUri = new String();
	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.bmGenerator.Modifier#destroy(com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	Model destroy(Model subModel) {
		Model result = baseModel;
		Model deletedClassModel = getClassModel(deleteClassUri);
		result.remove(deletedClassModel);
		destroyedclassModel.add(result);
		return result;
	}
	public static void main(String[] args){
		ClassDeleteModifier classDeleter=new ClassDeleteModifier();

		baseModel= loadModel(args[0]);
		System.out.println("----- Base Model -----");
		System.out.println("Size: "+baseModel.size());
		baseModel.write(System.out,"TTL");
		System.out.println();

		classDeleter.deleteClassUri = "http://purl.org/ontology/mo/MusicArtist";
		System.out.println("----- Delete Model -----");
		Model m = classDeleter.destroy(null);
		System.out.println("Size: "+m.size());
		m.write(System.out,"TTL");
	}
}

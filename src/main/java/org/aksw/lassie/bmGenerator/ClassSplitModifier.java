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
public class ClassSplitModifier extends Modifier {

	int splitCount = 2;
	

	/**
	 * @param m
	 *@author sherif
	 */
	public ClassSplitModifier(Model m) {
		super(m);
	}

	public ClassSplitModifier() {
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.bmGenerator.Modifier#destroy(com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	Model destroy(Model subModel) {
		Model result = ModelFactory.createDefaultModel();
		List<String> classNames = getClasses(subModel);
		for(String className: classNames){
			Model sourceClassModel = getClassInstancesModel(className);
			//			subModel.remove(sourceClassModel);
			//divide the source class instances equally to target classes
			long targetClassSize = (long) Math.floor(sourceClassModel.size()/splitCount);
			long sourceClassModeloffset = 0L;
			
			//create split classes 
			List<String> splitTargetClassUri = new ArrayList<String>();
			for(int i=0 ; i<splitCount ; i++){
				splitTargetClassUri.add(className+"Split"+(i+1));
			}
			for(String targetClassUri:splitTargetClassUri){
				Model splitModel = getSubModel(sourceClassModel, targetClassSize, sourceClassModeloffset); 
				sourceClassModeloffset += targetClassSize;
				splitModel = renameClass(splitModel, className, targetClassUri);	
				result.add(splitModel);  

				//if some triples remains at end (<targetClassSize) add them to the last split
				if( (sourceClassModel.size()-sourceClassModeloffset) < targetClassSize){
					splitModel = getSubModel(sourceClassModel, sourceClassModel.size()-sourceClassModeloffset, sourceClassModeloffset); 
					splitModel = renameClass(splitModel, className, targetClassUri);
					destroyedModel.add(splitModel);
					break;
				}
			}
		}





		result.add(destroyedModel);
		return result;
	}

	public static void main(String[] args){

		Model m= loadModel(args[0]);
		ClassSplitModifier classSpliter = new ClassSplitModifier(m);
		System.out.println("----- Base Model -----");
		System.out.println("Size: "+baseModel.size());
//		//		baseModel.write(System.out,"TTL");
		System.out.println();
//
////		classSpliter.splitSourceClassUri = "http://purl.org/ontology/mo/MusicArtist";
////		classSpliter.splitTargetClassUri.add("http://purl.org/ontology/mo/MusicArtistSplit1");
////		classSpliter.splitTargetClassUri.add("http://purl.org/ontology/mo/MusicArtistSplit2");
		System.out.println("----- Split Model -----");
		Model destM = classSpliter.destroy(m);
		System.out.println("Size: "+destM.size());
		destM.write(System.out,"TTL");
//		try {
//			FileWriter outFile = new FileWriter(args[1]);
//			//			m.write(System.out, "TTL");
//			m.write(outFile, "TTL");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

}

/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author sherif
 *
 */
public class ClassSplitModifier extends Modifier {
	String splitSourceClassUri = new String();
	List<String> splitTargetClassUri = new ArrayList<String>();
	
	
	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.bmGenerator.Modifier#destroy(com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	Model destroy(Model subModel) {
		Model result = baseModel;
		Model sourceClassModel = getClassModel(splitSourceClassUri); 
		result.remove(sourceClassModel);
		//divide the source class instances equally to target classes
		long targetClassSize = (long) Math.floor(sourceClassModel.size()/splitTargetClassUri.size());
		long sourceClassModeloffset = 0L;
		
		for(String targetClassUri:splitTargetClassUri){
			Model splitModel = getSubModel(sourceClassModel, targetClassSize, sourceClassModeloffset); 
			sourceClassModeloffset += targetClassSize;
			splitModel = renameClass(splitModel, splitSourceClassUri, targetClassUri);	
			destroyedModel.add(splitModel);  
			
			//if some triples remains at end (<targetClassSize) add them to the last split
			if( (sourceClassModel.size()-sourceClassModeloffset) < targetClassSize){
				splitModel = getSubModel(sourceClassModel, sourceClassModel.size()-sourceClassModeloffset, sourceClassModeloffset); 
				splitModel = renameClass(splitModel, splitSourceClassUri, targetClassUri);
				destroyedModel.add(splitModel);
				break;
			}
		}
		result.add(destroyedModel);
		return result;
	}
	
	public static void main(String[] args){
		ClassSplitModifier classSpliter=new ClassSplitModifier();

		baseModel= loadModel(args[0]);
		System.out.println("----- Base Model -----");
		System.out.println("Size: "+baseModel.size());
//		baseModel.write(System.out,"TTL");
		System.out.println();

		classSpliter.splitSourceClassUri = "http://purl.org/ontology/mo/MusicArtist";
		classSpliter.splitTargetClassUri.add("http://purl.org/ontology/mo/MusicArtistSplit1");
		classSpliter.splitTargetClassUri.add("http://purl.org/ontology/mo/MusicArtistSplit2");
		System.out.println("----- Split Model -----");
		Model m = classSpliter.destroy(null);
		System.out.println("Size: "+m.size());
//		m.write(System.out,"TTL");
		try {
			FileWriter outFile = new FileWriter(args[1]);
//			m.write(System.out, "TTL");
			m.write(outFile, "TTL");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

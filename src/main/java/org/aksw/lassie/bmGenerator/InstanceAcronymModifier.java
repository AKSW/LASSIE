/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * @author sherif
 *
 */
public class InstanceAcronymModifier extends Modifier{
	
	public int maxAcronymLength = 4;
	/**
	 * @param m
	 *@author sherif
	 */
	public InstanceAcronymModifier(Model m) {
		super(m);
	}

	public InstanceAcronymModifier() {
	}


	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.benchmarker.Modifier#destroy()
	 */
	@Override
	Model destroy(Model subModel) {
		System.out.println();
		System.out.println("Generating acronym of " + subModel.size() + " triples.");
		
		StmtIterator sItr = subModel.listStatements();

		while (sItr.hasNext()) {
			Statement stmt = sItr.nextStatement();	
			Statement acronyamedStatement = acronyam(stmt);
			destroyedModel.add(acronyamedStatement);
		}
		return destroyedModel;
	}

	
	
	private Statement acronyam(Statement stmt){
		if(!stmt.getObject().isLiteral()){
			System.err.println(stmt.getObject() + " is not a literal object");
			System.err.println("Can NOT get acronyam for non-literal object!!");
//			System.exit(1);
			return stmt;
		}
		 String objectLitral     = stmt.getObject().asNode().getLiteral().getLexicalForm();
		 String acronyamedLitral = acronyam(objectLitral);
		 RDFNode acronyamedObject= ResourceFactory.createTypedLiteral(acronyamedLitral);
		 Statement result        = ResourceFactory.createStatement(stmt.getSubject(), stmt.getPredicate(), acronyamedObject);
		return result;
	}
	
	

	private String acronyam(String s){
		if(s.length()<2)
			return s;

		String result= new String();

		if(s.contains(" ")){
			String[] splitStr = s.split(" ", Integer.MAX_VALUE);
			int i=0;
			for( ; i < Math.min(maxAcronymLength,splitStr.length); i++){
				if(!splitStr[i].equals(" ") && !splitStr[i].equals("")){
					result += splitStr[i].replace(splitStr[i].substring(1),"");
					}
				}
			result = result.toUpperCase();
			// If there still some words just concatenate them after abbreviation
			for( ; i<splitStr.length ; i++){
				result += " " + splitStr[i] ;
			}

		}else{
			result = s.replace(s.substring(1),"").toUpperCase();
		}
//		System.out.println("Source:    "+s);
//		System.out.println("Destroyed: "+result);
		
		return result;
	}
	
	public static void main(String args[]){
		InstanceAcronymModifier a = new InstanceAcronymModifier();
		System.out.println(a.acronyam("sdsadasd jkhkh kjkljl jhkjh jkbnk"));
	}
}

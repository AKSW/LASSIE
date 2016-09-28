/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import java.util.Random;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

/**
 * @author sherif
 *
 */
public class InstanceMisspellingModifier extends Modifier {
	
	private final String alphabet="abcdefghijklmnopqrstuvwxzyABCDEFGHIJKLMNOPQRSTUVWXYZ"; 
	private final int MISSPELING_MODIFIERS_COUNT = 4 ;   // No of misspelling modifiers so far
	
	private int missplingRate =1;
	
	private double permutationRatio = 1/(double)MISSPELING_MODIFIERS_COUNT;
	private double insertionRatio   = 1/(double)MISSPELING_MODIFIERS_COUNT;
	private double deletionRatio    = 1/(double)MISSPELING_MODIFIERS_COUNT;
	private double substitutionRatio= 1/(double)MISSPELING_MODIFIERS_COUNT;
	
	private long  permutationTriplesCount = 0;
	private long  insertionTriplesCount = 0;
	private long  deletionTriplesCount	= 0;
	private long  substitutionTriplesCount= 0;
	
	

	/**
	 * @param m
	 *@author sherif
	 */
	public InstanceMisspellingModifier(Model m) {
		super(m);
	}

	public InstanceMisspellingModifier() {
	}

	/**
	 * @return the missplingRate
	 */
	public int getMissplingRate() {
		return missplingRate;
	}


	/**
	 * @return the permutationRatio
	 */
	public double getPermutationRatio() {
		return permutationRatio;
	}


	/**
	 * @return the insertionRatio
	 */
	public double getInsertionRatio() {
		return insertionRatio;
	}


	/**
	 * @return the deletionRatio
	 */
	public double getDeletionRatio() {
		return deletionRatio;
	}


	/**
	 * @return the substitutionRatio
	 */
	public double getSubstitutionRatio() {
		return substitutionRatio;
	}


	/**
	 * @param missplingRate the missplingRate to set
	 */
	public void setMissplingRate(int missplingRate) {
		this.missplingRate = missplingRate;
	}


	/**
	 * @param permutationRatio the permutationRatio to set
	 */
	public void setPermutationRatio(double permutationRatio) {
		if(permutationRatio < 0 || permutationRatio >1){
			System.out.println(permutationRatio + " must be between 0 and 1, using the default value: " + this.permutationRatio);
			return;
		}
		this.permutationRatio = permutationRatio;
	}


	/**
	 * @param insertionRatio the insertionRatio to set
	 */
	public void setInsertionRatio(double insertionRatio) {
		if(insertionRatio < 0 || insertionRatio >1){
			System.out.println(insertionRatio + " must be between 0 and 1, using the default value: " + this.insertionRatio);
			return;
		}
		this.insertionRatio = insertionRatio;
	}


	/**
	 * @param deletionRatio the deletionRatio to set 
	 */
	public void setDeletionRatio(double deletionRatio) {
		if(deletionRatio < 0 || deletionRatio >1){
			System.out.println(deletionRatio + " must be between 0 and 1, using the default value: " + this.deletionRatio);
			return;
		}
		this.deletionRatio = deletionRatio;
	}


	/**
	 * @param substitutionRatio the substitutionRatio to set
	 */
	public void setSubstitutionRatio(double substitutionRatio) {
		if(substitutionRatio < 0 || substitutionRatio >1){
			System.out.println(substitutionRatio + " must be between 0 and 1, using the default value: " + this.substitutionRatio);
			return;
		}
		this.substitutionRatio = substitutionRatio;
	}




	
	
	

	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.benchmarker.Modifier#destroy() 
	 */
	@Override
	Model destroy(Model subModel) {
		permutationTriplesCount = (long) Math.ceil(subModel.size() * permutationRatio);
		insertionTriplesCount   = (long) Math.ceil(subModel.size() * insertionRatio);
		deletionTriplesCount	= (long) Math.ceil(subModel.size() * deletionRatio);
		// to avoid leaving any triples
		substitutionTriplesCount= permutationTriplesCount + insertionTriplesCount + deletionTriplesCount - subModel.size(); 
		
		System.out.println();
		System.out.println("Misspelling " + subModel.size() + " triples.");
		System.out.println("\tMisspelling by character permutation " + permutationTriplesCount + " triples.");
		System.out.println("\tMisspelling by character insertion " + insertionTriplesCount + " triples.");
		System.out.println("\tMisspelling by character deletion " + deletionTriplesCount + " triples.");
		System.out.println("\tMisspelling by character substitution " + substitutionTriplesCount + " triples.");
		
		StmtIterator sItr = subModel.listStatements();

		while (sItr.hasNext()) {
			Statement stmt = sItr.nextStatement();	
			Statement misspelledStatement = misspelling(stmt); 
			destroyedModel.add(misspelledStatement);    
		}
		return destroyedModel;
	}

	private Statement misspelling(Statement stmt){
		if(!stmt.getObject().isLiteral()){
//			System.err.println(stmt.getObject() + " is not a literal object");
//			System.err.println("Can NOT misspelling a non-literal object!!");
			//			System.exit(1);
			return stmt;
		}
		String objectLitral     = stmt.getObject().asNode().getLiteral().getLexicalForm();
		String misspelledLitral = new String();
		if(permutationTriplesCount>0){
			misspelledLitral=charPermutation(objectLitral);
			permutationTriplesCount--;
		}else if(insertionTriplesCount>0){
			misspelledLitral=charInsertion(objectLitral);
			insertionTriplesCount--;
		}else if(deletionTriplesCount>0){
			misspelledLitral=charDeletion(objectLitral);
			deletionTriplesCount--;
		}else if(substitutionTriplesCount>0){
			misspelledLitral=charSubstitution(objectLitral);
			substitutionTriplesCount--;
		}
		RDFNode misspelledObject= ResourceFactory.createTypedLiteral(misspelledLitral);
		Statement result        = ResourceFactory.createStatement(stmt.getSubject(), stmt.getPredicate(), misspelledObject);
		return result;
	}




	/**
	 * @param input string s
	 * @return misspelled string from the input String by replacing one or more 
	 * 			character Randomly selected from it with one or more character 
	 * 			randomly selected  from the alphabet
	 * @author sherif
	 */
	private String charSubstitution(String s){
		String result= new String();
		Random generator= new Random();
		for(int i=0 ; i<missplingRate ; i++){
			try{
				int randAlphaIndex=generator.nextInt(alphabet.length());
				int randIndexS=generator.nextInt(s.length());
				result= s.replace(s.charAt(randIndexS), alphabet.charAt(randAlphaIndex));
			}catch (Exception e) {
				if(s.length()>=2)
					result= s.replace(s.charAt(0), 'X');
				else
					result=s;
			}
		}
		//		System.out.println("Source:    "+s);
		//		System.out.println("Destroyed: "+result);
		return result;
	}


	/**
	 * @param input string s
	 * @return misspelled string from the input String with one or more character 
	 * 			Randomly removed from it 
	 * @author sherif
	 */
	private String charDeletion(String s){
		String result= new String();
		Random generator= new Random();
		for(int i=0 ; i<missplingRate ; i++){
			try{
				int randIndexS=generator.nextInt(s.length());
				String sub1 = s.substring(0,randIndexS);
				String sub2 = s.substring(randIndexS+1);
				result= sub1 + sub2;
			}catch (Exception e) {
				if(s.length()>=2)
					result= s.substring(1);
				else
					result=s;
			}
		}
		//		System.out.println("Source:    "+s);
		//		System.out.println("Destroyed: "+result);
		return result;
	}


	/**
	 * @param input string s
	 * @return misspelled string from the input String with one or more character 
	 * 			Randomly removed from it 
	 * @author sherif
	 */
	private String charInsertion(String s){
		String result= new String();
		Random generator= new Random();
		for(int i=0 ; i<missplingRate ; i++){
			try{
				int randAlphaIndex=generator.nextInt(alphabet.length());
				int randIndexS=generator.nextInt(s.length());
				randIndexS = (randIndexS==s.length())? randIndexS-1:randIndexS;
				String sub1 = s.substring(0,randIndexS);
				String sub2 = s.substring(randIndexS);
				result= sub1 + alphabet.charAt(randAlphaIndex) + sub2;
			}catch (Exception e) {
				if(s.length()>=2)
					result= 'X' + s;
				else
					result=s;
			}
		}
		//		System.out.println("Source:    "+s);
		//		System.out.println("Destroyed: "+result);
		return result;
	}


	/**
	 * @param input string s
	 * @return misspelled string from the input String with one or more permutation of its character 
	 * @author sherif
	 */
	private String charPermutation(String s){
		String result= new String();
		Random generator= new Random();
		for(int i=0 ; i<missplingRate ; i++){
			try{
				int randIndexS    = generator.nextInt(s.length());
				int neighborIndex = (randIndexS==s.length())? randIndexS-1:randIndexS+1; 
				char[] charArray= s.toCharArray();
				char tmp = charArray[neighborIndex];
				charArray[neighborIndex]= charArray[randIndexS];
				charArray[randIndexS]=tmp;
				result= new String(charArray);
			}catch (Exception e) {
				if(s.length()>=2){
					char[] charArray= s.toCharArray();
					char tmp = charArray[0];
					charArray[0]= charArray[1];
					charArray[1]=tmp;
					result= new String(charArray);
				}
				else
					result=s;
			}
		}
		//		System.out.println("Source:    "+s);
		//		System.out.println("Destroyed: "+result);
		return result;
	}



	public static void main(String args[]){
		InstanceMisspellingModifier m= new InstanceMisspellingModifier();
		System.out.println("charDeletion: "+m.charDeletion("abcd"));
		System.out.println("charInsertion: "+m.charInsertion("abcd"));
		System.out.println("charPermutation: "+m.charPermutation("abcd"));
		System.out.println("charSubstitution: "+m.charSubstitution("abcd"));
		System.out.println(Math.floor(3/4));
	}

}






















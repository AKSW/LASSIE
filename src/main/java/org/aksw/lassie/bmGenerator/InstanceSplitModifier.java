/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDFS;

/**
 * @author sherif
 *
 */
public class InstanceSplitModifier extends Modifier{
	public int partsCount=2;
	public List<Property> splitProperties=new ArrayList<Property>(); 
	
	
	/**
	 * @param m
	 *@author sherif
	 */
	public InstanceSplitModifier(Model m) {
		super(m);
	}
	
	public InstanceSplitModifier() {
	}

	/**
	 * @param splitProperties the splitProperties to set
	 */
	public void setSplitProperties(List<Property> splitProperties) {
		this.splitProperties = splitProperties;
	}
	
	public void addSplitProperty(Property splitProperty) {
		this.splitProperties.add(splitProperty);
	}


	
	/**
	 * @param partsCount the partsCount to set
	 */
	public void setPartsCount(int partsCount) {
		this.partsCount = partsCount;
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.benchmarker.Modifier#destroy()
	 */
	@Override
	Model destroy(Model subModel) {
		System.out.println();
		System.out.println("Splitting " + subModel.size() + " triples.");
		
		StmtIterator sItr = subModel.listStatements();

		while (sItr.hasNext()) {
			Statement stat = sItr.nextStatement();	
			List<Statement> splitStat = split(stat);
			destroyedModel.add(splitStat);
		}
		return destroyedModel;
	}

	private List<Statement> split(Statement stmt){
		List<Statement> result= new ArrayList<Statement>();
		
		if(!stmt.getObject().isLiteral()){
			System.err.println(stmt.getObject() + " is not a literal object");
			System.err.println("Can NOT split a non-literal object!!");
//			System.exit(1);
			result.add(stmt);
			return result;
		}
		

		Resource  subject   = stmt.getSubject();     
		Property  predicate = stmt.getPredicate();   
		RDFNode   object    = stmt.getObject(); 
		String[] splitObject = object.toString().split(" ", partsCount);

		for(int i=0; i<splitObject.length ; i++){
			RDFNode newObject= ResourceFactory.createTypedLiteral(splitObject[i]);
			
			Statement resultStmt;
			// if no user defined new properties then create the new properties as the old property name + "i"
			if(splitProperties.isEmpty()){ 
				Property newPredicate = ResourceFactory.createProperty(predicate.toString()+i);
				resultStmt = ResourceFactory.createStatement(subject, newPredicate, newObject);
			}else{
				resultStmt = ResourceFactory.createStatement(subject, splitProperties.get(i), newObject);
			}
			result.add(resultStmt);
		}

		return result;
	}

	
	public static void main(String args[]){
		InstanceSplitModifier sM=new InstanceSplitModifier();
		Resource s = ResourceFactory.createResource("medo.test");
		RDFNode o= ResourceFactory.createTypedLiteral("Medo koko dodo");
		Statement stmt = ResourceFactory.createStatement(s, RDFS.label, o);
		sM.setPartsCount(3);
		List<Property> sP = new ArrayList<Property>();
//		sP.add(ResourceFactory.createProperty("Name1"));
//		sP.add(ResourceFactory.createProperty("Name2"));
//		sP.add(ResourceFactory.createProperty("Name3"));
		sM.setSplitProperties(sP);
		System.out.println(sP);
		System.out.println(sM.split(stmt));
		
		System.out.println(o);
		System.out.println(o.asNode().getLiteral().getLexicalForm());

	}
}

/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import java.util.ArrayList;
import java.util.List;

import jena.rdfcat;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * @author sherif
 *
 */
public class ClassRenameModifier extends Modifier {


	public ClassRenameModifier(Model m) {
		super(m);
	}

	public ClassRenameModifier() {
	}

	/* (non-Javadoc)
	 * @see org.aksw.lassie.bmGenerator.Modifier#destroy(com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	Model destroy(Model subModel) {
		Model removeModel = ModelFactory.createDefaultModel();
		Model addModel = ModelFactory.createDefaultModel();
		
		List<String> classNames = new ArrayList<String>();
		if (baseClasses.size() == 0) {
			classNames = getClasses(subModel);
		} else {
			classNames = baseClasses;
		}
		
		for (String className : classNames) {
			String ranamedClassName = className+"_RENAME";
			subModel = renameClass(subModel, className,  ranamedClassName);
			modifiedClasses.add(ranamedClassName);
		}
		
//		StmtIterator iter = subModel.listStatements((Resource) null, RDF.type,(RDFNode) null);
//
//		while (iter.hasNext()) {
//			Statement stmt      = iter.nextStatement();  
//			Resource  subject   = stmt.getSubject();     
//			Property  predicate = stmt.getPredicate();   
//			RDFNode   object    = stmt.getObject();   
//			removeModel.add(stmt);
//			RDFNode newObject = ResourceFactory.createResource(object.toString()+"_RENAME");
//			addModel.add( subject, predicate, newObject);
//		}
//		
//		subModel.remove(removeModel);
//		subModel.add(addModel);
		
		return subModel;
	}


	public static void main(String[] args){
		Model m= loadModel(args[0]);
		ClassRenameModifier classRenamer=new ClassRenameModifier(m);

		System.out.println("----- Base Model -----");
		System.out.println("Size: "+ m.size());
		baseModel.write(System.out,"TTL");
		System.out.println();

		System.out.println("----- Remaned Model -----");
		Model desM = classRenamer.destroy(m);
		System.out.println("Size: " + desM.size());
		desM.write(System.out,"TTL");
	}
}

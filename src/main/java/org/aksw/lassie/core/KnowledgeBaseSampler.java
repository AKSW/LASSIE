/**
 * 
 */
package org.aksw.lassie.core;

import java.util.HashSet;
import java.util.Set;

import org.aksw.lassie.kb.KnowledgeBase;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.OWL;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/**
 * @author sherif
 *
 */
public class KnowledgeBaseSampler {
	
	protected KnowledgeBase kn;
	
	double sampleRate = 0.5; 

   OWLDataFactory owlDataFactory = new OWLDataFactoryImpl();

	KnowledgeBaseSampler(){
		
	}
	
	Model getKBSample(double sRate){
		sampleRate = sRate;
		Model result = ModelFactory.createDefaultModel();
		Set<OWLClass> sourceClasses = getClasses(kn);
		return result;
	}
	
	
	private Set<OWLClass> getClasses(KnowledgeBase kb) {
		Set<OWLClass> classes = new HashSet<OWLClass>();

		//get all OWL classes
		String query = String.format("SELECT ?type WHERE {?type a <%s>.}", OWL.Class.getURI());
		ResultSet rs = kb.executeSelect(query);
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			if (qs.get("type").isURIResource()) {
				classes.add(owlDataFactory.getOWLClass(IRI.create(qs.get("type").asResource().getURI())));
			}
		}

		//fallback: check for ?s a ?type where ?type is not asserted to owl:Class
		if (classes.isEmpty()) {
			query = "SELECT ?type WHERE {?s a ?type.}";
			rs = kb.executeSelect(query);
			while (rs.hasNext()) {
				qs = rs.next();
				if (qs.get("type").isURIResource()) {
					classes.add(owlDataFactory.getOWLClass(IRI.create(qs.get("type").asResource().getURI())));
				}
			}
		}
		return classes;
	}
	/**
	 * @param args
	 * @author sherif
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

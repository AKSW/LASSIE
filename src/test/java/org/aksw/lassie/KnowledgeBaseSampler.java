/**
 * 
 */
package org.aksw.lassie;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.kb.RemoteKnowledgeBase;
import org.dllearner.core.ComponentInitException;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/**
 * @author sherif
 *
 */
public class KnowledgeBaseSampler {
	private KnowledgeBase kb;
	private double rate = 0.1d;
	
	/**
	 * @param kb
	 * @param rate
	 *@author sherif
	 */
	public KnowledgeBaseSampler(KnowledgeBase kb, double rate) {
		super();
		this.kb = kb;
		this.rate = rate;
	}

	
	public Set<OWLClass> getLeafClasses() {
		Set<OWLClass> classes = new HashSet<OWLClass>();

		//get all OWL classes
		String query = 
				"SELECT ?leaf WHERE{" +
				" ?leaf rdfs:subClassOf ?super." +
				" FILTER NOT EXISTS { ?sup rdfs:subClassOf ?leaf }" + 
//				" FILTER regex(?leaf, \"^http://dbpedia.org/ontology\") " \\too slow
				"}";
			
		ResultSet rs = kb.executeSelect(query);
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			OWLDataFactory owlDataFactory = new OWLDataFactoryImpl();
			if (qs.get("leaf").isURIResource() && qs.get("leaf").toString().startsWith("http://dbpedia.org/ontology")) {
				classes.add(owlDataFactory.getOWLClass(IRI.create(qs.get("leaf").asResource().getURI())));
			}
		}
		return classes;
	}
	
	public Model getSample(){
		Model result = ModelFactory.createDefaultModel();
		Set<OWLClass> leafClasses = getLeafClasses();
		for (OWLClass leafClass : leafClasses) {
			int leafClassIndividualsCount = kb.getReasoner().getIndividualsCount(leafClass);
			int sampleIndividualCount = (int) Math.ceil(leafClassIndividualsCount * rate);
			SortedSet<OWLIndividual> sampleIndividuals = kb.getReasoner().getIndividuals(leafClass, sampleIndividualCount);
		// TODO get data for each of the sampleIndividuals
		}
		
		return result;
	}
	
	/**
	 * @param args
	 * @author sherif
	 * @throws MalformedURLException 
	 * @throws ComponentInitException 
	 */
	public static void main(String[] args) throws MalformedURLException, ComponentInitException {
		KnowledgeBaseSampler kbSampler = new KnowledgeBaseSampler(new RemoteKnowledgeBase(new SparqlEndpoint(new URL("http://dbpedia.org/sparql"))), 0.1d);
		Set<OWLClass> leafClasses = kbSampler.getLeafClasses();
	
		System.out.println(kbSampler.getLeafClasses());
		System.out.println(kbSampler.getLeafClasses().size());
	}

}

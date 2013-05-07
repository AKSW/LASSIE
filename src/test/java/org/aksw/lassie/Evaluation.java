package org.aksw.lassie;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import org.aksw.lassie.bmGenerator.BenchmarkGenerator;
import org.aksw.lassie.bmGenerator.ClassSplitModifier;
import org.aksw.lassie.bmGenerator.MisspellingModifier;
import org.aksw.lassie.bmGenerator.Modifier;
import org.aksw.lassie.core.ExpressiveSchemaMappingGenerator;
import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.kb.LocalKnowledgeBase;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.log4j.Logger;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.NamedClass;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGenerator;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGeneratorImpl;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class Evaluation {
	
	
	private static final Logger logger = Logger.getLogger(Evaluation.class.getName());
	
	private SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
	private ExtractionDBCache cache = new ExtractionDBCache("cache");
	private SPARQLReasoner reasoner = new SPARQLReasoner(new SparqlEndpointKS(endpoint, cache), cache);
	private ConciseBoundedDescriptionGenerator cbdGenerator = new ConciseBoundedDescriptionGeneratorImpl(endpoint, cache);
	private String ontologyURL = "http://downloads.dbpedia.org/3.8/dbpedia_3.8.owl.bz2";
	private String referenceModelFile = "dbpedia-sample.ttl";
	private String dbpediaNamespace = "http://dbpedia.org/ontology/";
	
	private int maxNrOfInstances = 100;
	private int maxCBDDepth = 0;//0 means only the directly asserted triples
	
	/**
	 * Create a sample of DBpedia, i.e. the schema + for each class for max n instances the CBD.
	 * @return
	 */
	private Model createReferenceDataset(){
		try {
			Model model = ModelFactory.createDefaultModel();
			//try to load sample from cache
			File file = new File(referenceModelFile);
			if(file.exists()){
				model.read(new FileInputStream(file), "TURTLE");
				return model;
			} 
			//load schema
			BZip2CompressorInputStream is = new BZip2CompressorInputStream(new URL(ontologyURL).openStream());
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLOntology ontology = manager.loadOntologyFromOntologyDocument(is);
			
			is = new BZip2CompressorInputStream(new URL(ontologyURL).openStream());
			model.read(is, "RDF/XML");
			//for each class c_i get n random instances + their CBD
			for (OWLClass cls : ontology.getClassesInSignature()) {
				if(!cls.toStringID().startsWith(dbpediaNamespace)) continue;
				logger.info("Generating sample for " + cls + "...");
				SortedSet<Individual> individuals = reasoner.getIndividuals(new NamedClass(cls.toStringID()), maxNrOfInstances);
				for (Individual individual : individuals) {
					Model cbd = cbdGenerator.getConciseBoundedDescription(individual.getName(), maxCBDDepth);
					model.add(cbd);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			model.write(new FileOutputStream(file), "TURTLE");
			return model;
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private Model createTestDataset(Model referenceDataset){
		BenchmarkGenerator benchmarker= new BenchmarkGenerator(referenceDataset);
//		//  if we want to destroy some instances
//		Map<Modifier, Double> instanceModefiersAndRates= new HashMap<Modifier, Double>();
//		instanceModefiersAndRates.put(new MisspellingModifier(), 0.1d);
//		Model testDataset = benchmarker.destroyInstances(instanceModefiersAndRates);
		
		// if we want to  destroy some classes
		Map<Modifier, Double> classModefiersAndRates= new HashMap<Modifier, Double>();
		classModefiersAndRates.put(new ClassSplitModifier(), 0.6d);
		Model testDataset = benchmarker.destroyClasses (classModefiersAndRates);
		return testDataset;
	}
	
	public void run(){
		Model referenceDataset = createReferenceDataset();
		Model testDataset = createTestDataset(referenceDataset);
		
		KnowledgeBase source = new LocalKnowledgeBase(testDataset);
		KnowledgeBase target = new LocalKnowledgeBase(referenceDataset);
		
		ExpressiveSchemaMappingGenerator generator = new ExpressiveSchemaMappingGenerator(source, target);
		generator.run();
	}
	
	public static void main(String[] args) throws Exception {
		new Evaluation().run();
	}

}

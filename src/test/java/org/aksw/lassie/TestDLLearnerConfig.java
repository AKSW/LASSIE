/**
 * 
 */
package org.aksw.lassie;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.aksw.lassie.bmGenerator.BenchmarkGenerator;
import org.aksw.lassie.bmGenerator.Modifier;
import org.aksw.lassie.core.ExpressiveSchemaMappingGenerator;
import org.aksw.lassie.core.NonExistingLinksException;
import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.kb.LocalKnowledgeBase;
import org.aksw.lassie.kb.RemoteKnowledgeBase;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.log4j.Logger;
import org.dllearner.core.EvaluatedDescription;
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

import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * @author sherif
 *
 */
public class TestDLLearnerConfig extends ExpressiveSchemaMappingGenerator {
	private static final Logger logger = Logger.getLogger(TestDLLearnerConfig.class.getName());
	private SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
	private ExtractionDBCache cache = new ExtractionDBCache("cache");
	private String cacheDirectory = "cache";
	private SPARQLReasoner reasoner = new SPARQLReasoner(new SparqlEndpointKS(endpoint, cacheDirectory), cacheDirectory);
	private ConciseBoundedDescriptionGenerator cbdGenerator = new ConciseBoundedDescriptionGeneratorImpl(endpoint, cache);
	private int maxNrOfClasses = 5;//20;//-1 all classes
	private int maxNrOfInstancesPerClass = 20;
	private Set<NamedClass> dbpediaClasses = new TreeSet<NamedClass>();
	private String ontologyURL = "http://downloads.dbpedia.org/3.8/dbpedia_3.8.owl.bz2";
	private OWLOntology dbpediaOntology;
	private String dbpediaNamespace = "http://dbpedia.org/ontology/";
	private String referenceModelFile = "dbpedia-sample" + ((maxNrOfClasses > 0) ? ("_" + maxNrOfClasses + "_" + maxNrOfInstancesPerClass) : "") + ".ttl";
	private int maxCBDDepth = 0;//0 means only the directly asserted triples

	
	/**
	 * @param source
	 * @param target
	 *@author sherif
	 */
	public  TestDLLearnerConfig(KnowledgeBase source, KnowledgeBase target) {
		super(source, target);
	}
	
	public  TestDLLearnerConfig() {
		
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> test() {
		
		String sourceClassUri="http://dbpedia.org/ontology/Ambassador";
//		String sourceClassUri="http://dbpedia.org/ontology/Town";
//		String sourceClassUri="http://dbpedia.org/ontology/Continent";
//		String sourceClassUri="http://dbpedia.org/ontology/ProtectedArea";
//		String sourceClassUri="http://dbpedia.org/ontology/HistoricBuilding";
		
		Map<String, Object> result = new HashMap<String, Object>();

		//1. read class and instances
		NamedClass sourceClass = new NamedClass(sourceClassUri);
		// get perfect positive examples as some instance of the source class directly without using LIMES
		SortedSet<Individual> targetInstances = sourceKB.getReasoner().getIndividuals(sourceClass,maxNrOfInstancesPerClass);
		
		
//		NamedClass sourceClass = null;
//		SortedSet<Individual> targetInstances = null ;
//		ObjectInputStream in;
//		try {
//			in = new ObjectInputStream(new FileInputStream("sourceClass1.ser"));
//			sourceClass = (NamedClass) in.readObject();
//			System.out.println("\n---------- sourceClass.ser ----------");
//			System.out.println(sourceClass);
//
//			in = new ObjectInputStream(new FileInputStream("targetInstances1.ser"));
//			targetInstances = (SortedSet<Individual>) in.readObject();
//			System.out.println("\n---------- targetInstances.ser ----------");
//			System.out.println(targetInstances);
//
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}catch (ClassNotFoundException e) {
//			e.printStackTrace();
//		}

		//2. Compute Mapping
		try {
			currentClass = sourceClass;
			
			EvaluatedDescription singleMapping = computeMapping(targetInstances);
			System.out.println("\n---------- EvaluatedDescription ----------");
			System.out.println(singleMapping);
		} catch (NonExistingLinksException e) {
			System.out.println(e.getMessage() + "Skipped learning.");
			e.printStackTrace();
		}
		return result;
	}





	
	private Model createTestDataset(Model referenceDataset, Map<Modifier, Double> instanceModefiersAndRates, Map<Modifier, Double> classModefiersAndRates){
		BenchmarkGenerator benchmarker= new BenchmarkGenerator(referenceDataset);
		Model testDataset = ModelFactory.createDefaultModel();

		if(!instanceModefiersAndRates.isEmpty()){
			testDataset = benchmarker.destroyInstances(instanceModefiersAndRates);
		}

		if(!classModefiersAndRates.isEmpty()){
			testDataset = benchmarker.destroyClasses (classModefiersAndRates);
		}
		return testDataset;
	}
	
	private Model createDBpediaReferenceDataset(){
		try {
			//load schema
			BZip2CompressorInputStream is = new BZip2CompressorInputStream(new URL(ontologyURL).openStream());
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			dbpediaOntology = manager.loadOntologyFromOntologyDocument(is);

			//extract DBpedia classes
			for (OWLClass cls : dbpediaOntology.getClassesInSignature()) {
				if(!cls.toStringID().startsWith(dbpediaNamespace)) continue;
				dbpediaClasses.add(new NamedClass(cls.toStringID()));
			}
			if(maxNrOfClasses > 0){
				List<NamedClass> tmp = new ArrayList<NamedClass>(dbpediaClasses);
				Collections.shuffle(tmp, new Random(123));
				dbpediaClasses = new TreeSet<NamedClass>(tmp.subList(0, maxNrOfClasses));
			}

			Model model = ModelFactory.createDefaultModel();
			//add schema
			is = new BZip2CompressorInputStream(new URL(ontologyURL).openStream());
			model.read(is, null, "RDF/XML");
			
			//try to load sample from cache
			File file = new File(referenceModelFile);
			if(file.exists()){
				model.read(new FileInputStream(file), null, "TURTLE");
				filterModel(model);
				return model;
			} 

			is = new BZip2CompressorInputStream(new URL(ontologyURL).openStream());
			model.read(is, "RDF/XML");

			//for each class c_i get n random instances + their CBD
			for (NamedClass cls : dbpediaClasses) {
				logger.info("Generating sample for " + cls + "...");
				SortedSet<Individual> individuals = reasoner.getIndividuals(cls, maxNrOfInstancesPerClass);
				for (Individual individual : individuals) {
					Model cbd = cbdGenerator.getConciseBoundedDescription(individual.getName(), maxCBDDepth+2);
					model.add(cbd);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			filterModel(model);
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
	
	private void filterModel(Model model){
		//remove owl:FunctionalProperty axioms to avoid inconsistencies
		Model remove = ModelFactory.createDefaultModel();
		for (Statement st : model.listStatements(null, RDF.type, OWL.FunctionalProperty).toList()) {
			remove.add(st);
		}
//		//keep only ?s a <http://dbpedia.org/ontology/$> statements
//		for (Statement st : model.listStatements(null, RDF.type, (RDFNode)null).toList()) {
//			if(!st.getObject().equals(model.getResource("http://dbpedia.org/ontology"))){
//				remove.add(st);
//			}
//		}
		
		
		model.remove(remove);
	}
	
	


	/**
	 * @param args
	 * @author sherif
	 */
	public static void main(String[] args) {

		Model referenceDataset = new TestDLLearnerConfig().createDBpediaReferenceDataset();
		referenceDataset = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM_RDFS_INF, referenceDataset);
//		
//		Map<Modifier, Double> classModefiersAndRates= new HashMap<Modifier, Double>();
//		Map<Modifier, Double> instanceModefiersAndRates= new HashMap<Modifier, Double>();
//		//		instanceModefiersAndRates.put(new MisspellingModifier(), 0.1d);
//		//		Map<Modifier, Double> classModefiersAndRates= new HashMap<Modifier, Double>();
//				classModefiersAndRates.put(new ClassSplitModifier(), 0.5d);
//		//		classModefiersAndRates.put(new ClassMergeModifier(), 1d);
//		//		classModefiersAndRates.put(new ClassRenameModifier(), 1d);
//		//		classModefiersAndRates.put(new ClassTypeDeleteModifier(), 0.5d);
//		Model testDataset = new TestDLLearnerConfig(null,null).createTestDataset(referenceDataset, instanceModefiersAndRates, classModefiersAndRates);

		KnowledgeBase source = new LocalKnowledgeBase(referenceDataset, "http://dbpedia.org/ontology/");
		KnowledgeBase target = new LocalKnowledgeBase(referenceDataset, "http://dbpedia.org/ontology/");
		
		source = new RemoteKnowledgeBase(SparqlEndpoint.getEndpointDBpedia(), new ExtractionDBCache("cache"), "http://dbpedia.org/ontology/");
		target = source;
		
//		KnowledgeBase target = new LocalKnowledgeBase(testDataset);

		TestDLLearnerConfig tester = new TestDLLearnerConfig(source,target);
		tester.test();
		

	}
}

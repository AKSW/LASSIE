package org.aksw.lassie;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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
import org.aksw.lassie.bmGenerator.ClassDeleteModifier;
import org.aksw.lassie.bmGenerator.ClassMergeModifier;
import org.aksw.lassie.bmGenerator.ClassRenameModifier;
import org.aksw.lassie.bmGenerator.ClassSplitModifier;
import org.aksw.lassie.bmGenerator.ClassTypeDeleteModifier;
import org.aksw.lassie.bmGenerator.Modifier;
import org.aksw.lassie.core.ExpressiveSchemaMappingGenerator;
import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.kb.LocalKnowledgeBase;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.log4j.Logger;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.owl.Description;
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

	private String dbpediaNamespace = "http://dbpedia.org/ontology/";
	private OWLOntology dbpediaOntology;

	private Set<NamedClass> dbpediaClasses = new TreeSet<NamedClass>();


	private static Map<Modifier, Double> classModefiersAndRates= new HashMap<Modifier, Double>();
	private int maxNrOfClasses = 20;//-1 all classes
	private int maxNrOfInstancesPerClass = 100;

	private int maxCBDDepth = 0;//0 means only the directly asserted triples

	private String referenceModelFile = "dbpedia-sample" + ((maxNrOfClasses > 0) ? ("_" + maxNrOfClasses + "_" + maxNrOfInstancesPerClass) : "") + ".ttl";

	/**
	 * Create a sample of DBpedia, i.e. the schema + for each class for max n instances the CBD.
	 * @return
	 */
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
			//try to load sample from cache
			File file = new File(referenceModelFile);
			if(file.exists()){
				model.read(new FileInputStream(file), null, "TURTLE");
				return model;
			} 

			is = new BZip2CompressorInputStream(new URL(ontologyURL).openStream());
			model.read(is, "RDF/XML");

			//for each class c_i get n random instances + their CBD
			for (NamedClass cls : dbpediaClasses) {
				logger.info("Generating sample for " + cls + "...");
				SortedSet<Individual> individuals = reasoner.getIndividuals(cls, maxNrOfInstancesPerClass);
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

	private void loadDBpediaSchema(){

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

	public Map<String, Object> run(){
		Model referenceDataset = createDBpediaReferenceDataset();

		Map<Modifier, Double> instanceModefiersAndRates= new HashMap<Modifier, Double>();
		//		instanceModefiersAndRates.put(new MisspellingModifier(), 0.1d);
		//		Map<Modifier, Double> classModefiersAndRates= new HashMap<Modifier, Double>();
		//classModefiersAndRates.put(new ClassSplitModifier(), 0.5d);
		classModefiersAndRates.put(new ClassMergeModifier(), 0.5d);
		//		classModefiersAndRates.put(new ClassRenameModifier(), 1d);
		//		classModefiersAndRates.put(new ClassTypeDeleteModifier(), 0.5d);
		Model testDataset = createTestDataset(referenceDataset, instanceModefiersAndRates, classModefiersAndRates);

		KnowledgeBase source = new LocalKnowledgeBase(referenceDataset);
		KnowledgeBase target = new LocalKnowledgeBase(referenceDataset);
//		KnowledgeBase target = new LocalKnowledgeBase(testDataset);

		ExpressiveSchemaMappingGenerator generator = new ExpressiveSchemaMappingGenerator(source, target);
		
		Map<String, Object> result = generator.run(dbpediaClasses, dbpediaClasses);

		return result;
	}
	

	/**
	 * @param result
	 * @author sherif
	 */
	@SuppressWarnings("unchecked")
	private void printResults(Map<String, Object> result) {
		System.out.println("\n----------- RESULTS -----------");
		System.out.println("No of Classes:              " + maxNrOfClasses);
		System.out.println("No of Instance per Classes: " + maxNrOfInstancesPerClass);
		System.out.println("MODIFIER(S):");
		int j=1;
		for(Modifier m: classModefiersAndRates.keySet()){
			System.out.println(j++ + ". " + m.getClass().getSimpleName() + "\t" + classModefiersAndRates.get(m)*100 + "%");
		}
		for(String key:result.keySet()){
			if(key.equals("mapping")){
				System.out.println("\nFINAL MAPPING:");
				
				Map<NamedClass, Description> map = (Map<NamedClass, Description>) result.get(key);
				for(NamedClass nC: map.keySet()){
					System.out.println(nC + "\t" + map.get(nC));
				}
			}
			if(key.equals("mappingTop10")){
				System.out.println("\nTOP 10 MAPPINGS:");

				Map<NamedClass, List<? extends EvaluatedDescription>> map = (Map<NamedClass, List<? extends EvaluatedDescription>>) result.get(key);
				for(NamedClass nC: map.keySet()){
					System.out.println("\n"+ nC);
					List<? extends EvaluatedDescription> mapList = map.get(nC);
					int i=1;
					for (EvaluatedDescription ed : mapList) {
						System.out.println("\t" + i + ". " + ed.toString());
						if(i>10) 
							break;
						i++;
					}
				}
			}
			if(key.equals("coverage")){
				System.out.println("\nCOVERAGE:");
				Map<Integer, Double> map = (Map<Integer, Double>) result.get(key);
				for(Integer i : map.keySet()){
					System.out.println(i + "\t" + map.get(i));
				}
			}
		}
	}


	public static void main(String[] args) throws Exception {
		Evaluation evaluator = new Evaluation();
		Map<String, Object> result = evaluator.run();
		evaluator.printResults(result);
	}

}

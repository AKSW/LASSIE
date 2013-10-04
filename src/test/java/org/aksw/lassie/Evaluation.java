package org.aksw.lassie;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.aksw.lassie.bmGenerator.BenchmarkGenerator;
import org.aksw.lassie.bmGenerator.ClassMergeModifier;
import org.aksw.lassie.bmGenerator.ClassRenameModifier;
import org.aksw.lassie.bmGenerator.InstanceAbbreviationModifier;
import org.aksw.lassie.bmGenerator.InstanceAcronymModifier;
import org.aksw.lassie.bmGenerator.InstanceIdentityModifier;
import org.aksw.lassie.bmGenerator.InstanceMergeModifier;
import org.aksw.lassie.bmGenerator.InstanceMisspellingModifier;
import org.aksw.lassie.bmGenerator.InstanceSplitModifier;
import org.aksw.lassie.bmGenerator.Modifier;
import org.aksw.lassie.core.ExpressiveSchemaMappingGenerator;
import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.kb.KnowledgebaseSampleGenerator;
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
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.common.collect.Multimap;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class Evaluation {


	private static final Logger logger = Logger.getLogger(Evaluation.class.getName());

	private SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
	//	private SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpediaLiveAKSW();
	private SPARQLReasoner reasoner = new SPARQLReasoner(new SparqlEndpointKS(endpoint), "cache");
	private ConciseBoundedDescriptionGenerator cbdGenerator = new ConciseBoundedDescriptionGeneratorImpl(endpoint, "cache");
	private String ontologyURL = "http://downloads.dbpedia.org/3.8/dbpedia_3.8.owl.bz2";

	private String dbpediaNamespace = "http://dbpedia.org/ontology/";
	private OWLOntology dbpediaOntology;

	private Set<NamedClass> modifiedDbpediaClasses = new TreeSet<NamedClass>();
	private Set<NamedClass> classesToLearn 			= new TreeSet<NamedClass>();

	private static Map<Modifier, Double> classModifiersAndRates    = new HashMap<Modifier, Double>();
	private static Map<Modifier, Double> instanceModifiersAndRates = new HashMap<Modifier, Double>();

	private int maxNrOfClasses = 1;//-1 all classes
	private int maxNrOfInstancesPerClass = 20;

	private int maxCBDDepth = 0;//0 means only the directly asserted triples

	private String referenceModelFile = "dbpedia-sample" + ((maxNrOfClasses > 0) ? ("_" + maxNrOfClasses + "_" + maxNrOfInstancesPerClass) : "") + ".ttl";

	//constructors
	public Evaluation(){
		super();
	}

	public Evaluation(int maxNrOfClasses,int maxNrOfInstancesPerClass, Map<Modifier, Double> classModifiers2Rates, Map<Modifier, Double> instanceModifiers2Rates) {
		super();
		this.maxNrOfClasses = maxNrOfClasses;
		this.maxNrOfInstancesPerClass = maxNrOfInstancesPerClass;
		classModifiersAndRates = classModifiers2Rates;
		instanceModifiersAndRates = instanceModifiers2Rates;
	}

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
				classesToLearn.add(new NamedClass(cls.toStringID()));
			}
			if(maxNrOfClasses > 0){
				List<NamedClass> tmp = new ArrayList<NamedClass>(classesToLearn);
				Collections.shuffle(tmp, new Random(123));
				classesToLearn = new TreeSet<NamedClass>(tmp.subList(0, maxNrOfClasses));
			}
			//TODO remove
			//			dbpediaClasses = Sets.newHashSet(
			//					new NamedClass("http://dbpedia.org/ontology/Ambassador"), 
			//					new NamedClass("http://dbpedia.org/ontology/Continent"));

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
			for (NamedClass cls : classesToLearn) {
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

	private Model createTestDataset(Model referenceDataset, Map<Modifier, Double> instanceModifiersAndRates, Map<Modifier, Double> classModifiersAndRates){
		BenchmarkGenerator benchmarker= new BenchmarkGenerator(referenceDataset);
		Modifier.setNameSpace(dbpediaNamespace);
		benchmarker.setBaseClasses(classesToLearn);

		Model testDataset = ModelFactory.createDefaultModel();

		if(!instanceModifiersAndRates.isEmpty()){
			testDataset = benchmarker.destroyInstances(instanceModifiersAndRates);
		}

		if(!classModifiersAndRates.isEmpty()){
			testDataset = benchmarker.destroyClasses (classModifiersAndRates);
		}
		modifiedDbpediaClasses = benchmarker.getModifiedNamedClasses();
		return testDataset;
	}

	public Map<String, Object> run(){
		//create a sample of the knowledge base
		LocalKnowledgeBase sampleKB = KnowledgebaseSampleGenerator.createKnowledgebaseSample(endpoint, dbpediaNamespace, maxNrOfClasses, maxNrOfInstancesPerClass);

		//we assume that the target is the sample KB itself
		KnowledgeBase target = sampleKB;

		//we create the source KB by modifying the data of the sample KB  
		Model sampleKBModel = sampleKB.getModel();

		if(instanceModifiersAndRates.isEmpty() && classModifiersAndRates.isEmpty()){
			logger.error("No modifiers specified, EXIT");
			System.exit(1);
		}

		Model modifiedReferenceDataset = createTestDataset(sampleKBModel, instanceModifiersAndRates, classModifiersAndRates);

		KnowledgeBase source = new LocalKnowledgeBase(modifiedReferenceDataset, sampleKB.getNamespace());
		try {
			// just 4 test
			modifiedReferenceDataset.write(new FileOutputStream(new File("test.nt")),"TTL");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		ExpressiveSchemaMappingGenerator generator = new ExpressiveSchemaMappingGenerator(source, target);
		generator.setTargetDomainNameSpace(dbpediaNamespace);
		Map<String, Object> result = generator.run(modifiedDbpediaClasses);

		return result;
	}

	/** 
	 * 
	 * @author sherif
	 */
	private void setModefiersManually() {
		// instance modifiers
		//				instanceModifiersAndRates.put(new InstanceIdentityModifier(),1d);
		instanceModifiersAndRates.put(new InstanceMisspellingModifier(),	0.2d);
		//				instanceModifiersAndRates.put(new InstanceAbbreviationModifier(),	0.2d);
		//				instanceModifiersAndRates.put(new InstanceAcronymModifier(),		0.2d);
		//				instanceModifiersAndRates.put(new InstanceMergeModifier(),			0.2d);
		//				instanceModifiersAndRates.put(new InstanceSplitModifier(),			0.2d);


		// class modifiers
		//				classModifiersAndRates.put(new ClassIdentityModifier(), 1d);
		//				classModifiersAndRates.put(new ClassSplitModifier(),  		0.2d);
		//				classModifiersAndRates.put(new ClassDeleteModifier(),		0.2d);
		classModifiersAndRates.put(new ClassMergeModifier(),  		0.2d);
		//				classModifiersAndRates.put(new ClassRenameModifier(), 		0.2d);
		//				classModifiersAndRates.put(new ClassTypeDeleteModifier(), 	0.2d);
	}




	public Map<String, Object> runIntensionalEvaluation(){
		//create a sample of the knowledge base
		LocalKnowledgeBase sampleKB = KnowledgebaseSampleGenerator.createKnowledgebaseSample(endpoint, dbpediaNamespace, maxNrOfClasses, maxNrOfInstancesPerClass);

		//we assume that the target is the sample KB itself
		KnowledgeBase target = sampleKB;

		//we create the source KB by modifying the data of the sample KB
		Model sampleKBModel = sampleKB.getModel();

		// instance modifiers
		//		instanceModefiersAndRates.put(new InstanceIdentityModifier(),1d);
		instanceModifiersAndRates.put(new InstanceMisspellingModifier(),	0.2d);
		//		instanceModefiersAndRates.put(new InstanceAbbreviationModifier(),	0.2d);
		//		instanceModefiersAndRates.put(new InstanceAcronymModifier(),		0.2d);
		//				instanceModefiersAndRates.put(new InstanceMergeModifier(),			0.2d);
		//		instanceModefiersAndRates.put(new InstanceSplitModifier(),			0.2d);


		// class modifiers
		//		classModefiersAndRates.put(new ClassIdentityModifier(), 1d);
		//		classModefiersAndRates.put(new ClassSplitModifier(),  		0.2d);
		//		classModefiersAndRates.put(new ClassDeleteModifier(),		0.2d);
		//		classModefiersAndRates.put(new ClassMergeModifier(),  		0.2d);
		classModifiersAndRates.put(new ClassRenameModifier(), 		0.2d);
		//		classModefiersAndRates.put(new ClassTypeDeleteModifier(), 	0.2d);
		Model modifiedReferenceDataset = createTestDataset(sampleKBModel, instanceModifiersAndRates, classModifiersAndRates);

		KnowledgeBase source = new LocalKnowledgeBase(modifiedReferenceDataset, sampleKB.getNamespace());

		ExpressiveSchemaMappingGenerator generator = new ExpressiveSchemaMappingGenerator(source, target);
		generator.setTargetDomainNameSpace(dbpediaNamespace);

		//		return generator.run(modifiedDbpediaClasses, Sets.newHashSet(new NamedClass("http://dbpedia.org/ontology/Person")));
		return generator.run(modifiedDbpediaClasses);

	}


	/**
	 * @param result
	 * @author sherif
	 * @throws FileNotFoundException 
	 */
	@SuppressWarnings("unchecked")
	private void printResults(Map<String, Object> result) throws FileNotFoundException {
		System.setOut(new PrintStream(new FileOutputStream("results"+ ((maxNrOfClasses > 0) ? ("-" + maxNrOfClasses + "-" + maxNrOfInstancesPerClass) : "") + ".txt")));

		System.out.println("\n----------- RESULTS -----------");
		System.out.println("No of Classes:              " + maxNrOfClasses);
		System.out.println("No of Instance per Classes: " + maxNrOfInstancesPerClass);
		System.out.println("MODIFIER(S):");
		int j=1;
		for(Modifier m: classModifiersAndRates.keySet()){
			System.out.println(j++ + ". " + m.getClass().getSimpleName() + "\t" + classModifiersAndRates.get(m)*100 + "%");
		}
		for(Modifier m: instanceModifiersAndRates.keySet()){
			System.out.println(j++ + ". " + m.getClass().getSimpleName() + "\t" + instanceModifiersAndRates.get(m)*100 + "%");
		}
		for(String key:result.keySet()){
			if(key.equals("mapping")){
				System.out.println("\nFINAL MAPPING:");

				Map<NamedClass, Description> map = (Map<NamedClass, Description>) result.get(key);
				for(NamedClass nC: map.keySet()){
					System.out.println(nC + "\t" + map.get(nC));
				}
			}

			if(key.equals("coverage")){
				System.out.println("\nCOVERAGE:");
				Map<Integer, Double> iteration2coverage = (Map<Integer, Double>) result.get(key);
				Multimap<Integer, Map<NamedClass, Double>> iteration2sourceClass2PFMeasure = (Multimap<Integer, Map<NamedClass, Double>>) result.get("iteration2sourceClass2PFMeasure");
				System.out.println("IterationNr\tCoverage\tAVG-F\tclass->FMeasure");
				//compute the average F measure for all classes instance mappings
				double sum = 0d, count = 0f;
				for(Integer i : iteration2coverage.keySet()){
					Iterator<Map<NamedClass, Double>> iter = iteration2sourceClass2PFMeasure.get(i).iterator();
					while(iter.hasNext()){
						for(Double fm : iter.next().values()){
							sum += fm;
							count++; 
						}
					}
					System.out.println(i + "\t" + iteration2coverage.get(i) + "\t"+ (sum/count) + "\t" + iteration2sourceClass2PFMeasure.get(i));
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

			if(key.equals("posExamples")){
				System.out.println("\nPOSITIVE EXAMPLES:");

				Multimap<NamedClass, String> map = (Multimap<NamedClass, String>) result.get(key);
				for(NamedClass nC: map.keySet()){
					System.out.println("\n"+ nC);
					Collection<String> mapList = map.get(nC);
					int i=1;
					for (String str : mapList) {
						System.out.println("\t" + i++ + ". " + str);
					}
				}
			}

			if(key.equals("Modifier2pos")){
				Map<Modifier, Integer> map = (Map<Modifier, Integer>) result.get(key);
				for(Modifier m: map.keySet()){
					System.out.println("\nModifier: " + m + " Pos: "+ map.get(m));
				}
			}

			if(key.equals("modifier2optimalSolution")){
				Map<Modifier, Description> map = (Map<Modifier, Description>) result.get(key);
				for(Modifier m : map.keySet()){
					System.out.println("\nModifier: " + m + " Pos: "+ map.get(m).toString());
				}
			}
		}
	}

	public void printShortResults(Map<String, Object> result, String resultId) throws FileNotFoundException {
		System.setOut(new PrintStream(new FileOutputStream(resultId + ((maxNrOfClasses > 0) ? ("-" + maxNrOfClasses + "-" + maxNrOfInstancesPerClass) : "") + ".txt")));

		System.out.println("No of Classes:              " + maxNrOfClasses);
		System.out.println("No of Instance per Classes: " + maxNrOfInstancesPerClass);
		System.out.println("MODIFIER(S):");
		int j=1;
		for(Modifier m: classModifiersAndRates.keySet()){
			System.out.println(j++ + ". " + m.getClass().getSimpleName() + "\t" + classModifiersAndRates.get(m)*100 + "%");
		}
		for(Modifier m: instanceModifiersAndRates.keySet()){
			System.out.println(j++ + ". " + m.getClass().getSimpleName() + "\t" + instanceModifiersAndRates.get(m)*100 + "%");


			Map<Integer, Double> iteration2coverage = (Map<Integer, Double>) result.get("coverage");
			Multimap<Integer, Map<NamedClass, Double>> iteration2sourceClass2PFMeasure = (Multimap<Integer, Map<NamedClass, Double>>) result.get("iteration2sourceClass2PFMeasure");
			System.out.println("IterationNr\tCoverage\tAVG-F");
			//compute the average F measure for all classes instance mappings
			double sum = 0d, count = 0f;
			for(Integer i : iteration2coverage.keySet()){
				Iterator<Map<NamedClass, Double>> iter = iteration2sourceClass2PFMeasure.get(i).iterator();
				while(iter.hasNext()){
					for(Double fm : iter.next().values()){
						sum += fm;
						count++; 
					}
				}
				System.out.println(i + "\t" + iteration2coverage.get(i) + "\t"+ (sum/count) );
			}
		}
	}


	public static void main(String[] args) throws Exception {
		System.setOut(new PrintStream("/dev/null"));
		Evaluation evaluator = new Evaluation();
		long startTime = System.currentTimeMillis();
		evaluator.setModefiersManually();
		Map<String, Object> result = evaluator.run();
		//		Map<String, Object> result = evaluator.runIntensionalEvaluation();
		logger.info("FINAL RESULTS: " + result);

		evaluator.printShortResults(result, "resultsOf");

		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("\nTotal Time: " + totalTime + " ms");
	}

}

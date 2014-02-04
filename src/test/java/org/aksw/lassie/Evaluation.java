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
import java.util.HashSet;
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
import org.aksw.lassie.result.LassieResultRecorder;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.log4j.Logger;
import org.dllearner.core.EvaluatedDescription;
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
import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;

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
	protected static int maxNrOfIterations;
	
//	public NamedClass subTreeRootClass = new NamedClass("http://dbpedia.org/ontology/ChemicalSubstance");
//	public NamedClass subTreeRootClass = new NamedClass("http://dbpedia.org/ontology/Food");
	public NamedClass subTreeRootClass = new NamedClass("http://dbpedia.org/ontology/Agent");
	//constructors
	public Evaluation(){
		super();
	}

	public Evaluation(int maxNrOfClasses,int maxNrOfInstancesPerClass, 
			Map<Modifier, Double> classModifiers2Rates, Map<Modifier, Double> instanceModifiers2Rates) {
		super();
		this.maxNrOfClasses = maxNrOfClasses;
		this.maxNrOfInstancesPerClass = maxNrOfInstancesPerClass;
		classModifiersAndRates = classModifiers2Rates;
		instanceModifiersAndRates = instanceModifiers2Rates;
	}
	
	public Evaluation(int nrOfIterations, int maxNrOfClasses,int maxNrOfInstancesPerClass, 
			Map<Modifier, Double> classModifiers2Rates, Map<Modifier, Double> instanceModifiers2Rates) {
		this(maxNrOfClasses, maxNrOfInstancesPerClass, classModifiers2Rates, instanceModifiers2Rates);
		maxNrOfIterations = nrOfIterations;
		
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

	public Model createTestDataset(Model referenceDataset, Map<Modifier, Double> instanceModifiersAndRates, Map<Modifier, Double> classModifiersAndRates,
			int noOfclasses, int noOfInstancePerClass){
		
		Model classesWithAtLeastNInstancesModel = getModelOfClassesWithAtLeastNInstances(referenceDataset, noOfclasses, noOfInstancePerClass);
		Model differenceModel = referenceDataset.difference(classesWithAtLeastNInstancesModel);
		BenchmarkGenerator benchmarker= new BenchmarkGenerator(classesWithAtLeastNInstancesModel);
		Modifier.setNameSpace(dbpediaNamespace);
		benchmarker.setBaseClasses(classesToLearn);
		Model testDataset = ModelFactory.createDefaultModel();

		if(!instanceModifiersAndRates.isEmpty()){
			testDataset = benchmarker.destroyInstances(instanceModifiersAndRates);
		}

		if(!classModifiersAndRates.isEmpty()){
			testDataset = benchmarker.destroyClasses (classModifiersAndRates);
		}

		testDataset.add(differenceModel);
		modifiedDbpediaClasses = benchmarker.getModifiedNamedClasses();
		return testDataset;
	}

	/**
	 * @param referenceDataset
	 * @param nrOfclasses
	 * @param nrOfInstancePerClass
	 * @return
	 * @author sherif
	 */
	private Model getModelOfClassesWithAtLeastNInstances(Model referenceDataset, int nrOfclasses, int nrOfInstancePerClass) {

		//get list of classes
		Model resultModel = ModelFactory.createDefaultModel();
		String sparqlQueryString= 
				"SELECT ?type WHERE{" +
						"?s a ?type. FILTER regex(STR(?type), \"http://dbpedia.org/ontology/\") }" +
						"GROUP BY ?type " +
						"HAVING (count(?s) >= " + nrOfInstancePerClass +")";
		QueryFactory.create(sparqlQueryString);
		QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, referenceDataset);
		ResultSet selectResult = qexec.execSelect();
		List<NamedClass> classesWithEnoughInstances = new ArrayList<NamedClass>();
		while ( selectResult.hasNext())	{
			QuerySolution soln = selectResult.nextSolution() ;
			RDFNode cls = soln.get("type");
			classesWithEnoughInstances.add(new NamedClass(cls.toString()));

			//shuffle and select first n classes
			Collections.shuffle(classesWithEnoughInstances, new Random(123));
			classesWithEnoughInstances = classesWithEnoughInstances.subList(0, Math.min(nrOfclasses, classesWithEnoughInstances.size()));

		}
		classesToLearn = Sets.newHashSet(classesWithEnoughInstances);

		//add instances for each class
		for(NamedClass cls : classesWithEnoughInstances){
			Model m = ModelFactory.createDefaultModel();
			sparqlQueryString = "CONSTRUCT {?s ?p ?o} WHERE {?s a <" + cls.getName() + ">. ?s ?p ?o}";
			QueryFactory.create(sparqlQueryString);
			qexec = QueryExecutionFactory.create(sparqlQueryString, referenceDataset);
			m = qexec.execConstruct();
			resultModel.add(m);
		}

		logger.info("resultModel size: " + resultModel.size());
		return resultModel;
	}


	public LassieResultRecorder run(Set<NamedClass> testClasses, boolean useRemoteKB){
		//create a sample of the knowledge base
//		LocalKnowledgeBase sampleKB = KnowledgebaseSampleGenerator.createKnowledgebaseSample(endpoint, dbpediaNamespace, Integer.MAX_VALUE, maxNrOfInstancesPerClass);
//		LocalKnowledgeBase sampleKB = KnowledgebaseSampleGenerator.createKnowledgebaseSample(endpoint, dbpediaNamespace, maxNrOfClasses, maxNrOfInstancesPerClass, testClasses);
		LocalKnowledgeBase sampleKB = KnowledgebaseSampleGenerator.createKnowledgebaseSubTreeSample(endpoint, dbpediaNamespace, subTreeRootClass, maxNrOfInstancesPerClass);
		//we assume that the target is the sample KB itself
		KnowledgeBase targetKB = sampleKB;

		//we create the source KB by modifying the data of the sample KB  
		Model sampleKBModel = sampleKB.getModel();
		logger.info("sampleKBModel.size(): " + sampleKBModel.size());

		if(instanceModifiersAndRates.isEmpty() && classModifiersAndRates.isEmpty()){
			logger.error("No modifiers specified, EXIT");
			System.exit(1);
		}

		Model modifiedReferenceDataset = createTestDataset(sampleKBModel, instanceModifiersAndRates, classModifiersAndRates, maxNrOfClasses, maxNrOfInstancesPerClass);
		KnowledgeBase sourceKB = new LocalKnowledgeBase(modifiedReferenceDataset, sampleKB.getNamespace());

		ExpressiveSchemaMappingGenerator generator = new ExpressiveSchemaMappingGenerator(sourceKB, targetKB, endpoint, maxNrOfIterations);
		generator.setTargetDomainNameSpace(dbpediaNamespace);
		if(testClasses.size()>0){
			return generator.run(testClasses, useRemoteKB);
		}
		return generator.run(modifiedDbpediaClasses, useRemoteKB);
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




	public LassieResultRecorder runIntensionalEvaluation(boolean useRemoteKB){
		//create a sample of the knowledge base
		LocalKnowledgeBase sampleKB = KnowledgebaseSampleGenerator.createKnowledgebaseSample(endpoint, dbpediaNamespace, maxNrOfClasses, maxNrOfInstancesPerClass, new HashSet<NamedClass>());

		//we assume that the target is the sample KB itself
		KnowledgeBase target = sampleKB;

		//we create the source KB by modifying the data of the sample KB
		Model sampleKBModel = sampleKB.getModel();

		// instance modifiers
		//		instanceModefiersAndRates.put(new InstanceIdentityModifier(),1d);
		instanceModifiersAndRates.put(new InstanceMisspellingModifier(),	0.2d);
		//		instanceModifiersAndRates.put(new InstanceAbbreviationModifier(),	0.2d);
		//		instanceModifiersAndRates.put(new InstanceAcronymModifier(),		0.2d);
		//		instanceModefiersAndRates.put(new InstanceMergeModifier(),			0.2d);
		//		instanceModifiersAndRates.put(new InstanceSplitModifier(),			0.2d);


		// class modifiers
		//		classModefiersAndRates.put(new ClassIdentityModifier(), 1d);
		//		classModefiersAndRates.put(new ClassSplitModifier(),  		0.2d);
		//		classModefiersAndRates.put(new ClassDeleteModifier(),		0.2d);
		//		classModefiersAndRates.put(new ClassMergeModifier(),  		0.2d);
		classModifiersAndRates.put(new ClassRenameModifier(), 		0.2d);
		//		classModefiersAndRates.put(new ClassTypeDeleteModifier(), 	0.2d);
		Model modifiedReferenceDataset = createTestDataset(sampleKBModel, instanceModifiersAndRates, classModifiersAndRates, maxNrOfClasses, maxNrOfInstancesPerClass);

		KnowledgeBase source = new LocalKnowledgeBase(modifiedReferenceDataset, sampleKB.getNamespace());

		ExpressiveSchemaMappingGenerator generator = new ExpressiveSchemaMappingGenerator(source, target);
		generator.setTargetDomainNameSpace(dbpediaNamespace);

		//		return generator.run(modifiedDbpediaClasses, Sets.newHashSet(new NamedClass("http://dbpedia.org/ontology/Person")));
		return generator.run(modifiedDbpediaClasses, useRemoteKB);

	}

	
	public void printResults(Multimap<Integer, Map<String, Object>> result, String outputFile) throws FileNotFoundException {
		printResults(result, outputFile, false);
	}

	@SuppressWarnings("unchecked")
	public void printResults(Multimap<Integer, Map<String, Object>> result, String outputFile, boolean printShortResults) throws FileNotFoundException {
		System.setOut(new PrintStream(new FileOutputStream(outputFile )));

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

		//short version result
		for(Integer iterationNr=1 ; iterationNr<result.size() ; iterationNr++){
			double coverage = (Double) result.get(iterationNr).iterator().next().get("sourceClass2RealFMeasure");

			//compute avgRealFMeasure
			double avgRealFMeasure = 0d;
			Map<NamedClass, Double> mapF = (Map<NamedClass, Double>) result.get(iterationNr).iterator().next().get("sourceClass2RealFMeasure");
			for(NamedClass nC: mapF.keySet()){
				avgRealFMeasure += mapF.get(nC);
			}
			avgRealFMeasure /= (double) mapF.size();

			//compute avgPseudoFMeasure
			double avgPseudoFMeasure = 0d;
			Map<NamedClass, Double> mapPF = (Map<NamedClass, Double>) result.get(iterationNr).iterator().next().get("sourceClass2PseudoFMeasure");
			for(NamedClass nC: mapPF.keySet()){
				avgRealFMeasure += mapPF.get(nC);
			}
			avgRealFMeasure /= (double) mapPF.size();

			System.out.println("IterationNr\tCoverage\tavgRealFMeasure\tavgPseudoFMeasure");
			System.out.println(iterationNr + "\t" + coverage + "\t" + avgRealFMeasure + "\t" + avgPseudoFMeasure);

		}
		
		if(printShortResults){
			return;
		}
			
		//long version result
		for(Integer iterationNr=1 ; iterationNr<result.size() ; iterationNr++){
			System.out.println("\n****************** " + iterationNr + ". ITERATION ******************");
			Collection<Map<String, Object>> iterationResults = result.get(iterationNr);
			for(Map<String, Object> resultEntry : iterationResults){

				for(String key:resultEntry.keySet()){

					if(key.equals("coverage")){
						System.out.println("\nCOVERAGE: " + resultEntry.get(key));
					}

					if(key.equals("sourceClass2RealFMeasure")){
						System.out.println("\nSourceClass\tRealFMeasure:");
						Map<NamedClass, Double> map = (Map<NamedClass, Double>) resultEntry.get(key);
						for(NamedClass nC: map.keySet()){
							System.out.println(nC + "\t" + map.get(nC));
						}
					}

					if(key.equals("sourceClass2PseudoFMeasure")){
						System.out.println("\nSourceClass\tPseudoFMeasure:");
						Map<NamedClass, Double> map = (Map<NamedClass, Double>) resultEntry.get(key);
						for(NamedClass nC: map.keySet()){
							System.out.println(nC + "\t" + map.get(nC));
						}
					}

					if(key.equals("Top10Mapping")){
						System.out.println("\nTOP 10 MAPPINGS:");

						Map<NamedClass, List<? extends EvaluatedDescription>> map = (Map<NamedClass, List<? extends EvaluatedDescription>>) resultEntry.get(key);
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

					if(key.equals("sourceClass2PosExamples")){
						System.out.println("\nPOSITIVE EXAMPLES:");
						Multimap<NamedClass, String> map = (Multimap<NamedClass, String>) resultEntry.get(key);
						for(NamedClass nC: map.keySet()){
							System.out.println("\n"+ nC);
							Collection<String> mapList = map.get(nC);
							int i=1;
							for (String str : mapList) {
								System.out.println("\t" + i++ + ". " + str);
							}
						}
					}

					if(key.equals("sourceClass2NegativeExample")){
						System.out.println("\nNEGATIVE EXAMPLES:");
						Map<NamedClass, SortedSet<Individual>> map = (Map<NamedClass, SortedSet<Individual>>) resultEntry.get(key);
						for(NamedClass nC: map.keySet()){
							System.out.println("\n"+ nC);
							SortedSet<Individual> sortedSet = (SortedSet<Individual>) map.get(nC);
							int i=1;
							for (Individual ind : sortedSet) {
								System.out.println("\t" + i++ + ". " + ind);
							}
						}
					}
				}
			}
		}
	}


	public static void main(String[] args) throws Exception {

	}

}

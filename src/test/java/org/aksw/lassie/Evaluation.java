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
import org.aksw.lassie.bmGenerator.InstanceMisspellingModifier;
import org.aksw.lassie.bmGenerator.Modifier;
import org.aksw.lassie.core.LASSIEController;
import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.kb.KnowledgebaseSampleGenerator;
import org.aksw.lassie.kb.LocalKnowledgeBase;
import org.aksw.lassie.result.LassieResultRecorder;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGenerator;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGeneratorImpl;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class Evaluation {

	private static final Logger logger = Logger.getLogger(Evaluation.class);

	private SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
//	private SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpediaLiveAKSW();
	private SPARQLReasoner reasoner = new SPARQLReasoner() ;
	private ConciseBoundedDescriptionGenerator cbdGenerator = new ConciseBoundedDescriptionGeneratorImpl(endpoint, "cache");
	private String ontologyURL = "http://downloads.dbpedia.org/3.8/dbpedia_3.8.owl.bz2";

	private String dbpediaNamespace = "http://dbpedia.org/ontology/";
	private OWLOntology dbpediaOntology;

	private Set<OWLClass> modifiedDbpediaClasses = new TreeSet<>();
	private Set<OWLClass> classesToLearn 		 = new TreeSet<>();

	private static Map<Modifier, Double> classModifiersAndRates    = new HashMap<>();
	private static Map<Modifier, Double> instanceModifiersAndRates = new HashMap<>();

	private int maxNrOfClasses = -1; //-1 for all classes
	private int maxNrOfInstancesPerClass = 20;

	private int maxCBDDepth = 0;//0 means only the directly asserted triples

	private String referenceModelFile = "dbpedia-sample" + ((maxNrOfClasses > 0) ? ("_" + maxNrOfClasses + "_" + maxNrOfInstancesPerClass) : "") + ".ttl";
	protected static int maxNrOfIterations;
	
	OWLDataFactory owlDataFactory = new OWLDataFactoryImpl();
	
//	public OWLClass subTreeRootClass = owlDataFactory.getOWLClass(IRI.create("http://dbpedia.org/ontology/ChemicalSubstance"));
//	public OWLClass subTreeRootClass = owlDataFactory.getOWLClass(IRI.create("http://dbpedia.org/ontology/Food"));
//	public OWLClass subTreeRootClass = owlDataFactory.getOWLClass(IRI.create("http://dbpedia.org/ontology/Agent"));
	public OWLClass subTreeRootClass =  owlDataFactory.getOWLClass(IRI.create("http://dbpedia.org/ontology/Event"));
	//constructors
	public Evaluation(){
		super();
		SparqlEndpointKS sparqlEndpointKS = new SparqlEndpointKS(endpoint);
		try {
            sparqlEndpointKS.init();
        } catch (ComponentInitException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		reasoner.setSources(sparqlEndpointKS);
		try {
            reasoner.init();
        } catch (ComponentInitException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}

	public Evaluation(int maxNrOfClasses,int maxNrOfInstancesPerClass, 
			Map<Modifier, Double> classModifiers2Rates, Map<Modifier, Double> instanceModifiers2Rates) {
		this();
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
				classesToLearn.add(cls);
			}
			if(maxNrOfClasses > 0){
				List<OWLClass> tmp = new ArrayList<OWLClass>(classesToLearn);
				Collections.shuffle(tmp, new Random(123));
				classesToLearn = new TreeSet<OWLClass>(tmp.subList(0, maxNrOfClasses));
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
			for (OWLClass cls : classesToLearn) {
				logger.info("Generating sample for " + cls + "...");
				SortedSet<OWLIndividual> individuals = reasoner.getIndividuals(cls, maxNrOfInstancesPerClass);
				for (OWLIndividual individual : individuals) {
					Model cbd = cbdGenerator.getConciseBoundedDescription(individual.toStringID(), maxCBDDepth);
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
		BenchmarkGenerator benchmarker = new BenchmarkGenerator(classesWithAtLeastNInstancesModel);
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
		modifiedDbpediaClasses = benchmarker.getModifiedOWLClasses();
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
		List<OWLClass> classesWithEnoughInstances = new ArrayList<OWLClass>();
		while ( selectResult.hasNext())	{
			QuerySolution soln = selectResult.nextSolution() ;
			RDFNode cls = soln.get("type");
			classesWithEnoughInstances.add(owlDataFactory.getOWLClass(IRI.create(cls.toString())));

			//shuffle and select first n classes
			Collections.shuffle(classesWithEnoughInstances, new Random(123));
			classesWithEnoughInstances = classesWithEnoughInstances.subList(0, Math.min(nrOfclasses, classesWithEnoughInstances.size()));

		}
		classesToLearn = Sets.newHashSet(classesWithEnoughInstances);

		//add instances for each class
		for(OWLClass cls : classesWithEnoughInstances){
			Model m = ModelFactory.createDefaultModel();
			sparqlQueryString = "CONSTRUCT {?s ?p ?o} WHERE {?s a <" + cls.toStringID() + ">. ?s ?p ?o}";
			QueryFactory.create(sparqlQueryString);
			qexec = QueryExecutionFactory.create(sparqlQueryString, referenceDataset);
			m = qexec.execConstruct();
			resultModel.add(m);
		}

		return resultModel;
	}


	public LassieResultRecorder run(Set<OWLClass> testClasses, boolean useRemoteKB) throws ComponentInitException{
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
//		//TODO TEST
//		try {
//			modifiedReferenceDataset.write(new FileWriter("modifiedReferenceDataset.ttl"), "TTL");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		System.exit(0);
		KnowledgeBase sourceKB = new LocalKnowledgeBase(modifiedReferenceDataset, sampleKB.getNamespace());

		LASSIEController generator = new LASSIEController(sourceKB, targetKB, endpoint, maxNrOfIterations, testClasses);
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




	public LassieResultRecorder runIntensionalEvaluation(boolean useRemoteKB) throws ComponentInitException{
		//create a sample of the knowledge base
		LocalKnowledgeBase sampleKB = KnowledgebaseSampleGenerator.createKnowledgebaseSample(endpoint, dbpediaNamespace, maxNrOfClasses, maxNrOfInstancesPerClass, new HashSet<OWLClass>());

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

		LASSIEController generator = new LASSIEController(source, target, modifiedDbpediaClasses);
		generator.setTargetDomainNameSpace(dbpediaNamespace);

		//		return generator.run(modifiedDbpediaClasses, Sets.newHashSet(new OWLClass("http://dbpedia.org/ontology/Person")));
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
			Map<OWLClass, Double> mapF = (Map<OWLClass, Double>) result.get(iterationNr).iterator().next().get("sourceClass2RealFMeasure");
			for(OWLClass nC: mapF.keySet()){
				avgRealFMeasure += mapF.get(nC);
			}
			avgRealFMeasure /= (double) mapF.size();

			//compute avgPseudoFMeasure
			double avgPseudoFMeasure = 0d;
			Map<OWLClass, Double> mapPF = (Map<OWLClass, Double>) result.get(iterationNr).iterator().next().get("sourceClass2PseudoFMeasure");
			for(OWLClass nC: mapPF.keySet()){
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
						Map<OWLClass, Double> map = (Map<OWLClass, Double>) resultEntry.get(key);
						for(OWLClass nC: map.keySet()){
							System.out.println(nC + "\t" + map.get(nC));
						}
					}

					if(key.equals("sourceClass2PseudoFMeasure")){
						System.out.println("\nSourceClass\tPseudoFMeasure:");
						Map<OWLClass, Double> map = (Map<OWLClass, Double>) resultEntry.get(key);
						for(OWLClass nC: map.keySet()){
							System.out.println(nC + "\t" + map.get(nC));
						}
					}

					if(key.equals("Top10Mapping")){
						System.out.println("\nTOP 10 MAPPINGS:");

						Map<OWLClass, List<? extends EvaluatedDescription>> map = (Map<OWLClass, List<? extends EvaluatedDescription>>) resultEntry.get(key);
						for(OWLClass nC: map.keySet()){
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
						Multimap<OWLClass, String> map = (Multimap<OWLClass, String>) resultEntry.get(key);
						for(OWLClass nC: map.keySet()){
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
						Map<OWLClass, SortedSet<OWLIndividual>> map = (Map<OWLClass, SortedSet<OWLIndividual>>) resultEntry.get(key);
						for(OWLClass nC: map.keySet()){
							System.out.println("\n"+ nC);
							SortedSet<OWLIndividual> sortedSet = (SortedSet<OWLIndividual>) map.get(nC);
							int i=1;
							for (OWLIndividual ind : sortedSet) {
								System.out.println("\t" + i++ + ". " + ind);
							}
						}
					}
				}
			}
		}
	}


	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure("src/main/resources/log4j.properties");
		Evaluation e = new Evaluation();
		e.runIntensionalEvaluation(true);
		
	}

}

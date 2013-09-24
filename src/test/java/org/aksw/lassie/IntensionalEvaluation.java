package org.aksw.lassie;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.aksw.lassie.bmGenerator.BenchmarkGenerator;
import org.aksw.lassie.bmGenerator.ClassMergeModifier;
import org.aksw.lassie.bmGenerator.ClassRenameModifier;
import org.aksw.lassie.bmGenerator.ClassSplitModifier;
import org.aksw.lassie.bmGenerator.InstanceMisspellingModifier;
import org.aksw.lassie.bmGenerator.Modifier;
import org.aksw.lassie.core.ExpressiveSchemaMappingGenerator;
import org.aksw.lassie.core.NonExistingLinksException;
import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.kb.LocalKnowledgeBase;
import org.aksw.lassie.kb.RemoteKnowledgeBase;
import org.aksw.lassie.util.PrintUtils;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.log4j.Logger;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.AbstractLearningProblem;
import org.dllearner.core.AbstractReasonerComponent;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.core.owl.Description;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.NamedClass;
import org.dllearner.kb.OWLAPIOntology;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGenerator;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGeneratorImpl;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.learningproblems.PosNegLPStandard;
import org.dllearner.learningproblems.PosOnlyLP;
import org.dllearner.reasoning.FastInstanceChecker;
import org.dllearner.reasoning.SPARQLReasoner;
import org.dllearner.utilities.datastructures.SetManipulation;
import org.dllearner.utilities.examples.AutomaticNegativeExampleFinderSPARQL2;
import org.dllearner.utilities.owl.OWLAPIDescriptionConvertVisitor;
import org.dllearner.utilities.owl.OWLClassExpressionToSPARQLConverter;
import org.dllearner.utilities.owl.OWLEntityTypeAdder;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import com.sun.mail.handlers.multipart_mixed;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.selfconfig.ComplexClassifier;
import de.uni_leipzig.simba.selfconfig.MeshBasedSelfConfigurator;
import de.uni_leipzig.simba.selfconfig.SimpleClassifier;

public class IntensionalEvaluation {

	//	private SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
	private SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpediaLiveAKSW();
	private ExtractionDBCache cache = new ExtractionDBCache("cache");
	private SPARQLReasoner reasoner = new SPARQLReasoner(new SparqlEndpointKS(endpoint, cache), cache);
	private ConciseBoundedDescriptionGenerator cbdGenerator = new ConciseBoundedDescriptionGeneratorImpl(endpoint, cache);
	private String ontologyURL = "http://downloads.dbpedia.org/3.8/dbpedia_3.8.owl.bz2";

	private String dbpediaNamespace = "http://dbpedia.org/ontology/";
	private OWLOntology dbpediaOntology;

	private Set<NamedClass> ModifiedDbpediaClasses = new TreeSet<NamedClass>();
	private Set<NamedClass> dbpediaClasses 			= new TreeSet<NamedClass>();

	private static Map<Modifier, Double> classModefiersAndRates    = new HashMap<Modifier, Double>();
	private static Map<Modifier, Double> instanceModefiersAndRates = new HashMap<Modifier, Double>();

	private int maxNrOfClasses = -1;//-1 all classes
	private int maxNrOfInstancesPerClass = 20;

	private int maxCBDDepth = 0;//0 means only the directly asserted triples

	private String referenceModelFile = "dbpedia-sample" + ((maxNrOfClasses > 0) ? ("_" + maxNrOfClasses + "_" + maxNrOfInstancesPerClass) : "") + ".ttl";

	protected static final Logger logger = Logger.getLogger(ExpressiveSchemaMappingGenerator.class.getName());
	protected  Monitor mon;
	protected boolean posNegLearning = true;
	protected final boolean performCrossValidation = true;
	protected static final int maxNrOfIterations = 10;
	protected static final int coverageThreshold = 0;
	/** 
	 * The maximum number of positive examples, used for the SPARQL extraction
	 * and learning algorithm
	 */
	protected int maxNrOfPositiveExamples = 100;// 20;
	/**
	 * The maximum number of negative examples, used for the SPARQL extraction
	 * and learning algorithm
	 */
	protected int maxNrOfNegativeExamples = 100;//20;
	protected NamedClass currentClass;
	protected KnowledgeBase sourceKB;
	protected KnowledgeBase targetKB;
	protected String linkingProperty = OWL.sameAs.getURI();
	protected int maxRecursionDepth = 2;
	/**
	 * LIMES Config
	 */
	static double coverage_LIMES = 0.8;
	static double beta_LIMES = 1d;
	static String fmeasure_LIMES = "own";
	protected final int linkingMaxNrOfExamples_LIMES = 100;
	protected final int linkingMaxRecursionDepth_LIMES = 0;
	private String targetDomainNameSpace = "";
	protected List<Modifier> modifiers = new ArrayList<Modifier>();
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

	private Model createTestDataset(Model referenceDataset, Map<Modifier, Double> instanceModefiersAndRates, Map<Modifier, Double> classModefiersAndRates){
		BenchmarkGenerator benchmarker= new BenchmarkGenerator(referenceDataset);
		Modifier.setNameSpace(dbpediaNamespace);
		benchmarker.setBaseClasses(dbpediaClasses);

		Model testDataset = ModelFactory.createDefaultModel();

		if(!instanceModefiersAndRates.isEmpty()){
			testDataset = benchmarker.destroyInstances(instanceModefiersAndRates);
		}

		if(!classModefiersAndRates.isEmpty()){
			testDataset = benchmarker.destroyClasses (classModefiersAndRates);
		}
		ModifiedDbpediaClasses = benchmarker.getModifiedNamedClasses();
		return testDataset;
	}

	public Map<String, Object> runIntensionalEvaluation(){
		Model referenceDataset = createDBpediaReferenceDataset();
		// instance modifiers
		//		instanceModefiersAndRates.put(new InstanceIdentityModifier(),1d);
		instanceModefiersAndRates.put(new InstanceMisspellingModifier(),	0.2d);
		//		instanceModefiersAndRates.put(new InstanceAbbreviationModifier(),	0.2d);
		//		instanceModefiersAndRates.put(new InstanceAcronymModifier(),		0.2d);
		//		instanceModefiersAndRates.put(new InstanceMergeModifier(),			0.2d);
		//		instanceModefiersAndRates.put(new InstanceSplitModifier(),			0.2d);


		// class modifiers
		//		classModefiersAndRates.put(new ClassIdentityModifier(), 1d);
		//		classModefiersAndRates.put(new ClassSplitModifier(),  		0.2d);
		//		classModefiersAndRates.put(new ClassDeleteModifier(),		0.2d);
		//				classModefiersAndRates.put(new ClassMergeModifier(),  		0.2d);
		classModefiersAndRates.put(new ClassRenameModifier(), 		0.2d);
		//		classModefiersAndRates.put(new ClassTypeDeleteModifier(), 	0.2d);
		Model modifiedRefrenceDataset = createTestDataset(referenceDataset, instanceModefiersAndRates, classModefiersAndRates);
		try {
			// just 4 test
			modifiedRefrenceDataset.write(new FileOutputStream(new File("test.nt")),"TTL");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// initiate LASSIE
		initLassie(new LocalKnowledgeBase(modifiedRefrenceDataset), new RemoteKnowledgeBase(endpoint, cache, dbpediaNamespace));

		Map<String, Object> result = runLassie(ModifiedDbpediaClasses, dbpediaClasses);
		return result;
	}


	/*******************************************************************************
	 * 			LASSE Algorithm
	 *******************************************************************************/
	public Map<String, Object> runLassie(Set<NamedClass> sourceClasses, Set<NamedClass> targetClasses) {

		//		for(Entry<Modifier, Double> clsMod2Rat : classModefiersAndRates.entrySet()){
		//			System.out.println("Modifer Name: " + clsMod2Rat.getKey());
		//			System.out.println("source class: " + ModifiedDbpediaClasses.iterator().next().getURI());
		//			System.out.println("Optimal solution: " + clsMod2Rat.getKey().getOptimalSolution(ModifiedDbpediaClasses.iterator().next()));
		//		}
		//		System.exit(1);

		int pos = -2;
		Map<String, Object> result = new HashMap<String, Object>();
		//		Map<Modifier, Integer> modifier2pos = new HashMap<Modifier, Integer>();
		//		Map<Modifier, Description> modifier2optimalSolution = new HashMap<Modifier, Description>();

		Multimap<Modifier, Map< Map<NamedClass, Description>, Integer>> mod2rankedOptimalMapping = HashMultimap.create();

		//initially, the class expressions E_i in the target KB are the named classes D_i
		Collection<Description> targetClassExpressions = new TreeSet<Description>();
		targetClassExpressions.addAll(targetClasses);

		//perform the iterative schema matching
		Map<NamedClass, List<? extends EvaluatedDescription>> mappingTop10 = new HashMap<NamedClass, List<? extends EvaluatedDescription>>();
		//		do {
		//compute a set of links between each pair of class expressions (C_i, E_j), thus finally we get
		//a map from C_i to a set of instances in the target KB
		Multimap<NamedClass, String> links = performUnsupervisedLinking(sourceClasses, targetClassExpressions);
		result.put("posExamples", links);
		//for each source class C_i, compute a mapping to a class expression in the target KB based on the links
		for (NamedClass sourceClass : sourceClasses) {

			logger.info("+++++++++++++++++++++++++++++++++" + sourceClass + "+++++++++++++++++++++");
			currentClass = sourceClass;
			try {
				SortedSet<Individual> targetInstances = SetManipulation.stringToInd(links.get(sourceClass));
				List<? extends EvaluatedDescription> mappingList = computeMappings(targetInstances);
				mappingTop10.put(sourceClass, mappingList);

				for ( Entry<Modifier, Double> e : classModefiersAndRates.entrySet()) {
					//ModifiedDbpediaClasses.iterator().next().getURI());
					//clsMod2Rat.getKey().getOptimalSolution(ModifiedDbpediaClasses.iterator().next())
					Modifier clsModifier = e.getKey();
					System.out.println("Input source class:" + sourceClass);
					Description optimalSolution = clsModifier.getOptimalSolution(sourceClass);
					System.out.println("Output optimal solution" + optimalSolution);

					Map<NamedClass, Description> optimalMapping = new HashMap<NamedClass, Description>();
					optimalMapping.put(sourceClass, optimalSolution);
					System.out.println("sourceClass: "+sourceClass+"\noptimalSolution: "+optimalSolution);
					System.out.println("optimalMapping: "+ optimalMapping);

					Map< Map<NamedClass, Description>, Integer> optimalMapping2rank = new HashMap<Map<NamedClass,Description>, Integer>();
					pos = getIntensionalEvaluationRank(mappingList, clsModifier, sourceClass);
					optimalMapping2rank.put(optimalMapping, pos);
					System.out.println("optimalMapping2rank: "+ optimalMapping2rank);

					mod2rankedOptimalMapping.put(clsModifier, optimalMapping2rank);
					System.out.println("mod2rankedOptimalMapping: " + mod2rankedOptimalMapping);
				}

			} catch (NonExistingLinksException e) {
				logger.warn(e.getMessage() + "Skipped learning.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		result.put("mappingTop10", mappingTop10);
		result.put("mod2rankedOptimalMapping", mod2rankedOptimalMapping);
		System.out.println("RESULT:\n"+ result);
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
		for(Modifier m: instanceModefiersAndRates.keySet()){
			System.out.println(j++ + ". " + m.getClass().getSimpleName() + "\t" + instanceModefiersAndRates.get(m)*100 + "%");
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
			if(key.equals("mod2rankedOptimalMapping")){
				System.out.println("\nModifiers 2 ranked Optimal Mapping:");

				Multimap<Modifier, Map< Map<NamedClass, Description>, Integer>> map = (Multimap<Modifier, Map< Map<NamedClass, Description>, Integer>>) result.get(key);
				for( Modifier modifier : map.keySet()){
					System.out.println("\nModifier Name: " + modifier.getName());
					Collection<Map< Map<NamedClass, Description>, Integer>> optimalMapping2rank = map.get(modifier);

					for(Map< Map<NamedClass, Description>, Integer> class2description2rank : optimalMapping2rank){
//						System.out.print("\tclass2description2rank: " + class2description2rank);
						
						for(Map<NamedClass, Description> class2description : class2description2rank.keySet()){
							System.out.print("\tOptimal mapping: " + class2description );
							System.out.println("\t\tRank: " + class2description2rank.get(class2description));
						}
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

			if(key.equals("modifier2pos")){
				System.out.println("\nModifier2pos:");
				Map<Modifier, Integer> map = (Map<Modifier, Integer>) result.get(key);;
				for(Modifier m: map.keySet()){
					System.out.println("\nModifier: " + maxCBDDepth + " Pos: "+ map.get(m));
				}
			}
			
			if(key.equals("modifier2optimalSolution")){
				Map<Modifier, Description> map = new HashMap<Modifier, Description>();
				for(Modifier m: map.keySet()){
					System.out.println("\nModifier: " + m.getClass().getCanonicalName() + " Pos: "+ map.get(m).toString());
					System.out.println("\nModifier(getName): " + m.getName());
				}
			}
		}
	}



	/**
	 * @param modifiers the modifiers to set
	 */
	public void setModifiers(List<Modifier> modifiers) {
		this.modifiers = modifiers;
	}

	/**
	 * @param domainOntolog the domainOntolog to set
	 */
	public void setTargetDomainNameSpace(String uri) {
		this.targetDomainNameSpace = uri;
	}

	public IntensionalEvaluation() {
	}

	private void initLassie(KnowledgeBase source, KnowledgeBase target) {
		initLassie(source, target, OWL.sameAs.getURI());
	}

	private void initLassie(KnowledgeBase source, KnowledgeBase target, String linkingProperty) {
		this.sourceKB = source;
		this.targetKB = target;
		this.linkingProperty = linkingProperty;

		mon = MonitorFactory.getTimeMonitor("time");

		source.getReasoner().prepareSubsumptionHierarchy();
		target.getReasoner().prepareSubsumptionHierarchy();

		setTargetDomainNameSpace(dbpediaNamespace);
	}

	public int getIntensionalEvaluationRank(List<? extends EvaluatedDescription> descriptions, Modifier modifier, NamedClass cls){
		Description targetDescription = modifier.getOptimalSolution(cls);
//		int pos = descriptions.indexOf(targetDescription);
		int pos = 0 ;
		for(EvaluatedDescription ed : descriptions){
			pos++;
			if(targetDescription.equals(ed.getDescription())){
				return pos;
			}
		}
		return -1;
	}


	/**
	 * Run LIMES to generate owl:sameAs links
	 *
	 * @param sourceClasses
	 * @param targetClasses
	 */
	public Multimap<NamedClass, String> performUnsupervisedLinking(Set<NamedClass> sourceClasses, Collection<Description> targetClasses) {
		logger.info("Computing links...");
		//compute the Concise Bounded Description(CBD) for each instance
		//in each source class C_i, thus creating a model for each class
		Map<NamedClass, Model> sourceClassToModel = new HashMap<NamedClass, Model>();
		for (NamedClass sourceClass : sourceClasses) {
			//get all instances of C_i
			SortedSet<Individual> sourceInstances = getSourceInstances(sourceClass);
			sourceInstances = SetManipulation.stableShrinkInd(sourceInstances, linkingMaxNrOfExamples_LIMES);

			//get the fragment describing the instances of C_i
			logger.info("Computing fragment...");
			Model sourceFragment = getFragment(sourceInstances, sourceKB, linkingMaxRecursionDepth_LIMES);
			removeNonLiteralStatements(sourceFragment);
			logger.info("...got " + sourceFragment.size() + " triples.");
			sourceClassToModel.put(sourceClass, sourceFragment);
		}

		//compute the Concise Bounded Description(CBD) for each instance
		//in each each target class expression D_i, thus creating a model for each class expression
		Map<Description, Model> targetClassExpressionToModel = new HashMap<Description, Model>();
		for (Description targetClass : targetClasses) {
			// get all instances of D_i
			SortedSet<Individual> targetInstances = getTargetInstances(targetClass);
			//			targetInstances = SetManipulation.stableShrinkInd(targetInstances, linkingMaxNrOfExamples_LIMES);
			//			ArrayList<Individual> l = new ArrayList<Individual>(targetInstances);
			//			Collections.reverse(l);
			//			targetInstances = new TreeSet<Individual>(l.subList(0, Math.min(100, targetInstances.size())));

			// get the fragment describing the instances of D_i
			logger.info("Computing fragment...");
			Model targetFragment = getFragment(targetInstances, targetKB, linkingMaxRecursionDepth_LIMES);
			removeNonLiteralStatements(targetFragment);
			logger.info("...got " + targetFragment.size() + " triples.");
			targetClassExpressionToModel.put(targetClass, targetFragment);
		}

		Multimap<NamedClass, String> map = HashMultimap.create();

		//for each C_i
		for (Entry<NamedClass, Model> entry : sourceClassToModel.entrySet()) {
			NamedClass sourceClass = entry.getKey();
			Model sourceClassModel = entry.getValue();

			//for each D_i
			for (Entry<Description, Model> entry2 : targetClassExpressionToModel.entrySet()) {
				Description targetClassExpression = entry2.getKey();
				Model targetClassExpressionModel = entry2.getValue();

				logger.info("******* COMPUTING links between " + sourceClass + " and " + targetClassExpression + "******");
				Cache cache = getCache(sourceClassModel);
				Cache cache2 = getCache(targetClassExpressionModel);

				Mapping result = getDeterministicUnsupervisedMappings(cache, cache2);

				for (Entry<String, HashMap<String, Double>> mappingEntry : result.map.entrySet()) {
					String key = mappingEntry.getKey();
					HashMap<String, Double> value = mappingEntry.getValue();
					map.put(sourceClass, value.keySet().iterator().next());
				}
			}
		}
		return map;
	}

	private void removeNonLiteralStatements(Model m){
		StmtIterator iterator = m.listStatements();
		List<Statement> statements2Remove = new ArrayList<Statement>();
		while(iterator.hasNext()){
			Statement st = iterator.next();
			if(!st.getObject().isLiteral()){
				statements2Remove.add(st);
			}
		}
		m.remove(statements2Remove);
	}

	public Set<String> getAllProperties(Cache c) {
		//    	logger.info("Get all properties...");
		if (c.size() > 0) {
			HashSet<String> props = new HashSet<String>();
			for (Instance i : c.getAllInstances()) {
				props.addAll(i.getAllProperties());
			}
			return props;
		} else {
			return new HashSet<String>();
		}
	}

	/**
	 * Computes initial mappings
	 *
	 */
	public Mapping getDeterministicUnsupervisedMappings(Cache source, Cache target) {
		logger.info("Source size = "+source.getAllUris().size());
		logger.info("Target size = "+target.getAllUris().size());

		MeshBasedSelfConfigurator bsc = new MeshBasedSelfConfigurator(source, target, coverage_LIMES, beta_LIMES);
		bsc.setMeasure(fmeasure_LIMES);
		List<SimpleClassifier> cp = bsc.getBestInitialClassifiers();
		//		Set<String> measure =  new HashSet<String>();
		//		measure.add("trigrams");
		//		List<SimpleClassifier> cp = bsc.getBestInitialClassifiers(measure);
		if(cp.size() == 0) 
		{
			logger.warn("No property mapping found");
			return new Mapping();
		}
		ComplexClassifier cc = bsc.getZoomedHillTop(5, 5, cp);
		Mapping map = Mapping.getBestOneToOneMappings(cc.mapping);
		logger.info("Mapping size is " + map.getNumberofMappings());
		logger.info("Pseudo F-measure is " + cc.fMeasure);
		return map;
	}

	public Cache getCache(Model m) {
		Cache c = new MemoryCache();
		for (Statement s : m.listStatements().toList()) {
			if (s.getObject().isResource()) {
				c.addTriple(s.getSubject().getURI(), s.getPredicate().getURI(), s.getObject().asResource().getURI());
			} else {
				c.addTriple(s.getSubject().getURI(), s.getPredicate().getURI(), s.getObject().asLiteral().getLexicalForm());
			}
		}
		return c;
	}

	public EvaluatedDescription computeMapping(SortedSet<Individual> positiveExamples) throws NonExistingLinksException {
		return computeMappings(positiveExamples).get(0);
	}

	public List<? extends EvaluatedDescription> computeMappings(Description targetClassExpression) throws NonExistingLinksException {
		SortedSet<Individual> targetInstances = getTargetInstances(targetClassExpression);
		return computeMappings(targetInstances);
	}

	public List<? extends EvaluatedDescription> computeMappings(SortedSet<Individual> positiveExamples) throws NonExistingLinksException {
		//if there are no links to the target KB, then we can skip learning
		if (positiveExamples.isEmpty()) {
			throw new NonExistingLinksException();
		} else {
			//compute a mapping
			//get a sample of the positive examples
			SortedSet<Individual> positiveExamplesSample = SetManipulation.stableShrinkInd(positiveExamples, maxNrOfPositiveExamples);

			//starting from the positive examples, we first extract the fragment for them
			logger.info("Extracting fragment for positive examples...");
			mon.start();
			Model positiveFragment = getFragment(positiveExamplesSample, targetKB);
			mon.stop();
			logger.info("...got " + positiveFragment.size() + " triples in " + mon.getLastValue() + "ms.");
			//			for (Individual ind : positiveExamplesSample) {
			//				System.out.println(ResultSetFormatter.asText(
			//						com.hp.hpl.jena.query.QueryExecutionFactory.create("SELECT * WHERE {<" + ind.getName() + "> a ?o.}", positiveFragment).execSelect()));
			//			}

			//compute the negative examples
			logger.info("Computing negative examples...");
			MonitorFactory.getTimeMonitor("negative examples").start();
			AutomaticNegativeExampleFinderSPARQL2 negativeExampleFinder = new AutomaticNegativeExampleFinderSPARQL2(targetKB.getReasoner(), targetKB.getNamespace());
			SortedSet<Individual> negativeExamples = negativeExampleFinder.getNegativeExamples(positiveExamples, maxNrOfNegativeExamples);
			negativeExamples.removeAll(positiveExamples);
			MonitorFactory.getTimeMonitor("negative examples").stop();
			logger.info("Found " + negativeExamples.size() + " negative examples in " + MonitorFactory.getTimeMonitor("negative examples").getTotal() + "ms.");

			//get a sample of the negative examples
			SortedSet<Individual> negativeExamplesSample = SetManipulation.stableShrinkInd(negativeExamples, maxNrOfNegativeExamples);
			//create fragment for negative examples
			logger.info("Extracting fragment for negative examples...");
			mon.start();
			Model negativeFragment = getFragment(negativeExamplesSample, targetKB);
			mon.stop();
			logger.info("...got " + negativeFragment.size() + " triples in " + mon.getLastValue() + "ms.");

			logger.info("Learning input:");
			logger.info("Positive examples: " + positiveExamplesSample.size() + " with " + positiveFragment.size() + " triples, e.g. \n" + print(positiveExamplesSample, 3));
			logger.info("Negative examples: " + negativeExamplesSample.size() + " with " + negativeFragment.size() + " triples, e.g. \n" + print(negativeExamplesSample, 3));

			//create fragment consisting of both
			OntModel fullFragment = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
			fullFragment.add(positiveFragment);
			fullFragment.add(negativeFragment);
			filter(fullFragment, targetKB.getNamespace());

			//learn the class expressions
			return learnClassExpressions(fullFragment, positiveExamplesSample, negativeExamplesSample);
		}
	}

	private List<? extends EvaluatedDescription> learnClassExpressions(Model model, SortedSet<Individual> positiveExamples, SortedSet<Individual> negativeExamples) {
		try {
			cleanUpModel(model);
			OWLEntityTypeAdder.addEntityTypes(model);
			KnowledgeSource ks = convert(model);

			//initialize the reasoner
			logger.info("Initializing reasoner...");
			AbstractReasonerComponent rc = new FastInstanceChecker(ks);
			rc.init();
			rc.setSubsumptionHierarchy(targetKB.getReasoner().getClassHierarchy());
			logger.info("Done.");

			//initialize the learning problem
			logger.info("Initializing learning problem...");
			AbstractLearningProblem lp;
			if (!negativeExamples.isEmpty()) {
				lp = new PosNegLPStandard(rc, positiveExamples, negativeExamples);
			} else {
				lp = new PosOnlyLP(rc, positiveExamples);
			}
			lp.init();
			logger.info("Done.");

			//initialize the learning algorithm
			logger.info("Initializing learning algorithm...");
			CELOE la = new CELOE(lp, rc);
			la.setMaxExecutionTimeInSeconds(10);
			la.setNoisePercentage(25);
			la.init();
			logger.info("Done.");

			//apply the learning algorithm
			logger.info("Running learning algorithm...");
			la.start();
			logger.info(la.getCurrentlyBestEvaluatedDescription());

			return la.getCurrentlyBestEvaluatedDescriptions(10);
		} catch (ComponentInitException e) {
			logger.error(e);
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			OWLOntology ontology = ((OWLAPIOntology)convert(model)).createOWLOntology(man);
			try {
				man.saveOntology(ontology, new RDFXMLOntologyFormat(), new FileOutputStream(new File("inc.owl")));
			} catch (OWLOntologyStorageException e1) {
				e1.printStackTrace();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			System.exit(0);

		}
		return null;
	}



	private String print(Collection<Individual> individuals, int n){
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (Individual individual : individuals) {
			sb.append(individual.getName() + ",");
		}
		sb.append("...");
		return sb.toString();
	}

	/**
	 * Return all instances of the given class in the source KB.
	 *
	 * @param cls
	 * @return
	 */
	private SortedSet<Individual> getSourceInstances(NamedClass cls) {
		logger.info("Retrieving instances of class " + cls + "...");
		mon.start();
		SortedSet<Individual> instances = new TreeSet<Individual>();
		String query = String.format("SELECT DISTINCT ?s WHERE {?s a <%s>}", cls.getName());
		ResultSet rs = sourceKB.executeSelect(query);
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			instances.add(new Individual(qs.getResource("s").getURI()));
		}
		mon.stop();
		logger.info("...found " + instances.size() + " instances in " + mon.getLastValue() + "ms.");
		return instances;
	}

	/**
	 * Return all instances which are (assumed to be) contained in the target
	 * KB. Here we should apply a namespace filter on the URIs such that we get
	 * only instances which are really contained in the target KB.
	 *
	 * @param cls
	 * @return
	 */
	private SortedSet<Individual> getTargetInstances(Description desc) {
		return getInstances(desc, targetKB);
	}

	private SortedSet<Individual> getInstances(Description desc, KnowledgeBase kb) {
		logger.info("Retrieving instances of class expression " + desc + "...");
		mon.start();
		SortedSet<Individual> instances = new TreeSet<Individual>();
		OWLClassExpressionToSPARQLConverter converter = new OWLClassExpressionToSPARQLConverter();
		OWLClassExpression classExpression = OWLAPIDescriptionConvertVisitor.getOWLClassExpression(desc);
		Query query = converter.asQuery("?x", classExpression);
		ResultSet rs = kb.executeSelect(query.toString());
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			instances.add(new Individual(qs.getResource("x").getURI()));
		}
		mon.stop();
		logger.info("...found " + instances.size() + " instances in " + mon.getLastValue() + "ms.");
		return instances;
	}

	/**
	 * Return all instances which are (assumed to be) contained in the target
	 * KB. Here we should apply a namespace filter on the URIs such that we get
	 * only instances which are really contained in the target KB.
	 *
	 * @param cls
	 * @return
	 */
	private SortedSet<Individual> getTargetInstances(NamedClass cls) {
		logger.info("Retrieving instances to which instances of class " + cls + " are linked to via property " + linkingProperty + "...");
		mon.start();
		SortedSet<Individual> instances = new TreeSet<Individual>();
		String query = String.format("SELECT DISTINCT ?o WHERE {?s a <%s>. ?s <%s> ?o. FILTER(REGEX(?o,'^%s'))}", cls.getName(), linkingProperty, targetKB.getNamespace());
		ResultSet rs = sourceKB.executeSelect(query);
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			instances.add(new Individual(qs.getResource("o").getURI()));
		}
		mon.stop();
		logger.info("...found " + instances.size() + " instances in " + mon.getLastValue() + "ms.");
		return instances;
	}

	private KnowledgeSource convert(Model model) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			model.write(baos, "TURTLE", null);
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			OWLOntology ontology = man.loadOntologyFromOntologyDocument(new ByteArrayInputStream(baos.toByteArray()));
			return new OWLAPIOntology(ontology);
		} catch (Exception e) {
			e.printStackTrace();
			try {model.write(new FileOutputStream("error.ttl"), "TURTLE", null);
			model.write(new FileOutputStream("errors/" + PrintUtils.prettyPrint(currentClass) + "_conversion_error.ttl"), "TURTLE", null);
			} catch (FileNotFoundException e1) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Computes a fragment containing hopefully useful information about the
	 * resources.
	 *
	 * @param ind
	 */
	private Model getFragment(SortedSet<Individual> individuals, KnowledgeBase kb) {
		return getFragment(individuals, kb, maxRecursionDepth);
	}

	/**
	 * Computes a fragment containing hopefully useful information about the
	 * resources.
	 *
	 * @param ind
	 */
	private Model getFragment(SortedSet<Individual> individuals, KnowledgeBase kb, int recursionDepth) {
		//        OntModel fullFragment = ModelFactory.createOntologyModel();
		Model fullFragment = ModelFactory.createDefaultModel();
		int i = 1;
		Model fragment;
		for (Individual ind : individuals) {
			fragment = getFragment(ind, kb, recursionDepth);
			System.out.println(ind + ": " + fragment.size() + " triples");
			//			logger.info(i++  + "/" + individuals.size());
			fullFragment.add(fragment);
		}
		//        cleanUpModel(fullFragment);
		return fullFragment;
	}

	/**
	 * Computes a fragment containing hopefully useful information about the
	 * resource.
	 *
	 * @param ind
	 */
	private Model getFragment(Individual ind, KnowledgeBase kb) {
		return getFragment(ind, kb, maxRecursionDepth);
	}

	/**
	 * Computes a fragment containing hopefully useful information about the
	 * resource.
	 *
	 * @param ind
	 */
	private Model getFragment(Individual ind, KnowledgeBase kb, int recursionDepth) {
		logger.debug("Loading fragment for " + ind.getName());
		ConciseBoundedDescriptionGenerator cbdGen;
		if (kb.isRemote()) {
			cbdGen = new ConciseBoundedDescriptionGeneratorImpl(((RemoteKnowledgeBase) kb).getEndpoint(), ((RemoteKnowledgeBase) kb).getCache().getCacheDirectory());
		} else {
			cbdGen = new ConciseBoundedDescriptionGeneratorImpl(((LocalKnowledgeBase) kb).getModel());
		}
		Model cbd = ModelFactory.createDefaultModel();
		try{
			cbd = cbdGen.getConciseBoundedDescription(ind.getName(), 1);
		}catch (Exception e) {
			System.out.println("End Point(" + ((RemoteKnowledgeBase) kb).getEndpoint().toString() + ") Exception: "+ e);
		}
		logger.debug("Got " + cbd.size() + " triples.");
		return cbd;
	}

	private void cleanUpModel(Model model) {
		// filter out triples with String literals, as therein often occur
		// some syntax errors and they are not relevant for learning
		List<Statement> statementsToRemove = new ArrayList<Statement>();
		for (Iterator<Statement> iter = model.listStatements().toList().iterator(); iter.hasNext();) {
			Statement st = iter.next();
			RDFNode object = st.getObject();
			if (object.isLiteral()) {
				// statementsToRemove.add(st);
				Literal lit = object.asLiteral();
				if (lit.getDatatype() == null || lit.getDatatype().equals(XSD.xstring)) {
					st.changeObject("shortened", "en");
				} else if(lit.getDatatype().getURI().equals(XSD.gYear.getURI())){
					statementsToRemove.add(st);System.err.println("REMOVE "  + st);
				}
			}
			//remove statements like <x a owl:Class>
			if (st.getPredicate().equals(RDF.type)) {
				if (object.equals(RDFS.Class.asNode()) || object.equals(OWL.Class.asNode()) || object.equals(RDFS.Literal.asNode())
						|| object.equals(RDFS.Resource)) {
					statementsToRemove.add(st);
				}
			}
		}

		model.remove(statementsToRemove);
	}

	/**
	 * Filter triples which are not relevant based on the given knowledge base namespace.
	 * @param model
	 * @param namespace
	 */
	private void filter(Model model, String namespace){
		List<Statement> statementsToRemove = new ArrayList<Statement>();
		for (Iterator<Statement> iter = model.listStatements().toList().iterator(); iter.hasNext();) {
			Statement st = iter.next();
			Property predicate = st.getPredicate();
			if(predicate.equals(RDF.type)){
				if(!st.getObject().asResource().getURI().startsWith(namespace)){
					statementsToRemove.add(st);
				} else if(st.getObject().equals(OWL.FunctionalProperty.asNode())){
					statementsToRemove.add(st);
				} else if(st.getObject().isLiteral() && st.getObject().asLiteral().getDatatypeURI().equals(XSD.gYear.getURI())){
					statementsToRemove.add(st);
				}
			} else if(!predicate.equals(RDFS.subClassOf) && !predicate.equals(OWL.sameAs) && !predicate.asResource().getURI().startsWith(namespace)){
				statementsToRemove.add(st);
			}
		}
		model.remove(statementsToRemove);
	}

	private Set<NamedClass> getClasses(KnowledgeBase kb) {
		Set<NamedClass> classes = new HashSet<NamedClass>();

		//get all OWL classes
		String query = String.format("SELECT ?type WHERE {?type a <%s>.}", OWL.Class.getURI());
		ResultSet rs = kb.executeSelect(query);
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			if (qs.get("type").isURIResource()) {
				classes.add(new NamedClass(qs.get("type").asResource().getURI()));
			}
		}

		//fallback: check for ?s a ?type where ?type is not asserted to owl:Class
		if (classes.isEmpty()) {
			query = "SELECT ?type WHERE {?s a ?type.}";
			rs = kb.executeSelect(query);
			while (rs.hasNext()) {
				qs = rs.next();
				if (qs.get("type").isURIResource()) {
					classes.add(new NamedClass(qs.get("type").asResource().getURI()));
				}
			}
		}
		return classes;
	}

	public static void main(String[] args) throws Exception {
		IntensionalEvaluation intEvaluator = new IntensionalEvaluation();
		long startTime = System.currentTimeMillis();

		//		Map<String, Object> result = evaluator.run();
		Map<String, Object> result = intEvaluator.runIntensionalEvaluation();
		intEvaluator.printResults(result);

		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("totalTime: " + totalTime + " ms");
	}

}


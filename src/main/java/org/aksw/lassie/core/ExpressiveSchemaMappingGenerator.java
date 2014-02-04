package org.aksw.lassie.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.aksw.lassie.bmGenerator.Modifier;
import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.kb.LocalKnowledgeBase;
import org.aksw.lassie.kb.RemoteKnowledgeBase;
import org.aksw.lassie.result.LassieResultRecorder;
import org.aksw.lassie.util.PrintUtils;
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
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGenerator;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGeneratorImpl;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.learningproblems.PosNegLPStandard;
import org.dllearner.learningproblems.PosOnlyLP;
import org.dllearner.reasoning.FastInstanceChecker;
import org.dllearner.utilities.datastructures.SetManipulation;
import org.dllearner.utilities.examples.AutomaticNegativeExampleFinderSPARQL2;
import org.dllearner.utilities.owl.OWLAPIDescriptionConvertVisitor;
import org.dllearner.utilities.owl.OWLClassExpressionToSPARQLConverter;
import org.dllearner.utilities.owl.OWLEntityTypeAdder;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
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

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.multilinker.MappingMath;
import de.uni_leipzig.simba.selfconfig.ComplexClassifier;
import de.uni_leipzig.simba.selfconfig.MeshBasedSelfConfigurator;
import de.uni_leipzig.simba.selfconfig.ReferencePseudoMeasures;
import de.uni_leipzig.simba.selfconfig.SimpleClassifier;

public class ExpressiveSchemaMappingGenerator {

	//current status trackers
	protected static final Logger logger = Logger.getLogger(ExpressiveSchemaMappingGenerator.class.getName());
	protected Monitor mon;
	protected NamedClass currentClass;
	private int iterationNr = 1;

	//LASSIE configurations
	protected boolean posNegLearning = true;
	protected final boolean performCrossValidation = true;
	protected static int maxNrOfIterations = 10;
	protected static final int coverageThreshold = 0;
	private String targetDomainNameSpace = "";
	protected List<Modifier> modifiers = new ArrayList<Modifier>();

	//DL-Learner configurations
	/** The maximum number of positive examples, used for the SPARQL extraction and learning algorithm */
	protected int maxNrOfPositiveExamples = 100;// 20;
	/** The maximum number of negative examples, used for the SPARQL extraction and learning algorithm */
	protected int maxNrOfNegativeExamples = 100;//20;
	protected KnowledgeBase sourceKB;
	protected KnowledgeBase targetKB;
	protected int maxRecursionDepth = 2;

	// LIMES Configurations
	protected static final int numberOfDimensions = 7;
	static double coverage_LIMES = 0.8;
	static double beta_LIMES = 1d;
	static String fmeasure_LIMES = "own";
	protected final int linkingMaxNrOfExamples_LIMES = 100;
	protected final int linkingMaxRecursionDepth_LIMES = 0;
	private int numberOfLinkingIterations = 5;
	protected String linkingProperty = OWL.sameAs.getURI();
	protected Map<NamedClass, Map<Description, Mapping>> mappingResults = new HashMap<NamedClass, Map<Description, Mapping>>();

	//result recording
	LassieResultRecorder resultRecorder;
	private SparqlEndpoint endpoint;


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

	public ExpressiveSchemaMappingGenerator() {
	}

	public ExpressiveSchemaMappingGenerator(KnowledgeBase source, KnowledgeBase target, int nrOfIterations) {
		this(source, target, OWL.sameAs.getURI());
		maxNrOfIterations = nrOfIterations;
	}

	public ExpressiveSchemaMappingGenerator(KnowledgeBase source, KnowledgeBase target) {
		this(source, target, OWL.sameAs.getURI());
	}

	public ExpressiveSchemaMappingGenerator(KnowledgeBase source, KnowledgeBase target, String linkingProperty) {
		this.sourceKB = source;
		this.targetKB = target;
		this.linkingProperty = linkingProperty;

		mon = MonitorFactory.getTimeMonitor("time");

		source.getReasoner().prepareSubsumptionHierarchy();
		target.getReasoner().prepareSubsumptionHierarchy();
	}


	/**
	 * @param sourceKB2
	 * @param targetKB2
	 * @param endpoint
	 * @param maxNrOfIterations2
	 */
	public ExpressiveSchemaMappingGenerator(KnowledgeBase source, KnowledgeBase target, SparqlEndpoint endpoint,
			int maxNrOfIterations) {
		this(source, target, OWL.sameAs.getURI());
		this.endpoint = endpoint;
		this.maxNrOfIterations = maxNrOfIterations;
	}

	public LassieResultRecorder run(Set<NamedClass> sourceClasses, boolean useRemoteKB) {
		// get all classes D_i in target KB
		Set<NamedClass> targetClasses = getClasses(targetKB);
		logger.debug("targetClasses: " + targetClasses);
		return run(sourceClasses, targetClasses, useRemoteKB);
	}


	public LassieResultRecorder run(Set<NamedClass> sourceClasses, Set<NamedClass> targetClasses, boolean useRemoteKB) {

		resultRecorder = new LassieResultRecorder(maxNrOfIterations, sourceClasses);

		//initially, the class expressions E_i in the target KB are the named classes D_i
		Collection<Description> targetClassExpressions = new TreeSet<Description>();
		targetClassExpressions.addAll(targetClasses);

		//perform the iterative schema matching
		Map<NamedClass, Description> iterationResultConceptDescription = new HashMap<NamedClass, Description>();

		double totalCoverage = 0;
		do {
			long itrStartTime = System.currentTimeMillis();

			logger.info(iterationNr + ". ITERATION:");
			//compute a set of links between each pair of class expressions (C_i, E_j), thus finally we get
			//a map from C_i to a set of instances in the target KB
			Multimap<NamedClass, String> links = performUnsupervisedLinking(sourceClasses, targetClassExpressions);

			//for each source class C_i, compute a mapping to a class expression in the target KB based on the links
			for (NamedClass sourceClass : sourceClasses) {

				logger.info("+++++++++++++++++++++" + sourceClass + "+++++++++++++++++++++");
				currentClass = sourceClass;
				try {
					SortedSet<Individual> targetInstances = SetManipulation.stringToInd(links.get(sourceClass));

					resultRecorder.setPositiveExample(targetInstances, iterationNr, sourceClass);

					List<? extends EvaluatedDescription> mappingList = computeMappings(targetInstances, useRemoteKB);
					resultRecorder.setMapping(mappingList, iterationNr, sourceClass);

					iterationResultConceptDescription.put(sourceClass, mappingList.get(0).getDescription());
				} catch (NonExistingLinksException e) {
					logger.warn(e.getMessage() + " Skipped learning.");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			//set the target class expressions
			targetClassExpressions = iterationResultConceptDescription.values();
			double newTotalCoverage = computeCoverage(iterationResultConceptDescription);

			//if no better coverage then break
			//            if ((newTotalCoverage - totalCoverage) <= coverageThreshold) {
			//                break;
			//            }

			totalCoverage = newTotalCoverage;
			resultRecorder.getIterationRecord(iterationNr).setExecutionTime(System.currentTimeMillis() - itrStartTime);

		} while (iterationNr++ < maxNrOfIterations);

		return resultRecorder;
	}


	public Map<String, Object> runIntentionalEvaluation(Set<NamedClass> sourceClasses,
			Set<NamedClass> targetClasses, Map<Modifier, Double> instanceModefiersAndRates, Map<Modifier, Double> classModefiersAndRates) {
		for (Entry<Modifier, Double> clsMod2Rat : classModefiersAndRates.entrySet()) {
			System.out.println("Modifer Name: " + clsMod2Rat.getKey());
			System.out.println("Optimal solution: " + clsMod2Rat.getKey().getOptimalSolution(sourceClasses.iterator().next()));
		}
		System.exit(1);

		int pos = 0;
		Map<String, Object> result = new HashMap<String, Object>();
		Map<Modifier, Integer> modifier2pos = new HashMap<Modifier, Integer>();
		Map<Modifier, Description> modifier2optimalSolution = new HashMap<Modifier, Description>();

		//initially, the class expressions E_i in the target KB are the named classes D_i
		Collection<Description> targetClassExpressions = new TreeSet<Description>();
		targetClassExpressions.addAll(targetClasses);

		//perform the iterative schema matching
		Map<NamedClass, List<? extends EvaluatedDescription>> mappingTop10 = new HashMap<NamedClass, List<? extends EvaluatedDescription>>();
		int i = 0;
		//		do {
		//compute a set of links between each pair of class expressions (C_i, E_j), thus finally we get
		//a map from C_i to a set of instances in the target KB
		Multimap<NamedClass, String> links = performUnsupervisedLinking(sourceClasses, targetClassExpressions);
		result.put("posExamples", links);
		//for each source class C_i, compute a mapping to a class expression in the target KB based on the links
		for (NamedClass sourceClass : sourceClasses) {

			logger.info("Source class: " + sourceClass);
			currentClass = sourceClass;
			try {
				SortedSet<Individual> targetInstances = SetManipulation.stringToInd(links.get(sourceClass));
				List<? extends EvaluatedDescription> mappingList = computeMappings(targetInstances, false);
				mappingTop10.put(sourceClass, mappingList);

				for (Modifier modifier : modifiers) {
					if (modifier.isClassModifier()) {
						pos = intentionalEvaluation(mappingList, modifier, sourceClass);
						modifier2pos.put(modifier, pos);
						modifier2optimalSolution.put(modifier, modifier.getOptimalSolution(sourceClass));
					}
				}

			} catch (NonExistingLinksException e) {
				logger.warn(e.getMessage() + "Skipped learning.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//			if(pos<0)result
		//				break;
		//		} while (i++ <= maxNrOfIterations);
		result.put("Modifier2pos", modifier2pos);
		result.put("mappingTop10", mappingTop10);
		result.put("modifier2optimalSolution", modifier2optimalSolution);
		return result;
	}

	public int intentionalEvaluation(List<? extends EvaluatedDescription> descriptions, Modifier modifier, NamedClass cls) {
		Description targetDescription = modifier.getOptimalSolution(cls);
		int pos = descriptions.indexOf(targetDescription);
		return pos;
	}

	/**
	 * @param j
	 * @param sourceClass
	 * @param targetInstances
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @author sherif
	 */
	public void serializeCurrentObjects(int j, NamedClass sourceClass,
			SortedSet<Individual> targetInstances) throws IOException,
			FileNotFoundException {
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("sourceClass" + j + ".ser"));
		out.writeObject(sourceClass);

		out = new ObjectOutputStream(new FileOutputStream("targetInstances" + j + ".ser"));
		out.writeObject(targetInstances);
		j++;
		//		System.exit(1);
	}

	double computeCoverage(Map<NamedClass, Description> mapping) {

		double totalCoverage = 0;

		for (Entry<NamedClass, Description> entry : mapping.entrySet()) {
			NamedClass sourceClass = entry.getKey();
			Description targetDescription = entry.getValue();

			SortedSet<Individual> sourceInstances = getInstances(sourceClass, sourceKB);
			SortedSet<Individual> targetInstances = getInstances(targetDescription, targetKB);
			double coverage = computeDiceSimilarity(sourceInstances, targetInstances);

			resultRecorder.setCoverage(coverage, iterationNr, sourceClass);

			totalCoverage += coverage;
		}

		totalCoverage /= mapping.size();
		return totalCoverage;
	}

	double computeDiceSimilarity(Set<Individual> sourceInstances, Set<Individual> targetInstances) {
		SetView<Individual> intersection = Sets.intersection(sourceInstances, targetInstances);
		SetView<Individual> union = Sets.union(sourceInstances, targetInstances);

		/* Other approaches to compute coverage:
		 * double jaccard = (double) intersection.size() / (double) targetInstances.size();
		 * double overlap = (double) intersection.size() / (double) Math.min(sourceInstances.size(), targetInstances.size());
		 * double alpha = 1, beta = 0.2;
		 * SetView<Individual> sourceDifTarget = Sets.difference(sourceInstances, targetInstances);
		 * SetView<Individual> targetDifsource = Sets.difference(targetInstances, sourceInstances);
		 * double tversky = intersection.size() / (intersection.size() + alpha * sourceDifTarget.size() + beta * targetDifsource.size());*/

		double dice = 2 * ((double) intersection.size()) / (double)(sourceInstances.size() + targetInstances.size());
		return dice;
	}

	/**
	 * Run LIMES to generate owl:sameAs links
	 *
	 * @param sourceClasses
	 * @param targetClasses
	 */
	public Multimap<NamedClass, String> performUnsupervisedLinking(Set<NamedClass> sourceClasses, Collection<Description> targetClasses) {

		logger.info("Computing links...");
		logger.info("Source classes: " + sourceClasses);
		logger.info("Target classes: " + targetClasses);
		//compute the Concise Bounded Description(CBD) for each instance
		//in each source class C_i, thus create a model for each class
		Map<NamedClass, Model> sourceClassToModel = new HashMap<NamedClass, Model>();
		for (NamedClass sourceClass : sourceClasses) {
			//get all instances of C_i
			SortedSet<Individual> sourceInstances = getSourceInstances(sourceClass);
			//            sourceInstances = SetManipulation.stableShrinkInd(sourceInstances, linkingMaxNrOfExamples_LIMES);

			//get the fragment describing the instances of C_i
			logger.debug("Computing fragment...");
			Model sourceFragment = getFragment(sourceInstances, sourceKB, linkingMaxRecursionDepth_LIMES);
			removeNonStringLiteralStatements(sourceFragment);
			logger.debug("...got " + sourceFragment.size() + " triples.");
			sourceClassToModel.put(sourceClass, sourceFragment);
		}

		//compute the Concise Bounded Description(CBD) for each instance
		//in each each target class expression D_i, thus create a model for each class expression
		Map<Description, Model> targetClassExpressionToModel = new HashMap<Description, Model>();
		for (Description targetClass : targetClasses) {
			// get all instances of D_i
			SortedSet<Individual> targetInstances = getTargetInstances(targetClass);
			//            targetInstances = SetManipulation.stableShrinkInd(targetInstances, linkingMaxNrOfExamples_LIMES);

			// get the fragment describing the instances of D_i
			logger.debug("Computing fragment...");
			Model targetFragment = getFragment(targetInstances, targetKB, linkingMaxRecursionDepth_LIMES);
			removeNonStringLiteralStatements(targetFragment);
			logger.debug("...got " + targetFragment.size() + " triples.");
			targetClassExpressionToModel.put(targetClass, targetFragment);
		}

		Multimap<NamedClass, String> map = HashMultimap.create();

		//for each C_i
		for (Entry<NamedClass, Model> entry : sourceClassToModel.entrySet()) {
			NamedClass sourceClass = entry.getKey();
			Model sourceClassModel = entry.getValue();
			//			currentClass = sourceClass; 

			Cache cache = getCache(sourceClassModel);

			//for each D_i
			for (Entry<Description, Model> entry2 : targetClassExpressionToModel.entrySet()) {
				Description targetClassExpression = entry2.getKey();
				Model targetClassExpressionModel = entry2.getValue();

				logger.debug("Computing links between " + sourceClass + " and " + targetClassExpression + "...");

				Cache cache2 = getCache(targetClassExpressionModel);
				Mapping result = null;

				//buffers the mapping results and only carries out a computation if the mapping results are unknown
				if (mappingResults.containsKey(sourceClass)) {
					if (mappingResults.get(sourceClass).containsKey(targetClassExpression)) {
						result = mappingResults.get(sourceClass).get(targetClassExpression);
					}
				}

				if (result == null) {
					result = getDeterministicUnsupervisedMappings(cache, cache2, sourceClass);
					if (!mappingResults.containsKey(sourceClass)) {
						mappingResults.put(sourceClass, new HashMap<Description, Mapping>());
					}
					mappingResults.get(sourceClass).put(targetClassExpression, result);
				}

				//Keep record of the real F-Measures
				if(result.size > 0){
					double f = MappingMath.computeFMeasure(result, cache2.size());
					resultRecorder.setFMeasure(f, iterationNr, sourceClass);
					resultRecorder.setInstanceMapping(result, iterationNr, sourceClass);
				}

				for (Entry<String, HashMap<String, Double>> mappingEntry : result.map.entrySet()) {
					String key = mappingEntry.getKey();
					HashMap<String, Double> value = mappingEntry.getValue();
					map.put(sourceClass, value.keySet().iterator().next());
				}
			}
		}
		return map;
	}


	/**
	 * Run LIMES to generate owl:sameAs links
	 *
	 * @param sourceClasses
	 * @param targetClasses
	 */
	public Multimap<NamedClass, String> performUnsupervisedLinkingMultiThreaded(Set<NamedClass> sourceClasses, Collection<Description> targetClasses) {
		logger.info("Computing links...");
		logger.info("Source classes: " + sourceClasses);
		logger.info("Target classes: " + targetClasses);
		//compute the Concise Bounded Description(CBD) for each instance
		//in each source class C_i, thus creating a model for each class
		Map<NamedClass, Model> sourceClassToModel = new HashMap<NamedClass, Model>();
		for (NamedClass sourceClass : sourceClasses) {
			//get all instances of C_i
			SortedSet<Individual> sourceInstances = getSourceInstances(sourceClass);
			sourceInstances = SetManipulation.stableShrinkInd(sourceInstances, linkingMaxNrOfExamples_LIMES);

			//get the fragment describing the instances of C_i
			logger.debug("Computing fragment...");
			Model sourceFragment = getFragment(sourceInstances, sourceKB, linkingMaxRecursionDepth_LIMES);
			removeNonStringLiteralStatements(sourceFragment);
			logger.debug("...got " + sourceFragment.size() + " triples.");
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
			logger.debug("Computing fragment...");
			Model targetFragment = getFragment(targetInstances, targetKB, linkingMaxRecursionDepth_LIMES);
			removeNonStringLiteralStatements(targetFragment);
			logger.debug("...got " + targetFragment.size() + " triples.");
			targetClassExpressionToModel.put(targetClass, targetFragment);
		}

		final Multimap<NamedClass, String> map = HashMultimap.create();

		ExecutorService threadPool = Executors.newFixedThreadPool(7);
		List<Future<LinkingResult>> list = new ArrayList<Future<LinkingResult>>();

		//for each C_i
		for (Entry<NamedClass, Model> entry : sourceClassToModel.entrySet()) {
			final NamedClass sourceClass = entry.getKey();
			Model sourceClassModel = entry.getValue();

			final Cache sourceCache = getCache(sourceClassModel);

			//for each D_i
			for (Entry<Description, Model> entry2 : targetClassExpressionToModel.entrySet()) {
				final Description targetClassExpression = entry2.getKey();
				Model targetClassExpressionModel = entry2.getValue();

				logger.debug("Computing links between " + sourceClass + " and " + targetClassExpression + "...");

				final Cache targetCache = getCache(targetClassExpressionModel);

				//buffers the mapping results and only carries out a computation if the mapping results are unknown
				if (mappingResults.containsKey(sourceClass) && mappingResults.get(sourceClass).containsKey(targetClassExpression)) {
					Mapping result = mappingResults.get(sourceClass).get(targetClassExpression);
					for (Entry<String, HashMap<String, Double>> mappingEntry : result.map.entrySet()) {
						HashMap<String, Double> value = mappingEntry.getValue();
						map.put(sourceClass, value.keySet().iterator().next());
					}
				} else {
					list.add(threadPool.submit(new DeterministicUnsupervisedLinkingTask(sourceClass, targetClassExpression, sourceCache, targetCache)));
				}
			}
		}

		try {
			threadPool.shutdown();
			threadPool.awaitTermination(5, TimeUnit.HOURS);
			for (Future<LinkingResult> future : list) {
				try {
					LinkingResult result = future.get();
					if (!mappingResults.containsKey(result.source)) {
						mappingResults.put(result.source, new HashMap<Description, Mapping>());
					}
					mappingResults.get(result.source).put(result.target, result.mapping);
					for (Entry<String, HashMap<String, Double>> mappingEntry : result.mapping.map.entrySet()) {
						HashMap<String, Double> value = mappingEntry.getValue();
						map.put(result.source, value.keySet().iterator().next());
					}
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return map;
	}

	private void removeNonStringLiteralStatements(Model m) {
		StmtIterator iterator = m.listStatements();
		List<Statement> statements2Remove = new ArrayList<Statement>();
		while (iterator.hasNext()) {
			Statement st = iterator.next();
			if (!st.getObject().isLiteral()
					|| !(st.getObject().asLiteral().getDatatype() == null || 
					st.getObject().asLiteral().getDatatypeURI().equals(XSDDatatype.XSDstring.getURI()))){
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
	 * @param sourceClass 
	 *
	 */
	public Mapping getDeterministicUnsupervisedMappings(Cache source, Cache target, NamedClass sourceClass) {
		logger.info("Source size = " + source.getAllUris().size());
		logger.info("Target size = " + target.getAllUris().size());

		MeshBasedSelfConfigurator bsc = new MeshBasedSelfConfigurator(source, target, coverage_LIMES, beta_LIMES);
		//ensures that only the threshold 1.0 is tested. Can be set to a lower value
		//default is 0.3
		bsc.MIN_THRESHOLD = 0.9;
		bsc.setMeasure(fmeasure_LIMES);
		Set<String> measure =  new HashSet<String>();
		measure.add("trigrams");
		List<SimpleClassifier> cp = bsc.getBestInitialClassifiers(measure);

		if (cp.isEmpty()) {
			logger.warn("No property mapping found");
			return new Mapping();
		}
		//get subset of best initial classifiers

		Collections.sort(cp, new SimpleClassifierComparator());
		Collections.reverse(cp);
		if(cp.size() > numberOfDimensions)
			cp = cp.subList(0, numberOfDimensions);

		ComplexClassifier cc = bsc.getZoomedHillTop(5, numberOfLinkingIterations, cp);
		Mapping map = Mapping.getBestOneToOneMappings(cc.mapping);
		List<SimpleClassifier> x = cc.classifiers;
		logger.debug("Classifier: " + cc.classifiers);
		resultRecorder.setClassifier(cc.classifiers, iterationNr, sourceClass);
		resultRecorder.setPFMeasure(new ReferencePseudoMeasures().getPseudoFMeasure(source.getAllUris(), target.getAllUris(), map, 1.0),
				iterationNr, sourceClass);

		return map;
	}

	/**
	 * Computes initial mappings
	 *
	 */
	public Mapping getNonDeterministicUnsupervisedMappings(Cache source, Cache target) {
		logger.info("Source size = " + source.getAllUris().size());
		logger.info("Target size = " + target.getAllUris().size());
		//TODO @Axel: Add genetic algorithm variant
		return null;
	}

	private Set<NamedClass> getClasses(KnowledgeBase kb) {
		Set<NamedClass> classes = new HashSet<NamedClass>();

		//get all OWL classes
		String query = "SELECT ?type WHERE {?type a <" + OWL.Class.getURI() + ">.";
		if (kb.getNamespace() != null) {
			query += "FILTER(REGEX(STR(?type),'" + kb.getNamespace() + "'))";
		}
		query += "}";
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
			query = "SELECT DISTINCT ?type WHERE {?s a ?type.";
			if (kb.getNamespace() != null) {
				query += "FILTER(REGEX(STR(?type),'" + kb.getNamespace() + "'))";
			}
			query += "}";
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

	public EvaluatedDescription computeMapping(SortedSet<Individual> positiveExamples, boolean useRemoteKB) throws NonExistingLinksException {
		return computeMappings(positiveExamples, useRemoteKB).get(0);
	}

	public List<? extends EvaluatedDescription> computeMappings(Description targetClassExpression, boolean useRemoteKB) throws NonExistingLinksException {
		SortedSet<Individual> targetInstances = getTargetInstances(targetClassExpression);
		return computeMappings(targetInstances, useRemoteKB);
	}

	public List<? extends EvaluatedDescription> computeMappings(SortedSet<Individual> positiveExamples, boolean useRemoteKB) throws NonExistingLinksException {
		logger.info("positiveExamples: " + positiveExamples);
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

			AutomaticNegativeExampleFinderSPARQL2 negativeExampleFinder;
			if(useRemoteKB){
				negativeExampleFinder = new AutomaticNegativeExampleFinderSPARQL2(endpoint, targetKB.getNamespace());
			}else{
				negativeExampleFinder = new AutomaticNegativeExampleFinderSPARQL2(targetKB.getReasoner(), targetKB.getNamespace());
			}
			SortedSet<Individual> negativeExamples = negativeExampleFinder.getNegativeExamples(positiveExamples, maxNrOfNegativeExamples);
			negativeExamples.removeAll(positiveExamples);
			MonitorFactory.getTimeMonitor("negative examples").stop();
			logger.info("Found " + negativeExamples.size() + " negative examples in " + MonitorFactory.getTimeMonitor("negative examples").getTotal() + "ms.");
			logger.debug("Negative examples: " + negativeExamples);
			resultRecorder.setNegativeExample(negativeExamples, iterationNr, currentClass);

			//get a sample of the negative examples
			SortedSet<Individual> negativeExamplesSample = SetManipulation.stableShrinkInd(negativeExamples, maxNrOfNegativeExamples);

			//store negativeExamples 
			Map<NamedClass, SortedSet<Individual>> sourceClass2NegativeExample = new HashMap<NamedClass, SortedSet<Individual>>();
			sourceClass2NegativeExample.put(currentClass, negativeExamplesSample);

			//create fragment for negative examples
			logger.info("Extracting fragment for negative examples...");
			mon.start();
			Model negativeFragment;
			if(useRemoteKB){
				negativeFragment = getFragment(negativeExamplesSample, new RemoteKnowledgeBase(endpoint));
			}else{
				negativeFragment = getFragment(negativeExamplesSample, targetKB);
			}
			mon.stop();
			logger.info("...got " + negativeFragment.size() + " triples in " + mon.getLastValue() + "ms.");

			logger.info("Learning input:");
			logger.info("Positive examples: " + positiveExamplesSample.size() + " with " + positiveFragment.size() + " triples, e.g. \n" + print(positiveExamplesSample, 3));
			logger.info("Negative examples: " + negativeExamplesSample.size() + " with " + negativeFragment.size() + " triples, e.g. \n" + print(negativeExamplesSample, 3));

			//create fragment consisting of both
			OntModel fullFragment = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
			fullFragment.add(positiveFragment);
			fullFragment.add(negativeFragment);
			fullFragment.add(targetKB.executeConstruct("CONSTRUCT {?s <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?o.} WHERE {?s <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?o.}"));
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
			//            rc.setSubsumptionHierarchy(targetKB.getReasoner().getClassHierarchy());
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
			//            for (EvaluatedDescription d : la.getCurrentlyBestEvaluatedDescriptions(100, 0.2, true)) {
			//				logger.info(d);
			//			}
			return la.getCurrentlyBestEvaluatedDescriptions(10);
		} catch (ComponentInitException e) {
			logger.error(e);
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			OWLOntology ontology = ((OWLAPIOntology) convert(model)).createOWLOntology(man);
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

	private String print(Collection<Individual> individuals, int n) {
		StringBuilder sb = new StringBuilder();
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
		logger.debug("Retrieving instances of class " + cls + "...");
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
		logger.debug("...found " + instances.size() + " instances in " + mon.getLastValue() + "ms.");
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
		logger.trace("Retrieving instances of class expression " + desc + "...");
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
		logger.trace("...found " + instances.size() + " instances in " + mon.getLastValue() + "ms.");
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
		logger.trace("Retrieving instances to which instances of class " + cls + " are linked to via property " + linkingProperty + "...");
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
		logger.trace("...found " + instances.size() + " instances in " + mon.getLastValue() + "ms.");
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
			try {
				model.write(new FileOutputStream("error.ttl"), "TURTLE", null);
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
		Model fragment;
		for (Individual ind : individuals) {
			fragment = getFragment(ind, kb, recursionDepth);
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
		logger.trace("Loading fragment for " + ind.getName());
		ConciseBoundedDescriptionGenerator cbdGen;
		if (kb.isRemote()) {
			logger.debug("Quering remote KB");
			if (((RemoteKnowledgeBase) kb).getCache() != null) {
				cbdGen = new ConciseBoundedDescriptionGeneratorImpl(((RemoteKnowledgeBase) kb).getEndpoint(), 
						((RemoteKnowledgeBase) kb).getCache().getCacheDirectory());
			} else {
				cbdGen = new ConciseBoundedDescriptionGeneratorImpl(((RemoteKnowledgeBase) kb).getEndpoint());
			}
		} else {
			logger.debug("Quering local KB");
			cbdGen = new ConciseBoundedDescriptionGeneratorImpl(((LocalKnowledgeBase) kb).getModel());
		}
		Model cbd = ModelFactory.createDefaultModel();
		try {
			cbd = cbdGen.getConciseBoundedDescription(ind.getName(), 1, true);
		} catch (Exception e) {
			logger.error("End Point(" + ((RemoteKnowledgeBase) kb).getEndpoint().toString() + ") Exception: " + e);
		}
		logger.trace("Got " + cbd.size() + " triples.");
		return cbd;
	}

	private static void cleanUpModel(Model model) {
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
				} else if (lit.getDatatype().getURI().equals(XSD.gYear.getURI())) {
					statementsToRemove.add(st);
					//                    System.err.println("REMOVE " + st);
				} else if (lit.getDatatype().getURI().equals(XSD.gYearMonth.getURI())) {
					statementsToRemove.add(st);
					//					                    System.err.println("REMOVE " + st);
				}
			}
			//remove statements like <x a owl:Class>
			if (st.getPredicate().equals(RDF.type)) {
				if (object.equals(RDFS.Class.asNode()) || object.equals(OWL.Class.asNode()) || object.equals(RDFS.Literal.asNode())
						|| object.equals(RDFS.Resource)) {
					statementsToRemove.add(st);
				}
			}

			//remove unwanted properties
			String dbo = "http://dbpedia.org/ontology/";
			Set<String> blackList = Sets.newHashSet(dbo + "wikiPageDisambiguates",dbo + "wikiPageExternalLink",
					dbo + "wikiPageID", dbo + "wikiPageInterLanguageLink", dbo + "wikiPageRedirects", dbo + "wikiPageRevisionID",
					dbo + "wikiPageWikiLink");
			for(String bl: blackList){
				if (st.getPredicate().equals(bl)) {
					statementsToRemove.add(st);
				}
			}
		}

		model.remove(statementsToRemove);
	}

	/**
	 * Filter triples which are not relevant based on the given knowledge base
	 * namespace.
	 *
	 * @param model
	 * @param namespace
	 */
	private void filter(Model model, String namespace) {
		List<Statement> statementsToRemove = new ArrayList<Statement>();
		for (Iterator<Statement> iter = model.listStatements().toList().iterator(); iter.hasNext();) {
			Statement st = iter.next();
			Property predicate = st.getPredicate();
			if (predicate.equals(RDF.type)) {
				if (!st.getObject().asResource().getURI().startsWith(namespace)) {
					statementsToRemove.add(st);
				} else if (st.getObject().equals(OWL.FunctionalProperty.asNode())) {
					statementsToRemove.add(st);
				} else if (st.getObject().isLiteral() && st.getObject().asLiteral().getDatatypeURI().equals(XSD.gYear.getURI())) {
					statementsToRemove.add(st);
				}
			} else if (!predicate.equals(RDFS.subClassOf) && !predicate.equals(OWL.sameAs) && !predicate.asResource().getURI().startsWith(namespace)) {
				statementsToRemove.add(st);
			}
		}
		model.remove(statementsToRemove);
	}

	private SortedSet<Individual> getRelatedIndividualsNamespaceAware(KnowledgeBase kb, NamedClass nc, String targetNamespace) {
		SortedSet<Individual> relatedIndividuals = new TreeSet<Individual>();
		//get all individuals o which are connected to individuals s belonging to class nc
		//		String query = String.format("SELECT ?o WHERE {?s a <%s>. ?s <http://www.w3.org/2002/07/owl#sameAs> ?o. FILTER(REGEX(STR(?o),'%s'))}", nc.getName(), targetNamespace);
		//		ResultSet rs = executeSelect(kb, query);
		//		QuerySolution qs;
		//		while(rs.hasNext()){
		//			qs = rs.next();
		//			RDFNode object = qs.get("o");
		//			if(object.isURIResource()){
		//				
		//				String uri = object.asResource().getURI();
		//				//workaround for World Factbook - should be removed later
		//				uri = uri.replace("http://www4.wiwiss.fu-berlin.de/factbook/resource/", "http://wifo5-03.informatik.uni-mannheim.de/factbook/resource/");
		//				//workaround for OpenCyc - should be removed later
		//				uri = uri.replace("http://sw.cyc.com", "http://sw.opencyc.org");
		//				
		//				relatedIndividuals.add(new Individual(uri));
		//			}
		//		}
		return relatedIndividuals;
	}

	class DeterministicUnsupervisedLinkingTask implements Callable<LinkingResult>{

		private NamedClass source;
		private Description target;
		private Cache sourceCache;
		private Cache targetCache;

		/**
		 * 
		 */
		public DeterministicUnsupervisedLinkingTask(NamedClass source, Description target, Cache sourceCache, Cache targetCache) {
			this.source = source;
			this.target = target;
			this.sourceCache = sourceCache;
			this.targetCache = targetCache;
		}

		/* (non-Javadoc)
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public LinkingResult call() throws Exception {
			logger.info("Source size = " + sourceCache.getAllUris().size());
			logger.info("Target size = " + targetCache.getAllUris().size());

			MeshBasedSelfConfigurator bsc = new MeshBasedSelfConfigurator(sourceCache, targetCache, coverage_LIMES, beta_LIMES);
			//ensures that only the threshold 1.0 is tested. Can be set to a lower value
			//default is 0.3
			bsc.MIN_THRESHOLD = 0.6;
			bsc.setMeasure(fmeasure_LIMES);
			Set<String> measure =  new HashSet<String>();
			measure.add("trigrams");
			List<SimpleClassifier> cp = bsc.getBestInitialClassifiers(measure);

			if (cp.isEmpty()) {
				logger.warn("No property mapping found");
				return new LinkingResult(source, target, new Mapping());
			}
			//get subset of best initial classifiers

			Collections.sort(cp, new SimpleClassifierComparator());
			Collections.reverse(cp);
			if(cp.size() > numberOfDimensions)
				cp = cp.subList(0, numberOfDimensions);

			ComplexClassifier cc = bsc.getZoomedHillTop(5, numberOfLinkingIterations, cp);
			Mapping map = Mapping.getBestOneToOneMappings(cc.mapping);
			logger.info("Mapping size is " + map.getNumberofMappings());
			logger.info("Pseudo F-measure is " + cc.fMeasure);
			return new LinkingResult(source, target, map);
		}

	}

	class LinkingResult {
		Mapping mapping;
		NamedClass source;
		Description target;

		public LinkingResult(NamedClass source, Description target, Mapping mapping) {
			this.source = source;
			this.target = target;
			this.mapping = mapping;
		}
	}

	public static void main(String[] args) throws Exception {
		Model m = ModelFactory.createDefaultModel();
		m.read(new FileInputStream(new File("/tmp/inc.owl")), null);
		cleanUpModel(m);
	}
}

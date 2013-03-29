package org.aksw.lassie.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.kb.LocalKnowledgeBase;
import org.aksw.lassie.kb.RemoteKnowledgeBase;
import org.aksw.lassie.util.PrintUtils;
import org.apache.log4j.Logger;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.AbstractLearningProblem;
import org.dllearner.core.AbstractReasonerComponent;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.NamedClass;
import org.dllearner.kb.OWLAPIOntology;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGenerator;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGeneratorImpl;
import org.dllearner.learningproblems.PosNegLPStandard;
import org.dllearner.learningproblems.PosOnlyLP;
import org.dllearner.reasoning.FastInstanceChecker;
import org.dllearner.utilities.datastructures.SetManipulation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

public class ExpressiveSchemaMappingGenerator {
	
	
	private static final Logger logger = Logger.getLogger(ExpressiveSchemaMappingGenerator.class.getName());
	
	private final Monitor mon;
	
	private boolean posNegLearning = true;
	private final boolean performCrossValidation = true;
	private int fragmentDepth = 2;
	
	/**
	 * The maximum number of positive examples, used for the SPARQL extraction and learning algorithm
	 */
	private int maxNrOfPositiveExamples = 20;
	/**
	 * The maximum number of negative examples, used for the SPARQL extraction and learning algorithm
	 */
	private int maxNrOfNegativeExamples = 20;
	
	private NamedClass currentClass;

	private KnowledgeBase source;
	private KnowledgeBase target;
	private String linkingProperty = OWL.sameAs.getURI();
	
	public ExpressiveSchemaMappingGenerator(KnowledgeBase source, KnowledgeBase target) {
		this.source = source;
		this.target = target;
		
		mon = MonitorFactory.getTimeMonitor("time");
	}
	
	public ExpressiveSchemaMappingGenerator(KnowledgeBase source, KnowledgeBase target, String linkingProperty) {
		this.source = source;
		this.target = target;
		this.linkingProperty = linkingProperty;
		
		mon = MonitorFactory.getTimeMonitor("time");
	}
	
	public void setFragmentDepth(int fragmentDepth) {
		this.fragmentDepth = fragmentDepth;
	}
	
	public void run() {
		// get all classes C in source KB
		Set<NamedClass> sourceClasses = getClasses(source);

		// for each class c_i in C
		for (NamedClass cls : sourceClasses) {
			//compute the schema mapping
			computeMapping(cls);
		}
	}
	
	public List<? extends EvaluatedDescription> computeMapping(NamedClass cls){
		//get all instances of c_i
		SortedSet<Individual> sourceInstances = getSourceInstances(cls);
		
		//get all instances in the target KB to which instances of c_i are connected to
		//this are our positive examples
		SortedSet<Individual> targetInstances = getTargetInstances(cls);
		
		//compute an initial mapping
		List<? extends EvaluatedDescription> schemaMapping = initSchemaMapping(targetInstances);
		
		//update schema mapping until total coverage is maximized
//		do {
//			updateSchemaMapping();
//		} while (true);
		
		return schemaMapping;
	}
	
	private List<? extends EvaluatedDescription> initSchemaMapping(SortedSet<Individual> positiveExamples){
		//get a sample of the positive examples
		SortedSet<Individual> positiveExamplesSample = SetManipulation.stableShrinkInd(positiveExamples, maxNrOfPositiveExamples);
		
		//starting from the positive examples, we first extract the fragment for them
		logger.info("Extracting fragment for positive examples...");
		Model positiveFragment = getFragment(positiveExamplesSample, target);
		logger.info("...done.");
		
		//based on the fragment we try to find some good negative examples
		SortedSet<Individual> negativeExamples = new TreeSet<Individual>();
		mon.start();
		//find the classes the positive examples are asserted to
		Set<NamedClass> positiveExamplesClasses = new HashSet<NamedClass>();
		ParameterizedSparqlString template = new ParameterizedSparqlString("SELECT ?type WHERE {?s a ?type.}");
		for(Individual pos : positiveExamples){
			template.clearParams();
			template.setIri("s", pos.getName());
			ResultSet rs = QueryExecutionFactory.create(template.asQuery(), positiveFragment).execSelect();
			QuerySolution qs;
			while(rs.hasNext()){
				qs = rs.next();
				if(qs.get("type").isURIResource()){
					positiveExamplesClasses.add(new NamedClass(qs.getResource("type").getURI()));
				}
			}
		}
		System.out.println(positiveExamplesClasses);
		
		//get the negative examples
		for(NamedClass nc : positiveExamplesClasses){
			Set<NamedClass> parallelClasses = target.getReasoner().getSiblingClasses(nc);
			for(NamedClass parallelClass : parallelClasses){
				negativeExamples.addAll(target.getReasoner().getIndividuals(parallelClass, 5));
				negativeExamples.removeAll(positiveExamples);
			}
		}
		
		mon.stop();
		logger.info("Found " + negativeExamples.size() + " negative examples in " + mon.getLastValue() + "ms.");
		
		//get a sample of the negative examples
		SortedSet<Individual> negativeExamplesSample = SetManipulation.stableShrinkInd(negativeExamples, maxNrOfNegativeExamples);
		
		logger.info("#Positive examples: " + positiveExamplesSample.size());
		logger.info("#Negative examples: " + negativeExamplesSample.size());
		
		//create fragment for negative examples
		logger.info("Extracting fragment for negative examples...");
		Model negativeFragment = getFragment(negativeExamplesSample, target);
		logger.info("...done.");
		
		//create fragment consisting of both
		OntModel fullFragment = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
		fullFragment.add(positiveFragment);
		fullFragment.add(negativeFragment);
	
		//learn the class expressions
		return learnClassExpressions(fullFragment, positiveExamplesSample, negativeExamplesSample);
	}

	private List<? extends EvaluatedDescription> learnClassExpressions(Model model, SortedSet<Individual> positiveExamples, SortedSet<Individual> negativeExamples){
		try {
			KnowledgeSource ks = convert(model);
			
			//initialize the reasoner
			logger.info("Initializing reasoner...");
			AbstractReasonerComponent rc = new FastInstanceChecker(ks);
			rc.init();
			logger.info("Done.");
  
			//initialize the learning problem
			logger.info("Initializing learning problem...");
			AbstractLearningProblem lp;
			if(!negativeExamples.isEmpty()){
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
			e.printStackTrace();
		}
		return null;
	}
	
	
	private void updateSchemaMapping(){
		
	}
	
	/**
	 * Return all instances of the given class in the source KB.
	 * @param cls
	 * @return
	 */
	private SortedSet<Individual> getSourceInstances(NamedClass cls){
		logger.info("Retrieving instances of class " + cls + "...");
		SortedSet<Individual> instances = new TreeSet<Individual>();
		String query = String.format("SELECT DISTINCT ?s WHERE {?s a <%s>}", cls.getName());
		ResultSet rs = source.executeSelect(query);
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			instances.add(new Individual(qs.getResource("s").getURI()));
		}
		logger.info("...done.");
		return instances;
	}
	
	/**
	 * Return all instances which are (assumed to be) contained in the target KB.
	 * Here we should apply a namespace filter on the URIs such that we get only instances which are really contained in the target KB.
	 * @param cls
	 * @return
	 */
	private SortedSet<Individual> getTargetInstances(NamedClass cls){
		logger.info("Retrieving instances to which instances of class " + cls + " are linked to via property " + linkingProperty + "...");
		SortedSet<Individual> instances = new TreeSet<Individual>();
		String query = String.format("SELECT DISTINCT ?o WHERE {?s a <%s>. ?s <%s> ?o. FILTER(REGEX(?o,'^%s'))}", cls.getName(), linkingProperty, target.getNamespace());
		ResultSet rs = source.executeSelect(query);
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			instances.add(new Individual(qs.getResource("o").getURI()));
		}
		logger.info("...done.");
		return instances;
	}
	
	private KnowledgeSource convert(Model model){
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			model.write(baos, "TURTLE", null);
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			OWLOntology ontology = man.loadOntologyFromOntologyDocument(new ByteArrayInputStream(baos.toByteArray()));
			return new OWLAPIOntology(ontology);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			try {
				model.write(new FileOutputStream("errors/" + PrintUtils.prettyPrint(currentClass) + "_conversion_error.ttl"), "TURTLE", null);
			} catch (FileNotFoundException e1) {
				e.printStackTrace();
			}
		} 
		return null;
	}
	
	/**
	 * Computes a fragment containing hopefully useful information about the resources.
	 * @param ind
	 */
	private Model getFragment(SortedSet<Individual> individuals, KnowledgeBase kb){
		OntModel fullFragment = ModelFactory.createOntologyModel();
		int i = 1;
		for(Individual ind : individuals){
			logger.info(i++  + "/" + individuals.size());
			fullFragment.add(getFragment(ind, kb));
		}
		filter(fullFragment);
		return fullFragment;
	}
	
	private void filter(Model model) {
		// filter out triples with String literals, as there often occur are
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
				}
			}
			//remove statements like <x a owl:Class>
			if(st.getPredicate().equals(RDF.type)){
				if(object.equals(RDFS.Class.asNode()) || object.equals(OWL.Class.asNode()) || object.equals(RDFS.Literal.asNode())){
					statementsToRemove.add(st);
				}
			}
		}
		model.remove(statementsToRemove);
	}
	
	
	/**
	 * Computes a fragment containing hopefully useful information about the resource.
	 * @param ind
	 */
	private Model getFragment(Individual ind, KnowledgeBase kb){
		logger.debug("Loading fragment for " + ind.getName());
		ConciseBoundedDescriptionGenerator cbdGen;
		if(kb.isRemote()){
			cbdGen = new ConciseBoundedDescriptionGeneratorImpl(((RemoteKnowledgeBase)kb).getEndpoint());
		} else {
			cbdGen = new ConciseBoundedDescriptionGeneratorImpl(((LocalKnowledgeBase)kb).getModel());
		}
		
		Model cbd = cbdGen.getConciseBoundedDescription(ind.getName(), fragmentDepth);
		logger.debug("Got " + cbd.size() + " triples.");
		return cbd;
	}
	
	private Set<NamedClass> getClasses(KnowledgeBase kb){
		Set<NamedClass> classes = new HashSet<NamedClass>();
		
		//get all OWL classes
		String query = String.format("SELECT ?type WHERE {?s a <%s>.}", OWL.Class.getURI());
		ResultSet rs = kb.executeSelect(query);
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			if(qs.get("type").isURIResource()){
				classes.add(new NamedClass(qs.get("type").asResource().getURI()));
			}
		}
		
		//fallback: check for ?s a ?type where ?type is not asserted to owl:Class
		if(classes.isEmpty()){
			query = "SELECT ?type WHERE {?s a ?type.}";
			rs = kb.executeSelect(query);
			while(rs.hasNext()){
				qs = rs.next();
				if(qs.get("type").isURIResource()){
					classes.add(new NamedClass(qs.get("type").asResource().getURI()));
				}
			}
		}
		return classes;
	}
	
	private SortedSet<Individual> getRelatedIndividualsNamespaceAware(KnowledgeBase kb, NamedClass nc, String targetNamespace){
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
	
}

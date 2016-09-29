package org.aksw.lassie.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.aksw.lassie.bmGenerator.Modifier;
import org.aksw.lassie.core.exceptions.NonExistingLinksException;
import org.aksw.lassie.core.linking.UnsupervisedLinker;
import org.aksw.lassie.core.linking.WombatLinker;
import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.result.LassieResultRecorder;
import org.aksw.lassie.util.PrintUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.apache.log4j.Logger;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.AbstractLearningProblem;
import org.dllearner.core.AbstractReasonerComponent;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.kb.OWLAPIOntology;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.learningproblems.PosNegLPStandard;
import org.dllearner.learningproblems.PosOnlyLP;
import org.dllearner.reasoning.ClosedWorldReasoner;
import org.dllearner.utilities.Helper;
import org.dllearner.utilities.datastructures.SetManipulation;
import org.dllearner.utilities.examples.AutomaticNegativeExampleFinderSPARQL2;
import org.dllearner.utilities.owl.OWLEntityTypeAdder;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class LASSIEController {

    //current status trackers
    protected static final Logger logger = Logger.getLogger(LASSIEController.class.getName());

    protected Monitor mon;

    protected OWLClass currentClass;

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

    protected int MAX_RECURSION_DEPTH = 2;

    protected OWLDataFactory owlDataFactory = new OWLDataFactoryImpl();

    protected String linkingProperty = OWL.sameAs.getURI();

    //result recording
    LassieResultRecorder resultRecorder;
    private SparqlEndpoint endpoint;

    public  LASSIEController() throws ComponentInitException{
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

    public LASSIEController(KnowledgeBase source, KnowledgeBase target, int nrOfIterations, Set<OWLClass> sourceClasses) {
        this(source, target, OWL.sameAs.getURI(), sourceClasses);
        maxNrOfIterations = nrOfIterations;
    }

    public LASSIEController(KnowledgeBase source, KnowledgeBase target, Set<OWLClass> sourceClasses) {
        this(source, target, OWL.sameAs.getURI(), sourceClasses);
    }

    public LASSIEController(KnowledgeBase source, KnowledgeBase target, String linkingProperty, Set<OWLClass> sourceClasses) {
        this.sourceKB = source;
        this.targetKB = target;

        this.linkingProperty = linkingProperty;

        mon = MonitorFactory.getTimeMonitor("time");

        source.getReasoner().prepareSubsumptionHierarchy();
        target.getReasoner().prepareSubsumptionHierarchy();

        resultRecorder = new LassieResultRecorder(maxNrOfIterations, sourceClasses);
    }

    /**
     * @param sourceKB2
     * @param targetKB2
     * @param endpoint
     * @param maxNrOfIterations2
     */
    public LASSIEController(KnowledgeBase source, KnowledgeBase target, SparqlEndpoint endpoint,
                            int maxNrOfIterations, Set<OWLClass> sourceClasses) {
        this(source, target, OWL.sameAs.getURI(), sourceClasses);
        this.endpoint = endpoint;
        LASSIEController.maxNrOfIterations = maxNrOfIterations;

    }

    public LassieResultRecorder run(Set<OWLClass> sourceClasses, boolean useRemoteKB) {
        // get all classes D_i in target KB
        Set<OWLClass> targetClasses = getClasses(targetKB);
        logger.debug("targetClasses: " + targetClasses);
        return run(sourceClasses, targetClasses, useRemoteKB);
    }

    public LassieResultRecorder run(Set<OWLClass> sourceClasses, Set<OWLClass> targetClasses, boolean useRemoteKB) {

        resultRecorder = new LassieResultRecorder(maxNrOfIterations, sourceClasses);

        //initially, the class expressions E_i in the target KB are the named classes D_i
        Collection<OWLClassExpression> targetClassExpressions = new TreeSet<>();
        targetClassExpressions.addAll(targetClasses);

        //perform the iterative schema matching
        Map<OWLClass, OWLClassExpression> iterationResultConceptDescription = new HashMap<>();

        double totalCoverage = 0;
        do {
            long itrStartTime = System.currentTimeMillis();

            logger.info(iterationNr + ". ITERATION:");
            //compute a set of links between each pair of class expressions (C_i, E_j), thus finally we get
            //a map from C_i to a set of instances in the target KB
//          UnsupervisedLinker linker = new EuclidLinker(sourceKB, targetKB, linkingProperty, resultRecorder);
            UnsupervisedLinker linker = new WombatLinker(sourceKB, targetKB, linkingProperty, resultRecorder);
            Multimap<OWLClass, String> links = linker.link(sourceClasses, targetClassExpressions);

            //for each source class C_i, compute a mapping to a class expression in the target KB based on the links
            for (OWLClass sourceClass : sourceClasses) {

                logger.info("+++++++++++++++++++++" + sourceClass + "+++++++++++++++++++++");
                currentClass = sourceClass;
                try {
                    SortedSet<OWLIndividual> targetInstances = Helper.getIndividualSet(new TreeSet<>(links.get(sourceClass)));

                    resultRecorder.setPositiveExample(targetInstances, iterationNr, sourceClass);

                    List<? extends EvaluatedDescription<?>> mappingList = computeMappings(targetInstances, useRemoteKB);
                    resultRecorder.setMapping(mappingList, iterationNr, sourceClass);

                    OWLClassExpression oce = (OWLClassExpression) mappingList.get(0).getDescription();
                    iterationResultConceptDescription.put(sourceClass, oce);
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
        
        resultRecorder.totalCoverage = totalCoverage;
        return resultRecorder;
    }

    double computeCoverage(Map<OWLClass, OWLClassExpression> mapping) {

        double totalCoverage = 0;

        for (Entry<OWLClass, OWLClassExpression> entry : mapping.entrySet()) {
            OWLClass sourceClass = entry.getKey();
            OWLClassExpression targetDescription = entry.getValue();

            SortedSet<OWLIndividual> sourceInstances = sourceKB.getInstances(sourceClass);
            SortedSet<OWLIndividual> targetInstances = targetKB.getInstances(targetDescription);
            double coverage = computeDiceSimilarity(sourceInstances, targetInstances);

            resultRecorder.setCoverage(coverage, iterationNr, sourceClass);

            totalCoverage += coverage;
        }

        totalCoverage /= mapping.size();
        return totalCoverage;
    }

    double computeDiceSimilarity(Set<OWLIndividual> sourceInstances, Set<OWLIndividual> targetInstances) {
        SetView<OWLIndividual> intersection = Sets.intersection(sourceInstances, targetInstances);
        //		SetView<OWLIndividual> union = Sets.union(sourceInstances, targetInstances);

        /* Other approaches to compute coverage:
         * double jaccard = (double) intersection.size() / (double) targetInstances.size();
         * double overlap = (double) intersection.size() / (double) Math.min(sourceInstances.size(), targetInstances.size());
         * double alpha = 1, beta = 0.2;
         * SetView<OWLIndividual> sourceDifTarget = Sets.difference(sourceInstances, targetInstances);
         * SetView<OWLIndividual> targetDifsource = Sets.difference(targetInstances, sourceInstances);
         * double tversky = intersection.size() / (intersection.size() + alpha * sourceDifTarget.size() + beta * targetDifsource.size());*/

        double dice = 2 * ((double) intersection.size()) / (double)(sourceInstances.size() + targetInstances.size());
        return dice;
    }

    private Set<OWLClass> getClasses(KnowledgeBase kb) {
        Set<OWLClass> classes = new HashSet<OWLClass>();

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
                classes.add(owlDataFactory.getOWLClass(IRI.create(qs.get("type").asResource().getURI())));
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
                    classes.add(owlDataFactory.getOWLClass(IRI.create(qs.get("type").asResource().getURI())));
                }
            }
        }
        return classes;
    }

    public EvaluatedDescription<?> computeMapping(SortedSet<OWLIndividual> positiveExamples, boolean useRemoteKB) throws NonExistingLinksException, ComponentInitException {
        return computeMappings(positiveExamples, useRemoteKB).get(0);
    }

    public List<? extends EvaluatedDescription<?>> computeMappings(OWLClassExpression targetClassExpression, boolean useRemoteKB) throws NonExistingLinksException, ComponentInitException {
        SortedSet<OWLIndividual> targetInstances = targetKB.getInstances(targetClassExpression);
        return computeMappings(targetInstances, useRemoteKB);
    }

    public List<? extends EvaluatedDescription<?>> computeMappings(SortedSet<OWLIndividual> positiveExamples, boolean useRemoteKB) throws NonExistingLinksException, ComponentInitException {
        logger.info("positiveExamples: " + positiveExamples);
        //if there are no links to the target KB, then we can skip learning
        if (positiveExamples.isEmpty()) {
            throw new NonExistingLinksException();
        } else {
            //compute a mapping
            //get a sample of the positive examples
            SortedSet<OWLIndividual> positiveExamplesSample = SetManipulation.stableShrinkInd(positiveExamples, maxNrOfPositiveExamples);

            //starting from the positive examples, we first extract the fragment for them
            logger.info("Extracting fragment for positive examples...");
            mon.start();
            Model positiveFragment = targetKB.getFragment(positiveExamplesSample, MAX_RECURSION_DEPTH);
            mon.stop();
            logger.info("...got " + positiveFragment.size() + " triples in " + mon.getLastValue() + "ms.");
            //			for (OWLIndividual ind : positiveExamplesSample) {
            //				System.out.println(ResultSetFormatter.asText(
            //						org.apache.jena.query.QueryExecutionFactory.create("SELECT * WHERE {<" + ind.getName() + "> a ?o.}", positiveFragment).execSelect()));
            //			}

            //compute the negative examples
            logger.info("Computing negative examples...");
            MonitorFactory.getTimeMonitor("negative examples").start();

            AutomaticNegativeExampleFinderSPARQL2 negativeExampleFinder;
            //			if(useRemoteKB){
            negativeExampleFinder = new AutomaticNegativeExampleFinderSPARQL2(targetKB.getReasoner());
            //			}else{
            //				negativeExampleFinder = new AutomaticNegativeExampleFinderSPARQL2(targetKB.getReasoner(), targetKB.getNamespace());
            //			}
//            SortedSet<OWLIndividual> negativeExamples = negativeExampleFinder.getNegativeExamples(positiveExamples, maxNrOfNegativeExamples);
            SortedSet<OWLIndividual> negativeExamples = new TreeSet<>();
            negativeExamples.removeAll(positiveExamples);
            MonitorFactory.getTimeMonitor("negative examples").stop();
            logger.info("Found " + negativeExamples.size() + " negative examples in " + MonitorFactory.getTimeMonitor("negative examples").getTotal() + "ms.");
            logger.debug("Negative examples: " + negativeExamples);
            resultRecorder.setNegativeExample(negativeExamples, iterationNr, currentClass);

            //get a sample of the negative examples
            SortedSet<OWLIndividual> negativeExamplesSample = SetManipulation.stableShrinkInd(negativeExamples, maxNrOfNegativeExamples);

            //store negativeExamples 
            Map<OWLClass, SortedSet<OWLIndividual>> sourceClass2NegativeExample = new HashMap<OWLClass, SortedSet<OWLIndividual>>();
            sourceClass2NegativeExample.put(currentClass, negativeExamplesSample);

            //create fragment for negative examples
            logger.info("Extracting fragment for negative examples...");
            mon.start();
            Model negativeFragment;

            negativeFragment = targetKB.getFragment(negativeExamplesSample, MAX_RECURSION_DEPTH);

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

    private List<? extends EvaluatedDescription<?>> learnClassExpressions(Model model, SortedSet<OWLIndividual> positiveExamples, SortedSet<OWLIndividual> negativeExamples) {
        try {
            cleanUpModel(model);
            OWLEntityTypeAdder.addEntityTypes(model);
            KnowledgeSource ks = convert(model);

            //initialize the reasoner
            logger.info("Initializing reasoner...");
            AbstractReasonerComponent rc = new ClosedWorldReasoner(ks);
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
            CELOE la = new CELOE();
            la.setReasoner(rc);
            la.setLearningProblem(lp);
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
                man.saveOntology(ontology,  new RDFXMLDocumentFormat(), new FileOutputStream(new File("inc.owl")));
            } catch (OWLOntologyStorageException e1) {
                e1.printStackTrace();
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
            System.exit(0);

        }
        return null;
    }

    private String print(Collection<OWLIndividual> individuals, int n) {
        StringBuilder sb = new StringBuilder();
        for (OWLIndividual individual : individuals) {
            sb.append(individual.toStringID() + ",");
        }
        sb.append("...");
        return sb.toString();
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
                    //                                      System.err.println("REMOVE " + st);
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

    public static void main(String[] args) throws Exception {
        Model m = ModelFactory.createDefaultModel();
        m.read(new FileInputStream(new File("/tmp/inc.owl")), null);
        cleanUpModel(m);
    }

}

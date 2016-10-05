package org.aksw.lassie.core.linking;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.jamonapi.MonitorFactory;
import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.result.LassieResultRecorder;
import org.aksw.limes.core.datastrutures.GoldStandard;
import org.aksw.limes.core.evaluation.evaluator.EvaluatorFactory;
import org.aksw.limes.core.evaluation.evaluator.EvaluatorType;
import org.aksw.limes.core.evaluation.qualititativeMeasures.IQualitativeMeasure;
import org.aksw.limes.core.evaluation.qualititativeMeasures.PseudoRefFMeasure;
import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.cache.MemoryCache;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.ml.algorithm.euclid.ComplexClassifier;
import org.aksw.limes.core.ml.algorithm.euclid.MeshBasedSelfConfigurator;
import org.aksw.limes.core.ml.algorithm.euclid.SimpleClassifier;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.log4j.Logger;
import org.dllearner.utilities.datastructures.SetManipulation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;

import java.util.*;
import java.util.concurrent.*;

public class EuclidLinker extends AbstractUnsupervisedLinker{
    protected static final Logger logger = Logger.getLogger(EuclidLinker.class);

    // Euclid Configurations
    protected static final int numberOfDimensions = 7;
    static double coverage_LIMES = 0.8;
    static double beta_LIMES = 1d;
    static IQualitativeMeasure fmeasure_LIMES = EvaluatorFactory.create(EvaluatorType.PF_MEASURE);
    protected final int linkingMaxNrOfExamples_LIMES = 100;

    private int numberOfLinkingIterations = 5;

    protected Map<OWLClass, Map<OWLClassExpression, AMapping>> mappingResults = new HashMap<>();

    private int iterationNr = 0;



    public EuclidLinker(KnowledgeBase sourceKB, KnowledgeBase targetKB, String linkingProperty, LassieResultRecorder resultRecorder, int iterationNr){
        super(sourceKB, targetKB, linkingProperty, resultRecorder, iterationNr);
    }

    @Override
    public Multimap<OWLClass, String> link(Set<OWLClass> sourceClasses, Collection<OWLClassExpression> targetClasses) {
        logger.info("Computing links...");
        logger.info("Source classes: " + sourceClasses);
        logger.info("Target classes: " + targetClasses);
        //compute the Concise Bounded Description(CBD) for each instance
        //in each source class C_i, thus create a model for each class
        Map<OWLClass, Model> sourceClassToModel = new HashMap<OWLClass, Model>();
        for (OWLClass sourceClass : sourceClasses) {
            //get all instances of C_i
            SortedSet<OWLIndividual> sourceInstances = getSourceInstances(sourceClass);

            //get the fragment describing the instances of C_i
            logger.debug("Computing fragment...");
            Model sourceFragment = sourceKB.getFragment(sourceInstances, FRAGMENT_DEPTH);
            removeNonStringLiteralStatements(sourceFragment);
            logger.debug("...got " + sourceFragment.size() + " triples.");
            sourceClassToModel.put(sourceClass, sourceFragment);
        }

        //compute the Concise Bounded Description(CBD) for each instance
        //in each each target class expression D_i, thus create a model for each class expression
        Map<OWLClassExpression, Model> targetCBDsModel = new HashMap<OWLClassExpression, Model>();
        for (OWLClassExpression targetClass : targetClasses) {
            // get all instances of D_i
            SortedSet<OWLIndividual> targetInstances = targetKB.getInstances(targetClass);
            //            targetInstances = SetManipulation.stableShrinkInd(targetInstances, linkingMaxNrOfExamples_LIMES);

            // get the fragment describing the instances of D_i
            logger.debug("Computing fragment...");
            Model targetFragment = targetKB.getFragment(targetInstances, FRAGMENT_DEPTH);
            removeNonStringLiteralStatements(targetFragment);
            logger.debug("...got " + targetFragment.size() + " triples.");
            targetCBDsModel.put(targetClass, targetFragment);
        }

        Multimap<OWLClass, String> map = HashMultimap.create();

        //for each C_i
        for (Map.Entry<OWLClass, Model> entry : sourceClassToModel.entrySet()) {
            OWLClass sourceClass = entry.getKey();
            Model sourceClassModel = entry.getValue();

            ACache cache = modelToCache(sourceClassModel);

            //for each D_i
            for (Map.Entry<OWLClassExpression, Model> entry2 : targetCBDsModel.entrySet()) {
                OWLClassExpression targetClassExpression = entry2.getKey();
                Model targetClassExpressionModel = entry2.getValue();

                logger.debug("Computing links between " + sourceClass + " and " + targetClassExpression + "...");

                ACache cache2 = modelToCache(targetClassExpressionModel);

                AMapping result = null;

                //buffers the mapping results and only carries out a computation if the mapping results are unknown
                if (mappingResults.containsKey(sourceClass)) {
                    if (mappingResults.get(sourceClass).containsKey(targetClassExpression)) {
                        result = mappingResults.get(sourceClass).get(targetClassExpression);
                    }
                }

                if (result == null) {
                    result = getDeterministicUnsupervisedMappings(cache, cache2, sourceClass);
                    if (!mappingResults.containsKey(sourceClass)) {
                        mappingResults.put(sourceClass, new HashMap<OWLClassExpression, AMapping>());
                    }
                    mappingResults.get(sourceClass).put(targetClassExpression, result);
                }

                //Keep record of the real F-Measures
                if(result.size() > 0){
                    double f = ((PseudoRefFMeasure)EvaluatorFactory.create(EvaluatorType.PRF_MEASURE)).calculate(result,
                            new GoldStandard(MappingFactory.createDefaultMapping(), cache.getAllUris(), cache2.getAllUris()), 1.0d);
                    resultRecorder.setFMeasure(f, iterationNr, sourceClass);
//                    resultRecorder.setInstanceMapping(result, iterationNr, sourceClass);
                }

                for (Map.Entry<String, HashMap<String, Double>> mappingEntry : result.getMap().entrySet()) {
                    HashMap<String, Double> value = mappingEntry.getValue();
                    map.put(sourceClass, value.keySet().iterator().next());
                }
            }
        }
        return map;
    }


    @Override
    public Multimap<OWLClass, String> linkMultiThreaded(Set<OWLClass> sourceClasses, Collection<OWLClassExpression> targetClasses) {
        logger.info("Computing links...");
        logger.info("Source classes: " + sourceClasses);
        logger.info("Target classes: " + targetClasses);
        //compute the Concise Bounded Description(CBD) for each instance
        //in each source class C_i, thus creating a model for each class
        Map<OWLClass, Model> sourceClassToModel = new HashMap<OWLClass, Model>();
        for (OWLClass sourceClass : sourceClasses) {
            //get all instances of C_i
            SortedSet<OWLIndividual> sourceInstances = getSourceInstances(sourceClass);
            sourceInstances = SetManipulation.stableShrinkInd(sourceInstances, linkingMaxNrOfExamples_LIMES);

            //get the fragment describing the instances of C_i
            logger.debug("Computing fragment...");
            Model sourceFragment = sourceKB.getFragment(sourceInstances, FRAGMENT_DEPTH);
            removeNonStringLiteralStatements(sourceFragment);
            logger.debug("...got " + sourceFragment.size() + " triples.");
            sourceClassToModel.put(sourceClass, sourceFragment);
        }

        //compute the Concise Bounded Description(CBD) for each instance
        //in each each target class expression D_i, thus creating a model for each class expression
        Map<OWLClassExpression, Model> targetClassExpressionToModel = new HashMap<OWLClassExpression, Model>();
        for (OWLClassExpression targetClass : targetClasses) {
            // get all instances of D_i
            SortedSet<OWLIndividual> targetInstances = targetKB.getInstances(targetClass);
            //          targetInstances = SetManipulation.stableShrinkInd(targetInstances, linkingMaxNrOfExamples_LIMES);
            //          ArrayList<OWLIndividual> l = new ArrayList<OWLIndividual>(targetInstances);
            //          Collections.reverse(l);
            //          targetInstances = new TreeSet<OWLIndividual>(l.subList(0, Math.min(100, targetInstances.size())));

            // get the fragment describing the instances of D_i
            logger.debug("Computing fragment...");
            Model targetFragment = targetKB.getFragment(targetInstances, FRAGMENT_DEPTH);
            removeNonStringLiteralStatements(targetFragment);
            logger.debug("...got " + targetFragment.size() + " triples.");
            targetClassExpressionToModel.put(targetClass, targetFragment);
        }

        final Multimap<OWLClass, String> map = HashMultimap.create();

        ExecutorService threadPool = Executors.newFixedThreadPool(7);
        List<Future<LinkingResult>> list = new ArrayList<Future<LinkingResult>>();

        //for each C_i
        for (Map.Entry<OWLClass, Model> entry : sourceClassToModel.entrySet()) {
            final OWLClass sourceClass = entry.getKey();
            Model sourceClassModel = entry.getValue();

            final ACache sourceCache = modelToCache(sourceClassModel);

            //for each D_i
            for (Map.Entry<OWLClassExpression, Model> entry2 : targetClassExpressionToModel.entrySet()) {
                final OWLClassExpression targetClassExpression = entry2.getKey();
                Model targetClassExpressionModel = entry2.getValue();

                logger.debug("Computing links between " + sourceClass + " and " + targetClassExpression + "...");

                final ACache targetCache = modelToCache(targetClassExpressionModel);

                //buffers the mapping results and only carries out a computation if the mapping results are unknown
                if (mappingResults.containsKey(sourceClass) && mappingResults.get(sourceClass).containsKey(targetClassExpression)) {
                    AMapping result = mappingResults.get(sourceClass).get(targetClassExpression);
                    for (Map.Entry<String, HashMap<String, Double>> mappingEntry : result.getMap().entrySet()) {
                        HashMap<String, Double> value = mappingEntry.getValue();
                        map.put(sourceClass, value.keySet().iterator().next());
                    }
                } else {
                    list.add(threadPool.submit(new EuclidTask(sourceClass, targetClassExpression, sourceCache, targetCache)));
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
                        mappingResults.put(result.source, new HashMap<OWLClassExpression, AMapping>());
                    }
                    mappingResults.get(result.source).put(result.target, result.mapping);
                    for (Map.Entry<String, HashMap<String, Double>> mappingEntry : result.mapping.getMap().entrySet()) {
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






    /**
     * Computes initial mappings
     * @param sourceClass
     *
     */
    public AMapping getDeterministicUnsupervisedMappings(ACache source, ACache target, OWLClass sourceClass) {
        logger.info("Source size = " + source.getAllUris().size());
        logger.info("Target size = " + target.getAllUris().size());

        MeshBasedSelfConfigurator bsc = new MeshBasedSelfConfigurator(source, target, coverage_LIMES);
        //ensures that only the threshold 1.0 is tested. Can be set to a lower value
        //default is 0.3
        bsc.MIN_THRESHOLD = 0.9;
        bsc.setMeasure(fmeasure_LIMES);
        Set<String> measure =  new HashSet<>();
        measure.add("trigrams");
        //      measure.add("euclidean");
        //      measure.add("levenshtein");
        //      measure.add("jaccard");
        List<SimpleClassifier> cp = bsc.getBestInitialClassifiers(measure);

        if (cp.isEmpty()) {
            logger.warn("No property mapping found");
            return MappingFactory.createDefaultMapping();
        }
        //get subset of best initial classifiers

        Collections.sort(cp);
        Collections.reverse(cp);
        if(cp.size() > numberOfDimensions)
            cp = cp.subList(0, numberOfDimensions);

        ComplexClassifier cc = bsc.getZoomedHillTop(5, numberOfLinkingIterations, cp);
        AMapping map = cc.mapping.getBestOneToOneMappings(cc.mapping);
        logger.debug("Classifier: " + cc.classifiers);
        resultRecorder.setClassifier(cc.classifiers, iterationNr, sourceClass);
        resultRecorder.setPFMeasure(new PseudoRefFMeasure().calculate(map, new GoldStandard(MappingFactory
                .createDefaultMapping(), source.getAllUris(), target.getAllUris()), 1.0d), iterationNr, sourceClass);

        return map;
    }

    /**
     * Convert Jena Model to LIMES ACache
     * @param m
     * @return
     */
    public ACache modelToCache(Model m) {
        ACache c = new MemoryCache();
        for (Statement s : m.listStatements().toList()) {
            if (s.getObject().isResource()) {
                c.addTriple(s.getSubject().getURI(), s.getPredicate().getURI(), s.getObject().asResource().getURI());
            } else {
                c.addTriple(s.getSubject().getURI(), s.getPredicate().getURI(), s.getObject().asLiteral().getLexicalForm());
            }
        }
        return c;
    }


    /**
     * Return all instances which are (assumed to be) contained in the target
     * KB. Here we should apply a namespace filter on the URIs such that we get
     * only instances which are really contained in the target KB.
     *
     * @param cls
     * @return
     */
    private SortedSet<OWLIndividual> getTargetInstances(OWLClass cls) {
        logger.trace("Retrieving instances to which instances of class " + cls + " are linked to via property " + linkingProperty + "...");
        mon.start();
        SortedSet<OWLIndividual> instances = new TreeSet<>();
        String query = String.format("SELECT DISTINCT ?o WHERE {?s a <%s>. ?s <%s> ?o. FILTER(REGEX(?o,'^%s'))}", cls.toStringID(), linkingProperty, targetKB.getNamespace());
        ResultSet rs = sourceKB.executeSelect(query);
        QuerySolution qs;
        while (rs.hasNext()) {
            qs = rs.next();
            instances.add(owlDataFactory.getOWLNamedIndividual(IRI.create(qs.getResource("o").getURI())));

        }
        mon.stop();
        logger.trace("...found " + instances.size() + " instances in " + mon.getLastValue() + "ms.");
        return instances;
    }
}

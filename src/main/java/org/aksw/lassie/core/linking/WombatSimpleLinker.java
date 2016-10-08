package org.aksw.lassie.core.linking;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.result.LassieResultRecorder;
import org.aksw.limes.core.datastrutures.GoldStandard;
import org.aksw.limes.core.evaluation.qualititativeMeasures.PseudoFMeasure;
import org.aksw.limes.core.exceptions.UnsupportedMLImplementationException;
import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.cache.MemoryCache;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.ml.algorithm.*;
import org.aksw.limes.core.ml.algorithm.wombat.AWombat;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;


public class WombatSimpleLinker extends AbstractUnsupervisedLinker{

    protected static final Logger logger = Logger.getLogger(WombatSimpleLinker.class);

    protected PseudoFMeasure pseudoFMeasure = new PseudoFMeasure();

    protected Map<OWLClass, Map<OWLClassExpression, AMapping>> mappingResults = new HashMap<>();


    public WombatSimpleLinker(KnowledgeBase sourceKB, KnowledgeBase targetKB, String linkingProperty, LassieResultRecorder resultRecorder, int iterationNr){
        super(sourceKB, targetKB, linkingProperty, resultRecorder, iterationNr);
    }

    @Override
    public Multimap<OWLClass, String> link(Set<OWLClass> sourceClasses, Set<OWLClassExpression> targetClasses) {
        logger.info("Computing links...");
        logger.info("Source classes: " + sourceClasses);
        logger.info("Target classes: " + targetClasses);

        //compute the Concise Bounded Description(CBD) for each instance
        //in each source class C_i, thus create a model for each class
        Map<OWLClass, Model> sourceClassToModel = new HashMap<>();
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
        Map<OWLClassExpression, Model> targetCBDsModel = new HashMap<>();
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
        for (Entry<OWLClass, Model> entry : sourceClassToModel.entrySet()) {
            OWLClass sourceClass = entry.getKey();
            Model sourceClassModel = entry.getValue();

            ACache sourceCache = modelToCache(sourceClassModel);

            //for each D_i
            for (Entry<OWLClassExpression, Model> entry2 : targetCBDsModel.entrySet()) {
                OWLClassExpression targetClassExpression = entry2.getKey();
                Model targetClassExpressionModel = entry2.getValue();

                logger.debug("Computing links between " + sourceClass + " and " + targetClassExpression + "...");

                ACache targetCache = modelToCache(targetClassExpressionModel);

                AMapping resultMapping = null;

                //buffers the mapping results and only carries out a computation if the mapping results are unknown
                //@todo: does this code really what it should? the buffered results are never used!
                if (mappingResults.containsKey(sourceClass)) {
                    if (mappingResults.get(sourceClass).containsKey(targetClassExpression)) {
                        resultMapping = mappingResults.get(sourceClass).get(targetClassExpression);
                    }
                }
                if (resultMapping == null) {
                    resultMapping = linkUsingWombatUnsupervised(sourceCache, targetCache);

                    if(resultMapping == null){
                        System.out.println("No entity matching between " + sourceClass + " and " + targetClassExpression);
                    } else{
                        resultMapping = resultMapping.getBestOneToOneMappings(resultMapping);
                        System.out.println("Found " + resultMapping.size() + " matched entities between " + sourceClass + " and " + targetClassExpression);
                        if (!mappingResults.containsKey(sourceClass)) {
                            mappingResults.put(sourceClass, new HashMap<OWLClassExpression, AMapping>());
                        }
                        mappingResults.get(sourceClass).put(targetClassExpression, resultMapping);

                        //Keep record of the PFM
                        if(resultMapping.getSize() > 0){
                            double pfm = pseudoFMeasure.calculate(resultMapping, new GoldStandard(null, sourceCache, targetCache));
                            resultRecorder.setPFMeasure(pfm, iterationNr , sourceClass);
                            resultRecorder.setInstanceMapping(resultMapping, iterationNr, sourceClass);
                        }
                        for (Entry<String, HashMap<String, Double>> mappingEntry : resultMapping.getMap().entrySet()) {
                            HashMap<String, Double> value = mappingEntry.getValue();
                            map.put(sourceClass, value.keySet().iterator().next());
                        }
                    }
                }
            }
        }
        return map;
    }

    public static class OWLClassFragmentContainer<T extends OWLClassExpression> {

        private final T owlClassExpression;
        private final Model model;
        private final ACache cache;

        public OWLClassFragmentContainer(T owlClassExpression, Model model) {
            this(owlClassExpression, model, modelToCache(model));
        }

        public OWLClassFragmentContainer(T owlClassExpression, Model model, ACache cache) {
            this.owlClassExpression = owlClassExpression;
            this.model = model;
            this.cache = cache;
        }

        public ACache getCache() {
            return cache;
        }

        public Model getModel() {
            return model;
        }

        public T getOwlClassExpression() {
            return owlClassExpression;
        }
    }

    public static class CBDTask<T extends OWLClassExpression> implements Callable<List<OWLClassFragmentContainer<T>>> {

        private KnowledgeBase kb;
        private Set<T> sourceClasses;
        private int depth;

        public CBDTask(KnowledgeBase kb, Set<T> sourceClasses, int depth) {
            this.kb = kb;
            this.sourceClasses = sourceClasses;
            this.depth = depth;
        }

        public List<OWLClassFragmentContainer<T>> call() {
            List<OWLClassFragmentContainer<T>> sourceClassToModel = new ArrayList<>(sourceClasses.size());
            for (T sourceClass : sourceClasses) {
                //get all instances of C_i
                SortedSet<OWLIndividual> sourceInstances = kb.getInstances(sourceClass);
                //get the fragment describing the instances of C_i
                logger.debug("Computing fragment...");
                Model sourceFragment = kb.getFragment(sourceInstances, depth);
                removeNonStringLiteralStatements(sourceFragment);
                logger.debug("...got " + sourceFragment.size() + " triples.");
                sourceClassToModel.add(new OWLClassFragmentContainer<T>(sourceClass, sourceFragment));
            }
            return sourceClassToModel;
        }
    }

    public static class LinkingResult {
        private OWLClassFragmentContainer<OWLClass> source;
        private OWLClassFragmentContainer<OWLClassExpression> target;
        private AMapping mapping;

        public LinkingResult(OWLClassFragmentContainer<OWLClass> source, OWLClassFragmentContainer<OWLClassExpression> target, AMapping mapping) {
            this.source = source;
            this.target = target;
            this.mapping = mapping;
        }
    }

    public static class LinkingTask implements Callable<LinkingResult> {

        private OWLClassFragmentContainer<OWLClass> source;
        private OWLClassFragmentContainer<OWLClassExpression> target;

        public LinkingTask(OWLClassFragmentContainer<OWLClass> source, OWLClassFragmentContainer<OWLClassExpression> target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public LinkingResult call() throws Exception {
            return new LinkingResult(source, target, linkUsingWombatUnsupervised(source.getCache(), target.getCache()));
        }
    }

    @Override
    public Multimap<OWLClass, String> linkMultiThreaded(
            Set<OWLClass> sourceClasses,
            Set<OWLClassExpression> targetClasses) {
        Multimap<OWLClass, String> map = HashMultimap.create();
        // subdivide sourceClasses for parallelization
        int numThreads = Runtime.getRuntime().availableProcessors() - 1;
        List<Set<OWLClass>> sources = divideForParallel(sourceClasses, numThreads);
        List<Set<OWLClassExpression>> targets = divideForParallel(targetClasses, numThreads);
        List<Future<List<OWLClassFragmentContainer<OWLClass>>>> sources2 = new ArrayList<>();
        List<Future<List<OWLClassFragmentContainer<OWLClassExpression>>>> targets2 = new ArrayList<>();
        List<Future<LinkingResult>> result = new ArrayList<>();
        List<OWLClassFragmentContainer<OWLClassExpression>> mergedTarget = new ArrayList<>();
        ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(numThreads);
        // get CBDs for source
        for (Set<OWLClass> s : sources) {
            sources2.add(threadPool.schedule(new CBDTask<>(sourceKB, s, FRAGMENT_DEPTH), 0l, TimeUnit.SECONDS));
        }
        // get CBDs for target
        for (Set<OWLClassExpression> t : targets) {
            targets2.add(threadPool.schedule(new CBDTask<>(targetKB, t, FRAGMENT_DEPTH), 0l, TimeUnit.SECONDS));
        }
        try {
            threadPool.shutdown();
            threadPool.awaitTermination(5, TimeUnit.HOURS);
            // merge targets2
            for (Future<List<OWLClassFragmentContainer<OWLClassExpression>>> future : targets2) {
                List<OWLClassFragmentContainer<OWLClassExpression>> m = future.get();
                mergedTarget.addAll(m);
            }
            //for each s in source2 run linkingtask
            threadPool = Executors.newScheduledThreadPool(numThreads);

            for (Future<List<OWLClassFragmentContainer<OWLClass>>> future : sources2) {
                for (OWLClassFragmentContainer<OWLClass> fragment : future.get()) {
                    OWLClass sourceClass = fragment.getOwlClassExpression();
                    for (OWLClassFragmentContainer<OWLClassExpression> fragment2 : mergedTarget) {
                        OWLClassExpression targetClassExpression = fragment2.getOwlClassExpression();
                        //buffers the mapping results and only carries out a computation if the mapping results are unknown
                        if (mappingResults.containsKey(sourceClass) && mappingResults.get(sourceClass).containsKey(targetClassExpression))
                            for (Entry<String, HashMap<String, Double>> mappingEntry : mappingResults.get(sourceClass).get(targetClassExpression).getMap().entrySet())
                                map.put(sourceClass, mappingEntry.getValue().keySet().iterator().next());
                        else {
                            result.add(threadPool.schedule(new LinkingTask(fragment, fragment2), 0, TimeUnit.SECONDS));
                        }
                    }
                }
            }
            threadPool.shutdown();
            threadPool.awaitTermination(5, TimeUnit.HOURS);

            for (Future<LinkingResult> resultFuture : result) {
                LinkingResult res = resultFuture.get();
                AMapping resultMapping = res.mapping;
                OWLClassFragmentContainer<OWLClass> source = res.source;
                OWLClassFragmentContainer<OWLClassExpression> target = res.target;
                //Keep record of the PFM
                if (resultMapping == null) {
                    System.out.println("No entity matching between " + source.getOwlClassExpression() + " and " + target.getOwlClassExpression());
                } else {
                    resultMapping = resultMapping.getBestOneToOneMappings(resultMapping);
                    if (!mappingResults.containsKey(source.getOwlClassExpression())) {
                        mappingResults.put(source.getOwlClassExpression(), new HashMap<OWLClassExpression, AMapping>());
                    }
                    mappingResults.get(source.getOwlClassExpression()).put(target.getOwlClassExpression(), resultMapping);
                }
                if (resultMapping.getSize() > 0) {
                    double pfm = pseudoFMeasure.calculate(resultMapping, new GoldStandard(null, source.getCache(), target.getCache()));
                    resultRecorder.setPFMeasure(pfm, iterationNr, source.getOwlClassExpression());
                    resultRecorder.setInstanceMapping(resultMapping, iterationNr, source.getOwlClassExpression());
                }
                for (Entry<String, HashMap<String, Double>> mappingEntry : resultMapping.getMap().entrySet()) {
                    HashMap<String, Double> value = mappingEntry.getValue();
                    map.put(source.getOwlClassExpression(), value.keySet().iterator().next());
                }
            }
        } catch (InterruptedException|ExecutionException e) {
            e.printStackTrace();
        }
        return map;
    }

    private <T> List<Set<T>> divideForParallel (Set<T> collection, int numThreads) {
        // subdivide sourceClasses for parallelization
        int subsetSize = collection.size() / numThreads;
        int i = 0;
        Set<T> subset = new HashSet<>();
        List<Set<T>> container = new ArrayList<>();
        for (T t : collection) {
            if (i != 0 && i % subsetSize == 0 && collection.size() - i > subsetSize) {
                container.add(subset);
                subset = new HashSet<>();
            }
            subset.add(t);
            i++;
        }
        container.add(subset);
        return container;
    }


    private static AMapping linkUsingWombatUnsupervised(ACache sourceCache, ACache targetCache) {
        UnsupervisedMLAlgorithm wombatSimpleU = null;
        try {
            wombatSimpleU = MLAlgorithmFactory.createMLAlgorithm(WombatSimple.class,
                    MLImplementationType.UNSUPERVISED).asUnsupervised();
        } catch (UnsupportedMLImplementationException e) {
            e.printStackTrace();
        }
        wombatSimpleU.setParameter(AWombat.PARAMETER_MIN_PROPERTY_COVERAGE, 1.0);
        wombatSimpleU.setParameter(AWombat.PARAMETER_ATOMIC_MEASURES, "trigrams");
        wombatSimpleU.init(wombatSimpleU.getParameters() , sourceCache, targetCache);
        MLResults mlResults = null;
        try {
            mlResults = wombatSimpleU.learn(new PseudoFMeasure());
        } catch (UnsupportedMLImplementationException e) {
            e.printStackTrace();
        }
        AMapping resultMap = null;
        if(mlResults != null){
            resultMap = wombatSimpleU.predict(sourceCache, targetCache, mlResults);
        }
        return resultMap;
    }

    /**
     * Convert Jena Model to LIMES Cache
     * @param m
     * @return
     */
    public static ACache modelToCache(Model m) {
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

}

package org.aksw.lassie.core.linking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.result.LassieResultRecorder;
import org.aksw.limes.core.datastrutures.GoldStandard;
import org.aksw.limes.core.evaluation.qualititativeMeasures.PseudoFMeasure;
import org.aksw.limes.core.exceptions.UnsupportedMLImplementationException;
import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.cache.MemoryCache;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.ml.algorithm.LearningParameter;
import org.aksw.limes.core.ml.algorithm.MLAlgorithmFactory;
import org.aksw.limes.core.ml.algorithm.MLImplementationType;
import org.aksw.limes.core.ml.algorithm.MLResults;
import org.aksw.limes.core.ml.algorithm.UnsupervisedMLAlgorithm;
import org.aksw.limes.core.ml.algorithm.WombatSimple;
import org.aksw.limes.core.ml.algorithm.wombat.AWombat;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.jamonapi.MonitorFactory;


public class WombatLinker extends AbstractUnsupervisedLinker{

    protected static final Logger logger = Logger.getLogger(WombatLinker.class);
    
    protected PseudoFMeasure pseudoFMeasure = new PseudoFMeasure();
    
    protected Map<OWLClass, Map<OWLClassExpression, AMapping>> mappingResults = new HashMap<>();

    public static int iterationNr = 0;
    
    public WombatLinker(KnowledgeBase sourceKB, KnowledgeBase targetKB, String linkingProperty, LassieResultRecorder resultRecorder){
        this.sourceKB = sourceKB;
        this.targetKB = targetKB; 

        this.linkingProperty = linkingProperty;

        mon = MonitorFactory.getTimeMonitor("time");

        this.resultRecorder = resultRecorder;
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

                AMapping result = null;

                //buffers the mapping results and only carries out a computation if the mapping results are unknown
                if (mappingResults.containsKey(sourceClass)) {
                    if (mappingResults.get(sourceClass).containsKey(targetClassExpression)) {
                        result = mappingResults.get(sourceClass).get(targetClassExpression);
                    }
                }

                if (result == null) {
                    result = linkUsingWombatUnsupervised(sourceCache, targetCache);
                    if (!mappingResults.containsKey(sourceClass)) {
                        mappingResults.put(sourceClass, new HashMap<OWLClassExpression, AMapping>());
                    }
                    mappingResults.get(sourceClass).put(targetClassExpression, result);
                }

                //Keep record of the real F-Measures
                if(result.getSize() > 0){
                    double pfm = pseudoFMeasure.calculate(result, new GoldStandard(null, sourceCache, targetCache));
                    resultRecorder.setFMeasure(pfm, iterationNr , sourceClass);
                    resultRecorder.setInstanceMapping(result, iterationNr, sourceClass);
                }

                for (Entry<String, HashMap<String, Double>> mappingEntry : result.getMap().entrySet()) {
                    HashMap<String, Double> value = mappingEntry.getValue();
                    map.put(sourceClass, value.keySet().iterator().next());
                }
            }
        }
        iterationNr++;
        return map;
    }



    @Override
    public Multimap<OWLClass, String> linkMultiThreaded(
            Set<OWLClass> sourceClasses,
            Collection<OWLClassExpression> targetClasses) {
        return null;
    }

    private AMapping linkUsingWombatUnsupervised(ACache sourceCache, ACache targetCache) {
        UnsupervisedMLAlgorithm wombatSimpleU = null;
        try {
            wombatSimpleU = MLAlgorithmFactory.createMLAlgorithm(WombatSimple.class,
                    MLImplementationType.UNSUPERVISED).asUnsupervised();
        } catch (UnsupportedMLImplementationException e) {
            e.printStackTrace();
        }
        wombatSimpleU.setParameter(AWombat.PARAMETER_MIN_PROPERTY_COVERAGE, 1.0);
        wombatSimpleU.init(null , sourceCache, targetCache);
        MLResults mlModel = null;
        try {
            mlModel = wombatSimpleU.learn(new PseudoFMeasure());
        } catch (UnsupportedMLImplementationException e) {
            e.printStackTrace();
        }
        AMapping resultMap = wombatSimpleU.predict(sourceCache, targetCache, mlModel);
        return resultMap;
    } 
    
    /**
     * Convert Jena Model to LIMES Cache
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

}

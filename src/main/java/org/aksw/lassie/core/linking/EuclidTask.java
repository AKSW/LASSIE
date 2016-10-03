package org.aksw.lassie.core.linking;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.aksw.limes.core.evaluation.evaluator.EvaluatorFactory;
import org.aksw.limes.core.evaluation.evaluator.EvaluatorType;
import org.aksw.limes.core.evaluation.qualititativeMeasures.IQualitativeMeasure;
import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.ml.algorithm.euclid.ComplexClassifier;
import org.aksw.limes.core.ml.algorithm.euclid.MeshBasedSelfConfigurator;
import org.aksw.limes.core.ml.algorithm.euclid.SimpleClassifier;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;

public class EuclidTask implements Callable<LinkingResult>{
    protected static final Logger logger = Logger.getLogger(EuclidTask.class);


    protected OWLClass source;
    protected OWLClassExpression target;
    protected ACache sourceCache;
    protected ACache targetCache;


    public IQualitativeMeasure F_MEASURE = EvaluatorFactory.create(EvaluatorType.PF_MEASURE);
    public double COVERAGE = 0.8;
    public int ITER_COUNT = 2;
    protected static final int DIM_COUNT = 7;

    /**
     *
     */
    public EuclidTask(OWLClass source, OWLClassExpression target, ACache sourceCache, ACache targetCache) {
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


        MeshBasedSelfConfigurator bsc = new MeshBasedSelfConfigurator(sourceCache, targetCache, COVERAGE);
        //ensures that only the threshold 1.0 is tested. Can be set to a lower value
        //default is 0.3
        bsc.MIN_THRESHOLD = 0.6;

        bsc.setMeasure(F_MEASURE);
        Set<String> measure = new HashSet<>();
        measure.add("trigrams");
        List<SimpleClassifier> cp = bsc.getBestInitialClassifiers(measure);

        if (cp.isEmpty()) {
            logger.warn("No property mapping found");
            return new LinkingResult(source, target, MappingFactory.createDefaultMapping());
        }
        //get subset of best initial classifiers

        Collections.sort(cp);
        Collections.reverse(cp);
        if(cp.size() > DIM_COUNT)
            cp = cp.subList(0, DIM_COUNT);

        ComplexClassifier cc = bsc.getZoomedHillTop(5, ITER_COUNT , cp);
        AMapping map = cc.mapping.getBestOneToOneMappings(cc.mapping);
        logger.info("AMapping size is " + map.getNumberofMappings());
        logger.info("Pseudo F-measure is " + cc.fMeasure);
        return new LinkingResult(source, target, map);
    }

}



package org.aksw.lassie.core.linking;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.aksw.lassie.core.SimpleClassifierComparator;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.selfconfig.ComplexClassifier;
import de.uni_leipzig.simba.selfconfig.MeshBasedSelfConfigurator;
import de.uni_leipzig.simba.selfconfig.SimpleClassifier;

public class EuclidTask implements Callable<LinkingResult>{
    protected static final Logger logger = Logger.getLogger(EuclidTask.class);
    

    protected OWLClass source;
    protected OWLClassExpression target;
    protected Cache sourceCache;
    protected Cache targetCache;
    
    
    public String F_MEASURE = "own";
    public double COVERAGE = 0.8;
    public double BETA = 1d;
    public int ITER_COUNT = 2;
    protected static final int DIM_COUNT = 7;

    /**
     * 
     */
    public EuclidTask(OWLClass source, OWLClassExpression target, Cache sourceCache, Cache targetCache) {
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


        MeshBasedSelfConfigurator bsc = new MeshBasedSelfConfigurator(sourceCache, targetCache, COVERAGE, BETA);
        //ensures that only the threshold 1.0 is tested. Can be set to a lower value
        //default is 0.3
        bsc.MIN_THRESHOLD = 0.6;
        
        bsc.setMeasure(F_MEASURE);
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
        if(cp.size() > DIM_COUNT)
            cp = cp.subList(0, DIM_COUNT);

        ComplexClassifier cc = bsc.getZoomedHillTop(5, ITER_COUNT , cp);
        Mapping map = Mapping.getBestOneToOneMappings(cc.mapping);
        logger.info("Mapping size is " + map.getNumberofMappings());
        logger.info("Pseudo F-measure is " + cc.fMeasure);
        return new LinkingResult(source, target, map);
    }

}



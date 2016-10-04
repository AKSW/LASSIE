package org.aksw.lassie.core.linking;

import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.result.LassieResultRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnsupervisedLinkerFactory {
    private static final Logger logger = LoggerFactory.getLogger(UnsupervisedLinkerFactory.class);


    public static  UnsupervisedLinker createUnsupervisedLinker(LinkerType linkerType, 
            KnowledgeBase sourceKB, KnowledgeBase targetKB, String linkingProperty, 
            LassieResultRecorder resultRecorder, int iterationNr) {
        
        logger.info("Creating UnsupervisedLinker of type: " + linkerType.toString());
        
        switch(linkerType){
            case EUCLID:
                return new EuclidLinker(sourceKB, targetKB, linkingProperty, resultRecorder, iterationNr);
            case WOMBAT_SIMPLE:
            default:
                return new WombatSimpleLinker(sourceKB, targetKB, linkingProperty, resultRecorder, iterationNr);
        } 
    }

}

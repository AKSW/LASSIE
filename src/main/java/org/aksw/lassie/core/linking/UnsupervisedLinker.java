package org.aksw.lassie.core.linking;

import java.util.Set;

import org.aksw.limes.core.io.mapping.AMapping;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;

import com.google.common.collect.Multimap;

public interface UnsupervisedLinker {

    Multimap<OWLClass, String> link(Set<OWLClass> sourceClasses, Set<OWLClassExpression> targetClasses);
    Multimap<OWLClass, String> linkMultiThreaded(Set<OWLClass> sourceClasses, Set<OWLClassExpression> targetClasses);
    
    public AMapping getOracleMapping();

    public void setOracleMapping(AMapping oracleMapping); 
}

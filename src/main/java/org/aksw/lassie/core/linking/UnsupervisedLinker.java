package org.aksw.lassie.core.linking;

import java.util.Collection;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;

import com.google.common.collect.Multimap;

public interface UnsupervisedLinker {

    abstract public Multimap<OWLClass, String> link(Set<OWLClass> sourceClasses, Collection<OWLClassExpression> targetClasses);
    abstract public Multimap<OWLClass, String> linkMultiThreaded(Set<OWLClass> sourceClasses, Collection<OWLClassExpression> targetClasses);
}
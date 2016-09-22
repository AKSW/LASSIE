package org.aksw.lassie.core.linking;

import java.util.Collection;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;

import com.google.common.collect.Multimap;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public abstract class AbstractUnsupervisedLinker {
    protected OWLDataFactory owlDataFactory = new OWLDataFactoryImpl();
    
    abstract public Multimap<OWLClass, String> link(Set<OWLClass> sourceClasses, Collection<OWLClassExpression> targetClasses);
    abstract public Multimap<OWLClass, String> linkMultiThreaded(Set<OWLClass> sourceClasses, Collection<OWLClassExpression> targetClasses);
}

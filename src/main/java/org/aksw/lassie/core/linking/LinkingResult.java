package org.aksw.lassie.core.linking;

import org.aksw.limes.core.io.mapping.AMapping;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;


public class LinkingResult {
    public AMapping mapping;
    public OWLClass source;
    public OWLClassExpression target;

    public LinkingResult(OWLClass source, OWLClassExpression target, AMapping mapping) {
        this.source = source;
        this.target = target;
        this.mapping = mapping;
    }
}
package org.aksw.lassie.core.linking;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;

import de.uni_leipzig.simba.data.Mapping;

public class LinkingResult {
    public Mapping mapping;
    public OWLClass source;
    public OWLClassExpression target;

    public LinkingResult(OWLClass source, OWLClassExpression target, Mapping mapping) {
        this.source = source;
        this.target = target;
        this.mapping = mapping;
    }
}
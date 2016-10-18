package org.aksw.lassie.kb;

import java.util.SortedSet;

import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.log4j.Logger;
import org.dllearner.core.ComponentInitException;
import org.dllearner.kb.LocalModelBasedSparqlEndpointKS;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGenerator;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGeneratorImpl;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.model.OWLIndividual;


public class LocalKnowledgeBase extends AbstractKnowledgeBase {

    protected static final Logger logger = Logger.getLogger(LocalKnowledgeBase.class);


    private Model model;

    public LocalKnowledgeBase(Model model) throws ComponentInitException {
        this(model, null);
    }

    public LocalKnowledgeBase(Model model, String namespace) throws ComponentInitException {
        this.model = model;
        this.namespace = namespace;

        LocalModelBasedSparqlEndpointKS localModelBasedSparqlEndpointKS = new LocalModelBasedSparqlEndpointKS(model);
        localModelBasedSparqlEndpointKS.init();
        reasoner = new SPARQLReasoner(localModelBasedSparqlEndpointKS);
        reasoner.init();
    }

    public Model getModel() {
        return model;
    }

    @Override
    public ResultSet executeSelect(String query) {		
        ResultSet rs = QueryExecutionFactory.create(query, getModel()).execSelect();
        return rs;
    }

    @Override
    public Model executeConstruct(String query) {
        Model model = QueryExecutionFactory.create(query, getModel()).execConstruct();
        return model;
    }

    @Override
    public boolean isRemote() {
        return false;
    }


    /**
     * Computes a fragment containing hopefully useful information about the
     * resources.
     *
     * @param ind
     */
    @Override
    public Model getFragment(SortedSet<OWLIndividual> individuals, int recursionDepth) {
        //        OntModel fullFragment = ModelFactory.createOntologyModel();
        Model fullFragment = ModelFactory.createDefaultModel();
        Model fragment;
        for (OWLIndividual ind : individuals) {
            fragment = getFragment(ind, recursionDepth);
            fullFragment.add(fragment);
        }
        //        cleanUpModel(fullFragment);
        return fullFragment;
    }


    /**
     * Computes a fragment containing hopefully useful information about the
     * resource.
     *
     * @param ind
     */
    @Override
    public Model getFragment(OWLIndividual ind, int recursionDepth) {
        logger.trace("Loading fragment for " + ind.toStringID());
        ConciseBoundedDescriptionGenerator cbdGen;

        logger.debug("Quering local KB");
        cbdGen = new ConciseBoundedDescriptionGeneratorImpl(getModel());

        Model cbd = ModelFactory.createDefaultModel();
        try {
            cbd = cbdGen.getConciseBoundedDescription(ind.toStringID(), 1, true);
        } catch (Exception e) {
            logger.error("Exception: " + e);
        }
        logger.trace("Got " + cbd.size() + " triples.");
        return cbd;
    }
    

    

}

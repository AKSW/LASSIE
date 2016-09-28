package org.aksw.lassie.kb;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.SortedSet;

import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.log4j.Logger;
import org.dllearner.core.ComponentInitException;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGenerator;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGeneratorImpl;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.model.OWLIndividual;



public class RemoteKnowledgeBase extends AbstractKnowledgeBase {
    
    protected static final Logger logger = Logger.getLogger(RemoteKnowledgeBase.class);


	private SparqlEndpoint endpoint;
	private ExtractionDBCache cache;

	public RemoteKnowledgeBase(SparqlEndpoint endpoint) throws ComponentInitException {
		this(endpoint, null, null);
	}
	
	public RemoteKnowledgeBase(SparqlEndpoint endpoint, ExtractionDBCache cache) throws ComponentInitException {
		this(endpoint, cache, null);
	}
	
	public RemoteKnowledgeBase(SparqlEndpoint endpoint, String namespace) {
		this.endpoint = endpoint;
		this.namespace = namespace;

		reasoner = new SPARQLReasoner(new SparqlEndpointKS(endpoint));
	}
	
	public RemoteKnowledgeBase(SparqlEndpoint endpoint, ExtractionDBCache cache, String namespace) throws ComponentInitException {
		this.endpoint = endpoint;
		this.cache = cache;
		this.namespace = namespace;

		SparqlEndpointKS ks = new SparqlEndpointKS(endpoint);
		ks.init();
        reasoner = new SPARQLReasoner(ks);
		reasoner.init();
	}

	public SparqlEndpoint getEndpoint() {
		return endpoint;
	}
	
	public ExtractionDBCache getCache() {
		return cache;
	}

	@Override
	public ResultSet executeSelect(String query) {
		ResultSet rs;
		if(cache != null){
			rs = SparqlQuery.convertJSONtoResultSet(cache.executeSelectQuery(endpoint, query));
		} else {
			QueryEngineHTTP qe = new QueryEngineHTTP(endpoint.getURL().toString(), query);
			for (String uri : endpoint.getDefaultGraphURIs()) {
				qe.addDefaultGraph(uri);
			}
			for (String uri : endpoint.getNamedGraphURIs()) {
				qe.addNamedGraph(uri);
			}
			rs = qe.execSelect();
			
		}
		return rs;
	}

	@Override
	public Model executeConstruct(String query) {
		Model model = null;
		if(cache != null){
			try {
				model = cache.executeConstructQuery(endpoint, query);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			QueryEngineHTTP qe = new QueryEngineHTTP(endpoint.getURL().toString(), query);
			for (String uri : endpoint.getDefaultGraphURIs()) {
				qe.addDefaultGraph(uri);
			}
			for (String uri : endpoint.getNamedGraphURIs()) {
				qe.addNamedGraph(uri);
			}
			model = qe.execConstruct();
		}
		return model;
	}

	@Override
	public boolean isRemote() {
		return true;
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
        
            logger.debug("Quering remote KB");
            if (getCache() != null) {
                String cacheDir = getCache().getCacheDirectory();
                SparqlEndpoint endPoint = getEndpoint();
                cbdGen = new ConciseBoundedDescriptionGeneratorImpl(endPoint , cacheDir);
            } else {
                cbdGen = new ConciseBoundedDescriptionGeneratorImpl(getEndpoint());
            }
        Model cbd = ModelFactory.createDefaultModel();
        try {
            cbd = cbdGen.getConciseBoundedDescription(ind.toStringID(), 1, true);
        } catch (Exception e) {
            logger.error("End Point(" + getEndpoint().toString() + ") Exception: " + e);
        }
        logger.trace("Got " + cbd.size() + " triples.");
        return cbd;
    }
}

package org.aksw.lassie.kb;

import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.log4j.Logger;
import org.dllearner.reasoning.SPARQLReasoner;
import org.dllearner.utilities.owl.OWLClassExpressionToSPARQLConverter;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public abstract class AbstractKnowledgeBase implements KnowledgeBase {
    protected static final Logger logger = Logger.getLogger(AbstractKnowledgeBase.class);
    protected Monitor mon = MonitorFactory.getTimeMonitor("time");
    protected OWLDataFactory owlDataFactory = new OWLDataFactoryImpl();
	protected SPARQLReasoner reasoner;
	protected String namespace;

	@Override
	public SPARQLReasoner getReasoner() {
		return reasoner;
	}
	
	@Override
	public String getNamespace() {
		return namespace;
	}
	
	@Override
    public SortedSet<OWLIndividual> getInstances(OWLClassExpression desc) {
        logger.trace("Retrieving instances of class expression " + desc + "...");
        mon.start();
        SortedSet<OWLIndividual> instances = new TreeSet<>();
        OWLClassExpressionToSPARQLConverter converter = new OWLClassExpressionToSPARQLConverter();
        Query query = converter.asQuery("?x", desc);
        ResultSet rs = executeSelect(query.toString());
        QuerySolution qs;
        while (rs.hasNext()) {
            qs = rs.next();
            instances.add(owlDataFactory.getOWLNamedIndividual(IRI.create(qs.getResource("x").getURI())));
        }
        mon.stop();
        logger.trace("...found " + instances.size() + " instances in " + mon.getLastValue() + "ms.");
        return instances;
    }


}

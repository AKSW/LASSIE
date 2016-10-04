package org.aksw.lassie.core.linking;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.result.LassieResultRecorder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public abstract class AbstractUnsupervisedLinker implements UnsupervisedLinker {
    protected static final Logger logger = Logger.getLogger(AbstractUnsupervisedLinker.class);
    
    protected final int FRAGMENT_DEPTH = 2;
    
    protected OWLDataFactory owlDataFactory = new OWLDataFactoryImpl();
    
    protected Monitor mon = MonitorFactory.getTimeMonitor("Time");
    
    protected KnowledgeBase sourceKB;
    protected KnowledgeBase targetKB;
    
    protected String linkingProperty = OWL.sameAs.getURI();
    
    protected LassieResultRecorder resultRecorder;

    protected int iterationNr;
    
    public AbstractUnsupervisedLinker(KnowledgeBase sourceKB, KnowledgeBase targetKB, String linkingProperty, LassieResultRecorder resultRecorder, int iterationNr){
        this.sourceKB = sourceKB;
        this.targetKB = targetKB; 
        this.linkingProperty = linkingProperty;
        this.resultRecorder = resultRecorder;
        this.iterationNr = iterationNr;
    }
    
    
    /**
     * Return all instances of the given class in the source KB.
     *
     * @param cls
     * @return
     */
    protected SortedSet<OWLIndividual> getSourceInstances(OWLClass cls) {
        logger.debug("Retrieving instances of class " + cls + "...");
        mon.start();
        SortedSet<OWLIndividual> instances = new TreeSet<>();
        String query = String.format("SELECT DISTINCT ?s WHERE {?s a <%s>}", cls.toStringID());
        ResultSet rs = sourceKB.executeSelect(query);
        QuerySolution qs;
        while (rs.hasNext()) {
            qs = rs.next();
            instances.add(owlDataFactory.getOWLNamedIndividual(IRI.create(qs.getResource("s").getURI())));
        }
        mon.stop();
        logger.debug("...found " + instances.size() + " instances in " + mon.getLastValue() + "ms.");
        return instances;
    }

    protected void removeNonStringLiteralStatements(Model m) {
        StmtIterator iterator = m.listStatements();
        List<Statement> statements2Remove = new ArrayList<Statement>();
        while (iterator.hasNext()) {
            Statement st = iterator.next();
//            System.out.println(st);
            if (!st.getObject().isLiteral() ){
//                    || !(st.getObject().asLiteral().getDatatype() == null || 
//                    st.getObject().asLiteral().getDatatypeURI().equals(XSDDatatype.XSDstring.getURI()))){
                statements2Remove.add(st);
            }
        }
        m.remove(statements2Remove);
    }
}

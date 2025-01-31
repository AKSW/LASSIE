package org.aksw.lassie;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.lassie.bmGenerator.BenchmarkGenerator;
import org.aksw.lassie.core.LASSIEController;
import org.aksw.lassie.core.exceptions.NonExistingLinksException;
import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.kb.LocalKnowledgeBase;
import org.aksw.lassie.kb.RemoteKnowledgeBase;
import org.aksw.lassie.result.LassieResultRecorder;
import org.aksw.lassie.util.PrintUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.StringRenderer;
import org.dllearner.kb.extraction.ExtractionAlgorithm;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlKnowledgeSource;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;

import com.google.common.collect.Sets;

//import cern.colt.Arrays;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class OntologyMatchingTest {

    private KnowledgeBase dbpedia;
    private KnowledgeBase worldFactBook;
    private KnowledgeBase openCyc;
    private KnowledgeBase linkedGeoData;

    private final int fragmentDepth = 3;

    protected OWLDataFactory owlDataFactory = new OWLDataFactoryImpl();

    @Before
    public void setUp() throws Exception {
        // render output
        StringRenderer.setRenderer(StringRenderer.Rendering.MANCHESTER_SYNTAX.getRenderer());

        // set logging properties
        Logger.getLogger(SparqlKnowledgeSource.class).setLevel(Level.WARN);
        Logger.getLogger(ExtractionAlgorithm.class).setLevel(Level.WARN);
        Logger.getLogger(org.dllearner.kb.extraction.Manager.class).setLevel(Level.WARN);
        Logger.getRootLogger().removeAllAppenders();
        Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout("%m%n")));
        //		Logger.getRootLogger().setLevel(Level.DEBUG);

        //DBpedia
        SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
        ExtractionDBCache cache = new ExtractionDBCache("cache");
        String namespace = "http://dbpedia.org/resource/";
        dbpedia = new RemoteKnowledgeBase(endpoint, cache, namespace);

        //World Factbook
        //TODO problem with World Factbook is that old FU Berlin server is useless because of bugs and current version
        //is provide by University Of Mannheim now with another namespace http://wifo5-03.informatik.uni-mannheim.de/factbook/resource/
        //but the DBpedia links are still to the old D2R server instance
        //workaround: replace namespace before learning
        endpoint = new SparqlEndpoint(new URL("http://wifo5-03.informatik.uni-mannheim.de/factbook/sparql"));
        cache = new ExtractionDBCache("cache");
        namespace = "http://www4.wiwiss.fu-berlin.de/factbook/resource/";
        worldFactBook = new RemoteKnowledgeBase(endpoint, cache, namespace);

        //local OpenCyc
        endpoint = new SparqlEndpoint(new URL("http://localhost:8890/sparql"));
        cache = new ExtractionDBCache("cache");
        namespace = "http://sw.cyc.com";
        openCyc = new RemoteKnowledgeBase(endpoint, cache, namespace);

        //LinkedGeoData
        endpoint = new SparqlEndpoint(new URL("http://linkedgeodata.org/sparql"));
        cache = new ExtractionDBCache("cache");
        namespace = "http://linkedgeodata.org/triplify/";
        linkedGeoData = new RemoteKnowledgeBase(endpoint, cache, namespace);
    }

    @Test
    public void testDBpediaWorldFactbook() {
//        LASSIEController matcher = new LASSIEController(dbpedia, worldFactBook);
        //		matcher.run();
    }

    @Test
    public void testDBpediaOpenCyc() {
//        LASSIEController matcher = new LASSIEController(dbpedia, openCyc);
        //		matcher.run();
    }

    @Test
    public void testDBpediaLinkedGeoData() {
//        LASSIEController matcher = new LASSIEController(dbpedia, linkedGeoData);
        //		matcher.run();
    }

    @Test
    public void testDBpediaLinkedGeoData2() {
        Set<OWLClass> sourceClasses = Sets.newHashSet(
                owlDataFactory.getOWLClass(IRI.create("http://dbpedia.org/ontology/City")),
                owlDataFactory.getOWLClass(IRI.create("http://dbpedia.org/ontology/Mountain")),
                owlDataFactory.getOWLClass(IRI.create("http://dbpedia.org/ontology/River"))
                );
        Set<OWLClass> targetClasses = Sets.newHashSet(
                owlDataFactory.getOWLClass(IRI.create("http://linkedgeodata.org/ontology/Village")),
                owlDataFactory.getOWLClass(IRI.create("http://linkedgeodata.org/ontology/City")),
                owlDataFactory.getOWLClass(IRI.create("http://linkedgeodata.org/ontology/River")),
                owlDataFactory.getOWLClass(IRI.create("http://linkedgeodata.org/ontology/Peak"))
                );
        LASSIEController matcher = new LASSIEController(dbpedia, linkedGeoData, sourceClasses);
        matcher.run(sourceClasses, targetClasses, false);
    }

    @Test
    public void testSingleClassOpenCycToDBpedia() throws ComponentInitException {
        OWLClass nc = owlDataFactory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx4r4fYeXvbPQdiKtoNafhmOew"));
        Set<OWLClass> sourceClasses = new HashSet<>(Arrays.asList(nc));
        LASSIEController matcher = new LASSIEController(openCyc, dbpedia, sourceClasses);
        try {
            List<? extends EvaluatedDescription> mapping = matcher.computeMappings(nc, false);
            Map<OWLClassExpression, List<? extends EvaluatedDescription>> alignment = new HashMap<OWLClassExpression, List<? extends EvaluatedDescription>>();
            alignment.put(nc, mapping);
            System.out.println(PrintUtils.toHTMLWithLabels(alignment, openCyc, dbpedia));
        } catch (NonExistingLinksException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSingleClassLinkedGeoDataToDBpedia() throws ComponentInitException {
        OWLClass nc = owlDataFactory.getOWLClass(IRI.create("http://linkedgeodata.org/ontology/Aerodrome"));
        Set<OWLClass> sourceClasses = new HashSet<>(Arrays.asList(nc));
        LASSIEController matcher = new LASSIEController(linkedGeoData, dbpedia, sourceClasses);
        try{
            List<? extends EvaluatedDescription> mapping = matcher.computeMappings(nc, false);
            Map<OWLClassExpression, List<? extends EvaluatedDescription>> alignment = new HashMap<>();
            alignment.put(nc, mapping);
            System.out.println(PrintUtils.toHTMLWithLabels(alignment, linkedGeoData, dbpedia));
        } catch (NonExistingLinksException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSingleClassDBpediaToLinkedGeoData() throws ComponentInitException {
        OWLClass nc = owlDataFactory.getOWLClass(IRI.create("http://dbpedia.org/ontology/AdministrativeRegion"));
        Set<OWLClass> sourceClasses = new HashSet<>(Arrays.asList(nc));
        LASSIEController matcher = new LASSIEController(dbpedia, linkedGeoData, sourceClasses);
        try {
            List<? extends EvaluatedDescription> mapping = matcher.computeMappings(nc, false);
            Map<OWLClassExpression, List<? extends EvaluatedDescription>> alignment = new HashMap<>();
            alignment.put(nc, mapping);
            System.out.println(PrintUtils.toHTMLWithLabels(alignment, dbpedia, linkedGeoData));
        } catch (NonExistingLinksException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSingleClassPeel_0ToPeel_1() throws IOException, ComponentInitException {
        java.io.InputStream peel0File = getClass().getClassLoader().getResourceAsStream("datasets/music/peel_0.ttl" );
        java.io.InputStream peel1File = getClass().getClassLoader().getResourceAsStream("datasets/music/peel_2.ttl" );
        KnowledgeBase peel_0 = new LocalKnowledgeBase(ModelFactory.createDefaultModel().read(peel0File, null, "TTL"));
        KnowledgeBase peel_1= new LocalKnowledgeBase(ModelFactory.createDefaultModel().read(peel1File, null, "TTL"));
        OWLClass nc = owlDataFactory.getOWLClass(IRI.create("http://linkedgeodata.org/ontology/Aerodrome"));
        Set<OWLClass> sourceClasses = new HashSet<>(Arrays.asList(nc));
        
        Set<OWLClass> singOwlClass = Collections.singleton(owlDataFactory.getOWLClass(IRI.create("http://purl.org/ontology/mo/MusicArtist")));
        LASSIEController matcher = new LASSIEController(peel_0, peel_1, singOwlClass);
        LassieResultRecorder run = matcher.run(singOwlClass, false);

        System.in.read();
    }

    /**
     * Tests the mapping from peel_0 to peel_k, where k 1, 2, ...,10
     * representing destruction ratio of 10%, 20%, ...,100% respectively
     * for one class "MusicArtist" in peel_0
     * splitted to MusicArtistSplit1, and MusicArtistSplit2 in peel_k
     * 
     * @throws IOException
     * @author sherif
     * @throws ComponentInitException 
     */
    @Test
    public void testSingleClassPeel_0ToPeel_k() throws IOException, ComponentInitException {
        java.io.InputStream peel0File = getClass().getClassLoader().getResourceAsStream("datasets/music/peel_0.ttl" );
        KnowledgeBase peel_0 = new LocalKnowledgeBase(ModelFactory.createDefaultModel().read(peel0File, null, "TTL"));
        BenchmarkGenerator benchmarker= new BenchmarkGenerator();
        String inClassUri = "http://purl.org/ontology/mo/MusicArtist";
        for(double dRatio = 0.1d; dRatio<=1d; dRatio+=0.1d){
            System.out.println("Genarating Peel data with destruction ratio = "+dRatio*100+"% ...");
            Model inModel = ModelFactory.createDefaultModel();
            inModel.read(getClass().getClassLoader().getResourceAsStream("datasets/music/peel_0.ttl" ), null, "TTL");
            Model peelKModel = benchmarker.bmPeel(inModel, inClassUri, dRatio); 
            KnowledgeBase peel_k = new LocalKnowledgeBase(peelKModel);
            Set<OWLClass> singOwlClass = Collections.singleton(owlDataFactory.getOWLClass(IRI.create("http://purl.org/ontology/mo/MusicArtist")));
            LASSIEController matcher = new LASSIEController(peel_0, peel_k, singOwlClass);
            LassieResultRecorder run = matcher.run(singOwlClass, false);
            System.out.println("\nPrevious results was for destruction ratio = "+dRatio*100+"%\n");
            System.err.println("Press enter to run the next iteration ...");
            System.in.read();
        }


    }

    private void save(String filename, Map<OWLClassExpression, List<? extends EvaluatedDescription>> mapping, KnowledgeBase source, KnowledgeBase target){
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(filename));
            out.write(PrintUtils.toHTMLWithLabels(mapping, source, target));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void save(Map<OWLClassExpression, List<? extends EvaluatedDescription>> mapping, String filename){
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(filename));
            out.write(PrintUtils.toHTML(mapping));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}

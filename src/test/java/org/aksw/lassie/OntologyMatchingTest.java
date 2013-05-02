package org.aksw.lassie;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.lassie.core.ExpressiveSchemaMappingGenerator;
import org.aksw.lassie.core.NonExistingLinksException;
import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.kb.RemoteKnowledgeBase;
import org.aksw.lassie.util.PrintUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.owl.Description;
import org.dllearner.core.owl.NamedClass;
import org.dllearner.kb.extraction.ExtractionAlgorithm;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlKnowledgeSource;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.io.ToStringRenderer;

import com.google.common.collect.Sets;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

public class OntologyMatchingTest {
	
	private KnowledgeBase dbpedia;
	private KnowledgeBase worldFactBook;
	private KnowledgeBase openCyc;
	private KnowledgeBase linkedGeoData;
	
	private final int fragmentDepth = 3;

	@Before
	public void setUp() throws Exception {
		// render output
		ToStringRenderer.getInstance().setRenderer(new ManchesterOWLSyntaxOWLObjectRendererImpl());
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
		ExpressiveSchemaMappingGenerator matcher = new ExpressiveSchemaMappingGenerator(dbpedia, worldFactBook);
		matcher.run();
	}
	
	@Test
	public void testDBpediaOpenCyc() {
		ExpressiveSchemaMappingGenerator matcher = new ExpressiveSchemaMappingGenerator(dbpedia, openCyc);
		matcher.run();
	}
	
	@Test
	public void testDBpediaLinkedGeoData() {
		ExpressiveSchemaMappingGenerator matcher = new ExpressiveSchemaMappingGenerator(dbpedia, linkedGeoData);
		matcher.run();
	}
	
	@Test
	public void testDBpediaLinkedGeoData2() {
		ExpressiveSchemaMappingGenerator matcher = new ExpressiveSchemaMappingGenerator(dbpedia, linkedGeoData);
		Set<NamedClass> sourceClasses = Sets.newHashSet(
				new NamedClass("http://dbpedia.org/ontology/City"),
				new NamedClass("http://dbpedia.org/ontology/Mountain"),
				new NamedClass("http://dbpedia.org/ontology/River")
				);
		Set<NamedClass> targetClasses = Sets.newHashSet(
				new NamedClass("http://linkedgeodata.org/ontology/Village"),
				new NamedClass("http://linkedgeodata.org/ontology/City"),
				new NamedClass("http://linkedgeodata.org/ontology/River"),
				new NamedClass("http://linkedgeodata.org/ontology/Peak")
				);
		matcher.run(sourceClasses, targetClasses);
	}
	
	@Test
	public void testSingleClassOpenCycToDBpedia() {
		ExpressiveSchemaMappingGenerator matcher = new ExpressiveSchemaMappingGenerator(openCyc, dbpedia);
		NamedClass nc = new NamedClass("http://sw.opencyc.org/concept/Mx4r4fYeXvbPQdiKtoNafhmOew");
		try {
			List<? extends EvaluatedDescription> mapping = matcher.computeMappings(nc);
			Map<Description, List<? extends EvaluatedDescription>> alignment = new HashMap<Description, List<? extends EvaluatedDescription>>();
			alignment.put(nc, mapping);
			System.out.println(PrintUtils.toHTMLWithLabels(alignment, openCyc, dbpedia));
		} catch (NonExistingLinksException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testSingleClassLinkedGeoDataToDBpedia() {
		ExpressiveSchemaMappingGenerator matcher = new ExpressiveSchemaMappingGenerator(linkedGeoData, dbpedia);
		matcher.setFragmentDepth(fragmentDepth);
		NamedClass nc = new NamedClass("http://linkedgeodata.org/ontology/Aerodrome");
		try {
			List<? extends EvaluatedDescription> mapping = matcher.computeMappings(nc);
			Map<Description, List<? extends EvaluatedDescription>> alignment = new HashMap<Description, List<? extends EvaluatedDescription>>();
			alignment.put(nc, mapping);
			System.out.println(PrintUtils.toHTMLWithLabels(alignment, linkedGeoData, dbpedia));
		} catch (NonExistingLinksException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testSingleClassDBpediaToLinkedGeoData() {
		ExpressiveSchemaMappingGenerator matcher = new ExpressiveSchemaMappingGenerator(dbpedia, linkedGeoData);
		NamedClass nc = new NamedClass("http://dbpedia.org/ontology/AdministrativeRegion");
		try {
			List<? extends EvaluatedDescription> mapping = matcher.computeMappings(nc);
			Map<Description, List<? extends EvaluatedDescription>> alignment = new HashMap<Description, List<? extends EvaluatedDescription>>();
			alignment.put(nc, mapping);
			System.out.println(PrintUtils.toHTMLWithLabels(alignment, dbpedia, linkedGeoData));
		} catch (NonExistingLinksException e) {
			e.printStackTrace();
		}
	}
	
	private void save(String filename, Map<Description, List<? extends EvaluatedDescription>> mapping, KnowledgeBase source, KnowledgeBase target){
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
	
	private void save(Map<Description, List<? extends EvaluatedDescription>> mapping, String filename){
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

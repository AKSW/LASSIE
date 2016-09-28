/**
 * 
 */
package org.aksw.lassie.kb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.log4j.Logger;
import org.dllearner.core.ComponentInitException;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGenerator;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGeneratorImpl;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * @author Lorenz Buehmann
 *
 */
public class KnowledgebaseSampleGenerator {

	private static final Logger logger = Logger.getLogger(KnowledgebaseSampleGenerator.class.getName());

	private static String cacheDir = "sparql-cache";
	private static int maxCBDDepth = 0;

	public static LocalKnowledgeBase createKnowledgebaseSample(SparqlEndpoint endpoint, String namespace, 
			int maxNrOfClasses, int maxNrOfInstancesPerClass, Set<OWLClass> testClasses) throws ComponentInitException{

		Model model = ModelFactory.createDefaultModel();

		//try to load existing sample from file system
		HashFunction hf = Hashing.md5();
		HashCode hc = hf.newHasher().putString(endpoint.getURL().toString(), Charsets.UTF_8).hash();
		String filename = hc.toString() + ("-" + ((maxNrOfClasses == Integer.MAX_VALUE) ? "all" : maxNrOfClasses)) + "-" + maxNrOfInstancesPerClass + ".ttl.bz2";
		File file = new File(filename);

		if(!file.exists()){//if not exists
			logger.info("Generating sample...");
			long startTime = System.currentTimeMillis();
			SPARQLReasoner reasoner = new SPARQLReasoner();
			SparqlEndpointKS sparqlEndpointKS = new SparqlEndpointKS(endpoint);
			sparqlEndpointKS.setCacheDir(cacheDir);
			try {
                sparqlEndpointKS.init();
            } catch (ComponentInitException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            reasoner.setSources(sparqlEndpointKS);
            try {
                reasoner.init();
            } catch (ComponentInitException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
			ConciseBoundedDescriptionGenerator cbdGen = new ConciseBoundedDescriptionGeneratorImpl(endpoint, cacheDir);

			Set<OWLClass> classes = new HashSet<OWLClass>();
			if(testClasses.size() > 0){
				classes = testClasses;
			}else{
				//get all OWL classes
				classes = reasoner.getOWLClasses(namespace);
			}
			if(maxNrOfClasses != -1 && maxNrOfClasses != Integer.MAX_VALUE){
				List<OWLClass> tmpClasses = new ArrayList<OWLClass>(classes);
				Collections.shuffle(tmpClasses);
				//get for each class n instances and compute the CBD for each instance
				getClassesInstances(maxNrOfClasses, maxNrOfInstancesPerClass,model, reasoner, cbdGen, new HashSet<OWLClass>(tmpClasses));
			}else{
				//get for all classes n instances and compute the CBD for each instance
				maxNrOfClasses = classes.size();
				getClassesInstances(maxNrOfClasses, maxNrOfInstancesPerClass,model, reasoner, cbdGen, classes);
			}
			logger.info("...done in " + (System.currentTimeMillis() - startTime) + "ms");
			//add schema
			model.add(reasoner.loadOWLSchema());
			logger.debug("Writing sample to disk...");
			startTime = System.currentTimeMillis();
			try {
				CompressorOutputStream out = new CompressorStreamFactory()
				.createCompressorOutputStream(CompressorStreamFactory.BZIP2, new FileOutputStream(file));
				model.write(out,"TURTLE");
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (CompressorException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			logger.debug("...done in " + (System.currentTimeMillis() - startTime) + "ms");
		} else {
			logger.info("Loading sample from disk...");
			long startTime = System.currentTimeMillis();
			try {
				CompressorInputStream in = new CompressorStreamFactory().
						createCompressorInputStream(CompressorStreamFactory.BZIP2, new FileInputStream(file));
				model.read(in, null, "TURTLE");
				in.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (CompressorException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			logger.info("...done in " + (System.currentTimeMillis() - startTime) + "ms");
		}

		return new LocalKnowledgeBase(model, namespace);
	}

	/**
	 * get for each class n instances and compute the CBD for each instance
	 * @param maxNrOfClasses
	 * @param maxNrOfInstancesPerClass
	 * @param model
	 * @param reasoner
	 * @param cbdGen
	 * @param classes
	 * @author sherif
	 */
	private static void getClassesInstances(int maxNrOfClasses,
			int maxNrOfInstancesPerClass, Model model, SPARQLReasoner reasoner,
			ConciseBoundedDescriptionGenerator cbdGen, Set<OWLClass> classes) {
		int i = 0;
		for (OWLClass cls : classes) {
			logger.debug("\t...processing class " + cls + "...");
			SortedSet<OWLIndividual> individuals = reasoner.getIndividuals(cls, maxNrOfInstancesPerClass*2);

			Model classSample = ModelFactory.createDefaultModel();
			int cnt = 0;
			Model cbd;
			for (OWLIndividual individual : individuals) {
				try {
					cbd = cbdGen.getConciseBoundedDescription(individual.toStringID(), maxCBDDepth);
					classSample.add(cbd);
					if(cnt++ == maxNrOfInstancesPerClass){
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			model.add(classSample);
			if(!classSample.isEmpty()){
				i++;
			} else {
				logger.debug("Skipping empty class " + cls);
			}
			if(i == maxNrOfClasses){
				break;
			}
		}
	}

	public static LocalKnowledgeBase createKnowledgebaseSample(SparqlEndpoint endpoint, int maxNrOfClasses, int maxNrOfInstancesPerClass) throws ComponentInitException{
		return createKnowledgebaseSample(endpoint, null, maxNrOfClasses, maxNrOfInstancesPerClass, new HashSet<OWLClass>());
	}

	public static LocalKnowledgeBase createKnowledgebaseSample(SparqlEndpoint endpoint, int maxNrOfInstancesPerClass) throws ComponentInitException{
		return createKnowledgebaseSample(endpoint, Integer.MAX_VALUE, maxNrOfInstancesPerClass);
	}

	public static LocalKnowledgeBase createKnowledgebaseSample(SparqlEndpoint endpoint, String namespace, int maxNrOfInstancesPerClass) throws ComponentInitException{
		return createKnowledgebaseSample(endpoint, null, Integer.MAX_VALUE, maxNrOfInstancesPerClass, new HashSet<OWLClass>());
	}

	public static void main(String[] args) throws Exception {
		LocalKnowledgeBase kb = createKnowledgebaseSample(SparqlEndpoint.getEndpointDBpedia(), "http://dbpedia.org/ontology", 100);

	}

	public static LocalKnowledgeBase createKnowledgebaseSubTreeSample(SparqlEndpoint endpoint, String namespace, 
			OWLClass subTreeRootClass, int maxNrOfInstancesPerClass) throws ComponentInitException{

		Model model = ModelFactory.createDefaultModel();

		//try to load existing sample from file system
		HashFunction hf = Hashing.md5();
		HashCode hc = hf.newHasher().putString(endpoint.getURL().toString(), Charsets.UTF_8).hash();
		String filename = hc.toString() + ("-" + subTreeRootClass.toStringID().substring(subTreeRootClass.toStringID().lastIndexOf("/")+1) + 
				(( maxNrOfInstancesPerClass == Integer.MAX_VALUE) ? "-all" : maxNrOfInstancesPerClass)) + ".ttl.bz2";
		File file = new File(filename);

		if(!file.exists()){//if not exists
			logger.info("Generating sample sub-tree from root " + subTreeRootClass.toStringID() + " ...");
			long startTime = System.currentTimeMillis();
			SparqlEndpointKS endPoint = new SparqlEndpointKS(endpoint);
			endPoint.setCacheDir(cacheDir);
			try {
                endPoint.init();
            } catch (ComponentInitException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
			SPARQLReasoner reasoner = new SPARQLReasoner(endPoint);
			try {
                reasoner.init();
            } catch (ComponentInitException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
			ConciseBoundedDescriptionGenerator cbdGen = new ConciseBoundedDescriptionGeneratorImpl(endpoint, cacheDir);

			//get all OWL classes
			Set<OWLClassExpression> subclasses = reasoner.getSubClasses(subTreeRootClass, false);
			Set<OWLClass> nextLevelClasses = reasoner.getOWLClasses(namespace);
			//get for each sub-class of subTreeRootClass n instances and compute the CBD for each instance

			for (OWLClassExpression cls : subclasses) {
				logger.debug("\t...processing class " + cls + "...");
				SortedSet<OWLIndividual> individuals = reasoner.getIndividuals(cls, maxNrOfInstancesPerClass*2);

				Model classSample = ModelFactory.createDefaultModel();
				int cnt = 0;
				Model cbd;
				for (OWLIndividual individual : individuals) {
					try {
						cbd = cbdGen.getConciseBoundedDescription(individual.toStringID(), maxCBDDepth);
						classSample.add(cbd);
						if(cnt++ == maxNrOfInstancesPerClass){
							break;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				model.add(classSample);
				if(classSample.isEmpty()){
					logger.debug("Skipping empty class " + cls);
				}
			}
			logger.info("...done in " + (System.currentTimeMillis() - startTime) + "ms");
			//add schema
			model.add(reasoner.loadOWLSchema());
			logger.debug("Writing sample to disk...");
			startTime = System.currentTimeMillis();
			try {
				CompressorOutputStream out = new CompressorStreamFactory()
				.createCompressorOutputStream(CompressorStreamFactory.BZIP2, new FileOutputStream(file));
				model.write(out,"TURTLE");
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (CompressorException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			logger.debug("...done in " + (System.currentTimeMillis() - startTime) + "ms");
		} else {
			logger.info("Loading sample from disk...");
			long startTime = System.currentTimeMillis();
			try {
				CompressorInputStream in = new CompressorStreamFactory().
						createCompressorInputStream(CompressorStreamFactory.BZIP2, new FileInputStream(file));
				model.read(in, null, "TURTLE");
				in.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (CompressorException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			logger.info("...done in " + (System.currentTimeMillis() - startTime) + "ms");
		}

		return new LocalKnowledgeBase(model, namespace);
	}
}

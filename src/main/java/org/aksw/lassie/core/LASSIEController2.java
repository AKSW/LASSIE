package org.aksw.lassie.core;

import com.google.common.collect.Multimap;
import org.aksw.lassie.bmGenerator.Modifier;
import org.aksw.lassie.core.exceptions.NonExistingLinksException;
import org.aksw.lassie.core.linking.EuclidLinker;
import org.aksw.lassie.core.linking.UnsupervisedLinker;
import org.aksw.lassie.core.linking.WombatSimpleLinker;
import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.cache.Instance;
import org.aksw.limes.core.io.mapping.AMapping;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.utilities.Helper;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

public class LASSIEController2 extends LASSIEController {

    public LASSIEController2(KnowledgeBase source, KnowledgeBase target, int nrOfIterations, Set<OWLClass> sourceClasses) {
        super(source, target, nrOfIterations, sourceClasses);
    }

    public Map<String, Object> runIntentionalEvaluation(Set<OWLClass> sourceClasses,
            Set<OWLClass> targetClasses, Map<Modifier, Double> instanceModefiersAndRates, Map<Modifier, Double> classModefiersAndRates) {
        for (Map.Entry<Modifier, Double> clsMod2Rat : classModefiersAndRates.entrySet()) {
            System.out.println("Modifer Name: " + clsMod2Rat.getKey());
            System.out.println("Optimal solution: " + clsMod2Rat.getKey().getOptimalSolution(sourceClasses.iterator().next()));
        }
        System.exit(1);

        int pos = 0;
        Map<String, Object> result = new HashMap<String, Object>();
        Map<Modifier, Integer> modifier2pos = new HashMap<Modifier, Integer>();

        Map<Modifier, OWLClassExpression> modifier2optimalSolution = new HashMap<Modifier, OWLClassExpression>();
        //initially, the class expressions E_i in the target KB are the named classes D_i
        Set<OWLClassExpression> targetClassExpressions = new TreeSet<OWLClassExpression>();
        targetClassExpressions.addAll(targetClasses);

        //perform the iterative schema matching
        Map<OWLClass, List<? extends EvaluatedDescription<?>>> mappingTop10 = new HashMap<OWLClass, List<? extends EvaluatedDescription<?>>>();
        int iterationNr = 0;
        //		do {
        //compute a set of links between each pair of class expressions (C_i, E_j), thus finally we get
        //a map from C_i to a set of instances in the target KB
        //        UnsupervisedLinker linker = new EuclidLinker(sourceKB, targetKB, linkingProperty, resultRecorder);
        UnsupervisedLinker linker = new WombatSimpleLinker(sourceKB, targetKB, linkingProperty, resultRecorder,iterationNr);
        Multimap<OWLClass, String> links = linker.link(sourceClasses, targetClassExpressions);
        result.put("posExamples", links);
        //for each source class C_i, compute a mapping to a class expression in the target KB based on the links
        for (OWLClass sourceClass : sourceClasses) {

            logger.info("Source class: " + sourceClass);
            currentClass = sourceClass;
            try {
                SortedSet<OWLIndividual> targetInstances = Helper.getIndividualSet(new TreeSet<>(links.get(sourceClass)));
                List<? extends EvaluatedDescription<?>> mappingList = computeMappings(targetInstances, false);
                mappingTop10.put(sourceClass, mappingList);

                for (Modifier modifier : modifiers) {
                    if (modifier.isClassModifier()) {
                        pos = intentionalEvaluation(mappingList, modifier, sourceClass);
                        modifier2pos.put(modifier, pos);
                        modifier2optimalSolution.put(modifier, modifier.getOptimalSolution(sourceClass));
                    }
                }

            } catch (NonExistingLinksException e) {
                logger.warn(e.getMessage() + "Skipped learning.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //			if(pos<0)result
        //				break;
        //		} while (i++ <= maxNrOfIterations);
        result.put("Modifier2pos", modifier2pos);
        result.put("mappingTop10", mappingTop10);
        result.put("modifier2optimalSolution", modifier2optimalSolution);
        return result;
    }

    public int intentionalEvaluation(List<? extends EvaluatedDescription<?>> descriptions, Modifier modifier, OWLClass cls) {
        OWLClassExpression targetDescription = modifier.getOptimalSolution(cls);
        int pos = descriptions.indexOf(targetDescription);
        return pos;
    }

    /**
     * @param j
     * @param sourceClass
     * @param targetInstances
     * @throws IOException
     * @throws FileNotFoundException
     * @author sherif
     */
    public void serializeCurrentObjects(int j, OWLClass sourceClass,
            SortedSet<OWLIndividual> targetInstances) throws IOException,
    FileNotFoundException {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("sourceClass" + j + ".ser"));
        out.writeObject(sourceClass);
        out.close();
        out = new ObjectOutputStream(new FileOutputStream("targetInstances" + j + ".ser"));
        out.writeObject(targetInstances);
        j++;
        out.close();
    }



    public Set<String> getAllProperties(ACache c) {
        //    	logger.info("Get all properties...");
        if (c.size() > 0) {
            HashSet<String> props = new HashSet<String>();
            for (Instance i : c.getAllInstances()) {
                props.addAll(i.getAllProperties());
            }
            return props;
        } else {
            return new HashSet<String>();
        }
    }




    /**
     * Computes initial mappings
     *
     */
    public AMapping getNonDeterministicUnsupervisedMappings(ACache source, ACache target) {
        logger.info("Source size = " + source.getAllUris().size());
        logger.info("Target size = " + target.getAllUris().size());
        //TODO @Axel: Add genetic algorithm variant
        return null;
    }


    private SortedSet<OWLIndividual> getRelatedIndividualsNamespaceAware(KnowledgeBase kb, OWLClass nc, String targetNamespace) {
        SortedSet<OWLIndividual> relatedIndividuals = new TreeSet<>();
        //get all individuals o which are connected to individuals s belonging to class nc
        //		String query = String.format("SELECT ?o WHERE {?s a <%s>. ?s <http://www.w3.org/2002/07/owl#sameAs> ?o. FILTER(REGEX(STR(?o),'%s'))}", nc.getName(), targetNamespace);
        //		ResultSet rs = executeSelect(kb, query);
        //		QuerySolution qs;
        //		while(rs.hasNext()){
        //			qs = rs.next();
        //			RDFNode object = qs.get("o");
        //			if(object.isURIResource()){
        //
        //				String uri = object.asResource().getURI();
        //				//workaround for World Factbook - should be removed later
        //				uri = uri.replace("http://www4.wiwiss.fu-berlin.de/factbook/resource/", "http://wifo5-03.informatik.uni-mannheim.de/factbook/resource/");
        //				//workaround for OpenCyc - should be removed later
        //				uri = uri.replace("http://sw.cyc.com", "http://sw.opencyc.org");
        //
        //				relatedIndividuals.add(new OWLIndividual(uri));
        //			}
        //		}
        return relatedIndividuals;
    }



}

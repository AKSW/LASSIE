/**
 *
 */
package org.aksw.lassie.evaluation.lance;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.aksw.lassie.TestUsingToyDataset;
import org.aksw.lassie.bmGenerator.Modifier;
import org.aksw.lassie.core.LASSIEController;
import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.kb.LocalKnowledgeBase;
import org.aksw.lassie.result.LassieResultRecorder;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.reader.CSVMappingReader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dllearner.core.ComponentInitException;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/**
 * @author sherif
 *
 */
public class TestUsingLANCEDatasets {
    private static final Logger logger = Logger.getLogger(TestUsingLANCEDatasets.class);

    static int nrOfExperimentRepeats = 1;
    static Map<Modifier, Double> classModifiersAndRates    = new HashMap<>();
    static Map<Modifier, Double> instanceModifiersAndRates = new HashMap<>();
    static int maxNrOfIterations = 3;
    static int nrOfClasses = 1;
    static int nrOfInstancesPerClass = 5;

    static String outputFile = "lanceTestResult.txt";
    static Set<OWLClass> testClasses = new HashSet<>();

    static String datasetRootPath = "datasets/SPIMBENCH/";
    static String[] datasetExperimentSizes = {"10K", "50K"};
    static String[] datasetExperimentFlavours = {"COMPLEX", "SEMANTICS", "SIMPLE", "STRUCTURE", "VALUE"};
    static String datasetSourceName = "Tbox1.nt";
    static String datasetTargetName = "Tbox2.nt";
    static String datasetGSName = "gs.txt";

    static String sourceDatasetFile = "datasets/spimbench/Tbox1.nt";
    static String targetDatasetFile = "datasets/spimbench/Tbox2.nt";
    static String goldStandardFile  = "datasets/spimbench/gs.txt";

    public static Model readModel(String fileNameOrUri)
    {
        
        long startTime = System.currentTimeMillis();
        Model model = ModelFactory.createDefaultModel();
        java.io.InputStream in = FileManager.get().open( fileNameOrUri ); ;
        if (in == null) {
            throw new IllegalArgumentException(
                    "File: " + fileNameOrUri + " not found");
        }
        if(fileNameOrUri.contains(".ttl") || fileNameOrUri.contains(".n3")){
            logger.info("Opening Turtle file");
            model.read(in, null, "TTL");
        }else if(fileNameOrUri.contains(".rdf")){
            logger.info("Opening RDFXML file");
            model.read(in, null);
        }else if(fileNameOrUri.contains(".nt")){
            logger.info("Opening N-Triples file");
            model.read(in, null, "N-TRIPLE");
        }else{
            logger.info("Content negotiation to get RDFXML from " + fileNameOrUri);
            model.read(fileNameOrUri);
        }
        logger.info("Loading " + fileNameOrUri + " is done in " + (System.currentTimeMillis()-startTime) + "ms.");
        return model;
    }

    public  static void test(String sourceDatasetFile, String targetDatasetFile, String goldStandardFile, String outputFile)
            throws IOException, ComponentInitException{

        Model toyDatasetModel = readModel(sourceDatasetFile);
        Model targetKBModel   = readModel(targetDatasetFile);
        
        String oracleFile = Paths.get("src/main/resources/").toAbsolutePath().toString() + '/' + goldStandardFile;
        CSVMappingReader  mappingReader = new CSVMappingReader(oracleFile); 
        AMapping oracleMapping = mappingReader.readTwoColumnFile();
        
            long startTime = System.currentTimeMillis();
            OWLDataFactory owlDataFactory = new OWLDataFactoryImpl();
//            testClasses.add(owlDataFactory.getOWLClass(IRI.create("http://dbpedia.org/ontology/Sport")));
            LocalKnowledgeBase sourceKB = new LocalKnowledgeBase(toyDatasetModel, "http://dbpedia.org/ontology/");
            KnowledgeBase targetKB = new LocalKnowledgeBase(targetKBModel, "http://dbpedia.org/ontology/");
            LASSIEController controller = new LASSIEController(sourceKB, targetKB, maxNrOfIterations, testClasses);
            controller.setTargetDomainNameSpace("http://dbpedia.org/ontology/");
            controller.setOracleMapping(oracleMapping);
//            LassieResultRecorder experimentResults = controller.run(testClasses, false);
            LassieResultRecorder experimentResults = controller.run(false);
            logger.info("Experiment Results:\n" + experimentResults.toString());
            experimentResults.saveToFile(outputFile);

            long experimentTime = System.currentTimeMillis() - startTime;
            System.out.println("Experiment time: " + experimentTime + "ms.");
            logger.info("Experiment is done in " + experimentTime + "ms.");
    }

    public  static void testAll() throws IOException, ComponentInitException{
        for (String datasetExperimentSize : datasetExperimentSizes) {
            for (String datasetExperimentFlavour : datasetExperimentFlavours) {
                String path = datasetRootPath + datasetExperimentSize + "/" + datasetExperimentFlavour + "/";
                        test(path + datasetSourceName, path + datasetTargetName, path + datasetGSName,
                                datasetExperimentSize + "_" + datasetExperimentFlavour + "_" + "result.txt");
            }
        }
    }

    public static void main(String args[]) throws IOException, ComponentInitException{
        Logger.getRootLogger().setLevel(Level.DEBUG);
        String path = datasetRootPath + datasetExperimentSizes[0] + "/" + datasetExperimentFlavours[2] + "/";
//        test(path + datasetSourceName, path + datasetTargetName, path + datasetGSName, outputFile);
        testAll();
    }
}

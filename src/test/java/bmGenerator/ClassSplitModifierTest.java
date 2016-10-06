package bmGenerator;

import static org.junit.Assert.assertTrue;

import org.aksw.lassie.bmGenerator.ClassSplitModifier;
import org.aksw.lassie.bmGenerator.Modifier;
import org.apache.jena.rdf.model.Model;
import org.junit.Test;

public class ClassSplitModifierTest {
    
    final static String toyDatasetFile = "src/main/resources/datasets/toydataset/toydataset_scientist_mammal_plant.nt";
    
    @Test
    public void testClassSplitModifier(){
        String toyDatasetFile = "src/main/resources/datasets/toydataset/toydataset_scientist_mammal_plant.nt";
        Model startModel = Modifier.loadModel(toyDatasetFile);
        Modifier modifer = new ClassSplitModifier(startModel);
        Model destroedModel = modifer.destroy(startModel);
        assertTrue(destroedModel.size() > startModel.size());
    }

}

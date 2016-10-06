package bmGenerator;

import static org.junit.Assert.assertTrue;

import org.aksw.lassie.bmGenerator.ClassDeleteModifier;
import org.aksw.lassie.bmGenerator.Modifier;
import org.apache.jena.rdf.model.Model;
import org.junit.Test;

public class ClassDeleteModifierTest {
    
    final static String toyDatasetFile = "src/main/resources/datasets/toydataset/toydataset_scientist_mammal_plant.nt";
    
    @Test
    public void testClassDeleteModifier(){
        String toyDatasetFile = "src/main/resources/datasets/toydataset/toydataset_scientist_mammal_plant.nt";
        Model startModel = Modifier.loadModel(toyDatasetFile);
        Modifier modifier = new ClassDeleteModifier(startModel);
        Model destroedModel = modifier.destroy(startModel);
        assertTrue(destroedModel.size() == 0);
    }

}

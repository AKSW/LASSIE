package bmGenerator;

import static org.junit.Assert.assertTrue;

import org.aksw.lassie.bmGenerator.InstanceMisspellingModifier;
import org.aksw.lassie.bmGenerator.Modifier;
import org.apache.jena.rdf.model.Model;
import org.junit.Test;

public class InstanceMisspellingModifierTest {
    
    final static String toyDatasetFile = "src/main/resources/datasets/toydataset/toydataset_scientist.nt";
    
    @Test
    public void testInstanceMissspellingModifier(){
        Model startModel = Modifier.loadModel(toyDatasetFile);
        Modifier modifer = new InstanceMisspellingModifier(startModel);
        Model destroedModel = modifer.destroy(startModel);
        assertTrue(destroedModel.size() == startModel.size());

    }

    


}

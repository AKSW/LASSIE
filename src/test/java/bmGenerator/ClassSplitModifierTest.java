package bmGenerator;

import static org.junit.Assert.assertTrue;

import org.aksw.lassie.bmGenerator.ClassSplitModifier;
import org.aksw.lassie.bmGenerator.Modifier;
import org.apache.jena.rdf.model.Model;
import org.junit.Test;

public class ClassSplitModifierTest {
    
    final static String toyDatasetFile = "src/main/resources/datasets/toydataset/toydataset_scientist.nt";
    
    @Test
    public void testClassSplitModifier(){
        Model startModel = Modifier.loadModel(toyDatasetFile);
        Modifier modifer = new ClassSplitModifier(startModel);
        Model destroedModel = modifer.destroy(startModel);
        destroedModel.write(System.out, "TTL");
        assertTrue(destroedModel.size() == startModel.size());
        
        

    }

}

package bmGenerator;

import static org.junit.Assert.assertTrue;

import org.aksw.lassie.bmGenerator.ClassRenameModifier;
import org.aksw.lassie.bmGenerator.Modifier;
import org.apache.jena.rdf.model.Model;
import org.junit.Test;

public class ClassRenameModifierTest {
    
    final static String toyDatasetFile = "src/main/resources/datasets/toydataset/toydataset_scientist_mammal_plant.nt";

    @Test
    public void testClassRename(){
        Model startModel = Modifier.loadModel(toyDatasetFile);
        Modifier modifier = new ClassRenameModifier(startModel);
        Model destroedModel = modifier.destroy(startModel);
        assertTrue(startModel.size() == destroedModel.size());
        
        destroedModel.write(System.out, "TTL");
    }

}

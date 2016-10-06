/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * @author sherif
 *
 */
public class ClassSplitModifier extends Modifier {

    private int splitCount = 2;


    public ClassSplitModifier() {
        isClassModifier = true;
    }

    public ClassSplitModifier(int c) {
        if (c > 2) {
            splitCount = c;
        }
    }

    public ClassSplitModifier(Model m) {
        super(m);
        isClassModifier = true;
    }

    public ClassSplitModifier(Model m, int c) {
        super(m);
        if (c > 2) {
            splitCount = c;
        }
    }


    /**
     * @return the splitCount
     */
    public int getSplitCount() {
        return splitCount;
    }

    /**
     * @param splitCount the splitCount to set
     */
    public void setSplitCount(int splitCount) {
        this.splitCount = splitCount;
    }

    /* (non-Javadoc)
     * @see de.uni_leipzig.simba.bmGenerator.Modifier#destroy(org.apache.jena.rdf.model.Model)
     */
    @Override
    public Model destroy(Model subModel) {
        Model result = ModelFactory.createDefaultModel();

        List<String> classNames = new ArrayList<String>();
        if (baseClasses.size() == 0) {
            classNames = getClasses(subModel);
        } else {
            classNames = baseClasses;
        }

        for(String className: classNames){
            Model sourceClassModel = getClassInstancesModel(className);

            //divide the source class instances equally to target classes
            long targetClassSize = (long) Math.floor(sourceClassModel.size()/splitCount);
            long sourceClassModelOffset = 0L;

            //create split classes URIs
            List<String> splitTargetClassUri = new ArrayList<String>();
            for(int i=0 ; i<splitCount ; i++){
                String splitUri = className +"_SPLIT_"+(i+1);
                splitTargetClassUri.add(splitUri);
                modifiedClasses.add(splitUri);
            }
            //generate optimal solution
            List<OWLClassExpression> children = new ArrayList<>();
            for (String uri : splitTargetClassUri) {
                children.add(owlDataFactory.getOWLClass(IRI.create(uri)));
            }
            OWLObjectUnionOf optimalSolution = owlDataFactory.getOWLObjectUnionOf(new TreeSet<> (children));
            optimalSolutions.put(owlDataFactory.getOWLClass(IRI.create(className)), optimalSolution);
            //			System.out.println("********************** optimalSolutions: "+optimalSolutions);

            //perform splitting
            for(String targetClassUri:splitTargetClassUri){
                Model splitModel = getSubModel(sourceClassModel, targetClassSize, sourceClassModelOffset); 
                sourceClassModelOffset += targetClassSize;
                splitModel = renameClass(splitModel, className, targetClassUri);	
                result.add(splitModel);  
                destroyedModel.add(splitModel);

                //if some triples remains at end (<targetClassSize) add them to the last split
                if( (sourceClassModel.size() - sourceClassModelOffset) < targetClassSize){
                    splitModel = getSubModel(sourceClassModel, sourceClassModel.size()-sourceClassModelOffset, sourceClassModelOffset); 
                    splitModel = renameClass(splitModel, className, targetClassUri);
                    result.add(splitModel);
                    destroyedModel.add(splitModel);
                    break;
                }
            }
        }

        return result;
    }

    public static void main(String[] args){

        String toyDatasetFile = "src/main/resources/datasets/toydataset/toydataset_scientist_mammal_plant.nt";
        Model m = loadModel(toyDatasetFile);
        ClassSplitModifier classSpliter = new ClassSplitModifier(m);

        System.out.println();

        Model destM = classSpliter.destroy(m);
        System.out.println("----- Base Model -----");
        System.out.println("Size: "+baseModel.size());
        System.out.println("----- Split Model -----");    
        System.out.println("Size: "+destM.size());
    }

}

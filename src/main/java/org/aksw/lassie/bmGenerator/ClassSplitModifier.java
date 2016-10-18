/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

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
    public Model destroy(final Model subModel) {
        Model result = ModelFactory.createDefaultModel();

        List<String> classNames = new ArrayList<>();
        if (baseClasses.size() == 0) {
            classNames = getClasses(subModel);
        } else {
            classNames = baseClasses;
        }

        for(String oldClassName: classNames){
            Model sourceClassModel = getClassInstancesModel(oldClassName);

            //create split classes URIs
            List<String> splitTargetClassUri = new ArrayList<>();
            for(int i = 0 ; i < splitCount ; i++){
                String splitUri = oldClassName + "_SPLIT_" + (i+1);
                splitTargetClassUri.add(splitUri);
                modifiedClassesURIs.add(splitUri);
            }
            //generate optimal solution
            List<OWLClassExpression> children = new ArrayList<>();
            for (String uri : splitTargetClassUri) {
                children.add(owlDataFactory.getOWLClass(IRI.create(uri)));
            }
            OWLObjectUnionOf optimalSolution = owlDataFactory.getOWLObjectUnionOf(new TreeSet<> (children));
            optimalSolutions.put(owlDataFactory.getOWLClass(IRI.create(oldClassName)), optimalSolution);

            //divide the source class instances equally to target classes
            ResIterator subjectItr = sourceClassModel.listSubjects();
            int i = 0;
            while(subjectItr.hasNext()){
                Resource r = subjectItr.next();
                String newClassName = splitTargetClassUri.get(i);
                Model splitModel = getchangedResourceTypeModel(r, oldClassName, newClassName, subModel);
                result.add(splitModel);
                i = (i + 1) % splitCount;
            }
        }
        return result;
    }


    private Model getchangedResourceTypeModel(Resource r,String oldClassName, String newClassName, final Model subModel) {
        Model resultModel = ModelFactory.createDefaultModel();
        String sparqlQueryString= "CONSTRUCT {?s ?p ?o} WHERE {<" + r.getURI() +"> ?p ?o. ?s ?p ?o} " ;
        QueryFactory.create(sparqlQueryString);
        QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, subModel);
        Model instanceMode = qexec.execConstruct();

        StmtIterator sItr = instanceMode.listStatements(); 
        while(sItr.hasNext()){
            Statement stmt = sItr.nextStatement();
            if(stmt.getPredicate().equals(RDF.type) && stmt.getObject().toString().equals(oldClassName)){
                resultModel.add(r, RDF.type,  ResourceFactory.createResource(newClassName));
            }else if(!stmt.getPredicate().equals(RDF.type)){
                resultModel.add(stmt);
            }
        }
        return resultModel;
    }

    public Model _destroy(final Model subModel) {
        Model result = ModelFactory.createDefaultModel();

        List<String> classNames = new ArrayList<>();
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
            List<String> splitTargetClassUri = new ArrayList<>();
            for(int i = 0 ; i < splitCount ; i++){
                String splitUri = className + "_SPLIT_" + (i+1);
                splitTargetClassUri.add(splitUri);
                modifiedClassesURIs.add(splitUri);
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

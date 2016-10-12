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

/**
 * @author sherif
 *
 */
public class ClassMergeModifier extends Modifier{
    //	List<String> mergeSourceClassUris = new ArrayList<String>();
    //	String mergeTargetClassuri = new String();
    private int mergeCount = 2;



    /**
     * @return the mergeCount
     */
    public int getMergeCount() {
        return mergeCount;
    }

    /**
     * @param mergeCount the mergeCount to set
     */
    public void setMergeCount(int mergeCount) {
        this.mergeCount = mergeCount;
    }

    /**
     * @param m
     *@author sherif
     */
    public ClassMergeModifier(Model m) {
        super(m);
        isClassModifier = true;
    }

    public ClassMergeModifier() {
        isClassModifier = true;
    }

    /* (non-Javadoc)
     * @see de.uni_leipzig.simba.bmGenerator.Modifier#destroy(org.apache.jena.rdf.model.Model)
     */
    @Override
    public Model destroy(Model subModel) {
        Model result = ModelFactory.createDefaultModel();

        List<String> classNames = new ArrayList<>();
        if (baseClasses.size() == 0) {
            classNames = getClasses(subModel);
        } else {
            classNames = baseClasses;
        }

        List<String> mergeSourceClassUris = new ArrayList<>();

        for(int i = 0 ; i < classNames.size() ; i += mergeCount){
            String mergeTargetClassUri = new String();
            mergeSourceClassUris.removeAll(mergeSourceClassUris);
            for(int j = 0 ; j < mergeCount && i+j < classNames.size() ; j++){
                mergeSourceClassUris.add(j,classNames.get(i+j)); 
                mergeTargetClassUri = mergeTargetClassUri.concat(classNames.get(i+j));
                mergeTargetClassUri = (j+1 == mergeCount)? mergeTargetClassUri : mergeTargetClassUri.concat("_MERGE_");
            }
            modifiedClassesURIs.add(mergeTargetClassUri);

            for(String sourceClassUri : mergeSourceClassUris){
                Model sourceClassModel = getClassInstancesModel(sourceClassUri);
                sourceClassModel = renameClass(sourceClassModel, sourceClassUri, mergeTargetClassUri);
                result.add(sourceClassModel);

                //generate optimal solution
                List<OWLClassExpression> children = new ArrayList<>();
                for (String uri : mergeSourceClassUris) {
                    children.add(owlDataFactory.getOWLClass(IRI.create(uri)));
                }
                OWLObjectUnionOf optimalSolution = owlDataFactory.getOWLObjectUnionOf(new TreeSet<> (children));
                optimalSolutions.put(owlDataFactory.getOWLClass(IRI.create(mergeTargetClassUri)), optimalSolution);

            }
        }
        return result;
    }


    public Model _destroy(final Model subModel) {
        Model result = ModelFactory.createDefaultModel();

        List<String> classNames = new ArrayList<>();
        if (baseClasses.size() == 0) {
            classNames = getClasses(subModel);
        } else {
            classNames = baseClasses;
        }

        

        // get merge class URIs
        List<String> mergeSourceClassUris;
        for(int i = 0 ; i < classNames.size() ; i += mergeCount){
            String mergeTargetClassUri = new String();
            mergeSourceClassUris = new ArrayList<>();
            for(int j = 0 ; j < mergeCount && i+j < classNames.size() ; j++){
                mergeSourceClassUris.add(j,classNames.get(i+j)); 
                mergeTargetClassUri = mergeTargetClassUri.concat(classNames.get(i+j));
                mergeTargetClassUri = (j+1 == mergeCount)? mergeTargetClassUri : mergeTargetClassUri.concat("_MERGE_");
            }
            modifiedClassesURIs.add(mergeTargetClassUri);

            // generate merged resources 
            ResIterator subjectItr = subModel.listSubjects();
            while(subjectItr.hasNext()){
                Resource r = subjectItr.next();
                Model splitModel = getChangedResourceTypeModel(r, mergeSourceClassUris, mergeTargetClassUri, subModel);
                result.add(splitModel);
            }
        }
        return result;
    }

    private Model getChangedResourceTypeModel(Resource r, List<String> oldClassNames, String newClassName, final Model startModel) {
        Model resultModel = ModelFactory.createDefaultModel();
        String sparqlQueryString= "CONSTRUCT {?s ?p ?o} WHERE {<" + r.getURI() +"> ?p ?o. ?s ?p ?o} " ;
        QueryFactory.create(sparqlQueryString);
        QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, startModel);
        Model instanceMode = qexec.execConstruct();

        StmtIterator sItr = instanceMode.listStatements(); 
        while(sItr.hasNext()){
            Statement stmt = sItr.nextStatement();
            if(stmt.getPredicate().equals(RDF.type)){
                for(String oldClassName : oldClassNames){
                    if(stmt.getObject().toString().equals(oldClassName)){
                        resultModel.add(r, RDF.type,  ResourceFactory.createResource(newClassName));
                    }
                }
            }else if(!stmt.getPredicate().equals(RDF.type)){
                resultModel.add(stmt);
            }
        }
        return resultModel;
    }


    public static void main(String[] args){
        Model m= loadModel("src/main/resources/datasets/toydataset/toydataset_scientist.nt");
        ClassMergeModifier classMerger=new ClassMergeModifier(m);

        System.out.println("----- Base Model -----\n");
        System.out.println("Size: "+m.size());
        baseModel.write(System.out,"TTL");
        System.out.println();

        System.out.println("\n\n----- Merge Model -----\n");
        Model desM = classMerger.destroy(m);
        System.out.println("Size: "+desM.size());
        desM.write(System.out,"TTL");

        //		Model test= ModelFactory.createDefaultModel();
        //		test.add(ResourceFactory.createResource("aaaa"),RDF.type,ResourceFactory.createResource("a"));
        //		test.add(ResourceFactory.createResource("bbbb"),RDF.type,ResourceFactory.createResource("b"));
        //		test.add(ResourceFactory.createResource("cccc"),RDF.type,ResourceFactory.createResource("c"));
        //		test.add(ResourceFactory.createResource("dddd"),RDF.type,ResourceFactory.createResource("d"));
        //		test.add(ResourceFactory.createResource("eeee"),RDF.type,ResourceFactory.createResource("e"));
        //		test.add(ResourceFactory.createResource("ffff"),RDF.type,ResourceFactory.createResource("f"));
        //		test.write(System.out,"TTL");
        //		System.out.println(test.size());
        //		System.out.println("----------------------------------");
        //		ClassMergeModifier classMerger=new ClassMergeModifier(test);
        //		Model desM = classMerger.destroy(test);
        //		System.out.println(desM.size());
        //		desM.write(System.out,"TTL");
        //		classMerger.renameClass(test, "a", "X").write(System.out,"TTL");


    }
}

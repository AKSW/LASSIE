package org.aksw.lassie.bmGenerator;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/**
 * @author Sherif
 *
 */
public abstract class Modifier {
	protected static Model baseModel = ModelFactory.createDefaultModel();
	static Model destroyedPropertiesModel = ModelFactory.createDefaultModel(); 		//destroyed properties model
	static Model destroyedClassModel      = ModelFactory.createDefaultModel();     //destroyed class model
	static Model destroyedModel           = ModelFactory.createDefaultModel();     //final destroyed model
	static List<Property> properties = new ArrayList<>();
	double destructionRatio = 0.5;
	long destroyedInstancesCount;
	boolean destroyProperty = false;
	
	public String inputClassUri  = null;
	public String outputClassUri = null;
	
	static protected String  nameSpace = "";
	static protected List<String> baseClasses     = new ArrayList<>();
	static protected List<String> modifiedClassesURIs = new ArrayList<>();
	protected boolean isClassModifier = false;
	
	OWLDataFactory owlDataFactory = new OWLDataFactoryImpl();
	
	Map<OWLClass, OWLClassExpression> optimalSolutions = new HashMap<>();
	
	Map<OWLClass, OWLClass> alteredClasses = new HashMap<>();
	
	public String getName(){
		return this.getClass().getCanonicalName();
	}
	
	public String getSimpleName(){
		return this.getClass().getSimpleName();
	}
	
	public boolean isClassModifier(){
		return isClassModifier;
	}
	
	/**
	 * @return the baseClasses
	 */
	public List<String> getBaseClasses() {
		return baseClasses;
	}
	
	/**
	 * @return the optimalSolutions
	 */
	public OWLClassExpression getOptimalSolution(OWLClass cls) {
		return optimalSolutions.get(cls);
	}

	/**
	 * @param baseClasses the baseClasses to set
	 */
	public void setBaseClasses(List<String> baseClasses) {
		Modifier.baseClasses = baseClasses;
	}

	public void setBaseClasses(Set<OWLClass> namedClasses) {
		for (OWLClass namedClass : namedClasses) {
			baseClasses.add(namedClass.toStringID());
		}
	}
	
	
	/**
	 * @return the modifiedClasses
	 */
	public List<String> getModifiedClasses() {
		return modifiedClassesURIs;
	}
	
	public Set<OWLClass> getModifiedOWLClasses() {
		Set<OWLClass> namedClasses = new TreeSet<OWLClass>();
		for (String mc : modifiedClassesURIs) {
			namedClasses.add(owlDataFactory.getOWLClass(IRI.create(mc)));
		}
		return namedClasses;
	}

	/**
	 * @param modifiedClasses the modifiedClasses to set
	 */
	public void setModifiedClasses(List<String> modifiedClasses) {
		Modifier.modifiedClassesURIs = modifiedClasses;
	}
	
	/**
	 * @return the nameSpace
	 */
	public static String getNameSpace() {
		return nameSpace;
	}

	/**
	 * @param nameSpace the nameSpace to set
	 */
	public static void setNameSpace(String nameSpace) {
		Modifier.nameSpace = nameSpace;
	}

	public Modifier(Model m){
		baseModel = m;
	}
	
	public Modifier(){
	}
	
	public abstract Model destroy(Model subModel);
	
	/**
	 * @return the baseModel
	 */
	public static Model getBaseModel() {
		return baseModel;
	}

	/**
	 * @param baseModel the baseModel to set
	 */
	public static void setBaseModel(Model baseModel) {
		Modifier.baseModel = baseModel;
	}
	
	/**
	 * @return a sub model contains the destroyed Properties
	 */
	public static Model getDestroyedPropertiesModel(Model m) {
		for(Property p: properties){
			StmtIterator sItr = m.listStatements(null, p,(RDFNode) null);
			while(sItr.hasNext()){
				Statement stmt = sItr.nextStatement();
				destroyedPropertiesModel.add(stmt);
			}
		}
		return destroyedPropertiesModel;
	}

	/**
	 * @param classUri
	 * @return Model containing all instances of the input class
	 * @author sherif
	 */
	protected Model getClassInstancesModel(String classUri){
		Model result=ModelFactory.createDefaultModel();
		String sparqlQueryString= "CONSTRUCT {?s ?p ?o} WHERE {?s a <"+classUri+">. ?s ?p ?o}";
		QueryFactory.create(sparqlQueryString);
		QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, baseModel);
		result = qexec.execConstruct();
		return result;
	}
	
	protected Model getClassInstancesModel(String classUri, Model m){
		Model result=ModelFactory.createDefaultModel();
		String sparqlQueryString= "CONSTRUCT {?s ?p ?o} WHERE {?s a <"+classUri+">. ?s ?p ?o}";
		QueryFactory.create(sparqlQueryString);
		QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, m);
		result =qexec.execConstruct();
		return result;
	}
	
	protected Model getClassInstancesModel(String classUri, Model m, long limit, long offset){
		Model result = ModelFactory.createDefaultModel();
		String sparqlQueryString= 	"CONSTRUCT {?s ?p ?o} " +
									" WHERE {?s a <"+classUri+">. ?s ?p ?o} " +
									" LIMIT " + limit + 
									" OFFSET " + offset;
		QueryFactory.create(sparqlQueryString);
		QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, m);
		result = qexec.execConstruct();
		return result;
	}
	
	
	/**
	 * @param subModelSize
	 * @param baseModelOffset
	 * @return A sub model of the fromModel with size subModelSize and starting from offset baseModelOffset
	 * @author Sherif
	 */
	protected Model getSubModel(Model inputModel, long subModelSize, long baseModelOffset) {
		Model subModel = ModelFactory.createDefaultModel();

		StmtIterator sItr = inputModel.listStatements();

		//skip statements tell the offset reached
		for(int i = 0 ; i < baseModelOffset ; i++){
			sItr.nextStatement();		
		}

		//Copy the sub-model
		for(int i = 0 ; i < subModelSize ; i++){
			Statement stat = sItr.nextStatement();	
			subModel.add(stat);
		}
		return subModel;
	}

	/**
	 * @param destroyedPropertiesModel the destroyedPropertiesModel to set
	 */
	public static void setDestroyedPropertiesModel(Model destroyedPropertiesModel) {
		Modifier.destroyedPropertiesModel = destroyedPropertiesModel;
	}

	public static List<String> getClasses(Model m){
		List<String> classNames = new ArrayList<String>();
		String sparqlQueryString= "select distinct ?class where {?s a ?class}";
		QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, m);
		ResultSet results = qexec.execSelect();
		
		while (results.hasNext()) {
		    QuerySolution row = results.next();
		    RDFNode className = row.get("class");
		    if(!nameSpace.equals("")){
		    	if(!className.toString().startsWith(nameSpace)) continue;
		    }
		    classNames.add(className.toString());
		}
		return classNames;
	}
	
	/**
	 * @param inputModel
	 * @param oldClassName
	 * @param newClassName
	 * @return
	 * @author Sherif
	 */
	public Model renameClass(final Model inputModel, String oldClassName,String newClassName) {
		Model result = ModelFactory.createDefaultModel();
		StmtIterator sItr = inputModel.listStatements();
		Resource oldClassURI = ResourceFactory.createResource(oldClassName);
		Resource newClassURI = ResourceFactory.createResource(newClassName);
        while(sItr.hasNext()){
            Statement stmt = sItr.nextStatement();
            if(stmt.getPredicate().equals(RDF.type) && stmt.getObject().equals(oldClassURI)){
                result.add(stmt.getSubject(), RDF.type, newClassURI);
            }else{
                result.add(stmt);
            }
        }
		return result;
	}
	
	
	
	
	   /**
     * @param model
     * @param oldClassUri
     * @param newClassUri
     * @return
     * @author Sherif
     */
    public Model _renameClass(Model model, String oldClassUri,String newClassUri) {
        Model result = model;
        Model inClassModel = ModelFactory.createDefaultModel();
        String sparqlQueryString = "CONSTRUCT {?s a <"+oldClassUri+">} WHERE {?s a <"+oldClassUri+">.}";
        QueryFactory.create(sparqlQueryString);
        QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, model);
        inClassModel = qexec.execConstruct();
        result.remove(inClassModel);
        StmtIterator sItr = inClassModel.listStatements();
        
        RDFNode newClassURI = ResourceFactory.createResource(newClassUri);
        while(sItr.hasNext()){
            Statement stmt = sItr.nextStatement();
            result.add(stmt.getSubject(), RDF.type, newClassURI);
        }
        return result;
    }
	

	/**
	 * @return the properties
	 */
	public List<Property> getProperties() {
		return properties;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(List<Property> p) {
		properties = p;
	}

	/**
	 * @param properties the properties to set
	 */
	public void addProperty(Property p) {
		properties.add(p);
	}


	/**
	 * @param fileNameOrUri
	 * @return Model containing thefileNameOrUri
	 * @author Sherif
	 */
	public static Model loadModel(String fileNameOrUri){
	    long start = System.currentTimeMillis();
	    Model model = ModelFactory.createDefaultModel();
		java.io.InputStream in = FileManager.get().open( fileNameOrUri );
		if (in == null) {
			throw new IllegalArgumentException(
					"File/URI: " + fileNameOrUri + " not found");
		}
		if(fileNameOrUri.endsWith(".ttl")){
			System.out.println("Opening Turtle file");
			model.read(in, null, "TTL");
		}else if(fileNameOrUri.endsWith(".rdf")){
			System.out.println("Opening RDFXML file");
			model.read(in, null);
		}else if(fileNameOrUri.endsWith(".nt")){
			System.out.println("Opening N-Triples file");
			model.read(in, null, "N-TRIPLE");
		}else{
			System.out.println("Content negotiation to get RDFXML from " + fileNameOrUri);
			model.read(fileNameOrUri);
		}

		System.out.println("loading "+ fileNameOrUri + " is done in " +  (System.currentTimeMillis() - start) + "ms");
		System.out.println();
		return model;
	}


}
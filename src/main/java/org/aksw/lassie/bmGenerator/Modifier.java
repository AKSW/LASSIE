/**
 * 
 */
package org.aksw.lassie.bmGenerator;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.dllearner.core.owl.Description;
import org.dllearner.core.owl.NamedClass;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

/**
 * @author Sherif
 *
 */
public abstract class Modifier {
	static Model baseModel = ModelFactory.createDefaultModel();
	static Model destroyedPropertiesModel = ModelFactory.createDefaultModel(); 		//destroyed properties model
	static Model destroyedClassModel      = ModelFactory.createDefaultModel();     //destroyed class model
	static Model destroyedModel           = ModelFactory.createDefaultModel();     //final destroyed model
	static List<Property> properties = new ArrayList<Property>();
	double destructionRatio = 0.5;
	long destroyedInstancesCount;
	boolean destroyProperty = false;
	
	public String inputClassUri  = null;
	public String outputClassUri = null;
	
	static protected String  nameSpace = "";
	static protected List<String> baseClasses     = new ArrayList<String>();
	static protected List<String> modifiedClasses = new ArrayList<String>();
	protected boolean isClassModifier = false;
	
	Map<NamedClass, Description> optimalSolutions = new HashMap<NamedClass, Description>();
	
	Map<NamedClass, NamedClass> alteredClasses = new HashMap<NamedClass, NamedClass>();
	
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
	public Description getOptimalSolution(NamedClass cls) {
		return optimalSolutions.get(cls);
	}

	/**
	 * @param baseClasses the baseClasses to set
	 */
	public void setBaseClasses(List<String> baseClasses) {
		Modifier.baseClasses = baseClasses;
	}

	public void setBaseClasses(Set<NamedClass> namedClasses) {
		for (NamedClass namedClass : namedClasses) {
			baseClasses.add(namedClass.getName());
		}
	}
	
	
	/**
	 * @return the modifiedClasses
	 */
	public List<String> getModifiedClasses() {
		return modifiedClasses;
	}
	
	public Set<NamedClass> getModifiedNamedClasses() {
		Set<NamedClass> namedClasses = new TreeSet<NamedClass>();
		for (String mc : modifiedClasses) {
			namedClasses.add(new NamedClass(mc));
		}
		return namedClasses;
	}

	/**
	 * @param modifiedClasses the modifiedClasses to set
	 */
	public void setModifiedClasses(List<String> modifiedClasses) {
		Modifier.modifiedClasses = modifiedClasses;
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
		baseModel=m;
	}
	
	public Modifier(){
	}
	
	abstract Model destroy(Model subModel);
	
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
		result =qexec.execConstruct();
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
		Model result=ModelFactory.createDefaultModel();
		String sparqlQueryString= 	"CONSTRUCT {?s ?p ?o} " +
									" WHERE {?s a <"+classUri+">. ?s ?p ?o} " +
									" LIMIT " + limit + 
									" OFFSET " + offset;
		QueryFactory.create(sparqlQueryString);
		QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, m);
		result =qexec.execConstruct();
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
		for(int i=0 ; i<baseModelOffset ; i++){
			sItr.nextStatement();		
		}

		//Copy the sub-model
		for(int i=0 ; i<subModelSize ; i++){
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
		    QuerySolution row= results.next();
		    RDFNode className= row.get("class");
		    if(!nameSpace.equals("")){
		    	if(!className.toString().startsWith(nameSpace)) continue;
		    }
		    classNames.add(className.toString());
		}
		return classNames;
	}
	
	/**
	 * @param model
	 * @param oldClassUri
	 * @param newClassUri
	 * @return
	 * @author Sherif
	 */
	public Model renameClass(Model model, String oldClassUri,String newClassUri) {
		Model result = model;
		Model inClassModel = ModelFactory.createDefaultModel();
		String sparqlQueryString= "CONSTRUCT {?s a <"+oldClassUri+">} WHERE {?s a <"+oldClassUri+">.}";
		QueryFactory.create(sparqlQueryString);
		QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, model);
		inClassModel = qexec.execConstruct();
		result.remove(inClassModel);
		StmtIterator sItr = inClassModel.listStatements();
		while(sItr.hasNext()){
			Statement stmt = sItr.nextStatement();
			Resource subject = stmt.getSubject();
			Property predicate = stmt.getPredicate();
			RDFNode object = ResourceFactory.createResource(newClassUri);
			result.add( subject, predicate, object);
		}
		return result;
	}
	
	public Model renameAllClasses(Model model, String oldClassUri,String newClassUri) {
		Model result=model;
		Model inClassModel=ModelFactory.createDefaultModel();
		String sparqlQueryString= "CONSTRUCT {?s a <"+oldClassUri+">} WHERE {?s a <"+oldClassUri+">.}";
		QueryFactory.create(sparqlQueryString);
		QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, model);
		inClassModel = qexec.execConstruct();

		result.remove(inClassModel);

		StmtIterator sItr = inClassModel.listStatements();
		while(sItr.hasNext()){
			Statement stmt = sItr.nextStatement();
			Resource subject = stmt.getSubject();
			Property predicate = stmt.getPredicate();
			RDFNode object = ResourceFactory.createResource(newClassUri);
			result.add( subject, predicate, object);
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
		Model model=ModelFactory.createDefaultModel();
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

		System.out.println("loading "+ fileNameOrUri + " is done!!");
		System.out.println();
		return model;
	}


}

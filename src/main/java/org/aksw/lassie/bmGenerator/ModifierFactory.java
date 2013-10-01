/**
 * 
 */
package org.aksw.lassie.bmGenerator;

import com.hp.hpl.jena.rdf.model.Model;


/**
 * @author sherif
 *
 */
public class ModifierFactory {

	public static final String INSTANCE_IDENTITY_MODIFIER 	= "instanceIdentity";
	public static final String INSTANCE_ABBREVIATION_MODIFIER	= "abbreviation";
	public static final String INSTANCE_ACRONYM_MODIFIER 		= "acronym";
	public static final String INSTANCE_MERGE_MODIFIER 		= "merge";
	public static final String INSTANCE_MISSPELING_MODIFIER 	= "misspelling";
	public static final String INSTANCE_PERMUTATION_MODIFIER 	= "permutation";
	public static final String INSTANCE_SPLIT_MODIFIER 		= "split";
	public static final String INSTANCE_SYNONYM_MODIFIER 		= "synoym";
	
	public static final String CLASS_IDENTITY_MODIFIER	= "classIdentity";
	public static final String CLASS_MERGE_MODIFIER 		= "classMerge";
	public static final String CLASS_SPLIT_MODIFIER 		= "classSplit";
	public static final String CLASS_DELETE_MODIFIER 		= "classDelete";
	public static final String CLASS_RENAME_MODIFIER 		= "classRename";
	public static final String CLASS_TYPE_DELETE_MODIFIER	= "classTypeDelete";

	public static Modifier getModifier(String name) {
		System.out.println("Getting Modifier with name: "+name);

		if (name.equalsIgnoreCase(INSTANCE_IDENTITY_MODIFIER))
			return new InstanceIdentityModifier();
		if(name.equalsIgnoreCase(INSTANCE_ABBREVIATION_MODIFIER))
			return new InstanceAbbreviationModifier();
		if(name.equalsIgnoreCase(INSTANCE_ACRONYM_MODIFIER))
			return new InstanceAcronymModifier();
		if (name.equalsIgnoreCase(INSTANCE_MERGE_MODIFIER))
			return new InstanceMergeModifier();
		if (name.equalsIgnoreCase(INSTANCE_MISSPELING_MODIFIER))
			return new InstanceMisspellingModifier();
		if (name.equalsIgnoreCase(INSTANCE_PERMUTATION_MODIFIER))
			return new InstancePermutationModifier();
		if (name.equalsIgnoreCase(INSTANCE_SPLIT_MODIFIER))
			return new InstanceSplitModifier();
		if (name.equalsIgnoreCase(INSTANCE_SYNONYM_MODIFIER))
			return new InstanceSynonymModifier();
		
		if (name.equalsIgnoreCase(CLASS_IDENTITY_MODIFIER))
			return new ClassIdentityModifier();
		if (name.equalsIgnoreCase(CLASS_MERGE_MODIFIER))
			return new ClassMergeModifier();
		if (name.equalsIgnoreCase(CLASS_SPLIT_MODIFIER))
			return new ClassSplitModifier();
		if (name.equalsIgnoreCase(CLASS_DELETE_MODIFIER))
			return new ClassDeleteModifier();
		if (name.equalsIgnoreCase(CLASS_RENAME_MODIFIER))
			return new ClassRenameModifier();
		if (name.equalsIgnoreCase(CLASS_TYPE_DELETE_MODIFIER))
			return new ClassTypeDeleteModifier();
		
		System.out.println("Sorry, The Modifier " + name + " is not yet implemented ... Exit with error");
		System.exit(1);
		return null;
	}
	
	public static Modifier getModifier(String name, Model m) {
		System.out.println("Getting Modifier with name: "+name);

		if (name.equalsIgnoreCase(INSTANCE_IDENTITY_MODIFIER))
			return new InstanceIdentityModifier();
		if(name.equalsIgnoreCase(INSTANCE_ABBREVIATION_MODIFIER))
			return new InstanceAbbreviationModifier();
		if(name.equalsIgnoreCase(INSTANCE_ACRONYM_MODIFIER))
			return new InstanceAcronymModifier();
		if (name.equalsIgnoreCase(INSTANCE_MERGE_MODIFIER))
			return new InstanceMergeModifier();
		if (name.equalsIgnoreCase(INSTANCE_MISSPELING_MODIFIER))
			return new InstanceMisspellingModifier();
		if (name.equalsIgnoreCase(INSTANCE_PERMUTATION_MODIFIER))
			return new InstancePermutationModifier();
		if (name.equalsIgnoreCase(INSTANCE_SPLIT_MODIFIER))
			return new InstanceSplitModifier();
		if (name.equalsIgnoreCase(INSTANCE_SYNONYM_MODIFIER))
			return new InstanceSynonymModifier();
		
		if (name.equalsIgnoreCase(CLASS_IDENTITY_MODIFIER))
			return new ClassIdentityModifier();
		if (name.equalsIgnoreCase(CLASS_MERGE_MODIFIER))
			return new ClassMergeModifier();
		if (name.equalsIgnoreCase(CLASS_SPLIT_MODIFIER))
			return new ClassSplitModifier();
		if (name.equalsIgnoreCase(CLASS_DELETE_MODIFIER))
			return new ClassDeleteModifier();
		if (name.equalsIgnoreCase(CLASS_RENAME_MODIFIER))
			return new ClassRenameModifier();
		if (name.equalsIgnoreCase(CLASS_TYPE_DELETE_MODIFIER))
			return new ClassTypeDeleteModifier();
		
		System.out.println("Sorry, The Modifier " + name + " is not yet implemented ... Exit with error");
		System.exit(1);
		return null;
	}
}

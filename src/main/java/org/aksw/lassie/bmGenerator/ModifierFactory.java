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

	public static final String ABBREVIATION_MODIFIER 	= "abbreviation";
	public static final String ACRONYM_MODIFIER 		= "acronym";
	public static final String MERGE_MODIFIER 		= "merge";
	public static final String MISSPELING_MODIFIER 	= "misspelling";
	public static final String PERMUTATION_MODIFIER 	= "permutation";
	public static final String SPLIT_MODIFIER 		= "split";
	public static final String SYNONYM_MODIFIER 		= "synoym";
	
	public static final String CLASS_MERGE_MODIFIER 	= "classMerge";
	public static final String CLASS_SPLIT_MODIFIER 	= "classSplit";
	public static final String CLASS_DELETE_MODIFIER 	= "classDelete";

	public static Modifier getModifier(String name) {
		System.out.println("Getting Modifier with name "+name);

		if(name.equalsIgnoreCase(ABBREVIATION_MODIFIER))
			return new AbbreviationModifier();
		if(name.equalsIgnoreCase(ACRONYM_MODIFIER))
			return new AcronymModifier();
		if (name.equalsIgnoreCase(MERGE_MODIFIER))
			return new MergeModifier();
		if (name.equalsIgnoreCase(MISSPELING_MODIFIER))
			return new MisspellingModifier();
		if (name.equalsIgnoreCase(PERMUTATION_MODIFIER))
			return new PermutationModifier();
		if (name.equalsIgnoreCase(SPLIT_MODIFIER))
			return new SplitModifier();
		if (name.equalsIgnoreCase(SYNONYM_MODIFIER))
			return new SynonymModifier();
		if (name.equalsIgnoreCase(CLASS_MERGE_MODIFIER))
			return new ClassMergeModifier();
		if (name.equalsIgnoreCase(CLASS_SPLIT_MODIFIER))
			return new ClassSplitModifier();
		if (name.equalsIgnoreCase(CLASS_DELETE_MODIFIER))
			return new ClassDeleteModifier();
		
		System.out.println("Sorry, The Modifier " + name + " is not yet implemented ... Exit with error");
		System.exit(1);
		return null;
	}
	
	public static Modifier getModifier(String name, Model m) {
		System.out.println("Getting Modifier with name "+name);

		if(name.equalsIgnoreCase(ABBREVIATION_MODIFIER))
			return new AbbreviationModifier(m);
		if(name.equalsIgnoreCase(ACRONYM_MODIFIER))
			return new AcronymModifier(m);
		if (name.equalsIgnoreCase(MERGE_MODIFIER))
			return new MergeModifier(m);
		if (name.equalsIgnoreCase(MISSPELING_MODIFIER))
			return new MisspellingModifier(m);
		if (name.equalsIgnoreCase(PERMUTATION_MODIFIER))
			return new PermutationModifier(m);
		if (name.equalsIgnoreCase(SPLIT_MODIFIER))
			return new SplitModifier(m);
		if (name.equalsIgnoreCase(SYNONYM_MODIFIER))
			return new SynonymModifier(m);
		if (name.equalsIgnoreCase(CLASS_MERGE_MODIFIER))
			return new ClassMergeModifier(m);
		if (name.equalsIgnoreCase(CLASS_SPLIT_MODIFIER))
			return new ClassSplitModifier(m);
		if (name.equalsIgnoreCase(CLASS_DELETE_MODIFIER))
			return new ClassDeleteModifier(m);
		
		System.out.println("Sorry, The Modifier " + name + " is not yet implemented ... Exit with error");
		System.exit(1);
		return null;
	}
}

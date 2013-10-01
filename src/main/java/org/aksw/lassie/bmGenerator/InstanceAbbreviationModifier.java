/**
 *
 */
package org.aksw.lassie.bmGenerator;

import java.util.Random;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * @author sherif
 *
 */
public class InstanceAbbreviationModifier extends Modifier {

    /**
	 * @param m
	 *@author sherif
	 */
	public InstanceAbbreviationModifier(Model m) {
		super(m);
	}

	public InstanceAbbreviationModifier() {
	}
	/*
     * (non-Javadoc) @see de.uni_leipzig.simba.benchmarker.Modifier#destroy()
     */
    @Override
    Model destroy(Model subModel) {
        System.out.println();
        System.out.println("Abbriavating " + subModel.size() + " triples.");
        StmtIterator sItr = subModel.listStatements();

        while (sItr.hasNext()) {
            Statement stmt = sItr.nextStatement();
            Statement abbreviatedStatement = abbreviat(stmt);
            destroyedModel.add(abbreviatedStatement);
        }
        return destroyedModel;
    }

    private Statement abbreviat(Statement stmt) {
        if (!stmt.getObject().isLiteral()) {
            System.err.println(stmt.getObject() + " is not a literal object");
            System.err.println("Unable to abbreviat a non-literal object!!");
//			System.exit(1);
            return stmt;
        }
        String objectLitral = stmt.getObject().asNode().getLiteral().getLexicalForm();
        String abbreviatedLitral = abbreviat(objectLitral);
        RDFNode abbreviatedObject = ResourceFactory.createTypedLiteral(abbreviatedLitral);
        Statement result = ResourceFactory.createStatement(stmt.getSubject(), stmt.getPredicate(), abbreviatedObject);
        return result;
    }

    private String abbreviat(String s) {
        if (s.length() <= 2) {
            return s;
        }

        String result = new String();
        int randIndex;
        Random generator = new Random();

        if (s.contains(" ")) {
            String[] splitStr = s.split(" ", 10);
            try {
                randIndex = generator.nextInt(splitStr.length);
                if (splitStr[randIndex].length() == 0) {
                    randIndex = 0;
                }
            } catch (Exception e) {
                randIndex = 0;
            }
            if (!splitStr[randIndex].equals(" ") && !splitStr[randIndex].equals("")) {
                splitStr[randIndex] = splitStr[randIndex].replace(splitStr[randIndex].substring(1), ".");
            }

            for (int i = 0; i < splitStr.length; i++) {
                result += splitStr[i] + " ";
            }

        } else {
            result = s.replace(s.substring(1), ".");
        }
//		System.out.println("Source:    "+s);
//		System.out.println("Destroyed: "+result);
        return result;
    }
}

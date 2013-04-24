/**
 *
 */
package org.aksw.lassie.bmGenerator;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * @author sherif
 *
 */
public class PermutationModifier extends Modifier {

    /*
     * (non-Javadoc) @see de.uni_leipzig.simba.benchmarker.Modifier#destroy()
     */
    @Override
    Model destroy(Model subModel) {
        System.out.println();
        System.out.println("Performing permutation of " + subModel.size() + " triples.");

        StmtIterator sItr = subModel.listStatements();

        while (sItr.hasNext()) {
            Statement stmt = sItr.nextStatement();
            Statement permutatedStatement = permutat(stmt);
            destroyedModel.add(permutatedStatement);
        }
        return destroyedModel;
    }

    private Statement permutat(Statement stmt) {
        if (!stmt.getObject().isLiteral()) {
            System.err.println(stmt.getObject() + " is not a literal object");
            System.err.println("Can NOT permutat a non-literal object!!");
//			System.exit(1);
            return stmt;
        }
        String objectLitral = stmt.getObject().asNode().getLiteral().getLexicalForm();
        String permutatedLitral = permutat(objectLitral);
        RDFNode permutatedObject = ResourceFactory.createTypedLiteral(permutatedLitral);
        Statement result = ResourceFactory.createStatement(stmt.getSubject(), stmt.getPredicate(), permutatedObject);
        return result;
    }

    private String permutat(String s) {
        String result = new String();

        if (s.contains(" ")) {
            String[] splitStr = s.split(" ", 2);
            result = splitStr[1] + " " + splitStr[0];
        } else {
            result = s;
        }
//		System.out.println("Source:    "+s);
//		System.out.println("Destroyed: "+result);
        return result;
    }
}

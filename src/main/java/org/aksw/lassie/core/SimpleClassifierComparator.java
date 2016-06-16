/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.lassie.core;

import java.util.Comparator;

import de.uni_leipzig.simba.selfconfig.SimpleClassifier;

/**
 *
 * @author ngonga
 */
public class SimpleClassifierComparator implements Comparator<SimpleClassifier> {

    @Override
    public int compare(SimpleClassifier o1, SimpleClassifier o2) {
        if (o1.fMeasure > o2.fMeasure) {
            return 1;
        }
        if (o1.fMeasure < o2.fMeasure) {
            return -1;
        }
        return 0;
    }
}

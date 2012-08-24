/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml.crf;

import cc.mallet.classify.Classifier;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Iterator;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class MyClassifierEvaluator {

    private Classifier classifier;

    public MyClassifierEvaluator(String path2Classifier) {
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new FileInputStream(path2Classifier));
            classifier =  (Classifier) ois.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public MyClassifierEvaluator(Classifier classifier) {
        this.classifier = classifier;
    }
    
    public void evaluate(InstanceList testingInstances) {
        System.err.println("Testing accuracy: " + classifier.getAccuracy(testingInstances));
            LabelAlphabet labelAlphabet = classifier.getLabelAlphabet();
            Iterator iterator = labelAlphabet.iterator();
            while (iterator.hasNext()) {
                Object label = iterator.next();
                double p = classifier.getPrecision(testingInstances, label);
                double r = classifier.getRecall(testingInstances, label);
                double f1 = classifier.getF1(testingInstances, label);

                System.out.println("Precision[" + label + "] = " + p);
                System.out.println("Recall[" + label + "] = " + r);
                System.out.println("F1[" + label + "] = " + f1);
                System.out.println("");
            }
    }
}


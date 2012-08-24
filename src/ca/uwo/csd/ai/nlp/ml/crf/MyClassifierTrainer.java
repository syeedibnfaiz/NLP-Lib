/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml.crf;

import ca.uwo.csd.ai.nlp.ml.ExtendedTrial;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSelection;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple wrapper for <code>ClassifierTrainer</code>.
 * @author Syeed Ibn Faiz
 */
public class MyClassifierTrainer {
    ClassifierTrainer trainer;
    Classifier classifier;
    
    public MyClassifierTrainer() {
        trainer = new MaxEntTrainer();
    }

    public MyClassifierTrainer(double prior) {
        trainer = new MaxEntTrainer(prior);
    }    

    public MyClassifierTrainer(ClassifierTrainer trainer) {
        this.trainer = trainer;
    }
    public Classifier train(InstanceList trainingInstances) {        
        classifier = trainer.train(trainingInstances);
        writeExtremeFeatures();
        return classifier;
    }
    public Classifier train(InstanceList trainingInstances, InstanceList testingInstances) {
        classifier = trainer.train(trainingInstances);
        printPerformance(trainingInstances, "Training\n=======");
        if (testingInstances != null) {
            printPerformance(testingInstances, "Testing\n=======");
        }        
        return classifier;
    }

    private void writeExtremeFeatures() {
        if (classifier instanceof MaxEnt) {
            MaxEnt m = (MaxEnt) classifier;
            try {
                //PrintWriter rankWriter = new PrintWriter("rank.log");
                PrintWriter extremeWriter = new PrintWriter("extreme.log");
                //m.printRank(rankWriter);
                m.printExtremeFeatures(extremeWriter, 70);
                //rankWriter.close();
                extremeWriter.close();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(MyClassifierTrainer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    /**
     * Train a classifier
     * @param trainingInstances
     * @param trainingPortion The percentage to be used for training (<=1.0), the rest is used for testing.
     * @return
     */
    public Classifier train(InstanceList trainingInstances, double trainingPortion) {
        InstanceList[] instanceLists =
                trainingInstances.split(new Random(), new double[]{trainingPortion, (1-trainingPortion)});
        //InstanceList[] instanceLists =
        //        trainingInstances.splitInOrder(new double[]{trainingPortion, (1-trainingPortion)});
        return this.train(instanceLists[0], instanceLists[1]);
    }

    public void printPerformance(InstanceList instanceList, String description) {
        System.out.println(description);
        System.out.println("Accuracy: " + classifier.getAccuracy(instanceList));
        LabelAlphabet labelAlphabet = classifier.getLabelAlphabet();
        Iterator iterator = labelAlphabet.iterator();
        while (iterator.hasNext()) {
            Object label = iterator.next();
            double p = classifier.getPrecision(instanceList, label);
            double r = classifier.getRecall(instanceList, label);
            double f1 = classifier.getF1(instanceList, label);

            System.out.println("Precision[" + label + "] = " + p);
            System.out.println("Recall[" + label + "] = " + r);
            System.out.println("F1[" + label + "] = " + f1);
            System.out.println("");
        }

        ExtendedTrial trial = new ExtendedTrial(classifier, instanceList);
        System.out.println("Overall performance\n=====");
        System.out.println("Precision: " + trial.getPrecision());
        System.out.println("Recall: " + trial.getRecall());
        System.out.println("F1: " + trial.getF1());
        System.out.println("Fall-out: " + trial.getFallOut());

        trial.showErrors();
    }
    
    public void saveModel(String fileName) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(classifier);
        } catch (IOException ex) {
            Logger.getLogger(CRFTrainer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Classifier getClassifier() {
        return classifier;
    }

    
}

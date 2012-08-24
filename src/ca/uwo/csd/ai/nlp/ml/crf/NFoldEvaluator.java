/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml.crf;

import cc.mallet.classify.AdaBoostTrainer;
import cc.mallet.classify.BaggingTrainer;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.DecisionTreeTrainer;
import cc.mallet.classify.FeatureSelectingClassifierTrainer;
import cc.mallet.classify.MaxEntGETrainer;
import cc.mallet.classify.MaxEntL1Trainer;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.NaiveBayesEMTrainer;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSelector;
import cc.mallet.types.InstanceList;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class NFoldEvaluator {

    public void evaluate(MyClassifierTrainer trainer, InstanceList instances, int n) {
        InstanceList.CrossValidationIterator cvIt = instances.crossValidationIterator(n);                
        Alphabet targetAlphabet = instances.getTargetAlphabet();
        double result[][][] = new double[n][4][targetAlphabet.size()];
        Object labels[] = new Object[targetAlphabet.size()];
        targetAlphabet.toArray(labels);

        int run = 0;
        while (cvIt.hasNext()) {
            
            InstanceList[] nextSplit = cvIt.nextSplit();            
            InstanceList trainingInstances = nextSplit[0];
            InstanceList testingInstances = nextSplit[1];
            
            //added at 3:06pm on 06/12/2011
            //trainer should be initialized every time!
            trainer = new MyClassifierTrainer();
            Classifier classifier = trainer.train(trainingInstances, testingInstances);
            //Classifier classifier = trainer.train(trainingInstances);
            Trial trial = new Trial(classifier, testingInstances);

            result[run][0][0] = trial.getAccuracy();
            for (int i = 0; i < labels.length; i++) {
                result[run][1][i] = trial.getPrecision(labels[i]);
                result[run][2][i] = trial.getRecall(labels[i]);
                result[run][3][i] = trial.getF1(labels[i]);
            }
            run++;
        }
        System.out.println("--------"+n+"-fold cross validation result--------");
        double avgAccuracy = 0.0;
        double avgPrecision[] = new double[targetAlphabet.size()];
        double avgRecall[] = new double[targetAlphabet.size()];
        double avgF1[] = new double[targetAlphabet.size()];
        for (int i = 0; i < n; i++) {
            avgAccuracy += result[i][0][0];
            for (int j = 0; j < targetAlphabet.size(); j++) {
                avgPrecision[j] += result[i][1][j];
                avgRecall[j] += result[i][2][j];
                avgF1[j] += result[i][3][j];
            }
        }
        
        double sdAccuracy = 0;
        double sdPrecision[] = new double[targetAlphabet.size()];
        double sdRecall[] = new double[targetAlphabet.size()];
        double sdF1[] = new double[targetAlphabet.size()];
        for (int i = 0; i < n; i++) {
            sdAccuracy += (avgAccuracy/n - result[i][0][0])*(avgAccuracy/n - result[i][0][0]);
            for (int j = 0; j < targetAlphabet.size(); j++) {
                sdPrecision[j] += (avgPrecision[j]/n - result[i][1][j])*(avgPrecision[j]/n - result[i][1][j]);
                sdRecall[j] += (avgRecall[j]/n - result[i][2][j])*(avgRecall[j]/n - result[i][2][j]);
                sdF1[j] += (avgF1[j]/n - result[i][3][j])*(avgF1[j]/n - result[i][3][j]);
            }
        }
        
        System.out.println("Accuracy: " + avgAccuracy/n+ " (+- " + Math.sqrt(sdAccuracy/n) + ")");
        for (int i = 0; i < labels.length; i++) {
            System.out.println("Precision["+labels[i]+"]: " + avgPrecision[i]/n + " (+- " + Math.sqrt(sdPrecision[i]/n) + ")");
            System.out.println("Recall["+labels[i]+"]: " + avgRecall[i]/n+ " (+- " + Math.sqrt(sdRecall[i]/n) + ")");
            System.out.println("F1["+labels[i]+"]: " + avgF1[i]/n+ " (+- " + Math.sqrt(sdF1[i]/n) + ")");
        }

    }
    
    /**
     * Approximate document-wise n-fold cv
     * @param trainer
     * @param instances
     * @param n 
     */
    public void evaluateInOrder(MyClassifierTrainer trainer, InstanceList instances, int n) {
        
        double proportions[] = new double[n];
        for (int i = 0; i < n; i++) {
            proportions[i] = 1.0/n;
        }
        InstanceList[] splits = instances.splitInOrder(proportions);
        Alphabet targetAlphabet = instances.getTargetAlphabet();
        double result[][][] = new double[n][4][targetAlphabet.size()];
        Object labels[] = new Object[targetAlphabet.size()];
        targetAlphabet.toArray(labels);

        int run = 0;
        for (int k = 0; k < n; k++) {                        
            InstanceList trainingInstances = new InstanceList(splits[0].getPipe());
            InstanceList testingInstances = splits[k];
            for (int i = 0; i < n; i++) {
                if (i != k) {
                    trainingInstances.addAll(splits[i]);
                }
            }
            
            //added at 3:06pm on 06/12/2011
            //trainer should be initialized every time!
            trainer = new MyClassifierTrainer();
            Classifier classifier = trainer.train(trainingInstances, testingInstances);
            //Classifier classifier = trainer.train(trainingInstances);
            Trial trial = new Trial(classifier, testingInstances);

            result[run][0][0] = trial.getAccuracy();
            for (int i = 0; i < labels.length; i++) {
                result[run][1][i] = trial.getPrecision(labels[i]);
                result[run][2][i] = trial.getRecall(labels[i]);
                result[run][3][i] = trial.getF1(labels[i]);
            }
            run++;
        }
        System.out.println("--------"+n+"-fold cross validation result--------");
        double avgAccuracy = 0.0;
        double avgPrecision[] = new double[targetAlphabet.size()];
        double avgRecall[] = new double[targetAlphabet.size()];
        double avgF1[] = new double[targetAlphabet.size()];
        for (int i = 0; i < n; i++) {
            avgAccuracy += result[i][0][0];
            for (int j = 0; j < targetAlphabet.size(); j++) {
                avgPrecision[j] += result[i][1][j];
                avgRecall[j] += result[i][2][j];
                avgF1[j] += result[i][3][j];
            }
        }
        
        double sdAccuracy = 0;
        double sdPrecision[] = new double[targetAlphabet.size()];
        double sdRecall[] = new double[targetAlphabet.size()];
        double sdF1[] = new double[targetAlphabet.size()];
        for (int i = 0; i < n; i++) {
            sdAccuracy += (avgAccuracy/n - result[i][0][0])*(avgAccuracy/n - result[i][0][0]);
            for (int j = 0; j < targetAlphabet.size(); j++) {
                sdPrecision[j] += (avgPrecision[j]/n - result[i][1][j])*(avgPrecision[j]/n - result[i][1][j]);
                sdRecall[j] += (avgRecall[j]/n - result[i][2][j])*(avgRecall[j]/n - result[i][2][j]);
                sdF1[j] += (avgF1[j]/n - result[i][3][j])*(avgF1[j]/n - result[i][3][j]);
            }
        }
        
        System.out.println("Accuracy: " + avgAccuracy/n+ " (+- " + Math.sqrt(sdAccuracy/n) + ")");
        for (int i = 0; i < labels.length; i++) {
            System.out.println("Precision["+labels[i]+"]: " + avgPrecision[i]/n + " (+- " + Math.sqrt(sdPrecision[i]/n) + ")");
            System.out.println("Recall["+labels[i]+"]: " + avgRecall[i]/n+ " (+- " + Math.sqrt(sdRecall[i]/n) + ")");
            System.out.println("F1["+labels[i]+"]: " + avgF1[i]/n+ " (+- " + Math.sqrt(sdF1[i]/n) + ")");
        }

    }
}

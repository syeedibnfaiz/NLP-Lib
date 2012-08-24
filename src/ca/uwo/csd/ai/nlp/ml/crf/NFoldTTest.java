/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ml.crf;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ml.PDTBConnectiveInstance;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class NFoldTTest {

    public void evaluate(InstanceList instanceList, int N) {
        try {
            double[] errorRates = genNFoldEvaluate(instanceList, N);
            System.out.println("Error rates:");
            for (int i = 0; i < N; i++) {
                System.out.println(errorRates[i]);
            }
        } catch (IOException ex) {
            Logger.getLogger(NFoldTTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    double[] genNFoldEvaluate(InstanceList instanceList, int N) throws IOException {
        double errorRates[] = new double[N];
        InstanceList.CrossValidationIterator cvIt = instanceList.crossValidationIterator(N);
        int i = 0;
        while (cvIt.hasNext()) {

            InstanceList[] nextSplit = cvIt.nextSplit();
            InstanceList trainingInstances = nextSplit[0];
            InstanceList testingInstances = nextSplit[1];

            MyClassifierTrainer trainer = new MyClassifierTrainer();
            trainer.train(trainingInstances);

            double localCount[] = genEvaluate(trainer.getClassifier(), testingInstances);
            double tp = localCount[0];
            double fp = localCount[1];
            double fn = localCount[2];
            double tn = localCount[3];

            double errorRate = (fp + fn) / (tp + tn + fp + fn);
            errorRates[i++] = errorRate;
        }
        return errorRates;
    }

    double[] genEvaluate(Classifier classifier, InstanceList testingInstances) {
        int countTP = 0;
        int countFP = 0;
        int countFN = 0;
        int countTN = 0;

        for (Instance instance : testingInstances) {
            Classification classification = classifier.classify(instance);
            String target = instance.getTarget().toString();
            if (classification.bestLabelIsCorrect()) {
                if (target.equals("true")) {
                    countTP++;
                } else {
                    countTN++;
                }
            } else {
                if (target.equals("true")) {
                    countFN++;
                } else {
                    countFP++;
                }
            }
        }

        double count[] = new double[]{countTP, countFP, countFN, countTN};
        return count;
    }
}

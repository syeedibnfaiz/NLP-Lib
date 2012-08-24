
package ca.uwo.csd.ai.nlp.weka;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LibSVM;
import weka.core.Instances;
import weka.core.Utils;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class WekaTest {

    public static void main(String[] args) throws IOException, Exception {
        Instances instances = new Instances(new BufferedReader(new FileReader("aimed.arff")));
        instances.setClassIndex(instances.numAttributes() - 1);

        double precision = 0;
        double recall = 0;
        double fmeasure = 0;

        int size = instances.numInstances() / 10;
        int begin = 0;
        int end = size - 1;
        for (int i = 1; i <= 10; i++) {
            System.out.println("Iteration: " + i);
            Instances trainingInstances = new Instances(instances);
            Instances testingInstances = new Instances(instances, begin, (end - begin));
            for (int j = 0; j < (end - begin); j++) {
                trainingInstances.delete(begin);
            }
            
            LibSVM svm = new LibSVM();
            String[] options = Utils.splitOptions("-S 0 -K 2");
            svm.setOptions(options);
            svm.buildClassifier(trainingInstances);
            
            Evaluation evaluation = new Evaluation(testingInstances);
            evaluation.evaluateModel(svm, testingInstances);
            
            precision += evaluation.precision(1);
            recall += evaluation.recall(1);
            fmeasure += evaluation.fMeasure(1);
            
            //update
            begin = end + 1;
            end += size;
            if (i == (9)) {
                end = instances.numInstances();
            }
        }

        System.out.println("Precision: " + precision/10.0);
        System.out.println("Recall: " + recall/10.0);
        System.out.println("Fmeasure: " + fmeasure/10.0);

    }
}

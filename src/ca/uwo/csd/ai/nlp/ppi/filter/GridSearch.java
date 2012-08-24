/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import java.util.ArrayList;
import java.util.List;
import kernel.CustomKernel;
import kernel.KernelManager;
import libsvm.Instance;
import libsvm.SVMPredictor;
import libsvm.SVMTrainer;
import libsvm.svm_model;
import libsvm.svm_parameter;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class GridSearch {
    CustomKernel kernel;
    public GridSearch() {
        kernel = new SimpleKernel();
    }
            
    public double search(List<Instance> relationInstances) {
        return search(relationInstances, 5, -5, 15, 2);
    }
    public double search(List<Instance> relationInstances, int v, int start, int end, int step) {
        double bestFscore = 0;
        double bestC = 0;
        
        for (int i = start; i <= end; i += step) {
            double C = Math.pow(2, i);            
            double fscore = evaluate(relationInstances, v, C);
            System.out.println("C: " + C + ", Fscore: " + fscore);
            if (fscore > bestFscore) {
                bestFscore = fscore;
                bestC = C;
            }
        }
        return bestC;
    }
    
    private double evaluate(List<Instance> relationInstances, int v, double C) {
        int size = relationInstances.size();
        int chunkSize = size / v;
        int begin = 0;
        int end = chunkSize - 1;
        
        double fscore = 0;
        for (int i = 0; i < v; i++) {
            List<Instance> trainingRelationInstances = new ArrayList<Instance>();
            List<Instance> testingRelationInstances = new ArrayList<Instance>();
            for (int j = 0; j < size; j++) {
                if (j >= begin && j <= end) {
                    testingRelationInstances.add(relationInstances.get(j));
                } else {
                    trainingRelationInstances.add(relationInstances.get(j));
                }
            }
            fscore += evaluate(trainingRelationInstances, testingRelationInstances, C);
            begin = end + 1;
            end = begin + chunkSize - 1;
            if (end >= size) {
                end = size - 1;
            }
        }
        
        return fscore / v;
    }
    
    public double evaluate(List<Instance> trainingInstances, List<Instance> testingInstances, double C) {                
        
        KernelManager.setCustomKernel(kernel);
        svm_parameter param = new svm_parameter();        
        param.C = C;
        param.cache_size = 2500;

        svm_model model = SVMTrainer.train(trainingInstances, param);
        double[] predictions = SVMPredictor.predict(testingInstances, model, false);

        int tp = 0;
        int fp = 0;
        int fn = 0;        

        for (int k = 0; k < predictions.length; k++) {
            if (testingInstances.get(k).getLabel() == predictions[k]) {
                if (testingInstances.get(k).getLabel() > 0) {
                    tp++;                    
                }
            } else if (testingInstances.get(k).getLabel() > 0) {
                fn++;
            } else {
                fp++;                
            }
        }
        
        double precision = tp * 1.0 / (tp + fp);
        double recall = tp * 1.0 / (tp + fn);
        double fscore = 2 * precision * recall / (precision + recall);
        
        return fscore;
    }
    
    private List<Instance> getSVMInstances(List<RelationInstance> relationInstances) {
        List<Instance> instances = new ArrayList<Instance>();
        for (RelationInstance relInstance : relationInstances) {
            if (relInstance.interaction) {
                instances.add(new Instance(+1, relInstance));
            } else {
                instances.add(new Instance(-1, relInstance));
            }
        }
        return instances;
    }
}

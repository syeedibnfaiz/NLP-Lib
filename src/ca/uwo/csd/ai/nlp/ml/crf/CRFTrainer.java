/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml.crf;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFCacheStaleIndicator;
import cc.mallet.fst.CRFOptimizableByBatchLabelLikelihood;
import cc.mallet.fst.CRFTrainerByThreadedLabelLikelihood;
import cc.mallet.fst.CRFTrainerByValueGradients;
import cc.mallet.fst.MultiSegmentationEvaluator;
import cc.mallet.fst.PerClassAccuracyEvaluator;
import cc.mallet.fst.SimpleTagger;
import cc.mallet.fst.ThreadedOptimizable;
import cc.mallet.fst.TokenAccuracyEvaluator;
import cc.mallet.fst.TransducerEvaluator;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.fst.ViterbiWriter;
import cc.mallet.optimize.Optimizable;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.InstanceList;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.uib.cipr.matrix.io.MatrixVectorWriter;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class CRFTrainer {
    private CRF crf;

    /**
     * Train CRF
     * @param trainingInstances
     * @param testingInstances
     * @param pipe The pipe to be used to construct the CRF
     */
    public void train(InstanceList trainingInstances, InstanceList testingInstances, Pipe pipe) {
        crf = new CRF(pipe, null);
        /*crf.addStatesForLabelsConnectedAsIn(trainingInstances);
        crf.addStartState();
        CRFTrainerByThreadedLabelLikelihood trainer = new CRFTrainerByThreadedLabelLikelihood(crf, 32);
        */
        //new config
        crf.addFullyConnectedStatesForLabels();
        crf.addStartState();
        //crf.setWeightsDimensionAsIn(trainingInstances, false);
        crf.setWeightsDimensionDensely();
        int numThreads = 32;
        CRFOptimizableByBatchLabelLikelihood batchOptLabel =
            new CRFOptimizableByBatchLabelLikelihood(crf, trainingInstances, numThreads);
        ThreadedOptimizable optLabel = new ThreadedOptimizable(
                batchOptLabel, trainingInstances, crf.getParameters().getNumFactors(),
                new CRFCacheStaleIndicator(crf));
        // CRF trainer
        Optimizable.ByGradientValue[] opts =
                new Optimizable.ByGradientValue[]{optLabel};
        // by default, use L-BFGS as the optimizer
        CRFTrainerByValueGradients trainer =
                new CRFTrainerByValueGradients(crf, opts);
        
        //CRFTrainerByThreadedLabelLikelihood trainer = new CRFTrainerByThreadedLabelLikelihood(crf, 32);
        //trainer.setGaussianPriorVariance(0.1);
        trainer.addEvaluator(new PerClassAccuracyEvaluator(trainingInstances, "training"));
        if (testingInstances != null) {
            trainer.addEvaluator(new PerClassAccuracyEvaluator(testingInstances, "testing"));
            trainer.addEvaluator(new TokenAccuracyEvaluator(testingInstances, "testing"));
            
            /*ViterbiWriter viterbiWriter = new ViterbiWriter(
                    "dis_con_crf", // output file prefix
                    new InstanceList[]{trainingInstances, testingInstances},
                    new String[]{"train", "test"}) {

                @Override
                public boolean precondition(TransducerTrainer tt) {
                    return tt.getIteration() % Integer.MAX_VALUE == 0;
                }
            };
            trainer.addEvaluator(viterbiWriter);*/
        }

        // all setup done, train until convergence
        trainer.setMaxResets(0);

        trainer.train(trainingInstances);
        optLabel.shutdown(); // clean exit for all the threads
        //trainer.shutdown();
        try {
            PrintWriter writer = new PrintWriter("crf_raw.txt");
            trainer.getCRF().print(writer);
            writer.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CRFTrainer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void train(InstanceList trainingInstances, Pipe pipe, double trainingPortion) {
        InstanceList[] instanceLists =
                trainingInstances.split(new Random(), new double[]{trainingPortion, (1.0-trainingPortion)});
        this.train(instanceLists[0], instanceLists[1], pipe);
    }
    
    public void saveModel(String fileName) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(crf);
        } catch (IOException ex) {
            Logger.getLogger(CRFTrainer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}

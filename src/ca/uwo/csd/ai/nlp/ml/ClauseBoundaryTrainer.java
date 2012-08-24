/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.io.CONLL01TextReader;
import ca.uwo.csd.ai.nlp.ling.Chunk;
import ca.uwo.csd.ai.nlp.ling.ann.ClauseBoundaryAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.DiscourseMarkerAnnotator;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ml.crf.CRFTrainer;
import ca.uwo.csd.ai.nlp.ml.crf.MyClassifierTrainer;
import cc.mallet.classify.Classifier;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.PrintTokenSequenceFeatures;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.tsf.OffsetConjunctions;
import cc.mallet.pipe.tsf.RegexMatches;
import cc.mallet.pipe.tsf.TokenFirstPosition;
import cc.mallet.pipe.tsf.TokenTextCharPrefix;
import cc.mallet.types.InstanceList;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class ClauseBoundaryTrainer {
    private CONLL01TextReader textReader;
    private Text trainingText;
    private Text testingText;
    private MyClassifierTrainer classifierTrainer;
    private Pipe pipe;
    private DiscourseMarkerAnnotator discourseMarkerAnnotator;
    private ClauseBoundaryAnnotator clauseBoundaryAnnotator;    //only required for larning end point, since CLS_BN_S is needed as a feature
    private boolean startingBoundary;   //whether to learn starting boundary

    public ClauseBoundaryTrainer() {
        this(true);
    }

    public ClauseBoundaryTrainer(boolean startingBoundary) {
        this.startingBoundary = startingBoundary;
        classifierTrainer = new MyClassifierTrainer();
        textReader = new CONLL01TextReader();
        discourseMarkerAnnotator = new DiscourseMarkerAnnotator(false);
        if (startingBoundary == false) {
            clauseBoundaryAnnotator = new ClauseBoundaryAnnotator(ClauseBoundaryAnnotator.defaultPath2StartModel);
        }
    }

    public void train(String trainingFile) {
        train(trainingFile, null);
    }

    public void train(String trainingFile, String testingFile) {
        trainingText = textReader.read(new File(trainingFile));
        System.out.println(trainingText.size());
        //System.out.println(trainingText);

        if (pipe == null) pipe = defaultPipe();

        InstanceList trainingInstanceList = new InstanceList(pipe);
        for (Sentence sentence : trainingText) {
            //sentence = discourseMarkerAnnotator.annotate(sentence);
            //when classifying end candidates take help of start boundary classifier/annotator. chunk2featurevector uses them as features
            if (!startingBoundary) sentence = clauseBoundaryAnnotator.annotate(sentence);
            ArrayList<Chunk> chunks = sentence.getChunks();
            for (Chunk chunk : chunks) {
                trainingInstanceList.addThruPipe(new ChunkInstance(chunk));
            }
        }

        InstanceList testingInstanceList = null;
        if (testingFile != null) {
            testingText = textReader.read(new File(testingFile));
            System.out.println(testingText.size());
            testingInstanceList = new InstanceList(pipe);
            for (Sentence sentence : testingText) {
                //sentence = discourseMarkerAnnotator.annotate(sentence);
                if (!startingBoundary) sentence = clauseBoundaryAnnotator.annotate(sentence);
                ArrayList<Chunk> chunks = sentence.getChunks();
                for (Chunk chunk : chunks) {
                    testingInstanceList.addThruPipe(new ChunkInstance(chunk));
                }
            }
        }
        classifierTrainer.train(trainingInstanceList, testingInstanceList);
    }

    private Pipe defaultPipe() {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        pipes.add(new Chunk2FeatureVector(startingBoundary));
        pipes.add(new PrintFeatureVector());
        return new SerialPipes(pipes);
    }

    public void save(String fileName) {
        classifierTrainer.saveModel(fileName);
    }

    /**
     * Train model for CONLL 2001 shared task1
     */
    public static void trainTask1() {
        ClauseBoundaryTrainer trainer = new ClauseBoundaryTrainer();
        trainer.train(".\\resource\\ml\\data\\clause_data\\train1", ".\\resource\\ml\\data\\clause_data\\testa1");
        //trainer.save(".\\resource\\ml\\models\\clause_start_maxent.model");
    }
    
    /**
     * Train model for CONLL 2001 shared task 2. task2 must be run after task1, because
     * in learning the end boundary we use the start boundary info as features.
     */
    public static void trainTask2() {
        ClauseBoundaryTrainer trainer = new ClauseBoundaryTrainer(false);
        trainer.train(".\\resource\\ml\\data\\clause_data\\train2", ".\\resource\\ml\\data\\clause_data\\testa2");

        //trainer.save(".\\resource\\ml\\models\\clause_end_maxent.model");
    }

    
    public static void main(String args[]) {
        trainTask1();
    }
}

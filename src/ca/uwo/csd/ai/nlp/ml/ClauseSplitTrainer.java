/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.io.CONLL01TextReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Chunk;
import ca.uwo.csd.ai.nlp.ling.ann.ClauseBoundaryAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.DiscourseMarkerAnnotator;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ml.crf.MyClassifierTrainer;
import cc.mallet.classify.AdaBoostTrainer;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.DecisionTreeTrainer;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntGETrainer;
import cc.mallet.classify.MaxEntL1Trainer;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.InstanceList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 * @deprecated 
 */
public class ClauseSplitTrainer {
    private GenericTextReader textReader;
    private Text trainingText;
    private Text testingText;
    private MyClassifierTrainer openBracketClassifier;
    private Pipe pipe;    
    private ClauseBoundaryAnnotator clauseBoundaryAnnotator;

    public ClauseSplitTrainer() {
        openBracketClassifier = new MyClassifierTrainer();
        //openBracketClassifier = new MyClassifierTrainer(new AdaBoostTrainer(new DecisionTreeTrainer(10)));
        textReader = new GenericTextReader("\n\n", "\n", " ", new String[]{"Word","POS", "CHUNK", "CLS_ANN"});
        clauseBoundaryAnnotator = new ClauseBoundaryAnnotator(false);
    }

    public void trainOpenBracketClassifier(String trainingFile, String testingFile) {
        trainingText = textReader.read(new File(trainingFile));
        System.out.println(trainingText.size());
        //System.out.println(trainingText);

        if (pipe == null) pipe = defaultPipe4OpenBracketClassifier();

        InstanceList trainingInstanceList = new InstanceList(pipe);
        
        for (Sentence sentence : trainingText) {
            sentence = clauseBoundaryAnnotator.annotate(sentence);
            ArrayList<Chunk> chunks = sentence.getChunks();            
            for (Chunk chunk : chunks) {
                TokWord word = sentence.get(chunk.getStart());
                if (word.getTag("CLS_BN_S").equals("S") && word.getTag("CLS_ANN").contains("(")) {
                    trainingInstanceList.addThruPipe(new ChunkInstance(chunk));
                }
            }
        }

        InstanceList testingInstanceList = null;
        if (testingFile != null) {
            testingText = textReader.read(new File(testingFile));
            System.out.println(testingText.size());
            testingInstanceList = new InstanceList(pipe);
            for (Sentence sentence : testingText) {

                sentence = clauseBoundaryAnnotator.annotate(sentence);
                ArrayList<Chunk> chunks = sentence.getChunks();
                for (Chunk chunk : chunks) {
                    TokWord word = sentence.get(chunk.getStart());
                    if (word.getTag("CLS_BN_S").equals("S") && word.getTag("CLS_ANN").contains("(")) {
                        testingInstanceList.addThruPipe(new ChunkInstance(chunk));
                    }
                }
            }
        }
        Classifier classifier = openBracketClassifier.train(trainingInstanceList, testingInstanceList);
        MaxEnt m = (MaxEnt) classifier;
        try {
            m.printExtremeFeatures(new PrintWriter("extreme.log"), 1);
            m.printRank(new PrintWriter("rank.log"));
            //m.print(new PrintWriter("out.log"));
            System.out.println("# of Features: " + m.getAlphabet().size());
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ClauseSplitTrainer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private Pipe defaultPipe4OpenBracketClassifier() {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        pipes.add(new ChunkClauseS2FeatureVector());
        pipes.add(new PrintFeatureVector());
        return new SerialPipes(pipes);
    }

    public void save(String fileName) {
        openBracketClassifier.saveModel(fileName);
    }

    public static void trainOpenBracket() {
        ClauseSplitTrainer trainer = new ClauseSplitTrainer();
        trainer.trainOpenBracketClassifier(".\\resource\\ml\\data\\clause_data\\train3", ".\\resource\\ml\\data\\clause_data\\testa3");
        trainer.save(".\\resource\\ml\\models\\clause_open_bracket.model");
    }

    public static void main(String args[]) {
        trainOpenBracket();

        /*GenericTextReader reader = new GenericTextReader("\n\n", "\n", " ", null);
        Text text = reader.read(new File(".\\resource\\ml\\data\\clause_data\\testa3"));
        System.out.println(text);*/
    }
}

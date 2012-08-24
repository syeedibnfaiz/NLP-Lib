/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.io.LPTextReader;
import ca.uwo.csd.ai.nlp.io.TextReader;
import ca.uwo.csd.ai.nlp.ling.ann.ClauseBoundaryAnnotator;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ml.crf.CRFTrainer;
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
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 *
 * @author Syeed Ibn Faiz
 * @deprecated 
 */
public class ClauseSplitCRFTrainer {

    private TextReader textReader;
    private Text trainingText;
    private Text testingText;
    private CRFTrainer crfTrainer;
    private Pipe pipe;
    private ClauseBoundaryAnnotator clauseBoundaryAnnotator;

    public ClauseSplitCRFTrainer() {
        textReader = new GenericTextReader("\n\n", "\n", " ", new String[]{"Word","POS", "CHUNK", "CLS_ANN"});
        clauseBoundaryAnnotator = new ClauseBoundaryAnnotator(false);
        crfTrainer = new CRFTrainer();
    }

    public ClauseSplitCRFTrainer(Pipe pipe) {
        this();
        this.pipe = pipe;
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
            sentence = clauseBoundaryAnnotator.annotate(sentence);
            trainingInstanceList.addThruPipe(new SentenceInstance(sentence));
        }

        InstanceList testingInstanceList = null;
        if (testingFile != null) {
            testingText = textReader.read(new File(testingFile));
            System.out.println(testingText.size());
            testingInstanceList = new InstanceList(pipe);
            for (Sentence sentence : testingText) {
                sentence = clauseBoundaryAnnotator.annotate(sentence);
                testingInstanceList.addThruPipe(new SentenceInstance(sentence));
            }
            crfTrainer.train(trainingInstanceList, testingInstanceList, pipe);
        } else {
            crfTrainer.train(trainingInstanceList, pipe, 0.8);
        }

    }

    public void save(String fileName) {
        crfTrainer.saveModel(fileName);
    }
    private Pipe defaultPipe() {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        pipes.add(new ClauseTaggedSentence2TokenSequence());
        pipes.add(new RegexMatches("PUNCTUATION", Pattern.compile("[,.;:?!-+]")));
        pipes.add(new TokenFirstPosition("First"));
        //pipes.add(new TokenSequencePOSTags());
        //pipes.add(new TokenSequenceConnLexiconTag());
        //pipes.add(new OffsetConjunctions(new int[][]{{-2}, {2}}));
        //pipes.add(new TokenTextCharPrefix("PREFIX=", 2));
        //pipes.add(new TokenTextCharPrefix("PREFIX=", 3));
        pipes.add(new PrintTokenSequenceFeatures());
        pipes.add(new TokenSequence2FeatureVectorSequence(true, true));
        return new SerialPipes(pipes);
    }
    public static void main(String args[]) {
        ClauseSplitCRFTrainer trainer = new ClauseSplitCRFTrainer();        
        trainer.train(".\\resource\\ml\\data\\clause_data\\train3", ".\\resource\\ml\\data\\clause_data\\testa3");
        trainer.save(".\\resource\\ml\\models\\clause_open_bracket_crf.model");
    }
}

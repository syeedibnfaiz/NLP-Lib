/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.io.LPTextReader;
import ca.uwo.csd.ai.nlp.ling.ann.ClauseBoundaryAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.ann.SerialAnnotator;
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
 */
public class DiscourseMarkerTrainer {
    
    //private LPTextReader textReader;
    private GenericTextReader textReader;
    private Text trainingText;
    private Text testingText;
    private CRFTrainer crfTrainer;
    private Pipe pipe;
    
    public DiscourseMarkerTrainer() {        
        //textReader = new LPTextReader("(\\||\\+|-|'|\\d|\\p{L})+|\\S");
        textReader = new GenericTextReader("\n", "\\s+", null, null);
        crfTrainer = new CRFTrainer();
    
    }

    public DiscourseMarkerTrainer(Pipe pipe) {
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
            if (sentence.length() >= 1024) continue; //skipping long sentence due to genia's limitation
    
            trainingInstanceList.addThruPipe(new SentenceInstance(sentence));
        }

        InstanceList testingInstanceList = null;
        if (testingFile != null) {
            testingText = textReader.read(new File(testingFile));
            System.out.println(testingText.size());
            testingInstanceList = new InstanceList(pipe);
            for (Sentence sentence : testingText) {
                if (sentence.length() >= 1024) continue; //skipping long sentence due to genia's limitation
    
                testingInstanceList.addThruPipe(new SentenceInstance(sentence));
            }
            crfTrainer.train(trainingInstanceList, testingInstanceList, pipe);
        } else {
            crfTrainer.train(trainingInstanceList, pipe, 0.7);
        }
        
    }

    public void save(String fileName) {
        crfTrainer.saveModel(fileName);
    }
    private Pipe defaultPipe() {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        pipes.add(new SeqTaggedSentence2TokenSequence());
        pipes.add(new RegexMatches("PUNCTUATION", Pattern.compile("[,.;:?!-+()]")));
        pipes.add(new TokenFirstPosition("First"));
        //pipes.add(new TokenSequencePOSTags());
        //pipes.add(new TokenSequenceConnLexiconTag());
        //pipes.add(new OffsetConjunctions(new int[][]{{-2}, {2}}));
        pipes.add(new RegexMatches("INITCAPS", Pattern.compile("[A-Z].*")));        
        //pipes.add(new TokenTextCharPrefix("PREFIX=", 2));
        //pipes.add(new TokenTextCharPrefix("PREFIX=", 3));
        pipes.add(new PrintTokenSequenceFeatures());
        pipes.add(new TokenSequence2FeatureVectorSequence(true, true));
        return new SerialPipes(pipes);
    }
    public static void main(String args[]) {
        DiscourseMarkerTrainer trainer = new DiscourseMarkerTrainer();
        //trainer.train(".\\resource\\ml\\discourse\\training\\input.txt", ".\\resource\\ml\\discourse\\training\\output.txt");
        //trainer.save(".\\resource\\ml\\models\\crf.model");
        trainer.train(".\\resource\\ml\\data\\discourse\\biodrb_conn.txt");
        trainer.save(".\\resource\\ml\\models\\disc_conn.model");
    }
}

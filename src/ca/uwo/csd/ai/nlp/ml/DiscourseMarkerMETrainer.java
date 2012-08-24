/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.io.LPTextReader;
import ca.uwo.csd.ai.nlp.ling.ann.CharniakParser;
import ca.uwo.csd.ai.nlp.ling.ann.ClauseAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.ClauseBoundaryAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.ann.SerialAnnotator;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ml.crf.CRFTrainer;
import ca.uwo.csd.ai.nlp.ml.crf.MyClassifierTrainer;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.PrintTokenSequenceFeatures;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.tsf.OffsetConjunctions;
import cc.mallet.pipe.tsf.RegexMatches;
import cc.mallet.pipe.tsf.TokenFirstPosition;
import cc.mallet.pipe.tsf.TokenTextCharPrefix;
import cc.mallet.types.InstanceList;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class DiscourseMarkerMETrainer {
    
    private Text trainingText;
    private Text testingText;    
    private Pipe pipe;    
    HashSet<String> connSet;
    GenericTextReader textReader;
    MyClassifierTrainer trainer;
    SerialAnnotator annotator;

    public DiscourseMarkerMETrainer() {
        //textReader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word","POS","CHUNK","CONN"});
        textReader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word","CONN"});
        trainer = new MyClassifierTrainer();
        connSet = new HashSet<String>();
        annotator = new SerialAnnotator();
        //annotator.add(new ClauseBoundaryAnnotator(false));
        //annotator.add(new ClauseAnnotator());
        //annotator.add(new ParserAnnotator());
        annotator.add(new CharniakParser());
        fillConnSet();
    }

    public DiscourseMarkerMETrainer(Pipe pipe) {
        this();
        this.pipe = pipe;
    }

    private void fillConnSet() {
        try {
            //BufferedReader reader = new BufferedReader(new FileReader("./resource/ml/data/discourse/all_conn_lexicon.txt"));
            BufferedReader reader = new BufferedReader(new FileReader("./resource/ml/data/discourse/explicit_conn_types"));
            String line;
            while ((line = reader.readLine()) != null) {
                connSet.add(line);
            }
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(DiscourseMarkerMETrainer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void train(String trainingFile, String parsedTrainingFile) throws IOException {
        train(trainingFile, parsedTrainingFile, null, null);
    }

    public void train(String trainingFile, String parsedTrainingFile, String testingFile, String parsedTestingFile) throws IOException {
        trainingText = textReader.read(new File(trainingFile));
        System.out.println(trainingText.size());
        System.out.println(trainingText);
        
        TreeFactory treeFactory = new LabeledScoredTreeFactory();
        BufferedReader reader = new BufferedReader(new FileReader(parsedTrainingFile));
        String line;
        if (pipe == null) pipe = defaultPipe();

        InstanceList trainingInstanceList = new InstanceList(pipe);
        for (Sentence sentence : trainingText) {
            //sentence = annotator.annotate(sentence);
            reader.readLine();
            line = reader.readLine();
            if (line.equals("")) continue;
            /*StringReader sr = new StringReader(line);
            TreeReader tr = new PennTreeReader(sr, treeFactory);
            sentence.setParseTree(tr.readTree());
            sentence.markAnnotation("PARSED");             
             */
            addInstancesThroughPipe(sentence, trainingInstanceList);
        }

        InstanceList testingInstanceList = null;
        if (testingFile != null) {
            testingText = textReader.read(new File(testingFile));
            System.out.println(testingText.size());
            testingInstanceList = new InstanceList(pipe);
            for (Sentence sentence : testingText) {
                sentence = annotator.annotate(sentence);
                addInstancesThroughPipe(sentence, testingInstanceList);
            }
            trainer.train(trainingInstanceList, testingInstanceList);
        } else {
            trainer.train(trainingInstanceList, 0.7);
        }
    }

    public void save(String fileName) {
        trainer.saveModel(fileName);
    }
    private Pipe defaultPipe() {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        pipes.add(new Connective2FeatureVector());
        pipes.add(new PrintFeatureVector());
        return new SerialPipes(pipes);
    }
    private void addInstancesThroughPipe(Sentence s, InstanceList instanceList) {
        ArrayList<String> words = s.getWords();
        for (int i = 0; i < words.size(); i++) {
            
            int j = -1;
            for (int k = 0; k < 4 && ((i + k) < words.size()); k++) {
                if (connSet.contains(s.toString(i, i + k).toLowerCase())) {
                    j = i + k;
                }
            }
            if (j != -1) {
                if (!s.get(i).getTag("CONN").startsWith("B")) {
                    instanceList.addThruPipe(new ConnectiveInstance(s, i, j, false));
                } else {
                    instanceList.addThruPipe(new ConnectiveInstance(s, i, j, true));
                }
                i = j;
            }
        }
    }
    public static void main(String args[]) {
        DiscourseMarkerMETrainer trainer = new DiscourseMarkerMETrainer();
        try {
            //trainer.train(".\\resource\\ml\\discourse\\training\\input.txt", ".\\resource\\ml\\discourse\\training\\output.txt");
            //trainer.save(".\\resource\\ml\\models\\crf.model");
            trainer.train("./resource/ml/data/discourse/biodrb_conn_2.txt", "./resource/ml/data/discourse/biodrb_conn_2_parsed.txt");
        } catch (IOException ex) {
            Logger.getLogger(DiscourseMarkerMETrainer.class.getName()).log(Level.SEVERE, null, ex);
        }
        //trainer.save(".\\resource\\ml\\models\\disc_conn_maxent.model");
        trainer.save("./resource/ml/models/disc_conn_maxent_syntax_tree.model");
    }
}

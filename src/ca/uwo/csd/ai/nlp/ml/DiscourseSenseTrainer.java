/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.ann.SerialAnnotator;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ml.crf.MyClassifierTrainer;
import ca.uwo.csd.ai.nlp.ml.crf.NFoldEvaluator;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class DiscourseSenseTrainer {
    
    private Text trainingText;
    private Text testingText;    
    private Pipe pipe;    
    HashSet<String> connSet;
    GenericTextReader textReader;
    MyClassifierTrainer trainer;
    SerialAnnotator annotator;

    public DiscourseSenseTrainer() {
        //textReader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word","POS","CHUNK","CONN"});
        textReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[]{"Word","CONN", "SENSE"});
        trainer = new MyClassifierTrainer();
        connSet = new HashSet<String>();
        annotator = new SerialAnnotator();        
    }

    public DiscourseSenseTrainer(Pipe pipe) {
        this();
        this.pipe = pipe;
    }
    
    public void train(String trainingFile, String parsedTrainingFile) throws IOException {
        train(trainingFile, parsedTrainingFile, null, null);
    }

    public void train(String trainingFile, String parsedTrainingFile, String testingFile, String parsedTestingFile) throws IOException {
        trainingText = textReader.read(new File(trainingFile));
        System.out.println(trainingText.size());
        //System.out.println(trainingText);
                
        BufferedReader reader = new BufferedReader(new FileReader(parsedTrainingFile));
        String line;
        if (pipe == null) pipe = defaultPipe();

        InstanceList trainingInstanceList = new InstanceList(pipe);
        for (Sentence sentence : trainingText) {
            
            line = reader.readLine();            
            if (line.equals("")) continue;
            
            sentence.setProperty("parse_tree", line);
            addInstancesThroughPipe(sentence, trainingInstanceList);
        }

        InstanceList testingInstanceList = null;
        if (testingFile != null) {
            testingText = textReader.read(new File(testingFile));
            System.out.println(testingText.size());
            testingInstanceList = new InstanceList(pipe);
            for (Sentence sentence : testingText) {
                sentence = annotator.annotate(sentence);
                //addInstancesThroughPipe(sentence, testingInstanceList);
            }
            trainer.train(trainingInstanceList, testingInstanceList);
        } else {
            /*trainer.train(trainingInstanceList, 0.9);
            analyzeErrors(trainingInstanceList);*/
            NFoldEvaluator evaluator = new NFoldEvaluator();
            evaluator.evaluate(trainer, trainingInstanceList, 10);
            
        }
        
        /*PrintWriter writer = new PrintWriter("pdtb_conn_only_syn.arff");
        Utils.convert2ARFF(trainingInstanceList, writer, "pdtb_conn_only_syn");
        writer.close();*/
    }

    private void analyzeErrors(InstanceList instances) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("sense_errors.log");
            Classifier classifier = trainer.getClassifier();
            for (Instance instance : instances) {            
                Classification classification = classifier.classify(instance);
                if (!classification.bestLabelIsCorrect()) {
                    writer.write(instance.getTarget() + "\n");
                    ConnectiveInstance conInstance = (ConnectiveInstance) instance.getSource();
                    Sentence s = conInstance.getSentence();
                    writer.write(s.toString(conInstance.getS(), conInstance.getE()) + " : " + conInstance.getLabel() + ": "+classification.getLabeling() +","+conInstance.getS() +"\n");
                    writer.write(s.toString("CONN")+"\n");
                    writer.write(s.getProperty("parse_tree") + "\n");
                }
            }
            writer.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NewDiscourseMarkerMETrainer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            writer.close();
        }
    }
    public void save(String fileName) {
        trainer.saveModel(fileName);
    }
    private Pipe defaultPipe() {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        pipes.add(new NewConnective2FeatureVector());
        pipes.add(new PrintFeatureVector());
        return new SerialPipes(pipes);
    }
    private void addInstancesThroughPipe(Sentence s, InstanceList instanceList) {
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (word.getTag("CONN").startsWith("B-") || word.getTag("CONN").startsWith("DB-")) {
                int start = i;
                int end = i;
                for (int k = start + 1; k < s.size(); k++) {
                    TokWord nextWord = s.get(k);
                    if (nextWord.getTag("CONN").startsWith("I-")) {
                        end = k;
                    } else {
                        break;
                    }
                }
                String label = word.getTag("SENSE").split("\\.")[0];
                instanceList.addThruPipe(new ConnectiveInstance(s, start, end, label));
                i = end;
            }
        }
    }
    public static void main(String args[]) {
        DiscourseSenseTrainer trainer = new DiscourseSenseTrainer();
        try {
            //trainer.train(".\\resource\\ml\\discourse\\training\\input.txt", ".\\resource\\ml\\discourse\\training\\output.txt");
            //trainer.save(".\\resource\\ml\\models\\crf.model");
            trainer.train("./resource/ml/data/pdtb/explicit_relations_w_iteonn_sense", "./resource/ml/data/pdtb/explicit_relations_tree");
        } catch (IOException ex) {
            Logger.getLogger(DiscourseSenseTrainer.class.getName()).log(Level.SEVERE, null, ex);
        }
        //trainer.save(".\\resource\\ml\\models\\disc_conn_maxent.model");
        //trainer.save("./resource/ml/models/pdtb/disc_conn_maxent_syntax_tree.model");
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.ann.SerialAnnotator;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ml.crf.MyClassifierTrainer;
import ca.uwo.csd.ai.nlp.ml.crf.NFoldEvaluator;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class NewDiscourseMarkerMETrainer {
    
    private Text trainingText;
    private Text testingText;    
    private Pipe pipe;    
    HashSet<String> connSet;
    GenericTextReader textReader;
    MyClassifierTrainer trainer;
    SerialAnnotator annotator;

    public NewDiscourseMarkerMETrainer() {
        //textReader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word","POS","CHUNK","CONN"});
        textReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[]{"Word","CONN"});
        trainer = new MyClassifierTrainer(1);
        connSet = new HashSet<String>();
        annotator = new SerialAnnotator();
        //annotator.add(new ClauseBoundaryAnnotator(false));
        //annotator.add(new ClauseAnnotator());
        //annotator.add(new ParserAnnotator());
        //annotator.add(new CharniakParser());
        fillConnSet();
    }

    public NewDiscourseMarkerMETrainer(Pipe pipe) {
        this();
        this.pipe = pipe;
    }

    private void fillConnSet() {
        try {
            //BufferedReader reader = new BufferedReader(new FileReader("./resource/ml/data/discourse/all_conn_lexicon.txt"));
            BufferedReader reader = new BufferedReader(new FileReader("./resource/ml/data/pdtb/explicit_conn_types"));
            String line;
            while ((line = reader.readLine()) != null) {
                connSet.add(line);
                System.out.println(line);
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
        //System.out.println(trainingText);
                
        BufferedReader reader = new BufferedReader(new FileReader(parsedTrainingFile));
        String line;
        if (pipe == null) pipe = defaultPipe();

        InstanceList trainingInstanceList = new InstanceList(pipe);
        for (Sentence sentence : trainingText) {
            //sentence = annotator.annotate(sentence);            
            line = reader.readLine();            
            if (line.equals("")) continue;
            /*StringReader sr = new StringReader(line);
            TreeReader tr = new PennTreeReader(sr, treeFactory);
            sentence.setParseTree(tr.readTree());
            sentence.markAnnotation("PARSED");             
             */
            sentence.setProperty("parse_tree", line);
            addInstancesThroughPipe(sentence, trainingInstanceList);
        }

        InstanceList testingInstanceList = null;
        if (testingFile != null) {
            testingText = textReader.read(new File(testingFile));
            System.out.println(testingText.size());
            testingInstanceList = new InstanceList(pipe);
            reader = new BufferedReader(new FileReader(parsedTestingFile));
            
            for (Sentence sentence : testingText) {
                line = reader.readLine();            
                if (line.equals("")) continue;
                sentence.setProperty("parse_tree", line);
                addInstancesThroughPipe(sentence, testingInstanceList);
            }
            
            trainer.train(trainingInstanceList, testingInstanceList);
            System.out.println("training size: " + trainingInstanceList.size());
            System.out.println("testing size: " + testingInstanceList.size());
        } else {
            /*trainer.train(trainingInstanceList, 0.9);
            analyzeErrors(trainingInstanceList);*/
            NFoldEvaluator evaluator = new NFoldEvaluator();
            evaluator.evaluate(trainer, trainingInstanceList, 10);            
                                    
        }
        
        /*PrintWriter writer = new PrintWriter("pdtb_conn_syn_chunk.arff");
        Utils.convert2ARFF(trainingInstanceList, writer, "pdtb_conn_syn_chunk");
        writer.close();*/
    }

    private void checkTrainingData(InstanceList trainingInstanceList) {
        
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (Instance i : trainingInstanceList) {
            ConnectiveInstance ci = (ConnectiveInstance) i;
            if (ci.getLabel().toString().equals("true")) {
                Sentence s = ci.getSentence();
                String conn = s.toString(ci.getS(), ci.getE()).toLowerCase();
                if (map.containsKey(conn)) {
                    map.put(conn, map.get(conn) + 1);
                } else {
                    map.put(conn, 1);
                }
            }
        }
        List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(map.entrySet());
        Collections.sort(list, new Comparator<Entry<String, Integer>>() {

            @Override
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        for (Entry<String, Integer> e : list) {
            System.out.println(e.getKey() + "-" + e.getValue());
        }
    }

    private void analyzeErrors(InstanceList instances) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("errors.log");
            Classifier classifier = trainer.getClassifier();
            for (Instance instance : instances) {            
                Classification classification = classifier.classify(instance);
                if (!classification.bestLabelIsCorrect()) {
                    writer.write(instance.getTarget() + "\n");
                    ConnectiveInstance conInstance = (ConnectiveInstance) instance.getSource();
                    Sentence s = conInstance.getSentence();
                    writer.write(s.toString(conInstance.getS(), conInstance.getE()) + " : " + conInstance.getLabel() + ": "+ conInstance.getS() +"\n");
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
        //pipes.add(new NewConnective2FeatureVector());
        pipes.add(new PDTBConnective2FeatureVector());
        pipes.add(new PrintFeatureVector());
        return new SerialPipes(pipes);
    }
    private void addInstancesThroughPipe(Sentence s, InstanceList instanceList) {        
        ArrayList<String> words = s.getWords();
        for (int i = 0; i < words.size(); i++) {
            
            int j = -1;
            if (!s.get(i).getTag("CONN").matches("(B.*)|(DB.*)")) {
                for (int k = 0; k < 4 && ((i + k) < words.size()); k++) {
                    if (connSet.contains(s.toString(i, i + k).toLowerCase())) {
                        j = i + k;
                    }
                }
            } else {
                j = i;
                for (int k = i + 1; k < words.size(); k++) {
                    if (s.get(k).getTag("CONN").startsWith("I")) {
                        j = k;
                    } else {
                        break;
                    }
                }
                
            }
            if (j != -1) {
                if (s.get(i).getTag("CONN").startsWith("B")) {
                    instanceList.addThruPipe(new ConnectiveInstance(s, i, j, true));
                } else if (s.get(i).getTag("CONN").startsWith("DB")) {                 //if..then, either..or
                    instanceList.addThruPipe(new ConnectiveInstance(s, i, j, true));
                } else if (!s.get(i).getTag("CONN").startsWith("DI")) {                 //don't take then of if..then                           
                    instanceList.addThruPipe(new ConnectiveInstance(s, i, j, false));                    
                }
                i = j;
            }
        }
    }
    public static void main(String args[]) {
        NewDiscourseMarkerMETrainer trainer = new NewDiscourseMarkerMETrainer();
        try {
            //trainer.train(".\\resource\\ml\\discourse\\training\\input.txt", ".\\resource\\ml\\discourse\\training\\output.txt");
            //trainer.save(".\\resource\\ml\\models\\crf.model");
            //trainer.train("./resource/ml/data/pdtb/explicit_relations_w_iteonn", "./resource/ml/data/pdtb/explicit_relations_tree");
            //trainer.train("./resource/ml/data/pdtb/explicit_relations_test_2_21", "./resource/ml/data/pdtb/explicit_relations_tree_test_2_21", "./resource/ml/data/pdtb/explicit_relations_test_23_24", "./resource/ml/data/pdtb/explicit_relations_tree_test_23_24");
            trainer.train("./resource/ml/data/pdtb/explicit_relations_w_iteonn", "./resource/ml/data/pdtb/explicit_relations_tree", "./resource/ml/data/pdtb/explicit_relations_test_23_24", "./resource/ml/data/pdtb/explicit_relations_tree_test_23_24");
        } catch (IOException ex) {
            Logger.getLogger(DiscourseMarkerMETrainer.class.getName()).log(Level.SEVERE, null, ex);
        }
        //trainer.save(".\\resource\\ml\\models\\disc_conn_maxent.model");
        //trainer.save("./resource/ml/models/pdtb/disc_conn_maxent_syntax_tree.model");
    }
}

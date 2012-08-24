/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.ann.SerialAnnotator;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ml.crf.MyClassifierTrainer;
import ca.uwo.csd.ai.nlp.ml.crf.NFoldEvaluator;
import ca.uwo.csd.ai.nlp.ml.crf.NFoldTTest;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
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
 * Trains a MaxEnt Model for discourse connective sense identification.
 * Needs PDTB, specifically IOB formatted annotation for discourse connective senses.
 * @author Syeed Ibn Faiz
 */
public class PDTBConnectiveSenseTrainer {
    
    private Text trainingText;
    private Text testingText;    
    private Pipe pipe;    
    HashSet<String> connSet;
    GenericTextReader textReader;
    MyClassifierTrainer trainer;
    SerialAnnotator annotator;

    public PDTBConnectiveSenseTrainer() {        
        textReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[]{"Word","CONN","SENSE"});
        trainer = new MyClassifierTrainer(1);
        connSet = new HashSet<String>();
        annotator = new SerialAnnotator();
        //annotator.add(new ClauseBoundaryAnnotator(false));
        //annotator.add(new ClauseAnnotator());
        //annotator.add(new ParserAnnotator());
        //annotator.add(new CharniakParser());        
    }    
    
    public void train(String trainingFile, String parsedTrainingFile, String trainingDepFile) throws IOException {
        train(trainingFile, parsedTrainingFile, trainingDepFile, null, null, null);
    }

    public void train(String trainingFile, String parsedTrainingFile, String trainingDepFile, String testingFile, String parsedTestingFile, String testingDepFile) throws IOException {
        trainingText = textReader.read(new File(trainingFile));
        System.out.println(trainingText.size());
        //System.out.println(trainingText);
                
        BufferedReader reader = new BufferedReader(new FileReader(parsedTrainingFile));
        String line;
        if (pipe == null) pipe = defaultPipe();

        InstanceList trainingInstanceList = new InstanceList(pipe);
        SimpleDepFileReader depFileReader = new SimpleDepFileReader();
        List<SimpleDepGraph> depGraphs = depFileReader.read(new File(trainingDepFile));
        for (int i = 0; i < trainingText.size(); i++) {
            Sentence sentence = trainingText.get(i);
            //SimpleDepGraph depGraph = depGraphs.get(i);
            SimpleDepGraph depGraph = null;
            //sentence = annotator.annotate(sentence);            
            line = reader.readLine();            
            if (line.equals("")) continue;
            //to save space
            sentence.setProperty("parse_tree", line);
            addInstancesThroughPipe(sentence, depGraph, trainingInstanceList);
            
        }

        InstanceList testingInstanceList = null;
        if (testingFile != null) {
            testingText = textReader.read(new File(testingFile));
            System.out.println(testingText.size());
            testingInstanceList = new InstanceList(pipe);
            reader = new BufferedReader(new FileReader(parsedTestingFile));
            depGraphs = depFileReader.read(new File(testingDepFile));
            for (int i = 0; i < testingText.size(); i++) {
                Sentence sentence = testingText.get(i);
                //SimpleDepGraph depGraph = depGraphs.get(i);
                SimpleDepGraph depGraph = null;
                
                line = reader.readLine();            
                if (line.equals("")) continue;
                
                sentence.setProperty("parse_tree", line);
                addInstancesThroughPipe(sentence, depGraph, testingInstanceList);
                //System.out.println(sentence.toString());
                //System.out.println(depGraph);
            }
            
            trainer.train(trainingInstanceList, testingInstanceList);
            
            //analyzeErrors(testingInstanceList);
            System.out.println("training size: " + trainingInstanceList.size());
            System.out.println("testing size: " + testingInstanceList.size());
            System.out.println("#of features:" + trainingInstanceList.getDataAlphabet().size());
        } else {
            /*trainer.train(trainingInstanceList, 0.9);
            analyzeErrors(trainingInstanceList);*/
            /*NFoldEvaluator evaluator = new NFoldEvaluator();
            evaluator.evaluate(trainer, trainingInstanceList, 10);*/
            
            showNFoldAccuracy(trainingInstanceList, 10);
            
            /*NFoldTTest tTest = new NFoldTTest();
            tTest.evaluate(trainingInstanceList, 10);*/
            
            /*InstanceList[] splits = trainingInstanceList.splitInOrder(new double[]{0.9, .1});
            trainer.train(splits[0], splits[1]);*/
            
            int positive = 0;
            int negative = 0;
            for (Instance instance : trainingInstanceList) {
                PDTBConnectiveInstance pInstance = (PDTBConnectiveInstance) instance;
                String s = String.valueOf(pInstance.label);
                if (s.equals("true")) {
                    positive++;
                } else {
                    negative++;
                }
            }
            System.out.println("positive: " + positive);
            System.out.println("negative: " + negative);
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
        Alphabet dataAlphabet = instances.getDataAlphabet();
        try {            
            writer = new PrintWriter("errors.log");
            Classifier classifier = trainer.getClassifier();
            for (Instance instance : instances) {            
                Classification classification = classifier.classify(instance);
                if (!classification.bestLabelIsCorrect()) {
                    writer.write(instance.getTarget() + "\n");
                    PDTBConnectiveInstance conInstance = (PDTBConnectiveInstance) instance.getSource();
                    Sentence s = conInstance.getSentence();
                    writer.write(s.toString(conInstance.getS(), conInstance.getE()) + " : " + conInstance.getLabel() + ": "+ conInstance.getS() +"\n");
                    writer.write(s.toString()+"\n");
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
    private void showNFoldAccuracy(InstanceList instanceList, int n) {
        InstanceList.CrossValidationIterator cvIt = instanceList.crossValidationIterator(n);
        double accuracies[] = new double[n];
        double accuracy = 0;
        int run = 0;
        while (cvIt.hasNext()) {
            InstanceList[] nextSplit = cvIt.nextSplit();
            InstanceList trainingInstances = nextSplit[0];
            InstanceList testingInstances = nextSplit[1];
            
            trainer = new MyClassifierTrainer(new MaxEntTrainer());
            Classifier classifier = trainer.train(trainingInstances);
            accuracies[run] = getAccuracy(classifier, testingInstances);
            accuracy += accuracies[run];
            run++;
        }
        System.out.println(n + "-Fold accuracy(avg): " + accuracy/n);
    }
    
    double getAccuracy(Classifier classifier, InstanceList instanceList) {
        int total = instanceList.size();
        int correct = 0;        
        for (Instance instance : instanceList) {            
            Classification classification = classifier.classify(instance);
            if (classification.bestLabelIsCorrect()) correct++;
        }        
        return (1.0*correct)/total;
    }
    public void save(String fileName) {
        trainer.saveModel(fileName);
    }
    private Pipe defaultPipe() {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        //pipes.add(new NewConnective2FeatureVector());
        //pipes.add(new PDTBConnective2FeatureVector());
        pipes.add(new PDTBConnective2FeatureVector2());
        //pipes.add(new PrintFeatureVector());
        return new SerialPipes(pipes);
    }
    private void addInstancesThroughPipe(Sentence s, SimpleDepGraph depGraph, InstanceList instanceList) {        
        ArrayList<String> words = s.getWords();
        for (int i = 0; i < words.size(); i++) {
            
            int j = -1;
            if (s.get(i).getTag("CONN").matches("(B.*)|(DB.*)")) {
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
                String sense = s.get(i).getTag("SENSE");
                if (sense.contains(".")) {
                    sense = sense.substring(0, sense.indexOf('.'));
                }
                if (!sense.equals("O")) {
                    instanceList.addThruPipe(new PDTBConnectiveInstance(s, i, j, sense, depGraph));
                }
                i = j;
            }
        }
    }
    
    public static void main(String args[]) {
        PDTBConnectiveSenseTrainer trainer = new PDTBConnectiveSenseTrainer();
        try {
            
            //trainer.train("./resource/ml/data/pdtb/conn_id/explicit_relations_2_22", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_2_22", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_2_22", 
            //        "./resource/ml/data/pdtb/conn_id/explicit_relations_23_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_23_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_23_24");
            //trainer.train("./resource/ml/data/pdtb/conn_id/explicit_relations_2_22", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_2_22", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_2_22", 
            //        "./resource/ml/data/pdtb/conn_id/explicit_relations_0_1", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_0_1", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_0_1");
            //trainer.train("./resource/ml/data/pdtb/conn_id/explicit_relations_2_22", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_2_22", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_2_22");
            //trainer.train("./resource/ml/data/pdtb/conn_id/explicit_relations_2_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_2_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_2_24");
            //trainer.train("./resource/ml/data/pdtb/conn_id/explicit_relations_2_21", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_2_21", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_2_21", 
            //        "./resource/ml/data/pdtb/conn_id/explicit_relations_23", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_23", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_23");
            //trainer.train("./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_2_22", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_tree_2_22", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_dep_2_22",
            //        "./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn","./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn_tree","./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn_dep");
            //trainer.train("./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn2.txt","./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn_tree","./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn_dep");
            //trainer.train("./resource/ml/data/pdtb/conn_id/explicit_relations_0_1", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_0_1", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_0_1");
            //trainer.train("./resource/ml/data/pdtb/conn_id/explicit_relations_2_21", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_2_21", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_2_21", 
            //        "./resource/ml/data/pdtb/conn_id/explicit_relations_23_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_23_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_23_24");            
            //trainer.train("./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_2_21", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_tree_2_21", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_dep_2_21",
            //        "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_23_24", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_tree_23_24", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_dep_23_24");
            //trainer.train("./resource/ml/data/pdtb/conn_id/gs/auto_explicit_relations_2_22", "./resource/ml/data/pdtb/conn_id/gs/auto_explicit_relations_tree_2_22", "./resource/ml/data/pdtb/conn_id/gs/auto_explicit_relations_dep_2_22");
            //trainer.train("./resource/ml/data/pdtb/conn_id/explicit_relations_23_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_23_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_23_24");
            
            
            //trainer.train("./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_sense_2_22", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_tree_2_22", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_dep_2_22");
            trainer.train("./resource/ml/data/pdtb/conn_id/biodrb/sense/biodrb_sense.iob","./resource/ml/data/pdtb/conn_id/biodrb/sense/biodrb_sense.tree","./resource/ml/data/pdtb/conn_id/biodrb/sense/biodrb_sense.dep");
        } catch (IOException ex) {
            Logger.getLogger(DiscourseMarkerMETrainer.class.getName()).log(Level.SEVERE, null, ex);
        }        
        //trainer.save("./resource/ml/models/biodrb/biodrb_conn.model");
    }
}

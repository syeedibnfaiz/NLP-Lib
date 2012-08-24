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
import ca.uwo.csd.ai.nlp.ling.analyzers.ConnectiveAnalyzer;
import ca.uwo.csd.ai.nlp.ml.crf.MyClassifierTrainer;
import ca.uwo.csd.ai.nlp.ml.crf.NFoldEvaluator;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEnt;
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
import java.io.FileWriter;
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
 * Trains a MaxEnt Model for discourse connective identification.
 * Needs PDTB, specifically IOB formatted annotation for discourse connectives.
 * @author Syeed Ibn Faiz
 */
public class PDTBConnectiveTrainer3TC {
    
    private Text trainingText;
    private Text testingText;    
    private Pipe pipe;        
    GenericTextReader textReader;
    MyClassifierTrainer trainers[];
    MyClassifierTrainer generalTrainer;
    FileWriter coordWriter;
    FileWriter subordWriter;
    FileWriter adverbWriter;
    final static ConnectiveAnalyzer CONN_ANALYZER = new ConnectiveAnalyzer();
    
    public PDTBConnectiveTrainer3TC() throws IOException {        
        textReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[]{"Word","CONN"});
        trainers = new MyClassifierTrainer[3];
        for (int i = 0; i < trainers.length; i++) {
            trainers[i] = new MyClassifierTrainer();
        }
        generalTrainer = new MyClassifierTrainer();
        coordWriter = new FileWriter("coord.log");
        subordWriter = new FileWriter("subord.log");
        adverbWriter = new FileWriter("adverbial.log");
    }
    
    public void closeFiles() throws IOException {
        coordWriter.close();
        subordWriter.close();
        adverbWriter.close();
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
        pipe = defaultPipe();

        InstanceList trainingInstanceLists[] = new InstanceList[trainers.length];
        for (int i = 0; i < trainingInstanceLists.length; i++) {
            trainingInstanceLists[i] = new InstanceList(pipe);
        }
        SimpleDepFileReader depFileReader = new SimpleDepFileReader();
        List<SimpleDepGraph> depGraphs = depFileReader.read(new File(trainingDepFile));
        for (int i = 0; i < trainingText.size(); i++) {
            Sentence sentence = trainingText.get(i);
            SimpleDepGraph depGraph = depGraphs.get(i);
            //SimpleDepGraph depGraph = null;
            //sentence = annotator.annotate(sentence);            
            line = reader.readLine();            
            if (line.equals("")) continue;
            //to save space
            sentence.setProperty("parse_tree", line);
            addInstancesThroughPipe(sentence, depGraph, trainingInstanceLists);
            
        }
        
        /*InstanceList generalInstanceList = new InstanceList(pipe);
        for (InstanceList instanceList : trainingInstanceLists) {
            for (Instance instance : instanceList) {
                generalInstanceList.add(instance);
            }
        }
        generalTrainer.train(generalInstanceList);*/
        
        InstanceList testingInstanceLists[] = null;
        if (testingFile != null) {
            testingText = textReader.read(new File(testingFile));
            System.out.println(testingText.size());
            testingInstanceLists = new InstanceList[trainers.length];
            for (int i = 0; i < testingInstanceLists.length; i++) {
                testingInstanceLists[i] = new InstanceList(pipe);
            }
            
            reader = new BufferedReader(new FileReader(parsedTestingFile));
            depGraphs = depFileReader.read(new File(testingDepFile));
            for (int i = 0; i < testingText.size(); i++) {
                Sentence sentence = testingText.get(i);
                SimpleDepGraph depGraph = depGraphs.get(i);
                
                line = reader.readLine();            
                if (line.equals("")) continue;
                
                sentence.setProperty("parse_tree", line);
                addInstancesThroughPipe(sentence, depGraph, testingInstanceLists);
                //System.out.println(sentence.toString());
                //System.out.println(depGraph);
            }
            
            InstanceList generalInstanceList = new InstanceList(pipe);
            for (int i = 0; i < trainers.length; i++) {
                System.out.println("---------------------");
                System.out.println("    Output for Model: " + (i + 1));
                System.out.println("---------------------");
                //trainers[i].train(trainingInstanceLists[i], testingInstanceLists[i]);
                generalInstanceList.addAll(trainingInstanceLists[i]);
                trainers[i].train(trainingInstanceLists[i]);
                
                System.out.println("training size: " + trainingInstanceLists[i].size());
                System.out.println("testing size: " + testingInstanceLists[i].size());
                System.out.println("#of features:" + trainingInstanceLists[i].getDataAlphabet().size());
            }
            generalTrainer = new MyClassifierTrainer();
            generalTrainer.train(generalInstanceList);
            //showResult(getResult(evaluate(testingInstanceLists)));
            showResult(getResult(interpolateEvaluate(testingInstanceLists)));
            
        } else {
            /*trainer.train(trainingInstanceList, 0.9);
            analyzeErrors(trainingInstanceList);*/
            /*NFoldEvaluator evaluator = new NFoldEvaluator();
                        
            for (int i = 0; i < trainers.length; i++) {
                System.out.println("---------------------");
                System.out.println("    Output for Model: " + (i + 1));
                System.out.println("---------------------");
                evaluator.evaluate(trainers[i], trainingInstanceLists[i], 10);
            }*/
            //showResult(NFoldEvaluate(trainingInstanceLists, 10));
            InstanceList generalInstanceList = new InstanceList(pipe);
            for (int i = 0; i < 3; i++) {                
                generalInstanceList.addAll(trainingInstanceLists[i]);
            }
            showGenResult(genNFoldEvaluate(generalInstanceList, 10));
        }
                
    }
    
    void showResult(double result[]) {
                
        System.out.println("Accuracy: " + result[0]);        
        System.out.println("Precision: " + result[1]);
        System.out.println("Recall: " + result[2]);        
        System.out.println("F1: " + result[3]);
    }
    double[] getResult(double count[]) {
        double result[] = new double[4];
        double tp = count[0];
        double fp = count[1];
        double fn = count[2];
        double tn = count[3];

        result[0] += (tp + tn) / (tp + tn + fp + fn);
        double lPrecision = tp / (tp + fp);
        double lRecall = tp / (tp + fn);
        result[1] = lPrecision;
        result[2] = lRecall;
        result[3] = 2 * lPrecision * lRecall / (lPrecision + lRecall);
        return result;
    }
    double[] NFoldEvaluate(InstanceList testingInstanceLists[], int N) throws IOException {                
        double accuracy = 0;
        double precision = 0;
        double recall = 0;
        double f1 = 0;
        InstanceList instanceList = new InstanceList(testingInstanceLists[0].getPipe());
        for (int i = 0; i < testingInstanceLists.length; i++) {
            instanceList.addAll(testingInstanceLists[i]);
        }
        InstanceList.CrossValidationIterator cvIt = instanceList.crossValidationIterator(N);
        ConnectiveAnalyzer connAnalyzer = new ConnectiveAnalyzer();
        while (cvIt.hasNext()) {            
            
            InstanceList[] nextSplit = cvIt.nextSplit();
            InstanceList trainingInstances = nextSplit[0];
            InstanceList testingInstances = nextSplit[1];
            InstanceList trainSets[] = new InstanceList[3];
            InstanceList testSets[] = new InstanceList[3];
            for (int i = 0; i < 3; i++) {
                trainSets[i] = new InstanceList(instanceList.getPipe());
                testSets[i] = new InstanceList(instanceList.getPipe());
            }            
            
            for (Instance instance : trainingInstances) {
                PDTBConnectiveInstance pdtbInstance = (PDTBConnectiveInstance) instance;
                Sentence s = pdtbInstance.getSentence();
                String conn = s.toString(pdtbInstance.getS(), pdtbInstance.getE());
                String cat = connAnalyzer.getCategory(conn.toLowerCase());
                if (cat == null) cat = "Conj-adverb";
                if (cat.startsWith("Coord")) {
                    trainSets[0].add(instance);
                } else if (cat.startsWith("Sub")) {
                    trainSets[1].add(instance);
                } else {
                    trainSets[2].add(instance);
                }
            }
            //train the general model
            InstanceList generalInstanceList = new InstanceList(instanceList.getPipe());
            for (InstanceList instances : trainSets) {
                generalInstanceList.addAll(instances);
            }
            generalTrainer = new MyClassifierTrainer();
            //generalTrainer.train(generalInstanceList);
            
            for (Instance instance : testingInstances) {
                PDTBConnectiveInstance pdtbInstance = (PDTBConnectiveInstance) instance;
                Sentence s = pdtbInstance.getSentence();
                String conn = s.toString(pdtbInstance.getS(), pdtbInstance.getE());
                String cat = connAnalyzer.getCategory(conn.toLowerCase());
                if (cat == null) cat = "Conj-adverb";
                if (cat.startsWith("Coord")) {
                    testSets[0].add(instance);
                } else if (cat.startsWith("Sub")) {
                    testSets[1].add(instance);
                } else {
                    testSets[2].add(instance);
                }
            }
            
            for (int i = 0; i < 3; i++) {
                trainers[i] = new MyClassifierTrainer();
                trainers[i].train(trainSets[i]);                
            }
            //double localCount[] = evaluate(testSets);
            double localCount[] = interpolateEvaluate(testSets);
            double tp = localCount[0];
            double fp = localCount[1];
            double fn = localCount[2];
            double tn =  localCount[3];
            
            accuracy += (tp + tn)/(tp + tn + fp + fn);
            double lPrecision = tp/(tp + fp);
            double lRecall = tp/(tp + fn);
            precision += lPrecision;
            recall += lRecall;
            f1 += 2*lPrecision*lRecall/(lPrecision + lRecall);
        }
        /*for (int j = 0; j < testingInstanceLists.length; j++) {
            double localCount[] = new double[4];
            InstanceList.CrossValidationIterator cvIt = testingInstanceLists[j].crossValidationIterator(N);
            while (cvIt.hasNext()) {
                InstanceList[] nextSplit = cvIt.nextSplit();
                InstanceList trainingInstances = nextSplit[0];
                InstanceList testingInstances = nextSplit[1];
                Classifier classifier = trainers[j].train(trainingInstances, testingInstances);
                for (Instance instance : testingInstances) {
                    Classification classification = classifier.classify(instance);
                    PDTBConnectiveInstance pdtbInstance = (PDTBConnectiveInstance) instance;
                    boolean b = (Boolean) pdtbInstance.label;
                    if (classification.bestLabelIsCorrect()) {
                        if (b == true) {
                            localCount[0] += 1.0;   //TP
                        } else {
                            localCount[3] += 1.0;   //TN
                        }
                    } else {
                        if (b == true) {
                            localCount[2] += 1.0;   //FN
                        } else {
                            localCount[1] += 1.0;   //FP
                        }
                    }
                }
            }
            for (int i = 0; i < 4; i++) {
                localCount[i] /= N;
                count[i] += localCount[i];
            }
        }*/   
        return new double[]{accuracy/N, precision/N, recall/N, f1/N};
    }
    double[] interpolateEvaluate(InstanceList testingInstanceLists[]) throws IOException {
        int countTP = 0;
        int countFP = 0;
        int countFN = 0;
        int countTN = 0;
        //double lambdas[] = new double[]{0.11, 0.108, 0.08};
        double lambdas[] = new double[]{0.91, 0.10, 0.18};
        MaxEnt generalClassifier = (MaxEnt) generalTrainer.getClassifier();
//        Alphabet gAlphabet = generalClassifier.getLabelAlphabet();
//        int gTIndex = gAlphabet.lookupIndex("true", false);
//        int gFIndex = gAlphabet.lookupIndex("false", false);
        //gAlphabet.dump();
        //alphabet.dump();
        
        for (int i = 0; i < 3; i++) {
            MaxEnt classifier = (MaxEnt) trainers[i].getClassifier();
            Alphabet alphabet = classifier.getLabelAlphabet();
            int tIndex = alphabet.lookupIndex("true", false);
            int fIndex = alphabet.lookupIndex("false", false);
            //alphabet.dump();
            //if (1 < 2) continue;
            for (Instance instance : testingInstanceLists[i]) {
                PDTBConnectiveInstance pdtbInstance = (PDTBConnectiveInstance) instance;
                boolean b = (Boolean)pdtbInstance.label;
                double scores[] = new double[2];
                double generalScores[] = new double[2];
                double finalScores[] = new double[2];
                classifier.getClassificationScores(instance, scores);
                //generalClassifier.getClassificationScores(instance, generalScores);
                //System.out.println("true: " + scores[tIndex] + ", false: " + scores[fIndex]);
                //finalScores[gTIndex] = lambdas[i]*scores[tIndex] + (1.0 - lambdas[i])*generalScores[gTIndex];
                //finalScores[gFIndex] = lambdas[i]*scores[fIndex] + (1.0 - lambdas[i])*generalScores[gFIndex];
                //if (finalScores[gTIndex] >= finalScores[gFIndex]) { //predicted as true
                //TC
                if (scores[tIndex] >= scores[fIndex]) { //predicted as true
                //GC
                //if (generalScores[gTIndex] >= generalScores[gFIndex]) { //predicted as true
                    if (b == true) countTP++;
                    else {
                        logError(i, "FP", pdtbInstance);
                        countFP++;
                    }
                } else {
                    if (b == true) {
                        logError(i, "FN", pdtbInstance);
                        countFN++;
                    }
                    else countTN++;
                }
            }
        }
        double count[] = new double[]{countTP, countFP, countFN, countTN};
        return count;
    }
    void logError(int type, String error, PDTBConnectiveInstance instance) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("-----").append(error).append("----\n");
        Sentence s = instance.getSentence();
        sb.append(s.toString()).append("\n");
        sb.append("CONN: ").append(s.toString(instance.getS(), instance.getE())).append("\n");
        sb.append("Position: ").append(instance.getS()).append("\n\n");
        if (type == 0) {
            coordWriter.write(sb.toString());
        } else if (type == 1) {
            subordWriter.write(sb.toString());
        } else {
            adverbWriter.write(sb.toString());
        }
    }
    double[] evaluate(InstanceList testingInstanceLists[]) {
        int countTP = 0;
        int countFP = 0;
        int countFN = 0;
        int countTN = 0;
        for (int i = 0; i < 3; i++) {
            Classifier classifier = trainers[i].getClassifier();
            if (classifier == null) {
                throw new RuntimeException("Null classifier!");
            }
            for (Instance instance : testingInstanceLists[i]) {
                Classification classification = classifier.classify(instance);
                PDTBConnectiveInstance pdtbInstance = (PDTBConnectiveInstance) instance;
                boolean b = (Boolean)pdtbInstance.label;
                if (classification.bestLabelIsCorrect()) {
                    if (b == true) countTP++;
                    else countTN++;
                } else {                    
                    if (b == true) countFN++;
                    else countFP++;
                }
            }
        }
        /*double result[] = new double[4];
        result[0] = (countTP + countTN) / (1.0*(countFN + countFP + countTN + countTP)); //accuracy
        result[1] = countTP/(1.0*(countTP + countFP));  //precision
        result[2] = countTP/(1.0*(countTP + countFN));  //recall
        result[3] = 2.0*result[1]*result[2]/(result[1] + result[2]);    //F1*/
        
        double count[] = new double[]{countTP, countFP, countFN, countTN};
        return count;
    }
    
    public void save(String fileName) {
        //trainer.saveModel(fileName);
    }
    private Pipe defaultPipe() {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();        
        //pipes.add(new PDTBConnective2FeatureVector());
        pipes.add(new PDTBConnective2FeatureVector2());
        //pipes.add(new PrintFeatureVector());
        return new SerialPipes(pipes);
    }
    private void addInstancesThroughPipe(Sentence s, SimpleDepGraph depGraph, InstanceList instanceLists[]) {        
        ArrayList<String> words = s.getWords();
        for (int i = 0; i < words.size(); i++) {
            
            int j = -1;
            if (!s.get(i).getTag("CONN").matches("(B.*)|(DB.*)")) {
                for (int k = 0; k < 4 && ((i + k) < words.size()); k++) {
                    if (CONN_ANALYZER.isBaseConnective(s.toString(i, i + k).toLowerCase())) {
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
                InstanceList instanceList = null;
                String conn = s.toString(i, j);                
                String cat = CONN_ANALYZER.getCategory(conn.toLowerCase());
                if (cat == null) cat = "Conj-adverb";
                if (cat.startsWith("Coord")) instanceList = instanceLists[0];
                else if (cat.startsWith("Sub")) instanceList = instanceLists[1];
                else instanceList = instanceLists[2];
                
                if (s.get(i).getTag("CONN").startsWith("B")) {
                    instanceList.addThruPipe(new PDTBConnectiveInstance(s, i, j, true, depGraph));
                } else if (s.get(i).getTag("CONN").startsWith("DB")) {                 //if..then, either..or
                    instanceList.addThruPipe(new PDTBConnectiveInstance(s, i, j, true, depGraph));
                } else if (!s.get(i).getTag("CONN").startsWith("DI")) {                 //don't take then of if..then                           
                    instanceList.addThruPipe(new PDTBConnectiveInstance(s, i, j, false, depGraph));                    
                }
                i = j;
            }
        }
    }
    
    double[][] genNFoldEvaluate(InstanceList instanceList, int N) throws IOException {                
        double accuracy[] = new double[3];
        double precision[] = new double[3];
        double recall[] = new double[3];
        double f1[] = new double[3];
        
        InstanceList.CrossValidationIterator cvIt = instanceList.crossValidationIterator(N);
        ConnectiveAnalyzer connAnalyzer = new ConnectiveAnalyzer();
        while (cvIt.hasNext()) {            
            
            InstanceList[] nextSplit = cvIt.nextSplit();
            InstanceList trainingInstances = nextSplit[0];
            InstanceList testingInstances = nextSplit[1];            
            InstanceList testSets[] = new InstanceList[3];
            for (int i = 0; i < 3; i++) {                
                testSets[i] = new InstanceList(instanceList.getPipe());
            }            
                        
            generalTrainer = new MyClassifierTrainer();
            generalTrainer.train(trainingInstances);
            
            for (Instance instance : testingInstances) {
                PDTBConnectiveInstance pdtbInstance = (PDTBConnectiveInstance) instance;
                Sentence s = pdtbInstance.getSentence();
                String conn = s.toString(pdtbInstance.getS(), pdtbInstance.getE());
                String cat = connAnalyzer.getCategory(conn.toLowerCase());
                if (cat == null) cat = "Conj-adverb";
                if (cat.startsWith("Coord")) {
                    testSets[0].add(instance);
                } else if (cat.startsWith("Sub")) {
                    testSets[1].add(instance);
                } else {
                    testSets[2].add(instance);
                }
            }
            
            for (int i = 0; i < 3; i++) {
                double localCount[] = genEvaluate(testSets[i]);
                double tp = localCount[0];
                double fp = localCount[1];
                double fn = localCount[2];
                double tn = localCount[3];

                accuracy[i] += (tp + tn) / (tp + tn + fp + fn);
                double lPrecision = tp / (tp + fp);
                double lRecall = tp / (tp + fn);
                precision[i] += lPrecision;
                recall[i] += lRecall;
                f1[i] += 2 * lPrecision * lRecall / (lPrecision + lRecall);
            }                        
        }
        double result[][] = new double[3][4];
        for (int i = 0; i < 3; i++) {
            result[i][0] = accuracy[i]/N;
            result[i][1] = precision[i]/N;
            result[i][2] = recall[i]/N;
            result[i][3] = f1[i]/N;
        }
        return result;
    }

    double[] genEvaluate(InstanceList testingInstances) {
        int countTP = 0;
        int countFP = 0;
        int countFN = 0;
        int countTN = 0;
        
        Classifier classifier = generalTrainer.getClassifier();
        if (classifier == null) {
            throw new RuntimeException("Null classifier!");
        }
        for (Instance instance : testingInstances) {
            Classification classification = classifier.classify(instance);
            PDTBConnectiveInstance pdtbInstance = (PDTBConnectiveInstance) instance;
            boolean b = (Boolean) pdtbInstance.label;
            if (classification.bestLabelIsCorrect()) {
                if (b == true) {
                    countTP++;
                } else {
                    countTN++;
                }
            } else {
                if (b == true) {
                    countFN++;
                } else {
                    countFP++;
                }
            }
        }
                
        double count[] = new double[]{countTP, countFP, countFN, countTN};
        return count;
    }
    void showGenResult(double result[][]) {
        String info[] = new String[]{"Coordinate", "Subordinate", "Conj-Adverb"};
        for (int i = 0; i < 3; i++) {
            System.out.println("--------" + info[i] + "-----------");
            System.out.printf("Accuracy: %.4f\n", result[i][0]);
            System.out.printf("Precision: %.4f\n", result[i][1]);
            System.out.printf("Recall: %.4f\n", result[i][2]);
            System.out.printf("F1: %.4f\n", result[i][3]);
            System.out.println("");
        }
        
    }
    public static void main(String args[]) {
        
        try {
            PDTBConnectiveTrainer3TC trainer = new PDTBConnectiveTrainer3TC();
            //trainer.train("./resource/ml/data/pdtb/conn_id/explicit_relations_2_22", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_2_22", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_2_22", 
            //        "./resource/ml/data/pdtb/conn_id/explicit_relations_23_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_23_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_23_24");
            //trainer.train("./resource/ml/data/pdtb/conn_id/explicit_relations_2_22", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_2_22", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_2_22", 
            //        "./resource/ml/data/pdtb/conn_id/explicit_relations_0_1", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_0_1", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_0_1");
            //trainer.train("./resource/ml/data/pdtb/conn_id/explicit_relations_2_22", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_2_22", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_2_22");
            //trainer.train("./resource/ml/data/pdtb/conn_id/explicit_relations_2_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_2_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_2_24");
            //trainer.train("./resource/ml/data/pdtb/conn_id/explicit_relations_2_21", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_2_21", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_2_21", 
            //        "./resource/ml/data/pdtb/conn_id/explicit_relations_23", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_23", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_23");
            //trainer.train("./resource/ml/data/pdtb/conn_id/explicit_relations_2_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_2_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_2_24",
            //        "./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn","./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn_tree","./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn_dep");
            //trainer.train("./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn2.txt","./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn_tree","./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn_dep");
            //trainer.train("./resource/ml/data/pdtb/conn_id/explicit_relations_0_1", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_0_1", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_0_1");
            //trainer.train("./resource/ml/data/pdtb/conn_id/explicit_relations_2_21", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_2_21", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_2_21", 
            //        "./resource/ml/data/pdtb/conn_id/explicit_relations_23_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_23_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_23_24");            
            trainer.train("./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_2_22", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_tree_2_22", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_dep_2_22");
            trainer.closeFiles();
        } catch (IOException ex) {
            Logger.getLogger(DiscourseMarkerMETrainer.class.getName()).log(Level.SEVERE, null, ex);
        }        
        //trainer.save("./resource/ml/models/pdtb/pdtb_conn.model");
    }
}
